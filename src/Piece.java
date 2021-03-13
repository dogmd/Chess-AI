import java.util.ArrayList;

public class Piece {
    static final int WHITE = 0;
    static final int BLACK = 1;

    static final int PAWN = 0, KNIGHT = 1, BISHOP = 2, ROOK = 3, QUEEN = 4, KING = 5;

    public static int getOpposite(int color) {
        if (color == -1) {
            return -1;
        }
        return color == WHITE ? BLACK : WHITE;
    }

    public static int getType(int piece) {
        if (piece != FastBoard.EMPTY) {
            return piece & 0b111;
        }
        return FastBoard.EMPTY;
    }

    public static int getColor(int piece) {
        if (piece != FastBoard.EMPTY) {
            return piece >> 3;
        }
        return FastBoard.EMPTY;
    }

    public static String getChar(int piece) {
        int type = getType(piece);
        int color = getColor(piece);
        String val;
        switch (type) {
            case KNIGHT: val = "N"; break;
            case BISHOP: val = "B"; break;
            case ROOK: val = "R"; break;
            case QUEEN: val = "Q"; break;
            case KING: val = "K"; break;
            default: val = "";
        }
        if (color == BLACK) {
            return val.toLowerCase();
        }
        return val;
    }

    public static String getFenChar(int piece) {
        int type = getType(piece);
        String val;
        if (type == Piece.PAWN) {
            val = "P";
            int color = getColor(piece);
            if (color == Piece.BLACK) {
                val = val.toLowerCase();
            }
            return val;
        }
        return getChar(piece);
    }

    // tables from https://www.chessprogramming.org/Simplified_Evaluation_Function#Piece-Square_Tables
    public static final int[] PAWN_W = new int[]{
            0,  0,  0,  0,  0,  0,  0,  0,
            50, 50, 50, 50, 50, 50, 50, 50,
            10, 10, 20, 30, 30, 20, 10, 10,
            5,  5, 10, 25, 25, 10,  5,  5,
            0,  0,  0, 20, 20,  0,  0,  0,
            5, -5,-10,  0,  0,-10, -5,  5,
            5, 10, 10,-20,-20, 10, 10,  5,
            0,  0,  0,  0,  0,  0,  0,  0
    };

    public static final int[] KNIGHT_W = new int[]{
            -50,-40,-30,-30,-30,-30,-40,-50,
            -40,-20,  0,  0,  0,  0,-20,-40,
            -30,  0, 10, 15, 15, 10,  0,-30,
            -30,  5, 15, 20, 20, 15,  5,-30,
            -30,  0, 15, 20, 20, 15,  0,-30,
            -30,  5, 10, 15, 15, 10,  5,-30,
            -40,-20,  0,  5,  5,  0,-20,-40,
            -50,-40,-30,-30,-30,-30,-40,-50
    };

    public static final int[] BISHOP_W = new int[]{
            -20,-10,-10,-10,-10,-10,-10,-20,
            -10,  0,  0,  0,  0,  0,  0,-10,
            -10,  0,  5, 10, 10,  5,  0,-10,
            -10,  5,  5, 10, 10,  5,  5,-10,
            -10,  0, 10, 10, 10, 10,  0,-10,
            -10, 10, 10, 10, 10, 10, 10,-10,
            -10,  5,  0,  0,  0,  0,  5,-10,
            -20,-10,-10,-10,-10,-10,-10,-20
    };

    public static final int[] ROOK_W = new int[]{
            0,  0,  0,  0,  0,  0,  0,  0,
            5, 10, 10, 10, 10, 10, 10,  5,
            -5,  0,  0,  0,  0,  0,  0, -5,
            -5,  0,  0,  0,  0,  0,  0, -5,
            -5,  0,  0,  0,  0,  0,  0, -5,
            -5,  0,  0,  0,  0,  0,  0, -5,
            -5,  0,  0,  0,  0,  0,  0, -5,
            0,  0,  0,  5,  5,  0,  0,  0
    };

    public static final int[] QUEEN_W = new int[]{
            -20,-10,-10, -5, -5,-10,-10,-20,
            -10,  0,  0,  0,  0,  0,  0,-10,
            -10,  0,  5,  5,  5,  5,  0,-10,
            -5,  0,  5,  5,  5,  5,  0, -5,
            0,  0,  5,  5,  5,  5,  0, -5,
            -10,  5,  5,  5,  5,  5,  0,-10,
            -10,  0,  5,  0,  0,  0,  0,-10,
            -20,-10,-10, -5, -5,-10,-10,-20
    };

    public static final int[] KING_WM = new int[]{
            -30,-40,-40,-50,-50,-40,-40,-30,
            -30,-40,-40,-50,-50,-40,-40,-30,
            -30,-40,-40,-50,-50,-40,-40,-30,
            -30,-40,-40,-50,-50,-40,-40,-30,
            -20,-30,-30,-40,-40,-30,-30,-20,
            -10,-20,-20,-20,-20,-20,-20,-10,
            20, 20,  0,  0,  0,  0, 20, 20,
            20, 30, 10,  0,  0, 10, 30, 20
    };

    public static final int[] KING_WE = new int[]{
            -50,-40,-30,-20,-20,-30,-40,-50,
            -30,-20,-10,  0,  0,-10,-20,-30,
            -30,-10, 20, 30, 30, 20,-10,-30,
            -30,-10, 30, 40, 40, 30,-10,-30,
            -30,-10, 30, 40, 40, 30,-10,-30,
            -30,-10, 20, 30, 30, 20,-10,-30,
            -30,-30,  0,  0,  0,  0,-30,-30,
            -50,-30,-30,-30,-30,-30,-30,-50
    };

    public static int getWeight(int piece, int row, int col, boolean isEndgame) {
        int type = Piece.getType(piece);
        int color = Piece.getColor(piece);
        int weight = getWeight(type);
        row = color == BLACK ? 7 - row : row;
        int index = row * 8 + col;
        switch (type) {
            case PAWN: return weight + PAWN_W[index];
            case BISHOP: return weight + BISHOP_W[index];
            case KNIGHT: return weight + KNIGHT_W[index];
            case ROOK: return weight + ROOK_W[index];
            case QUEEN: return weight + QUEEN_W[index];
            case KING: return weight + (isEndgame ? KING_WE[index] : KING_WM[index]);
            default: return -1;
        }
    }

    public static int getWeight(int type) {
        switch (type) {
            case PAWN: return 100;
            case BISHOP: return 330;
            case KNIGHT: return 320;
            case ROOK: return 500;
            case QUEEN: return 900;
            case KING: return 0;
            default: return -1;
        }
    }
}
