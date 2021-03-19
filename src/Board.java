import java.util.ArrayList;

public class Board {
    static final String DEFAULT_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

    int[] board; // stores the int value of the piece at the index
    int[][] pieces; // stores index of pieces of each colors
    int[] pieceCounts; // stores the number of pieces of each color
    boolean[] pins; // stores if square is part of pin
    boolean[] threatening; // stores if square is threatened
    boolean[] pawnThreats; // stores if pawn threatening square
    int threatCount;
    boolean[] hasMoved; // stores if any changes made at square
    ArrayList<Move> moves;
    boolean multiCheck, singleCheck;
    boolean[] checkPath; // stores if square is part of check path
    int activeColor, enPassantable, kingInd;
    Game game;
    long zobristKey;

    static final int EMPTY = -1;

    public Board() {
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

    public Board(String fen, Game game) {
        this();
        this.game = game;
        int index = 0;
        String[] fields = fen.split(" ");

        activeColor = fields[1].equals("w") ? Piece.WHITE : Piece.BLACK;
        for (int i = 0; i < fields[0].length(); i++) {
            char c = fields[0].charAt(i);
            int color = Character.isLowerCase(c) ? Piece.BLACK : Piece.WHITE;
            c = Character.toUpperCase(c);
            int type = -1;
            switch (c) {
                case 'P':
                    type = Piece.PAWN;
                    break;
                case 'N':
                    type = Piece.KNIGHT;
                    break;
                case 'B':
                    type = Piece.BISHOP;
                    break;
                case 'R':
                    type = Piece.ROOK;
                    break;
                case 'Q':
                    type = Piece.QUEEN;
                    break;
                case 'K':
                    type = Piece.KING;
                    break;
                case '/':
                    index--;
                    break;
                default:
                    int length = Integer.parseInt("" + c);
                    for (int j = 0; j < length; j++) {
                        board[index + j] = EMPTY;
                    }
                    index += length - 1;

                    break;
            }
            addPiece(color << 3 | type, index);
            index++;
        }

        activeColor = fields[1].equals("w") ? Piece.WHITE : Piece.BLACK;

        // castling
        hasMoved[0] = !fields[2].contains("q");
        hasMoved[7] = !fields[2].contains("k");
        hasMoved[56] = !fields[2].contains("Q");
        hasMoved[63] = !fields[2].contains("K");

        enPassantable = coorConvert(fields[3]);

        zobristKey = Zobrist.calcKey(this);
        updateInfo();
    }

    public Board(Board from) {
        this();
        moves = new ArrayList<>(from.moves.size());
        for (Move move : from.moves) {
            moves.add(new Move(move));
        }
        this.threatCount = from.threatCount;
        this.game = from.game;
        this.enPassantable = from.enPassantable;
        this.multiCheck = from.multiCheck;
        this.singleCheck = from.singleCheck;
        this.activeColor = from.activeColor;
        this.kingInd = from.kingInd;
        this.zobristKey = from.zobristKey;

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
        if (board[start] != EMPTY) {
            int color = Piece.getColor(board[start]);
            int type = Piece.getType(board[start]);
            zobristKey ^= Zobrist.piecesArr[type][color][start]; // unset start
        }
        if (board[end] != EMPTY) {
            int type = Piece.getType(board[end]);
            int color = Piece.getColor(board[end]);
            zobristKey ^= Zobrist.piecesArr[type][color][end]; // unset old end
            removePiece(end);
        }

        board[end] = board[start];
        board[start] = EMPTY;
        if (board[end] != EMPTY) {
            int color = Piece.getColor(board[end]);
            int type = Piece.getType(board[end]);
            zobristKey ^= Zobrist.piecesArr[type][color][end]; // set end
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
            for (int offInd = 0; offInd < MoveGenerator.ALL_OFFSETS.length; offInd++) {
                int offset = MoveGenerator.ALL_OFFSETS[offInd];
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
        ArrayList<Move> threats = new MoveGenerator().generateThreats(this);
        for (Move threat : threats) {
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
        moves = new MoveGenerator().generateMoves(this);
    }

    public void makeMove(Move move) {
        int oldCastleRights = getCastleRights();
        // handle en passant
        if (enPassantable != EMPTY) {
            int enCol = enPassantable % 8 + 1;
            zobristKey ^= Zobrist.enPassantFile[enCol]; // unset old en passant
            enPassantable = EMPTY;
        }
        if (Piece.getType(move.actor) == Piece.PAWN && !hasMoved[move.start] && Math.abs(move.end - move.start) == 16) {
            enPassantable = move.end + (Piece.getColor(move.actor) == Piece.WHITE ? 8 : -8);
            int enCol = enPassantable % 8 + 1;
            zobristKey ^= Zobrist.enPassantFile[enCol]; // set new en passant
        }

        hasMoved[move.start] = true;
        movePiece(move.start, move.end);

        if (move.type == Move.CASTLE) {
            // if a castle, captured represents index of rook
            int offset = move.start - move.end > 0 ? 1 : -1;
            movePiece(move.captured, move.end + offset);
        } else if (move.type == Move.PROMOTION) {
            int color = Piece.getColor(move.actor);
            zobristKey ^= Zobrist.piecesArr[Piece.PAWN][color][move.end]; // unset pawn
            zobristKey ^= Zobrist.piecesArr[Piece.getType(move.promoteTo)][color][move.end]; // set new type
            board[move.end] = move.promoteTo;
        } else if (move.type == Move.EN_PASSANT) {
            int row = move.start / 8;
            int col = move.end % 8;
            zobristKey ^= Zobrist.piecesArr[Piece.PAWN][Piece.getColor(move.captured)][row * 8 + col];
            removePiece(row * 8 + col);
        }

        activeColor = Piece.getOpposite(activeColor);
        updateInfo();

        int newCastleRights = getCastleRights();
        if (newCastleRights != oldCastleRights) {
            zobristKey ^= Zobrist.castlingRights[oldCastleRights]; // unset old castling rights
            zobristKey ^= Zobrist.castlingRights[newCastleRights]; // set new castling rights
        }
        zobristKey ^= Zobrist.activeColor;
        game.repeatHistory.push(zobristKey);
    }

    public void unmakeMove(Move move) {
        int oldCastleRights = getCastleRights();
        if (move.type != Move.PROMOTION) {
            movePiece(move.end, move.start);
        }

        if (enPassantable != EMPTY) {
            int enCol = enPassantable % 8 + 1;
            zobristKey ^= Zobrist.enPassantFile[enCol]; // unset old en passant
            enPassantable = EMPTY;
        }

        if (game.moveHistory.size() > 1) {
            Move lastMove = game.moveHistory.get(game.moveHistory.size() - 2);
            if (Piece.getType(lastMove.actor) == Piece.PAWN && lastMove.firstMove && Math.abs(lastMove.end - lastMove.start) == 16) {
                enPassantable = lastMove.end + (Piece.getColor(lastMove.actor) == Piece.WHITE ? 8 : -8);
                int enCol = enPassantable % 8 + 1;
                zobristKey ^= Zobrist.enPassantFile[enCol]; // set new en passant
            }
        }

        if (move.firstMove) {
            hasMoved[move.start] = false;
        }

        if (move.type == Move.EN_PASSANT) {
            int row = move.start / 8;
            int col = move.end % 8;
            addPiece(move.captured, row * 8 + col);
            zobristKey ^= Zobrist.piecesArr[Piece.PAWN][Piece.getColor(move.captured)][row * 8 + col];
        } else if (move.type == Move.CASTLE) {
            // if a castle, captured represents original index of rook
            int offset = move.start - move.end > 0 ? 1 : -1;
            movePiece(move.end + offset, move.captured);
        } else if (move.type == Move.PROMOTION) {
            int color = Piece.getColor(move.actor);
            zobristKey ^= Zobrist.piecesArr[Piece.getType(move.promoteTo)][color][move.end]; // unset new type
            zobristKey ^= Zobrist.piecesArr[Piece.PAWN][color][move.start]; // set pawn
            removePiece(move.end);
            addPiece(move.actor, move.start);
            board[move.end] = EMPTY;
        }

        if (move.type != Move.EN_PASSANT && move.type != Move.CASTLE) {
            addPiece(move.captured, move.end);
            if (move.captured != -1) {
                zobristKey ^= Zobrist.piecesArr[Piece.getType(move.captured)][Piece.getColor(move.captured)][move.end]; // set captured piece
            }
        }

        activeColor = Piece.getOpposite(activeColor);
        updateInfo();
        if (game.repeatHistory.size() > 0) {
            game.repeatHistory.pop();
        }

        int newCastleRights = getCastleRights();
        if (newCastleRights != oldCastleRights) {
            zobristKey ^= Zobrist.castlingRights[oldCastleRights]; // unset old castling rights
            zobristKey ^= Zobrist.castlingRights[newCastleRights]; // set new castling rights
        }
        zobristKey ^= Zobrist.activeColor;
    }

    public static String coorConvert(int index) {
        int col = index % 8;
        int row = index / 8;
        return "" + (char)(col + 'a') + (8 - row);
    }

    public static int coorConvert(String sq) {
        int index = -1;
        sq = sq.toLowerCase();
        if (sq.length() > 2) {
            int col = sq.charAt(0) - 'a';
            int row = 8 - Integer.parseInt(sq.substring(1));
            index = row * 8 + col;
        }
        return index;
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
        int castleRights = getCastleRights();
        if (castleRights == 0) {
            fen.append("-");
        } else {
            if ((castleRights & 0b1000) != 0) {
                fen.append("Q");
            }
            if ((castleRights & 0b0100) != 0) {
                fen.append("K");
            }
            if ((castleRights & 0b0010) != 0) {
                fen.append("q");
            }
            if ((castleRights & 0b0001) != 0) {
                fen.append("k");
            }
        }
        fen.append(" ");
        if (enPassantable == EMPTY) {
            fen.append("-");
        } else {
            fen.append(Board.coorConvert(enPassantable));
        }
        return fen.toString();
    }

    public int getCastleRights() {
        int whiteInd = activeColor == Piece.WHITE ? kingInd : getKingInd(Piece.getOpposite(activeColor));
        int blackInd = activeColor == Piece.BLACK ? kingInd : getKingInd(Piece.getOpposite(activeColor));
        int castleRights = 0;
        castleRights |= MoveGenerator.canCastle(whiteInd, -2, this) != -1 ? 1 << 3 : 0;
        castleRights |= MoveGenerator.canCastle(whiteInd, 2, this) != -1 ? 1 << 2 : 0;
        castleRights |= MoveGenerator.canCastle(blackInd, -2, this) != -1 ? 1 << 1 : 0;
        castleRights |= MoveGenerator.canCastle(blackInd, 2, this) != -1 ? 1 : 0;
        return castleRights;
    }

    public int getBasicMaterialScore(int color) {
        int total = 0;
        for (int i = 0; i < pieceCounts[color]; i++) {
            total += Piece.getWeight(board[pieces[color][i]] & 0b111);
        }
        return total;
    }
}
