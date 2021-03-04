import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class Pin {
    Piece piece;
    Piece pinnedBy;
    King pinnedTo;
    Set<Square> path;

    public Pin(Piece piece, Piece pinnedBy, King pinnedTo, Set<Square> path) {
        this.piece = piece;
        this.pinnedBy = pinnedBy;
        this.pinnedTo = pinnedTo;
        this.path = path;
    }

    public Pin(Pin pin, Board board) {
        path = new HashSet<>();
        for (Square sq : pin.path) {
            path.add(board.board[sq.row][sq.col]);
        }
    }
}
