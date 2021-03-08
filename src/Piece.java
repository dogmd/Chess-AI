import java.util.ArrayList;

public class Piece {
    Square square;
    PieceType type;
    int color;
    boolean hasMoved;
    ArrayList<Square> threatening;

    static final int WHITE = 0;
    static final int BLACK = 1;

    public Piece() {
        this.type = PieceType.PAWN;
        this.color = WHITE;
        threatening = new ArrayList<>();
    }

    public Piece(Piece p, Square sq) {
        this(p);
        sq.setPiece(this);
    }

    public Piece(Piece p) {
        this.type = p.type;
        this.color = p.color;
        this.hasMoved = p.hasMoved;
        this.square = p.square;
        threatening = new ArrayList<>();
    }

    public Piece(PieceType type, int color) {
        this.type = type;
        this.color = color;
        threatening = new ArrayList<>();
    }

    public Piece(Square square, PieceType type, int color) {
        square.setPiece(this);
        this.type = type;
        this.color = color;
        threatening = new ArrayList<>();
    }

    public Square getSquare() {
        return square;
    }

    public static boolean isWhite(Piece p) {
        return p.color == 0;
    }

    public boolean isWhite() {
        return isWhite(this);
    }

    public static int getOpposite(int color) {
        return color == WHITE ? BLACK : WHITE;
    }

    public char getChar() {
        char piece = 'a';
        switch(type) {
            case PAWN: piece = 'P'; break;
            case ROOK: piece = 'R'; break;
            case KNIGHT: piece = 'N'; break;
            case BISHOP: piece = 'B'; break;
            case QUEEN: piece = 'Q'; break;
            case KING: piece = 'K'; break;
        }
        if (color == BLACK) {
            piece = Character.toLowerCase(piece);
        }
        return piece;
    }

    public String toString() {
        String piece = "" + getChar();
        if (square != null) {
            piece += square.toString();
        }
        return piece;
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

    public int getWeight() {
        return getWeight(false);
    }

    public int getWeight(boolean isEndgame) {
        int weight = getWeight(this.type);
        int row = color == BLACK ? 7 - square.row : square.row;
        int index = row * 8 + square.col;
        switch (this.type) {
            case PAWN: return weight + PAWN_W[index];
            case BISHOP: return weight + BISHOP_W[index];
            case KNIGHT: return weight + KNIGHT_W[index];
            case ROOK: return weight + ROOK_W[index];
            case QUEEN: return weight + QUEEN_W[index];
            case KING: return weight + (isEndgame ? KING_WE[index] : KING_WM[index]);
            default: return -1;
        }
    }

    public static int getWeight(PieceType type) {
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
