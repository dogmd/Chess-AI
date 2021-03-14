import java.util.*;

public class Zobrist {
    static final int seed = 69696969;

    // one for each type, each color, and each square
    static long[][][] piecesArr = new long[8][2][64];
    // one for each combination of castling rights
    static long[] castlingRights = new long[16];
    // one for each en passant file, 0 for none
    static long[] enPassantFile = new long[9];
    static long activeColor;

    static Random rand;

    public static LinkedList<Long> getRandom() {
        int randCount = 8 * 2 * 64 + 16 + 9 + 1;
        rand = new Random(seed);
        LinkedList<Long> rands = new LinkedList<>();
        for (int i = 0; i < randCount; i++) {
            rands.add(rand.nextLong());
        }
        return rands;
    }

    public static void fillRandom() {
        Queue<Long> rands = getRandom();
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 2; j++) {
                for (int k = 0; k < 64; k++) {
                    piecesArr[i][j][k] = rands.poll();
                }
            }
        }

        for (int i = 0; i < 16; i++) {
            castlingRights[i] = rands.poll();
        }

        for (int i = 0; i < 9; i++) {
            enPassantFile[i] = rands.poll();
        }

        activeColor = rands.poll();
    }

    public static long calcKey(Board board) {
        // random numbers not instantiated
        if (activeColor == 0) {
            fillRandom();
        }
        long key = 0;

        for (int i = 0; i < 64; i++) {
            if (board.board[i] != Board.EMPTY) {
                int piece = board.board[i];
                int type = Piece.getType(piece);
                int color = Piece.getColor(piece);
                key ^= piecesArr[type][color][i];
            }
        }

        int enCol = 0;
        if (board.enPassantable == -1) {
            enCol = (board.enPassantable % 8) + 1;
        }
        key ^= enPassantFile[enCol];

        if (board.activeColor == Piece.BLACK) {
            key ^= activeColor;
        }

        key ^= castlingRights[board.getCastleRights()];

        return key;
    }
}
