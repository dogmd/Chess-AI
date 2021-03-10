import java.util.ArrayList;
import java.util.List;

public class FastBoard extends Board {
    int[] board; // stores the int value of the piece at the index
    int[][] pieces; // stores index of pieces of each colors
    int[] pieceCounts; // stores the number of pieces of each color
    boolean[] pins; // stores if square is part of pin
    boolean[] threatening; // stores if square is threatened
    boolean[] pawnThreats; // stores if pawn threatening square
    int threatCount;
    boolean[] hasMoved; // stores if any changes made at square
    ArrayList<FastMove> moves;
    boolean multiCheck, singleCheck;
    boolean[] checkPath; // stores if square is part of check path
    int activeColor, enPassantable, kingInd;
    Game game;

    static final int EMPTY = -1;

    public FastBoard() {
        board = new int[64];
        pieces = new int[2][16];
        pieceCounts = new int[2];
        pins = new boolean[64];
        threatening = new boolean[64];
        pawnThreats = new boolean[64];
        hasMoved = new boolean[64];
        checkPath = new boolean[64];
        moves = new ArrayList<>();
        enPassantable = EMPTY;
    }

    public FastBoard(FastBoard from) {
        this();
        moves = new ArrayList<>(from.moves.size());
        for (FastMove move : from.moves) {
            moves.add(new FastMove(move));
        }
        this.threatCount = from.threatCount;
        this.game = from.game;
        this.enPassantable = from.enPassantable;
        this.multiCheck = from.multiCheck;
        this.singleCheck = from.singleCheck;
        this.activeColor = from.activeColor;
        this.kingInd = from.kingInd;

        for (int i = 0; i < 2; i++) {
            pieceCounts[i] = from.pieceCounts[i];
            for (int j = 0; j < pieceCounts[i]; j++) {
                pieces[i][j] = from.pieces[i][j];
            }
        }

        for (int i = 0; i < 64; i++) {
            this.board[i] = from.board[i];
            this.pins[i] = from.pins[i];
            this.threatening[i] = from.threatening[i];
            this.pawnThreats[i] = from.pawnThreats[i];
            this.hasMoved[i] = from.hasMoved[i];
            this.checkPath[i] = from.checkPath[i];
        }
    }

