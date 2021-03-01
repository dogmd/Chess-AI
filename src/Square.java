import java.util.HashSet;
import java.util.Set;

public class Square {
    Piece piece;
    int row, col;
    Set<Piece> threatenedBy;

    public Square(int row, int col) {
        threatenedBy = new HashSet<>();
        this.row = row;
        this.col = col;
    }

    public Square(Square sq) {
        this.row = sq.row;
        this.col = sq.col;
        this.threatenedBy = new HashSet<>();
        if (sq.isOccupied()) {
            Piece p;
            if (sq.piece.type == PieceType.PAWN) {
                p = new Pawn((Pawn) sq.piece, this);
            } else if (sq.piece.type == PieceType.KING) {
                p = new King((King) sq.piece, this);
            } else {
                p = new Piece(sq.piece, this);
            }
            setPiece(p);
        }
    }

    public Piece getPiece() {
        return piece;
    }

    public void setPiece(Piece piece) {
        this.piece = piece;
        if (piece != null) {
            this.piece.square = this;
        }
    }

    public boolean isOccupied() {
        return this.piece != null;
    }

    public boolean isLight() {
        return (row + col) % 2 == 0; // Even col on even rows, odd col on odd rows
    }

    public String toString() {
        if (isOccupied() || Main.displayEnabled) {
            return Board.coorConvert(row, col);
        } else {
            return " ";
        }
    }
}
