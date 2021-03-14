import java.util.ArrayList;
import java.util.Collections;
import java.util.Stack;

public class ScottAgent extends Agent {
    long evalCount;
    int depth;
    Game copy;
    Move bestMove;
    double bestScore;
    int lastDepth;
    boolean searchCaptures;
    MovePath bestPath;
    MovePath root;

    public ScottAgent(String name, Game game, int color) {
        super(name, game, color);
        evalCount = 0;
        try {
            String[] fields = name.split(",");
            this.depth = Integer.parseInt(fields[0]);
            this.searchCaptures = Boolean.parseBoolean(fields[1]);
        } catch (Exception e) {
            this.depth = 3;
            this.searchCaptures = true;
        }
    }

    public double getMoveScore(Game g, Move move) {
        double total = 0;
        boolean isEndgame = g.isEndgame();
        int startRow = move.start / 8;
        int startCol = move.start - startRow * 8;
        int endRow = move.end / 8;
        int endCol = move.end - endRow * 8;
        if (move.isCapture()) {
            // not going to bother account for en passant here
            int actorWeight = Piece.getWeight(move.actor, startRow, startCol, isEndgame);
            int capturedWeight = Piece.getWeight(move.captured, endRow, endCol, isEndgame);
            total += 10 * capturedWeight - actorWeight;
        }
        if (move.type == Move.PROMOTION) {
            total += 2 * Piece.getWeight(move.promoteTo, endRow, endCol, isEndgame);
        }
        if (g.board.pawnThreats[move.end]) {
            int endActorWeight = 350;//Piece.getWeight(move.actor, endRow, endCol, isEndgame);
            total -= endActorWeight;
        }
        return total;
    }

    public double kingTrapWeight(Game g) {
        double total = 0;
        int endgameWeight = 12 - g.board.pieceCounts[0] - g.board.pieceCounts[1];
        if (endgameWeight > 0) {
            int otherKingInd = g.board.getKingInd(Piece.getOpposite(g.board.activeColor));
            int kingInd = g.board.kingInd;
            total += PrecomputedMoveData.distToCenter[otherKingInd];

            total += 14 - PrecomputedMoveData.distBetween[kingInd][otherKingInd];
            return total * 10 * endgameWeight;
        } else {
            return 0;
        }
    }

    public double getScore(Game g) {
        double total = 0;
        evalCount++;
        total += g.getMaterialScore() * (g.getActiveColor() == Piece.WHITE ? 1 : -1);
        if (g.isEndgame()) {
            total += kingTrapWeight(g);
        }

        return total;
    }

    public void processCaptures(ArrayList<Move> moves) {
        for (int i = moves.size() - 1; i >= 0; i--) {
            // TODO: change move generation to avoid this extra work
            Move move = moves.get(i);
            if (move.isCapture()) {
                move.score = getMoveScore(copy, move);
            } else {
                moves.remove(i);
            }
        }
        Collections.sort(moves);
    }

    public double searchCaptures(double alpha, double beta, MovePath path) {
        double score = getScore(copy);
        if (score >= beta) {
            return beta;
        }
        alpha = Math.max(alpha, score);

        ArrayList<Move> moves = new ArrayList<>(copy.board.moves);
        processCaptures(moves);

        for (Move move : moves) {
            copy.makeMove(move);
            path.next = new MovePath(move);
            path.zobristKey = copy.board.zobristKey;
            score = -searchCaptures(-beta, -alpha, path.next);
            copy.unmakeMove(move);

            if (score >= beta) {
                return beta;
            }
            alpha = Math.max(alpha, score);
        }

        return alpha;
    }

    public double search(int depth, double alpha, double beta, MovePath path) {
        ArrayList<Move> moves = new ArrayList<>(copy.board.moves);
        if (moves.size() == 0) {
            if (copy.board.isChecked()) {
                return -999999 + copy.fullMoves;
            }
            return 0;
        } else if (depth != this.depth && game.hasBeenSeen(copy.board.zobristKey)) {
            return 0;
        }
        if (depth == 0) {
            if (searchCaptures) {
                return searchCaptures(alpha, beta, path);
            } else {
                return getScore(copy);
            }
        }

        for (Move move : moves) {
            move.score = getMoveScore(copy, move);
        }
        Collections.sort(moves);

        for (Move move : moves) {
            copy.makeMove(move);
            path.next = new MovePath(move);
            path.zobristKey = copy.board.zobristKey;
            double score = -search(depth - 1, -beta, -alpha, path.next);
            copy.unmakeMove(move);
            if ((score > bestScore || bestMove == null) && depth == this.depth) {
                bestScore = score;
                bestMove = move;
                bestPath = path.next;
            }
            if (score > beta) {
                return beta;
            }
            alpha = Math.max(alpha, score);
        }

        return alpha;
    }

    public double getEval(Game game) {
        this.copy = new Game(game);
        this.game = game;
        this.bestScore = -999999;
        this.bestMove = null;
        this.lastDepth = this.depth;
        search(depth, -999999, 999999, new MovePath());
        return bestScore;
    }

    public Move getMove(Game game, int color) {
        this.evalCount = 0;
        this.copy = new Game(game);
        this.game = game;
        this.bestScore = -999999;
        this.bestMove = null;
        this.lastDepth = this.depth;
        long start = System.currentTimeMillis();
        root = new MovePath();
        root.zobristKey = copy.board.zobristKey;
        search(depth, -999999, 999999, root);
        if (!name.equals("evaluator")) {
            System.out.println(System.currentTimeMillis() - start + "ms\n" + bestPath + " " + bestScore);
        }
        return bestMove;
    }

    private class MovePath {
        MovePath next;
        Move move;
        long zobristKey;

        public MovePath(){}

        public MovePath(Move move) {
            this.move = move;
        }

        public String toString() {
            if (next != null && move != null) {
                return move.toString() + ", " + next.toString();
            } else if (next == null && move != null) {
                return move.toString();
            }
            return "";
        }
    }
}
