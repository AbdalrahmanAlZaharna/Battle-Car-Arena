import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class GameEngine implements PacketListener {
    private final CommandDispatcher tx;
    private final AtomicInteger hp1 = new AtomicInteger(100);
    private final AtomicInteger hp2 = new AtomicInteger(100);

    // --- SENSOR STATES ---
    private volatile boolean light1 = false, light2 = false;

    // --- GAME STATES ---
    private volatile boolean running = false;
    private boolean seenBothDark = false;   
    private long gameOverTime = 0;          

    // --- SETTINGS ---
    private long lastHit1 = 0;
    private long lastHit2 = 0;
    private static final int DAMAGE_PER_HIT = 10;      
    private static final long INVINCIBILITY_MS = 1000;  

    private final CopyOnWriteArrayList<GameStateListener> ls = new CopyOnWriteArrayList<>();

    public GameEngine(CommandDispatcher tx) {
        this.tx = tx;
    }

    public void addListener(GameStateListener l) { ls.add(l); }
    public void removeListener(GameStateListener l) { ls.remove(l); }

    @Override
    public synchronized void onPacket(Packet p) {
        long now = System.currentTimeMillis();

        if (p.team() == 1) light1 = p.light();
        else if (p.team() == 2) light2 = p.light();

        if (!running) {
            if (now - gameOverTime < 5000) return; 

            // Arming Logic
            if (!light1 && !light2) {
                if (!seenBothDark) {
                    seenBothDark = true;
                    broadcast("ARMED! Uncover to start.");
                }
            }

            // Start Logic
            if (seenBothDark && light1 && light2) {
                startGame();
                seenBothDark = false;
            } 
            else if (!seenBothDark) {
                broadcast("Cover sensors to Arm...");
            }
            
            if (seenBothDark && (!light1 || !light2)) {
                broadcast("ARMED! Waiting for Flash...");
            }

            if (!running) return;
        }

        // Hit Logic
        if (p.ir()) {
            if (p.team() == 1) {
                if (now - lastHit1 > INVINCIBILITY_MS) {
                    deductHp(1);
                    lastHit1 = now;
                }
            } else if (p.team() == 2) {
                if (now - lastHit2 > INVINCIBILITY_MS) {
                    deductHp(2);
                    lastHit2 = now;
                }
            }
        }
    }

    // --- TRAFFIC CONTROL RESET ---
    // Sends ONE clear command and waits. No spamming.
    private void safeReset() {
        try {
            // 1. Send OFF to ALL
            tx.send(Commands.setRgb(Commands.TEAM_ALL, 0));
            // 2. CRITICAL WAIT: Give XBee 300ms to deliver it before doing anything else
            Thread.sleep(300); 
        } catch (Exception e) {}
    }

    // --- STABILIZED START SEQUENCE ---
    private void startGame() {
        running = true;
        
        hp1.set(100);
        hp2.set(100);
        notifyHp();

        new Thread(() -> {
            try {
                // A. PRE-GAME CLEANUP
                safeReset(); 

                // B. ANIMATION: RED
                tx.send(Commands.setRgb(Commands.TEAM_ALL, 3));
                Thread.sleep(600); // Slow and steady
                
                // C. ANIMATION: GREEN
                tx.send(Commands.setRgb(Commands.TEAM_ALL, 1));
                Thread.sleep(600); 
                
                // D. ANIMATION: OFF (Flash)
                safeReset(); 
                
                // E. GAME START
                // Send colors individually to ensure both get it
                tx.send(Commands.setRgb(1, 1)); // Team 1 Green
                Thread.sleep(50); // Tiny gap
                tx.send(Commands.setRgb(2, 1)); // Team 2 Green
                
                // Enable Sound/Fire
                tx.send(Commands.beep(Commands.TEAM_ALL, 3));
                tx.send(Commands.fireMode(Commands.TEAM_ALL, 2));
                
                broadcast("GO! Match Started!");
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    private void deductHp(int team) {
        AtomicInteger hp = (team == 1) ? hp1 : hp2;
        int current = hp.get();
        
        if (current > 0) {
            hp.set(Math.max(0, current - DAMAGE_PER_HIT));
            tx.send(Commands.beep(team, 1));
            colors();
            notifyHp();
            broadcast("Hit on Team " + team + "!");
            if (hp.get() == 0) gameOver();
        }
    }

    private void gameOver() {
        if (!running) return;
        running = false;
        gameOverTime = System.currentTimeMillis();

        // Disable Guns
        tx.send(Commands.fireMode(Commands.TEAM_ALL, 0));
        
        // Play Sounds
        if (hp1.get() == 0) tx.send(Commands.beep(1, 2));
        if (hp2.get() == 0) tx.send(Commands.beep(2, 2));
        
        colors();
        broadcast("GAME OVER! Winner: " + (hp1.get() > 0 ? "Team 1" : "Team 2"));
        seenBothDark = false;
    }

    private void colors() {
        tx.send(Commands.setRgb(1, code(hp1.get())));
        tx.send(Commands.setRgb(2, code(hp2.get())));
    }

    private int code(int hp) {
        if (hp <= 0) return 3;   // red
        if (hp > 50) return 1;   // green
        return 2;                // yellow/blue
    }

    private void broadcast(String msg) {
        String t1Status = light1 ? "Dark" : "Bright";
        String t2Status = light2 ? "Dark" : "Bright";

        String status = "<html><div style='text-align:center;'>" 
                        + msg 
                        + "<br/>"
                        + "<span style='font-size:10px; color:blue;'>" 
                        + "[T1: " + t1Status + " | T2: " + t2Status + "]"
                        + "</span></div></html>";
        
        for (var l : ls) l.onState(status);
    }

    private void notifyHp() {
        for (var l : ls) {
            l.onHpUpdate(1, hp1.get());
            l.onHpUpdate(2, hp2.get());
        }
    }

    public void runLoop() {
        try {
            while (true) { Thread.sleep(500); }
        } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}