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

    abstract public double getEval(Game g);

    abstract public FastMove getMove(Game g, int color);
}
