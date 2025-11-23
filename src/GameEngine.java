import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class controls the rules of the game.
 * It tracks health, handles the start sequence, and processes hits.
 */
public class GameEngine implements PacketListener {
    // The tool we use to send messages to the cars
    private final CommandDispatcher tx;
    
    // Health for Team 1 and Team 2.
    // We use "AtomicInteger" so we can change the numbers safely from different threads.
    private final AtomicInteger hp1 = new AtomicInteger(100);
    private final AtomicInteger hp2 = new AtomicInteger(100);

    // --- SENSOR STATES ---
    // Stores if the sensors currently see light or are covered
    private volatile boolean light1 = false, light2 = false;

    // --- GAME STATES ---
    // Is the game currently active?
    private volatile boolean running = false;
    // Did we see the "Cover" step of the start sequence?
    private boolean seenBothDark = false;   
    // A timer to stop the game from restarting too quickly
    private long gameOverTime = 0;          

    // --- SETTINGS ---
    // Timers to track the last time a car was hit (for invincibility)
    private long lastHit1 = 0;
    private long lastHit2 = 0;
    // How much damage one shot does
    private static final int DAMAGE_PER_HIT = 10;      
    // How long a car stays invincible after getting hit (1 second)
    private static final long INVINCIBILITY_MS = 1000;  

    // A list of screens (like the Scoreboard) that want to know about game updates
    private final CopyOnWriteArrayList<GameStateListener> ls = new CopyOnWriteArrayList<>();

    // Constructor: connects the engine to the message sender
    public GameEngine(CommandDispatcher tx) {
        this.tx = tx;
    }

    // Add a screen to the list
    public void addListener(GameStateListener l) { ls.add(l); }
    // Remove a screen from the list
    public void removeListener(GameStateListener l) { ls.remove(l); }

    /**
     * This function runs every time we receive a message (packet) from a car.
     */
    @Override
    public synchronized void onPacket(Packet p) {
        long now = System.currentTimeMillis();

        // 1. Update our records of the light sensors
        if (p.team() == 1) light1 = p.light();
        else if (p.team() == 2) light2 = p.light();

        // 2. LOGIC FOR STARTING THE GAME (If game is NOT running)
        if (!running) {
            // If the last game ended less than 5 seconds ago, do nothing
            if (now - gameOverTime < 5000) return; 

            // Step A: Arming Logic
            // Both sensors must be covered (Dark)
            if (!light1 && !light2) {
                if (!seenBothDark) {
                    seenBothDark = true;
                    broadcast("ARMED! Uncover to start.");
                }
            }

            // Step B: Start Logic
            // Both sensors must become Uncovered (Bright) AFTER being Dark
            if (seenBothDark && light1 && light2) {
                startGame();
                seenBothDark = false;
            } 
            else if (!seenBothDark) {
                broadcast("Cover sensors to Arm...");
            }
            
            // If one is covered and one is not, tell them to wait
            if (seenBothDark && (!light1 || !light2)) {
                broadcast("ARMED! Waiting for Flash...");
            }

            // If the game did not start yet, stop here
            if (!running) return;
        }

        // 3. LOGIC FOR HITS (If game IS running)
        if (p.ir()) {
            if (p.team() == 1) {
                // Check if Team 1 is still invincible from the last hit
                if (now - lastHit1 > INVINCIBILITY_MS) {
                    deductHp(1); // Reduce health
                    lastHit1 = now; // Reset timer
                }
            } else if (p.team() == 2) {
                // Check if Team 2 is still invincible
                if (now - lastHit2 > INVINCIBILITY_MS) {
                    deductHp(2);
                    lastHit2 = now;
                }
            }
        }
    }

    // --- TRAFFIC CONTROL RESET ---
    // Turns off all lights with a delay to ensure the message gets through
    private void safeReset() {
        try {
            // 1. Send OFF command to everyone
            tx.send(Commands.setRgb(Commands.TEAM_ALL, 0));
            // 2. Wait 0.3 seconds to let the radio finish sending
            Thread.sleep(300); 
        } catch (Exception e) {}
    }

