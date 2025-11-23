import javax.swing.*;
import java.awt.*;

/**
 * The Window (UI) for the game.
 * It shows health bars and status messages.
 */
public class ScoreboardUI extends JFrame implements GameStateListener {
    private final JProgressBar a = new JProgressBar(0,100);
    private final JProgressBar b = new JProgressBar(0,100);
    private final JLabel msg = new JLabel("Waiting for start");

    /**
     * Constructor: Sets up the window size, title, and layout.
     */
    public ScoreboardUI(){
        super("Battle Car Scoreboard");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(420, 200);
        setLocationRelativeTo(null);

        a.setValue(100); a.setStringPainted(true);
        b.setValue(100); b.setStringPainted(true);

        JPanel p = new JPanel(new GridLayout(3,1,8,8));
        p.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        p.add(row("Team 1", a));
        p.add(row("Team 2", b));
        p.add(msg);
        setContentPane(p);
    }

    /**
     * Helper method to create a row with a label and a progress bar.
     */
    private JPanel row(String name, JProgressBar bar){
        JPanel r = new JPanel(new BorderLayout(8,8));
        r.add(new JLabel(name), BorderLayout.WEST);
        r.add(bar, BorderLayout.CENTER);
        return r;
    }

    /**
     * Updates the health bars on the screen.
     * Called by the Game Engine when a car gets hit.
     */
    @Override
    public void onHpUpdate(int team, int hp) {
        // Ensure UI updates happen on the correct thread (Swing Event Thread)
        SwingUtilities.invokeLater(() -> {
            if(team==1) a.setValue(hp);
            if(team==2) b.setValue(hp);
        });
    }

    /**
     * Updates the text message at the bottom of the window.
     * Used for messages like "Game Over" or "Match Started".
     */
    @Override
    public void onState(String text) {
        SwingUtilities.invokeLater(() -> msg.setText(text));
    }
}