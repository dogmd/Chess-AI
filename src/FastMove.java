import javax.swing.*;

public class FastMove extends Move implements Comparable<Move> {
    int start, end;
    int actor;
    int captured;
    int type;
    int promoteTo;
    boolean firstMove;
    double score;

    public FastMove(int actor, int captured, int start, int end, FastBoard board) {
        this.actor = actor;
        this.captured = captured;
        this.start = start;
        this.end = end;
        this.firstMove = !board.hasMoved[start];
    }

    public FastMove(FastMove from) {
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
        return captured != FastBoard.EMPTY && type != CASTLE;
    }

    public String toString() {
        if (type == CASTLE) {
            return end > start ? "0-0" : "0-0-0";
        } else {
            String piece = Piece.getChar(actor);
            String capture = isCapture() ? "x" : "";
            String dest = FastBoard.coorConvert(end);
            String promotion = type == PROMOTION ? Piece.getChar(promoteTo) : "";
            return piece + capture + dest + promotion;
        }
    }

    public int compareTo(FastMove move) {
        return (int)(move.score - this.score);
    }
}
