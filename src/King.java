public class King extends Piece {
    boolean queenCastle, kingCastle;

    public King(King k, Square sq) {
        super(k, sq);
        this.queenCastle = k.queenCastle;
        this.kingCastle = k.kingCastle;
    }

    public King(Piece p, boolean queenCastle, boolean kingCastle) {
        super(p);
        this.queenCastle = queenCastle;
        this.kingCastle = kingCastle;
    }

    public King(PieceType type, int color, boolean queenCastle, boolean kingCastle) {
        super(type, color);
        this.queenCastle = queenCastle;
        this.kingCastle = kingCastle;
    }
}
