public abstract class Agent {
    String settings;
    Game game;
    int color;

    public Agent(String settings, Game game, int color) {
        this.settings = settings;
        this.game = game;
        this.color = color;
    }

    abstract public double getScore(Game g);

    abstract public Move getMove(Game g, int color);
}
