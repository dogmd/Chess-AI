import java.util.ArrayList;
import java.util.Collections;
import java.util.Timer;
import java.util.TimerTask;


public class ScottAgent extends Agent {
    long evalCount;
    int currDepth, targetDepth;
    Game copy;
    Move bestMove;
    double bestScore;
    Move lastBestMove;
    double lastBestScore;
    boolean searchCaptures;
    MovePath bestPath;
    MovePath lastBestPath;
    MovePath root;
    TranspositionTable tt;
    boolean abortSearch;
    long maxMillis;
    Timer timer;

    static final double IMMEDIATE_MATE_SCORE = 100000;

    public ScottAgent(String name, Game game, int color) {
        super(name, game, color);
        copy = new Game(game);
        evalCount = 0;
        this.timer = new Timer();
        tt = new TranspositionTable(copy.board);
        try {
            String[] fields = name.split(",");
            this.targetDepth = Integer.parseInt(fields[0]);
            this.searchCaptures = Boolean.parseBoolean(fields[1]);
            this.maxMillis = Long.parseLong(fields[2]);
        } catch (Exception e) {
            this.targetDepth = -1;
            this.searchCaptures = true;
            this.maxMillis = 1000;
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
        Move storedMove = tt.getMove();
        if (storedMove != null && storedMove.equals(move)) {
            total += 10000;
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
        if (abortSearch) {
            return 0;
        }

        ArrayList<Move> moves = new ArrayList<>(copy.board.moves);
        if (moves.size() == 0) {
            if (copy.board.isChecked()) {
                return -IMMEDIATE_MATE_SCORE + (this.currDepth - depth);
            }
            return 0;
        } else if (depth != this.currDepth) {
            if (game.hasBeenSeen(copy.board.zobristKey)) {
                return 0;
            }
            alpha = Math.max(alpha, -IMMEDIATE_MATE_SCORE + (this.currDepth - depth));
            beta = Math.min(beta, IMMEDIATE_MATE_SCORE - (this.currDepth - depth));
            if (alpha >= beta) {
                return alpha;
            }
        }

        double storedEval = tt.lookupEval(depth, alpha, beta);
        if (storedEval != Double.MIN_VALUE) {
            if (this.currDepth == depth) {
                lastBestMove = tt.getMove();
                lastBestScore = tt.entries[tt.getIndex()].eval;
            }
            return storedEval;
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

        int evalType = TranspositionTable.Entry.UPPER;
        Move bestInPos = null;

        for (Move move : moves) {
            copy.makeMove(move);
            path.next = new MovePath(move);
            path.zobristKey = copy.board.zobristKey;
            double score = -search(depth - 1, -beta, -alpha, path.next);
            copy.unmakeMove(move);

            if (score >= beta) {
                tt.storeEval(depth, beta, TranspositionTable.Entry.LOWER, move);
                return beta;
            }

            if (score > alpha) {
                evalType = TranspositionTable.Entry.EXACT;
                bestInPos = move;

                alpha = score;
                if (depth == this.currDepth) {
                    lastBestScore = score;
                    lastBestMove = move;
                    lastBestPath = root.next;
                }
            }
        }

        tt.storeEval(depth, alpha, evalType, bestInPos);

        return alpha;
    }

    public double getEval(Game game) {
        this.game = game;
        this.bestScore = -999999;
        this.bestMove = null;
        search(targetDepth, -999999, 999999, new MovePath());
        return bestScore;
    }

    public Move getMove(Game game, int color) {
        tt.clear(); // might help?
        this.evalCount = 0;
        this.game = game;
        this.abortSearch = false;

        this.bestScore = -999999;
        this.bestMove = null;
        this.bestPath = null;
        this.lastBestScore = -999999;
        this.lastBestMove = null;
        this.lastBestPath = null;

        root = new MovePath();
        root.zobristKey = copy.board.zobristKey;

        if (this.targetDepth == -1) {
            targetDepth = Integer.MAX_VALUE;
        }
        Interrupter interrupter = new Interrupter(this);
        this.timer.schedule(interrupter, maxMillis);

        for (currDepth = 1; currDepth <= targetDepth; currDepth++) {
            search(currDepth, -9999999, 9999999, root);
            if (abortSearch) {
                break;
            } else {
                bestMove = lastBestMove;
                bestScore = lastBestScore;
                bestPath = lastBestPath;

                if (Math.abs(bestScore) + 1000 > IMMEDIATE_MATE_SCORE) {
                    int winningColor = color;
                    if (bestScore < 0) {
                        winningColor = Piece.getOpposite(color);
                    }

                    System.out.println((winningColor == Piece.WHITE ? "WHITE" : "BLACK") + " HAS MATE IN " + (currDepth + 1) / 2);
                    break;
                }
            }
        }
        interrupter.cancel();

        return bestMove;
    }

    private class Interrupter extends TimerTask {
        ScottAgent agent;

        public Interrupter(ScottAgent agent) {
            this.agent = agent;
        }

        @Override
        public void run() {
            agent.abortSearch = true;
        }
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