    public FastBoard(Board from, Game game) {
        this();
        this.game = game;
        this.multiCheck = from.multiCheck;
        this.activeColor = from.activeColor;

        // init hasMoved
        for (int i = 0; i < 64; i++) {
            hasMoved[i] = true;
        }

        // copy board
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                Square sq = from.board[i][j];
                int index = sq.getIndex();
                if (sq.isOccupied()) {
                    board[index] = sq.piece.toInt();
                    hasMoved[index] = sq.piece.hasMoved;
                    if (sq.piece.type == PieceType.PAWN && ((Pawn)sq.piece).enPassantable) {
                        enPassantable = index;
                    }
                    addPiece(sq.piece.toInt(), index);
                } else {
                    board[index] = EMPTY;
                }
            }
        }

        // copy pins
        for (Pin p : from.pins) {
            int offset = p.path.get(0).getIndex();
            for (Square sq : p.path) {
                pins[sq.getIndex()] = true;
            }
        }

        // copy threats
        for (Square sq : from.threatening) {
            threatening[sq.getIndex()] = true;
        }

        // copy check path
        for (Square sq : from.checkPath) {
            checkPath[sq.getIndex()] = true;
        }

        updateInfo();
    }

    public int getMaterialScore() {
        boolean isEndgame = isEndgame();
        int whiteTotal = 0, blackTotal = 0;
        for (int color = Piece.WHITE; color <= Piece.BLACK; color++) {
            for (int i = 0; i < pieceCounts[color]; i++) {
                int ind = pieces[color][i];
                int y = ind / 8;
                int x = ind - y * 8;
                if (color == 0) {
                    whiteTotal += Piece.getWeight(board[ind], y, x, isEndgame);
                } else {
                    blackTotal += Piece.getWeight(board[ind], y, x, isEndgame);
                }
            }
        }
        return whiteTotal - blackTotal;
    }

    public boolean isEndgame() {
        int cumScore = getBasicMaterialScore(activeColor) + getBasicMaterialScore(Piece.getOpposite(activeColor));
        return cumScore <= 1650;
    }

    public int getMobilityDiff() {
        return moves.size() - threatCount;
    }

    public void addPiece(int piece, int index) {
        if (piece != -1) {
            int color = Piece.getColor(piece);
            board[index] = piece;
            pieces[color][pieceCounts[color]] = index;
            pieceCounts[color]++;
        }
    }

    public void movePiece(int start, int end) {
        if (board[end] != EMPTY) {
            removePiece(end);
        }
        board[end] = board[start];
        board[start] = EMPTY;
        if (board[end] != EMPTY) {
            int color = Piece.getColor(board[end]);
            for (int i = 0; i < pieceCounts[color]; i++) {
                if (pieces[color][i] == start) {
                    pieces[color][i] = end;
                    break;
                }
            }
        }
    }

    public void removePiece(int index) {
        int piece = board[index];
        if (piece != EMPTY) {
            int color = Piece.getColor(piece);
            board[index] = EMPTY;
            for (int i = 0; i < pieceCounts[color]; i++) {
                if (pieces[color][i] == index) {
                    pieces[color][i] = pieces[color][pieceCounts[color] - 1]; // swap with last piece to avoid holes
                    pieces[color][pieceCounts[color] - 1] = EMPTY;
                    break;
                }
            }
            pieceCounts[color]--;
        }
    }

    public boolean isChecked() {
        if (kingInd == EMPTY) {
            return true;
        }
        return threatening[kingInd];
    }

    public boolean isCheckmate() {
        return isChecked() && moves.size() == 0;
    }

    public boolean isStalemate() {
        if (pieceCounts[0] == 1 && pieceCounts[1] == 1) { // only kings remain
            return true;
        }
        if (kingInd != EMPTY) {
            if (!threatening[kingInd]) {
                return moves.size() == 0;
            } else {
                return false;
            }
        }
        return false;
    }

    public void updatePinningAndCheck() {
        pins = new boolean[64];
        checkPath = new boolean[64];
        multiCheck = false;
        singleCheck = false;

        if (kingInd != EMPTY) {
            for (int offInd = 0; offInd < FastMoveGenerator.ALL_OFFSETS.length; offInd++) {
                int offset = FastMoveGenerator.ALL_OFFSETS[offInd];
                int curr = kingInd + offset;
                int offCount = 1;
                int pinning = EMPTY;
                for (int j = 0; j < PrecomputedMoveData.distToEdge[kingInd][offInd]; j++) {
                    int piece = board[curr];
                    if (piece != EMPTY) {
                        int color = Piece.getColor(piece);
                        int type = Piece.getType(piece);
                        if (color == activeColor) {
                            if (pinning == EMPTY) {
                                pinning = curr;
                            } else {
                                break;
                            }
                        } else {
                            boolean isPin = type == Piece.QUEEN;
                            isPin |= type == Piece.ROOK && (offset == -1 || offset == 1 || offset == 8 || offset == -8);
                            isPin |= type == Piece.BISHOP && (offset == -9 || offset == 9 || offset == 7 || offset == -7);
                            isPin |= offCount == 1 && type == Piece.PAWN && (offset == -9 || offset == 9 || offset == 7 || offset == -7);

                            if (pinning != EMPTY && isPin) {
                                for (int i = 0; i < offCount; i++) {
                                    pins[curr + offset * -1 * i] = true;
                                }
                            } else if (pinning == EMPTY) {
                                if (isPin) {
                                    if (singleCheck) {
                                        multiCheck = true;
                                    }
                                    for (int i = 0; i < offCount; i++) {
                                        checkPath[curr + offset * -1 * i] = true;
                                    }
                                    singleCheck = true;
                                }
                            }
                            break;
                        }
                    }
                    curr += offset;
                    offCount++;
                }
            }
        }
    }

    public void updateThreatening() {
        threatening = new boolean[64];
        pawnThreats = new boolean[64];
        threatCount = 0;
        ArrayList<FastMove> threats = new FastMoveGenerator().generateThreats(this);
        for (FastMove threat : threats) {
            if (!threatening[threat.end]) {
                threatening[threat.end] = true;
                threatCount++;
            }
        }

        int color = Piece.getOpposite(activeColor);
        for (int i = 0; i < pieceCounts[color]; i++) {
            int piece = board[pieces[color][i]];
            int type = Piece.getType(piece);
            int[] moves = null;
            if (type == Piece.KING) {
                moves = PrecomputedMoveData.kingMoves[pieces[color][i]];
            } else if (type == Piece.PAWN) {
                if (color == Piece.BLACK) {
                    moves = PrecomputedMoveData.blackPawnAttacks[pieces[color][i]];
                } else {
                    moves = PrecomputedMoveData.whitePawnAttacks[pieces[color][i]];
                }
            } else if (type == Piece.KNIGHT) {
                moves = PrecomputedMoveData.knightMoves[pieces[color][i]];
            }

            if (moves != null) {
                for (int j = 0; j < moves.length; j++) {
                    if (!threatening[moves[j]]) {
                        threatening[moves[j]] = true;
                        threatCount++;
                    }
                    if (moves[j] == kingInd && type == Piece.KNIGHT) {
                        checkPath[pieces[color][i]] = true;
                        if (singleCheck) {
                            multiCheck = true;
                        }
                        singleCheck = true;
                    }
                    if (type == Piece.PAWN) {
                        pawnThreats[moves[j]] = true;
                    }
                }
            }
        }
    }

    public void updateInfo() {
        kingInd = getKingInd(activeColor);
        updatePinningAndCheck();
        updateThreatening();
        moves = new FastMoveGenerator().generateMoves(this);
    }

    public void makeMove(FastMove move) {
        // handle en passant
        enPassantable = EMPTY;
        if (Piece.getType(move.actor) == Piece.PAWN && !hasMoved[move.start] && Math.abs(move.end - move.start) == 16) {
            enPassantable = move.end + (Piece.getColor(move.actor) == Piece.WHITE ? 8 : -8);
        }

        hasMoved[move.start] = true;
        movePiece(move.start, move.end);

        if (move.type == Move.CASTLE) {
            // if a castle, captured represents index of rook
            int offset = move.start - move.end > 0 ? 1 : -1;
            movePiece(move.captured, move.end + offset);
        } else if (move.type == Move.PROMOTION) {
            board[move.end] = move.promoteTo;
        } else if (move.type == Move.EN_PASSANT) {
            int row = move.start / 8;
            int col = move.end % 8;
            removePiece(row * 8 + col);
        }

        activeColor = Piece.getOpposite(activeColor);
        updateInfo();
    }

    public void unmakeMove(FastMove move) {
        movePiece(move.end, move.start);

        if (game.moveHistory.size() > 1) {
            FastMove lastMove = game.moveHistory.get(game.moveHistory.size() - 2);
            if (Piece.getType(lastMove.actor) == Piece.PAWN && lastMove.firstMove && Math.abs(lastMove.end - lastMove.start) == 16) {
                enPassantable = lastMove.end + (Piece.getColor(lastMove.actor) == Piece.WHITE ? 8 : -8);
            }
        }

        if (move.firstMove) {
            hasMoved[move.start] = false;
        }

        if (move.type == Move.EN_PASSANT) {
            int row = move.start / 8;
            int col = move.end % 8;
            addPiece(move.captured, row * 8 + col);
            enPassantable = move.end;
        } else if (move.type == Move.CASTLE) {
            // if a castle, captured represents original index of rook
            int offset = move.start - move.end > 0 ? 1 : -1;
            movePiece(move.end + offset, move.captured);
        } else if (move.type == Move.PROMOTION) {
            board[move.start] = move.actor;
        }

        if (move.type != Move.EN_PASSANT && move.type != Move.CASTLE) {
            addPiece(move.captured, move.end);
        }

        activeColor = Piece.getOpposite(activeColor);
        updateInfo();
    }

    public static String coorConvert(int index) {
        int col = index % 8;
        int row = index / 8;
        return "" + (char)(col + 'a') + (8 - row);
    }

    public int getKingInd(int color) {
        for (int i = 0; i < pieceCounts[color]; i++) {
            if (Piece.getType(board[pieces[color][i]]) == Piece.KING) {
                return pieces[color][i];
            }
        }
        return EMPTY;
    }

    public String toString() {
        StringBuilder fen = new StringBuilder();
        int empty = 0;
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                int ind = i * 8 + j;
                int sq = board[ind];
                if (sq != EMPTY) {
                    if (empty > 0) {
                        fen.append(empty);
                    }
                    empty = 0;
                    fen.append(Piece.getFenChar(sq));
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
        if (FastMoveGenerator.canCastle(kingInd, -2, this) != -1) {
            fen.append(activeColor == Piece.WHITE ? "Q" : "q");
        }
        if (FastMoveGenerator.canCastle(getKingInd(Piece.getOpposite(activeColor)), 2, this) != -1) {
            fen.append(activeColor == Piece.BLACK ? "K" : "k");
        }
        fen.append(" ");
        if (enPassantable == EMPTY) {
            fen.append("-");
        } else {
            fen.append(FastBoard.coorConvert(enPassantable));
        }
        return fen.toString();
    }

    public int getBasicMaterialScore(int color) {
        int total = 0;
        for (int i = 0; i < pieceCounts[color]; i++) {
            total += Piece.getWeight(board[pieces[color][i]] & 0b111);
        }
        return total;
    }
}
