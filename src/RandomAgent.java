import java.util.ArrayList;
import java.util.Random;

public class RandomAgent extends Agent {
    Random rand;
    public RandomAgent(String name, Game game, int color) {
        super(name, game, color);
        rand = new Random();
    }

    @Override
    public double getScore(Game g) {
        return 0;
    }

    public Move getMove(Game game, int color) {
        ArrayList<Move> moves = game.getMoves();
        if (moves.size() > 0) {
            return moves.get(rand.nextInt(moves.size()));
        }
        return null;
    }
}
