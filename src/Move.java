public class Move {
    Square start, end;
    Piece actor;
    Piece captured;
    int type;
    PieceType promoteTo;
    boolean firstMove, isThreat = true;

    public static final int CASTLE = 1;
    public static final int PROMOTION = 2;
    public static final int EN_PASSANT = 3;

    public Move(Piece actor, Square start, Square end) {
        this.start = start;
        this.end = end;
        this.actor = actor;
        this.firstMove = !actor.hasMoved;
        if (end.isOccupied()) {
            captured = end.piece;
        }
    }

    public Move(Piece actor, Piece captured, Square start, Square end) {
        this.start = start;
        this.end = end;
        this.actor = actor;
        this.firstMove = !actor.hasMoved;
        this.captured = captured;
    }

    public Move(Square start, Square end) {
        this.start = start;
        this.end = end;
        this.actor = start.piece;
        if (actor != null) {
            this.firstMove = !actor.hasMoved;
        }
        if (end.isOccupied()) {
            captured = end.piece;
        }
    }

    public Move(Piece actor, Square end) {
        this.end = end;
        this.start = actor.square;
        this.actor = actor;
        this.firstMove = !actor.hasMoved;
        if (end.isOccupied()) {
            captured = end.piece;
        }
    }

    public boolean isCapture() {
        return captured != null && type != CASTLE;
    }

    public String toString() {
        return "\n{start: " + start + ", end: " + end + ", actor: " + actor.getChar() + end + ", captured: " + captured + ", type: " + type + ", promoted_to: " + promoteTo + "}";
    }
}
