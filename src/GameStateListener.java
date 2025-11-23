/**
 * This is an interface for the screen.
 * It tells the Game Engine what functions the screen has.
 * The Game Engine calls these functions to update what you see.
 */
public interface GameStateListener {
    // Called when a team's health changes
    void onHpUpdate(int team, int hp);
    
    // Called when the status text changes (like "Game Over")
    void onState(String text);
}