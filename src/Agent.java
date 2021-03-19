public abstract class Agent {
    String name;
    Game game;
    int color;

    public Agent(String name, Game game, int color) {
        this.name = name;
        this.game = game;
        this.color = color;
    }

    abstract public double getScore(Game g);

    abstract public Move getMove(Game g, int color);
}
