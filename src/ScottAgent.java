import java.util.ArrayList;

public class ScottAgent extends Agent {
    private double mobilityWeight = 0.5;
    Move bestMove;
    long evalCount = 0;

    public ScottAgent(String name, Game game, int color) {
        super(name, game, color);
    }

    public double getScore(Game g) {
        return (100 * g.getMaterialScore() + g.getMobilityDiff()) * (g.getActiveColor() == Piece.WHITE ? 1 : -1);
    }

    public double search(Game g, int depth, double alpha, double beta, boolean maximizingPlayer) {
        if (depth == 0) {
            return getScore(g);
        } else {
            ArrayList<Move> moves = g.board.moves;
            if (moves.size() == 0) {
                if (g.board.isChecked(g.getActiveColor())) {
                    return -999999;
                }
                return 0;
            }

            if (maximizingPlayer) {
                double score = -999999;
                for (Move move : moves) {
                    g.makeMove(move);
                    score = Math.max(score, search(g, depth - 1, alpha, beta, false));
                    g.unmakeMove(move);
                    alpha = Math.max(alpha, score);
                    if (alpha >= beta) {
                        break;
                    }
                }
                return score;
            } else {
                double score = 999999;
                for (Move move : moves) {
                    g.makeMove(move);
                    score = -search(g, depth - 1, alpha, beta, true);
                    g.unmakeMove(move);
                    beta = Math.min(beta, score);
                    if (beta < alpha) {
                        break;
                    }
                }
                return score;
            }
        }
    }

    @Override
    public Move getMove(Game game, int color) {
        evalCount = 0;
        this.game = game;
        Game copy = new Game(game);
        copy.board.updateInfo();
        search(copy, 3, -999999, 999999, true);
        return game.board.translateMove(bestMove);
    }
}
