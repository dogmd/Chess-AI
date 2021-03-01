import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class Board {
    Square[][] board;
    Map<PieceType, List<List<Piece>>> pieces;
    ArrayList<Move> moves;
    int activeColor;

    public Board() {
        board = new Square[8][8];
        pieces = new HashMap<>();
        moves = new ArrayList<>();
        initPieces();

        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board[i].length; j++) {
                board[i][j] = new Square(i, j);
            }
        }
        updateInfo();
    }

    public Board(Board b) {
        initPieces();
        this.board = new Square[b.board.length][];
        for (int i = 0; i < b.board.length; i++) {
            this.board[i] = new Square[b.board[i].length];
            for (int j = 0; j < b.board[i].length; j++) {
                Square sq = new Square(b.board[i][j]);
                this.board[i][j] = sq;
                if (sq.isOccupied()) {
                    pieces.get(sq.piece.type).get(sq.piece.color).add(sq.piece);
                }
            }
        }
        this.activeColor = b.activeColor;
        moves = new ArrayList<>();
        for (Move move : b.moves) {
            moves.add(translateMove(move));
        }
    }

    public void updateInfo() {
        updateThreatening();
        updatePinning();
        updateMoves();
    }

    public ArrayList<Move> getMoves(int color) {
        ArrayList<Move> tempMoves = new ArrayList<>();
        for (List<List<Piece>> pieceType : pieces.values()) {
            for (Piece p : pieceType.get(color)) {
                ArrayList<Move> pieceMoves = MoveGenerator.generateMoves(this, p);
                for (Move move : pieceMoves) {
                    tempMoves.add(move);
                }
            }
        }
        return tempMoves;
    }

    public void updateMoves() {
        moves = getMoves(activeColor);
    }

    public Move translateMove(Move move) {
        return new Move(board[move.start.row][move.start.col], board[move.end.row][move.end.col]);
    }

    public void initPieces() {
        pieces = new HashMap<>();
        for (PieceType type : PieceType.values()) {
            List<List<Piece>> pieceList = new ArrayList<>();
            pieceList.add(new ArrayList<>());
            pieceList.add(new ArrayList<>());
            pieces.put(type, pieceList);
        }
    }

    public Board(String fen) {
        this();
        int row = 0, col = 0;
        String[] fields = fen.split(" ");
        for (int i = 0; i < fields[0].length(); i++) {
            char c = fields[0].charAt(i);
            int color = Character.isLowerCase(c) ? Piece.BLACK : Piece.WHITE;
            c = Character.toUpperCase(c);
            switch (c) {
                case '/':
                    row++;
                    col = 0;
                    break;
                case 'P':
                    int rowOffset = color == Piece.WHITE ? 1 : -1;
                    String backwardsSquare = coorConvert(row + rowOffset, col);
                    addPiece(row, col, new Pawn(PieceType.PAWN, color, backwardsSquare.equals(fields[3])));
                    col++;
                    break;
                case 'N':
                    addPiece(row, col, new Piece(PieceType.KNIGHT, color));
                    col++;
                    break;
                case 'B':
                    addPiece(row, col, new Piece(PieceType.BISHOP, color));
                    col++;
                    break;
                case 'R':
                    addPiece(row, col, new Piece(PieceType.ROOK, color));
                    col++;
                    break;
                case 'Q':
                    addPiece(row, col, new Piece(PieceType.QUEEN, color));
                    col++;
                    break;
                case 'K':
                    boolean queenCastle, kingCastle;
                    if (color == Piece.WHITE) {
                        queenCastle = fields[2].contains("Q");
                        kingCastle = fields[2].contains("K");
                    } else {
                        queenCastle = fields[2].contains("q");
                        kingCastle = fields[2].contains("k");
                    }
                    addPiece(row, col, new King(PieceType.KING, color, queenCastle, kingCastle));
                    col++;
                    break;
                default:
                    col += Integer.parseInt("" + c);
                    break;
            }
        }
        updateInfo();
    }

    public void addPiece(int row, int col, Piece piece) {
        board[row][col].setPiece(piece);
        pieces.get(piece.type).get(piece.color).add(piece);
    }

    public static String coorConvert(int row, int col) {
        return "" + (char)(col + 'a') + (1 + row);
    }

    public void makeMove(Move move) {
        makeMove(move, false);
    }

    public void makeMove(Move move, boolean end) {
        removeThreatening(move.actor);
        for (List<Piece> allColors : pieces.get(PieceType.PAWN)) {
            for (Piece p : allColors) {
                Pawn pawn = (Pawn) p;
                pawn.enPassantable = false;
            }
        }
        if (move.actor.type == PieceType.PAWN) {
            Pawn pawn = (Pawn) move.actor;
            if (!pawn.hasMoved && Math.abs(move.start.row - move.end.row) == 2) {
                pawn.enPassantable = true;
            }
        }

        move.actor.hasMoved = true;
        move.end.setPiece(move.actor);
        move.start.setPiece(null);
        if (move.type == Move.CASTLE) {
            move.captured.hasMoved = true;
            int offset = move.start.col - move.end.col > 0 ? 1 : -1;
            board[move.start.row][move.end.col + offset].setPiece(move.captured);
        } else if (move.type == Move.PROMOTION) {
            List<Piece> pawns = pieces.get(move.actor.type).get(move.actor.color);
            pawns.remove(move.actor);
            move.actor.type = move.promoteTo;
            pieces.get(move.promoteTo).get(move.actor.color).add(move.actor);
        }
        if (move.isCapture()) {
            removeThreatening(move.captured);
            pieces.get(move.captured.type).get(move.captured.color).remove(move.captured);
        }
        activeColor = Piece.getOpposite(activeColor);
        updateThreatening();
        if (!end) {
            updatePinning();
            updateMoves();
        }
    }

    public void unmakeMove(Move move) {
        if (move.type == Move.EN_PASSANT) {
            move.end.setPiece(null);
            Pawn pawn = (Pawn)(move.captured);
            pawn.enPassantable = true;
            addPiece(move.start.row, move.end.col, pawn);
        } else if (move.type == Move.CASTLE) {
            // handle rook
            int col = move.start.col - move.end.col > 0 ? 0 : 7;
            int row = move.start.row;
            int offset = move.start.col - move.end.col > 0 ? 1 : -1;
            board[row][move.end.col + offset].setPiece(null);
            board[row][col].setPiece(move.captured);
            move.captured.hasMoved = false;
            move.end.setPiece(null);
        } else if (move.type == Move.PROMOTION) {
            move.end.setPiece(move.captured);
            pieces.get(move.actor.type).get(move.actor.color).remove(move.actor);
            move.actor.type = PieceType.PAWN;
            addPiece(move.start.row, move.start.col, move.actor);
        } else {
            move.end.setPiece(move.captured);
        }
        if (move.isCapture() && move.type != Move.EN_PASSANT) {
            addPiece(move.end.row, move.end.col, move.captured);
        }
        if (move.firstMove) {
            move.actor.hasMoved = false;
        }
        move.start.setPiece(move.actor);
        activeColor = move.actor.color;
        updateInfo();
    }

    public void updateThreatening() {
        for (List<List<Piece>> allColors : pieces.values()) {
            for (List<Piece> color : allColors) {
                for (Piece p : color) {
                    removeThreatening(p);
                    addThreatening(p);
                }
            }
        }
    }

    public void updatePinning() {
        for (List<List<Piece>> allColors : pieces.values()) {
            for (List<Piece> color : allColors) {
                for (Piece p : color) {
                    p.pinnedBy = null;
                }
            }
        }
        for (List<List<Piece>> allColors : pieces.values()) {
            for (List<Piece> color : allColors) {
                for (Piece p : color) {
                    Piece pinned = MoveGenerator.getPinned(this, p);
                    if (pinned != null) {
                        pinned.pinnedBy = p;
                    }
                }
            }
        }
    }

    public void removeThreatening(Piece p) {
        if (p != null) {
            for (int i = p.threatening.size() - 1; i >= 0; i--) {
                Square sq = p.threatening.get(i);
                sq.threatenedBy.remove(p);
                p.threatening.remove(i);
            }
        }
    }

    public boolean isChecked(int color) {
        if (pieces.get(PieceType.KING).get(color).size() == 0) {
            return true;
        }
        King king = (King) pieces.get(PieceType.KING).get(color).get(0);
        for (Piece piece : king.square.threatenedBy) {
            if (piece.color != king.color) {
                return true;
            }
        }
        return false;
    }

    public void addThreatening(Piece p) {
        for (Move m : MoveGenerator.generateMoves(this, p, true)) {
            m.end.threatenedBy.add(p);
            p.threatening.add(m.end);
        }
    }

    public String toFen() {
        StringBuilder fen = new StringBuilder();
        int empty = 0;
        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board[i].length; j++) {
                Square sq = board[i][j];
                if (sq.isOccupied()) {
                    if (empty > 0) {
                        fen.append(empty);
                    }
                    empty = 0;
                    fen.append(sq.piece.getChar());
                } else {
                    empty++;
                }
            }
            if (empty > 0) {
                fen.append(empty);
                empty = 0;
            }
            fen.append("/");
        }
        fen.append(" ");
        fen.append(activeColor == Piece.WHITE ? "w " : "b ");
        for (List<Piece> allKings : pieces.get(PieceType.KING)) {
            for (Piece p : allKings) {
                if (MoveGenerator.canCastle(this, (King) p, -2) != null) {
                    fen.append(p.color == Piece.WHITE ? "Q" : "q");
                }
                if (MoveGenerator.canCastle(this, (King) p, 2) != null) {
                    fen.append(p.color == Piece.WHITE ? "K" : "k");
                }
            }
        }
        fen.append(" ");
        boolean hasEn = false;
        for (List<Piece> allColors : pieces.get(PieceType.PAWN)) {
            for (Piece p : allColors) {
                Pawn pawn = (Pawn) p;
                if (pawn.enPassantable) {
                    fen.append(pawn.square.toString());
                    hasEn = true;
                }
            }
        }
        if (!hasEn) {
            fen.append("-");
        }
        fen.append(" 0 1");
        return fen.toString();
    }

    public String toString() {
        if (Main.displayEnabled) {
            return toFen();
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < board.length; i++) {
            builder.append("  ");
            for (int j = 0; j < board[i].length; j++) {
                builder.append("+---");
            }
            builder.append("+\n").append(i + 1).append(" ");
            for (int j = 0; j < board[i].length; j++) {
                if (board[i][j].isOccupied()) {
                    builder.append("| ").append(board[i][j].piece.getChar()).append(" ");
                } else {
                    builder.append("| ").append(" ").append(" ");
                }
            }
            builder.append("|\n");
        }
        builder.append("  ");
        for (int i = 0; i < board[0].length; i++) {
            builder.append("+---");
        }
        builder.append("+\n  ");
        for (int i = 0; i < board[0].length; i++) {
            builder.append("  ").append((char)('a' + i)).append(" ");
        }
        return builder.toString();
    }
}
