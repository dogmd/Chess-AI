import java.util.ArrayList;

public class Move implements Comparable<Move> {
    int start, end;
    int actor;
    int captured;
    int type;
    int promoteTo;
    boolean firstMove;
    double score;
    String algebraic;

    public static final int CASTLE = 1;
    public static final int PROMOTION = 2;
    public static final int EN_PASSANT = 3;

    public Move() {}

    public Move(int move) {
        int mask = 0b1111;
        int sqMask = 0b111111;
        type = move & mask;
        promoteTo = (move >> 4) & mask;
        start = (move >> 8) & sqMask;
        end = (move >> 14) & sqMask;
        actor = (move >> 20) & mask;
        captured = (move >> 24) & mask;
        firstMove = ((move >> 28) & mask) == 1;

        if (captured == 0b1111) {
            captured = -1;
        }
    }

    public Move(int actor, int captured, int start, int end, Board board) {
        this.actor = actor;
        this.captured = captured;
        this.start = start;
        this.end = end;
        this.firstMove = !board.hasMoved[start];
    }

    public Move(Move from) {
        this.start = from.start;
        this.end = from.end;
        this.actor = from.actor;
        this.captured = from.captured;
        this.type = from.type;
        this.promoteTo = from.promoteTo;
        this.firstMove = from.firstMove;
        this.score = from.score;
    }

    public boolean isCapture() {
        return captured != Board.EMPTY && type != CASTLE;
    }

    public String toString() {
        if (algebraic != null) {
            return algebraic;
        }
        if (type == CASTLE) {
            return end > start ? "0-0" : "0-0-0";
        } else {
            String piece = Piece.getChar(actor);
            String capture = isCapture() ? "x" : "";
            String dest = Board.coorConvert(end);
            String promotion = type == PROMOTION ? Piece.getChar(promoteTo) : "";
            return piece + capture + dest + promotion;
        }
    }

    // Assumes board is in state right before move is made.
    public String getAlgebraic(Board board) {
        if (algebraic != null) {
            return algebraic;
        }

        int actorType = Piece.getType(actor);

        if (type == CASTLE) {
            algebraic = end > start ? "O-O" : "O-O-O";
            return algebraic;
        }

        algebraic = Piece.getChar(actor).toUpperCase();

        // check if ambiguous
        if (actorType != Piece.PAWN && actorType != Piece.KING) {
            for (Move move : board.moves) {
                if (move.start != this.start && move.end == this.end) {
                    if (Piece.getType(move.actor) == actorType) {
                        int startRow = this.start / 8;
                        int altStartRow = move.start / 8;
                        int startCol = this.start - startRow * 8;
                        int altStartCol = move.start - altStartRow * 8;

                        if (startCol != altStartCol) {
                            algebraic += (char)('a' + startCol);
                            break;
                        } else if (startRow != altStartRow) {
                            algebraic += (8 - startRow);
                            break;
                        }
                    }
                }
            }
        }

        if (isCapture()) {
            if (actorType == Piece.PAWN) {
                algebraic += (char)('a' + this.start % 8);
            }
            algebraic += "x";
        } else {
            if (type == EN_PASSANT) {
                algebraic += (char)('a' + this.start % 8) + "x";
            }
        }

        algebraic += (char)('a' + this.end % 8);
        algebraic += (8 - this.end / 8);

        if (type == PROMOTION) {
            algebraic += "=" + Piece.getFenChar(promoteTo).toUpperCase();
        }

        board.game.makeMove(this);
        if (board.isCheckmate()) {
            algebraic += "#";
        } else if (board.isChecked()) {
            algebraic += "+";
        }
        board.game.unmakeMove(this);

        return algebraic;
    }

    public int compareTo(Move move) {
        return (int)(move.score - this.score);
    }

    public boolean equals(Object other) {
        Move move;
        if (other instanceof Move) {
            move = (Move)other;
        } else {
            return false;
        }

        return move.type == this.type && move.promoteTo == this.promoteTo && move.start == this.start && move.end == this.end && move.actor == this.actor && move.captured == this.captured && move.firstMove == this.firstMove;
    }

    public int hashCode() {
        int captureHash = captured;
        if (captured == -1) {
            captureHash = 0b1111;
        }

        int hash = 0;
        hash |= type;
        hash |= promoteTo << 4;
        hash |= start << 8;
        hash |= end << 14;
        hash |= actor << 20;
        hash |= captureHash << 24;
        hash |= (firstMove ? 1 : 0) << 28;
        return hash;
    }
}
