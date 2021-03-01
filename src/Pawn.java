public class Pawn extends Piece {
    boolean enPassantable;

    public Pawn(Pawn p, Square sq) {
        super(p, sq);
        this.enPassantable = p.enPassantable;
    }

    public Pawn(Piece p, boolean enPassantable) {
        super(p);
        this.enPassantable = enPassantable;
    }

    public Pawn(PieceType type, int color, boolean enPassantable) {
        super(type, color);
        this.enPassantable = enPassantable;
    }
}
