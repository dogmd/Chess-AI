import java.util.ArrayList;

public class Piece {
    Square square;
    PieceType type;
    int color;
    boolean hasMoved;
    Piece pinnedBy;
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
        this.pinnedBy = p.pinnedBy;
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

    public boolean isPinned() {
        return pinnedBy != null;
    }

    public String toString() {
        String piece = "" + getChar();
        if (square != null) {
            piece += square.toString();
        }
        return piece;
    }

    public static int getWeight(PieceType type) {
        switch (type) {
            case PAWN: return 1;
            case BISHOP:
            case KNIGHT: return 3;
            case ROOK: return 5;
            case QUEEN: return 9;
            case KING: return 0;
            default: return -1;
        }
    }

    public int getWeight() {
        return getWeight(type);
    }
}
