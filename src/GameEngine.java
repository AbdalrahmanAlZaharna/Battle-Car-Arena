import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class GameEngine implements PacketListener {
    private final CommandDispatcher tx;
    private final AtomicInteger hp1 = new AtomicInteger(100);
    private final AtomicInteger hp2 = new AtomicInteger(100);

    private volatile boolean light1=false, light2=false; // both bright to start
    private volatile boolean running=false;

    private static final long VERIFY_MS   = 2000; // must hold IR true this long
    private static final long COOLDOWN_MS = 800;  // min gap between deductions

    private long irStart1 = -1, irStart2 = -1; // when IR first true
    private boolean armed1 = true, armed2 = true; // require IR false to re-arm
    private long lastHit1 = 0, lastHit2 = 0;

    private long startMs=0;
    private final long matchLenMs = 60000;

    private final CopyOnWriteArrayList<GameStateListener> ls = new CopyOnWriteArrayList<>();

    public GameEngine(CommandDispatcher tx){ this.tx = tx; }

    public void addListener(GameStateListener l){ ls.add(l); }
    public void removeListener(GameStateListener l){ ls.remove(l); }

    @Override
    public void onPacket(Packet p){
        if (p.team()==1) light1 = p.light();
        if (p.team()==2) light2 = p.light();

        if (!running && light1 && light2) startGame();
        if (!running) return;

        boolean ir = p.ir();
        long now = System.currentTimeMillis();

        if (p.team()==1) {
            if (ir) {
                if (armed1 && irStart1 < 0) irStart1 = now;
                if (armed1 && irStart1 >= 0 && now - irStart1 >= VERIFY_MS) {
                    if (now - lastHit1 >= COOLDOWN_MS) {
                        deductHp(1);
                        lastHit1 = now;
                        armed1 = false;
                    }
                }
            } else {
                irStart1 = -1;
                armed1 = true;
            }
        } else if (p.team()==2) {
            if (ir) {
                if (armed2 && irStart2 < 0) irStart2 = now;
                if (armed2 && irStart2 >= 0 && now - irStart2 >= VERIFY_MS) {
                    if (now - lastHit2 >= COOLDOWN_MS) {
                        deductHp(2);
                        lastHit2 = now;
                        armed2 = false;
                    }
                }
            } else {
                irStart2 = -1;
                armed2 = true;
            }
        }
    }

    public void runLoop() {
        try {
            while (true) {
                Thread.sleep(500);
                // no time-based game over
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }


    private void startGame(){
        startMs = System.currentTimeMillis();  // set timestamp first
        running = true;                        // then declare running

        hp1.set(100); hp2.set(100);
        notifyHp();
        notifyState("Match started");
        tx.send(Commands.beep(Commands.TEAM_ALL, 3));
        colors();
        tx.send(Commands.fireMode(Commands.TEAM_ALL, 2));

        // reset IR verification state
        irStart1 = irStart2 = -1;
        armed1 = armed2 = true;
        lastHit1 = lastHit2 = 0;
    }

    private void deductHp(int team){
        AtomicInteger hp = (team==1)? hp1 : hp2;
        hp.set(Math.max(0, hp.get()-10));
        tx.send(Commands.beep(team, 1));
        colors();
        notifyHp();
        if(hp.get()==0) gameOver();
    }

    private void gameOver(){
        if(!running) return;
        running = false;
        tx.send(Commands.fireMode(Commands.TEAM_ALL, 0));	
        if(hp1.get()==0) tx.send(Commands.beep(1,2));
        if(hp2.get()==0) tx.send(Commands.beep(2,2));
        colors();
        notifyState("Game over. HP1="+hp1.get()+" HP2="+hp2.get());
    }

    private void colors(){
        tx.send(Commands.setRgb(1, code(hp1.get())));
        tx.send(Commands.setRgb(2, code(hp2.get())));
    }
    private int code(int hp){
        if(hp<=0) return 3;   // red
        if(hp>60) return 1;   // green
        return 2;             // yellow
    }

    private void notifyHp(){
        for(var l: ls){ l.onHpUpdate(1, hp1.get()); l.onHpUpdate(2, hp2.get()); }
    }
    private void notifyState(String s){
        for(var l: ls){ l.onState(s); }
    }
}
