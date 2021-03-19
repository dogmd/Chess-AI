public class TranspositionTable {
    static final int TT_SIZE = 1024000;
    Board board;
    Entry[] entries;

    public TranspositionTable(Board board) {
        entries = new Entry[TT_SIZE];
        clear();
        this.board = board;
    }

    public int getIndex() {
        return (int)(Math.abs(board.zobristKey) % TT_SIZE);
    }

    public double lookupEval(int depth, double alpha, double beta) {
        Entry entry = entries[getIndex()];
        if (entry.key == board.zobristKey) {
            if (entry.depth >= depth) {
                if (entry.type == Entry.EXACT) {
                    return entry.eval;
                }
                if (entry.type == Entry.UPPER && entry.eval <= alpha) {
                    return entry.eval;
                }
                if (entry.type == Entry.LOWER && entry.eval >= beta) {
                    return entry.eval;
                }
            }
        }
        return Double.MIN_VALUE;
    }

    public Move getMove() {
        return entries[getIndex()].move;
    }

    public void storeEval(int depth, double eval, int evalType, Move move) {
        entries[getIndex()] = new Entry(board.zobristKey, eval, depth, evalType, move);
    }

    public void clear() {
        for (int i = 0; i < TT_SIZE; i++) {
            entries[i] = new Entry();
        }
    }

    public class Entry {
        public long key;
        public double eval;
        public Move move;
        public int depth;
        public int type;

        public static final int EXACT = 0;
        public static final int UPPER = 1;
        public static final int LOWER = 2;

        public Entry() {}

        public Entry(long key, double eval, int depth, int type, Move move) {
            this.key = key;
            this.eval = eval;
            this.move = move;
            this.depth = depth;
            this.type = type;
        }
    }
}
