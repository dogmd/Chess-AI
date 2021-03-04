import java.util.ArrayList;
import java.util.List;

public class ScottAgent extends Agent {
    long evalCount;
    int depth;

    public ScottAgent(String name, Game game, int color) {
        super(name, game, color);
        evalCount = 0;
        try {
            this.depth = Integer.parseInt(name);
        } catch (NumberFormatException e) {
            this.depth = 4;
        }
    }

//    public double getMoveScore() {
//
//    }

    public double kingTrapWeight(Game g) {
        double total = 0;
        int endgameWeight = 16;
        for (List<List<Piece>> allPieces : g.board.pieces.values()) {
            for (Piece p : allPieces.get(Piece.getOpposite(color))) {
                endgameWeight--;
            }
        }

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
        total += (100 * g.getMaterialScore() + g.getMobilityDiff()) * (color == Piece.WHITE ? 1 : -1);
        if (g.board.isChecked(g.getActiveColor())) {
            total += 200 * (g.getActiveColor() == color ? -1 : 1);
        }
        total += kingTrapWeight(g);
        return total;
    }

    public MovePath search(Game g, int depth, double alpha, double beta, MovePath path) {
        ArrayList<Move> moves = new ArrayList<>(g.board.moves);
        if (moves.size() == 0) {
            if (g.board.isChecked(g.getActiveColor())) {
                path.score = (g.getActiveColor() == color ? -1 : 1) * 9999999;
                return path;
            }
            path.score = 0;
            return path;
        }
        if (depth == 0) {
            path.score = getScore(g);
            return path;
        } else {
            boolean maximizingPlayer = g.getActiveColor() == color;
            if (maximizingPlayer) {
                MovePath bestPath = new MovePath(null, null, -999999);
                for (Move move : moves) {
                    evalCount++;
                    g.makeMove(move);
                    MovePath next = new MovePath(path, move, 0);
                    MovePath endPath = search(g, depth - 1, alpha, beta, next);
                    if (bestPath.move == null || endPath.score >= bestPath.score) {
                        if (endPath.score != bestPath.score || endPath.getLength() < bestPath.getLength()) {
                            bestPath = endPath;
                        }
                    }
                    alpha = Math.max(alpha, bestPath.score);
                    g.unmakeMove(move);
                    if (alpha > beta) {
                        break;
                    }
                }
                return bestPath;
            } else {
                MovePath worstPath = new MovePath(null, null, 999999);
                for (Move move : moves) {
                    evalCount++;
                    g.makeMove(move);
                    MovePath next = new MovePath(path, move, 0);
                    MovePath endPath = search(g, depth - 1, alpha, beta, next);
                    if (worstPath.move == null || endPath.score <= worstPath.score) {
                        if (endPath.score != worstPath.score || endPath.getLength() < worstPath.getLength()) {
                            worstPath = endPath;
                        }
                    }
                    beta = Math.min(beta, worstPath.score);
                    g.unmakeMove(move);
                    if (beta < alpha) {
                        break;
                    }
                }
                return worstPath;
            }
        }
    }

    @Override
    public Move getMove(Game game, int color) {
        evalCount = 0;
        this.game = game;
        Game copy = new Game(game);
        copy.board.updateInfo();
        long time = System.currentTimeMillis();
        MovePath bestPath = search(copy, depth, -9999999, 9999999, new MovePath(null, null, getScore(copy)));
        time = System.currentTimeMillis() - time;
        MovePath[] moves = new MovePath[depth];
        System.out.printf("%nBest path found after evaluating %d moves in %dms: %n", evalCount, time);
        int i = 0;
        while (bestPath.parent != null && bestPath.parent.parent != null) {
            moves[i] = bestPath;
            i++;
            bestPath = bestPath.parent;
        }
        moves[i] = bestPath;
        for (i = depth - 1; i >= 0; i--) {
            if (moves[i] != null && moves[i].move != null) {
                System.out.println("\t" + moves[i].move.toString().replaceAll("\n", "") + " " + moves[i].score);
            }
        }
        System.out.println(bestPath.parent + " " + bestPath.move + " " + bestPath.score);
        return game.board.translateMove(bestPath.move);
    }

    private static class MovePath {
        MovePath parent;
        double score;
        Move move;

        public MovePath() {}

        public MovePath(Move move, double score) {
            this(null, move, score);
        }

        public MovePath(MovePath parent, Move move, double score) {
            this.parent = parent;
            this.move = move;
            this.score = score;
        }

        public int getLength() {
            int total = 0;
            MovePath curr = this;
            if (move != null) {
                while (curr.parent != null) {
                    total++;
                    curr = curr.parent;
                }
            }
            return total;
        }

        public String toString() {
            MovePath curr = this;
            String out = "";
            if (move != null) {
                out = move.toString();
                while (curr.parent != null) {
                    out += ", " + curr.move.toString();
                    curr = curr.parent;
                }
            }
            return out;
        }
    }
}