    // --- START SEQUENCE ---
    // This plays the light animation and then starts the game
    private void startGame() {
        running = true;
        
        // Reset health back to 100
        hp1.set(100);
        hp2.set(100);
        notifyHp();

        // We run this in a background thread so the main program doesn't freeze
        new Thread(() -> {
            try {
                // A. Turn everything off first
                safeReset(); 

                // B. Turn everyone RED
                tx.send(Commands.setRgb(Commands.TEAM_ALL, 3));
                Thread.sleep(600); // Wait 0.6 seconds
                
                // C. Turn everyone GREEN
                tx.send(Commands.setRgb(Commands.TEAM_ALL, 1));
                Thread.sleep(600); 
                
                // D. Flash OFF
                safeReset(); 
                
                // E. START THE GAME
                // Turn lights to Green (Health 100) for each team specifically
                tx.send(Commands.setRgb(1, 1)); // Team 1 Green
                Thread.sleep(50); // Small wait to prevent traffic jam
                tx.send(Commands.setRgb(2, 1)); // Team 2 Green
                
                // Turn on the Sound and Guns
                tx.send(Commands.beep(Commands.TEAM_ALL, 3));
                tx.send(Commands.fireMode(Commands.TEAM_ALL, 2));
                
                broadcast("GO! Match Started!");
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    // Reduces health and checks if the game is over
    private void deductHp(int team) {
        AtomicInteger hp = (team == 1) ? hp1 : hp2;
        int current = hp.get();
        
        if (current > 0) {
            // Subtract damage, but do not go below 0
            hp.set(Math.max(0, current - DAMAGE_PER_HIT));
            
            // Play hit sound and update lights
            tx.send(Commands.beep(team, 1));
            colors();
            notifyHp();
            broadcast("Hit on Team " + team + "!");
            
            // If health is 0, end the game
            if (hp.get() == 0) gameOver();
        }
    }

    // Ends the game session
    private void gameOver() {
        if (!running) return;
        running = false;
        gameOverTime = System.currentTimeMillis(); // Start cooldown timer

        // Disable Guns for safety
        tx.send(Commands.fireMode(Commands.TEAM_ALL, 0));
        
        // Play "Die" sound for the loser
        if (hp1.get() == 0) tx.send(Commands.beep(1, 2));
        if (hp2.get() == 0) tx.send(Commands.beep(2, 2));
        
        colors(); // Update lights to show who lost
        broadcast("GAME OVER! Winner: " + (hp1.get() > 0 ? "Team 1" : "Team 2"));
        seenBothDark = false; // Reset the start logic
    }

    // Updates the LED colors based on current health
    private void colors() {
        tx.send(Commands.setRgb(1, code(hp1.get())));
        tx.send(Commands.setRgb(2, code(hp2.get())));
    }

    // Converts health number into a color code
    private int code(int hp) {
        if (hp <= 0) return 3;   // Red (Dead)
        if (hp > 50) return 1;   // Green (Healthy)
        return 2;                // Yellow/Blue (Hurt)
    }

    // Sends a text message to the Scoreboard Window
    private void broadcast(String msg) {
        String t1Status = light1 ? "Dark" : "Bright";
        String t2Status = light2 ? "Dark" : "Bright";

        // We use HTML to format the text nicely on two lines
        String status = "<html><div style='text-align:center;'>" 
                        + msg 
                        + "<br/>"
                        + "<span style='font-size:10px; color:blue;'>" 
                        + "[T1: " + t1Status + " | T2: " + t2Status + "]"
                        + "</span></div></html>";
        
        for (var l : ls) l.onState(status);
    }

    // Tells the Scoreboard to update the progress bars
    private void notifyHp() {
        for (var l : ls) {
            l.onHpUpdate(1, hp1.get());
            l.onHpUpdate(2, hp2.get());
        }
    }

    // Keeps the engine alive in the background
    public void runLoop() {
        try {
            while (true) { Thread.sleep(500); }
        } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}