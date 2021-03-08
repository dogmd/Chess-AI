import java.lang.reflect.Executable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ScottAgent extends Agent {
    long evalCount;
    int depth;
    Game copy;
    Move bestMove;
    double bestScore;
    int lastDepth;
    boolean searchCaptures;

    public ScottAgent(String name, Game game, int color) {
        super(name, game, color);
        evalCount = 0;
        try {
            String[] fields = name.split(",");
            this.depth = Integer.parseInt(fields[0]);
            this.searchCaptures = Boolean.parseBoolean(fields[1]);
        } catch (NumberFormatException e) {
            this.depth = 4;
            this.searchCaptures = true;
        }
    }

    public double getMoveScore(Game g, Move move) {
        double total = 0;
        boolean isEndgame = g.isEndgame();
        if (move.isCapture()) {
            total += 10 * (move.captured.getWeight(isEndgame) - move.actor.getWeight(isEndgame));
        }
        if (move.type == Move.PROMOTION) {
            total += 2 * Piece.getWeight(move.promoteTo);
        }
        for (Piece p : g.board.pieces.get(PieceType.PAWN).get(Piece.getOpposite(move.actor.color))) {
            if (p.threatening.contains(move.end)) {
                total -= move.actor.getWeight(isEndgame);
            }
        }
        return total;
    }

    public double kingTrapWeight(Game g) {
        double total = 0;
        int endgameWeight = 16;
        for (List<List<Piece>> allPieces : g.board.pieces.values()) {
            for (Piece p : allPieces.get(Piece.getOpposite(color))) {
                endgameWeight--;
            }
        }
        endgameWeight /= 2;

        Piece p = g.board.pieces.get(PieceType.KING).get(Piece.getOpposite(color)).get(0);
        int enemyCol = p.square.col;
        int enemyRow = p.square.row;
        int xDiff = Math.max(3 - enemyCol, enemyCol - 4);
        int yDiff = Math.max(3 - enemyRow, enemyRow - 4);
        total += xDiff + yDiff;

        p = g.board.pieces.get(PieceType.KING).get(color).get(0);
        int distX = Math.abs(p.square.col - enemyCol);
        int distY = Math.abs(p.square.row - enemyRow);
        total += 14 - (distX + distY);
        return total * 10 * endgameWeight;
    }

    public double getScore(Game g) {
        double total = 0;
        total += g.getMaterialScore() * (g.getActiveColor() == Piece.WHITE ? 1 : -1);
        total += g.getMobilityDiff() * (g.getActiveColor() == color ? 1 : -1);
        if (g.isEndgame()) {
            total += kingTrapWeight(g);
        }
        return total;
    }

    public double searchCaptures(double alpha, double beta) {
        double score = getScore(copy);
        if (score >= beta) {
            return beta;
        }
        alpha = Math.max(alpha, score);

        ArrayList<Move> moves = new ArrayList<>(copy.board.moves);
        for (int i = moves.size() - 1; i >= 0; i--) {
            Move move = moves.get(i);
            if (move.isCapture()) {
                move.score = getMoveScore(copy, move);
            } else {
                moves.remove(i);
            }
        }
        Collections.sort(moves);

        for (Move move : moves) {
            copy.makeMove(move);
            score = -searchCaptures(-beta, -alpha);
            copy.unmakeMove(move);

            if (score >= beta) {
                return beta;
            }
            alpha = Math.max(alpha, score);
        }

        return alpha;
    }

    public double search(int depth, double alpha, double beta) {
        if (depth == 0) {
            if (searchCaptures) {
                return searchCaptures(alpha, beta);
            } else {
                return getScore(copy);
            }
        }

        ArrayList<Move> moves = new ArrayList<>(copy.board.moves);
        if (moves.size() == 0) {
            if (copy.board.isChecked()) {
                return -999999 + copy.fullMoves;
            }
            return 0;
        }

        for (Move move : moves) {
            move.score = getMoveScore(copy, move);
        }
        Collections.sort(moves);

        for (Move move : moves) {
            copy.makeMove(move);
            double score = -search(depth - 1, -beta, -alpha);
            copy.unmakeMove(move);
            if ((score > bestScore || bestMove == null) && depth == this.depth) {
                bestScore = score;
                bestMove = move;
            }
            if (score >= beta) {
                return beta;
            }
            alpha = Math.max(alpha, score);
        }

        return alpha;
    }

    public Move getMove(Game game, int color) {
        this.copy = new Game(game);
        this.game = game;
        this.bestScore = -999999;
        this.bestMove = null;
        this.lastDepth = this.depth;
        copy.board.updateInfo();
        long start = System.currentTimeMillis();
        search(depth, -999999, 999999);
        System.out.println(System.currentTimeMillis() - start + "ms");
        return game.board.translateMove(bestMove);
    }
}
