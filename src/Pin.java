import java.util.ArrayList;

public class Pin {
    Piece piece;
    Piece pinnedBy;
    King pinnedTo;
    ArrayList<Square> path;

    public Pin(Piece piece, Piece pinnedBy, King pinnedTo, ArrayList<Square> path) {
        this.piece = piece;
        this.pinnedBy = pinnedBy;
        this.pinnedTo = pinnedTo;
        this.path = path;
    }

    public Pin(Pin pin, Board board) {
        path = new ArrayList<>();
        for (Square sq : pin.path) {
            path.add(board.board[sq.row][sq.col]);
        }
    }
}
