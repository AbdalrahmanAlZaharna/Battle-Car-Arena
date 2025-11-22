public interface GameStateListener {
    void onHpUpdate(int team, int hp);
    void onState(String text);
}
