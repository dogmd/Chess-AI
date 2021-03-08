import java.util.*;

public class MoveGenerator {
    public static final int[][] KNIGHT_MOVES = new int[][]{
            new int[]{2, 1}, new int[]{2, -1}, new int[]{-2, 1}, new int[]{-2, -1},
            new int[]{1, 2}, new int[]{-1, 2}, new int[]{1, -2}, new int[]{-1, -2}
    };
    public static final int[][] BISHOP_MOVES = new int[][]{
            new int[]{1, 1}, new int[]{-1, 1}, new int[]{1, -1}, new int[]{-1, -1}
    };
    public static final int[][] ROOK_MOVES = new int[][]{
            new int[]{0, 1}, new int[]{0, -1}, new int[]{1, 0}, new int[]{-1, 0}
    };
    public static final int[][] QUEEN_MOVES = new int[][]{
            new int[]{0, 1}, new int[]{0, -1}, new int[]{1, 0}, new int[]{-1, 0},
            new int[]{1, 1}, new int[]{-1, 1}, new int[]{1, -1}, new int[]{-1, -1}
    };
    public static final int[][] KING_MOVES = new int[][]{
            new int[]{0, 1}, new int[]{0, -1}, new int[]{1, 0}, new int[]{-1, 0},
            new int[]{1, 1}, new int[]{-1, 1}, new int[]{1, -1}, new int[]{-1, -1}
    };

    public static final PieceType[] PROMOTE_OPTIONS = new PieceType[]{PieceType.BISHOP, PieceType.QUEEN, PieceType.ROOK, PieceType.KNIGHT};
    public static final int[] CASTLE_DIRECTIONS = new int[]{-2, 2};


    public static ArrayList<Move> generateMoves(Board board, Piece p, boolean includeThreats) {
        ArrayList<Move> moves = new ArrayList<>();
        switch (p.type) {
            case BISHOP:
                moves = generateTracedMoves(board, p, BISHOP_MOVES, includeThreats);
                break;
            case ROOK:
                moves = generateTracedMoves(board, p, ROOK_MOVES, includeThreats);
                break;
            case QUEEN:
                moves = generateTracedMoves(board, p, QUEEN_MOVES, includeThreats);
                break;
            case PAWN:
                moves = generatePawnMoves(board, p, includeThreats);
                break;
            case KNIGHT:
                moves = generateKnightMoves(board, p, includeThreats);
                break;
            case KING:
                moves = generateKingMoves(board, p, includeThreats);
                break;
        }
        moves.removeIf(Objects::isNull);
        if ((p.type == PieceType.KING && board.activeColor == p.color) || board.isPinned(p) || (board.isChecked() && p.color == board.activeColor)) {
            moves.removeIf(move -> (resultsInCheck(board, move)));
        }
        return moves;
    }

    public static ArrayList<Move> generateMoves(Board board, Piece p) {
        return generateMoves(board, p, false);
    }

    public static boolean resultsInCheck(Board board, Move move) {
        for (Pin pin : board.pins) {
            if (pin.piece == move.actor && !pin.path.contains(move.end)) {
                return true;
            }
        }
        if (move.actor.type == PieceType.KING) {
            // Check for continuing checks
            int xDiff = move.end.col - move.start.col;
            int yDiff = move.end.row - move.start.row;
            for (int i = 0; i < board.checkVectors.length; i++) {
                if (board.checkVectors[i]) {
                    int xOff = KING_MOVES[i][0];
                    int yOff = KING_MOVES[i][1];
                    if (xOff == 0) {
                        yOff *= -1;
                    } else if (yOff == 0) {
                        xOff *= -1;
                    } else if (xOff != yOff) {
                        int temp = xOff;
                        xOff = yOff;
                        yOff = temp;
                    } else {
                        xOff *= -1;
                        yOff *= -1;
                    }

                    if (xDiff == xOff && yDiff == yOff) {
                        return true;
                    }
                }
            }
            return board.threatening.contains(move.end);
        } else if (board.multiCheck) {
            return true;
        } else if (board.isChecked()) {
            return !board.checkPath.contains(move.end);
        }
        return false;
    }

    public static ArrayList<Move> generateTracedMoves(Board board, Piece p, int[][] moveList, boolean includeThreats) {
        ArrayList<Move> moves = new ArrayList<>();
        for (int[] offsets : moveList) {
            moves.addAll(tracePath(board, p, offsets[0], offsets[1], includeThreats));
        }
        return moves;
    }

    public static ArrayList<Move> generateKnightMoves(Board board, Piece p, boolean includeThreatening) {
        ArrayList<Move> moves = new ArrayList<>();
        for (int[] offsets : KNIGHT_MOVES) {
            moves.add(offsetPath(board, p, offsets[0], offsets[1], includeThreatening));
        }
        return moves;
    }

    public static ArrayList<Move> generateKingMoves(Board board, Piece p, boolean includeThreatening) {
        ArrayList<Move> moves = new ArrayList<>();
        if (p.type == PieceType.KING) {
            King king = (King) p;
            for (int offset : CASTLE_DIRECTIONS) {
                Piece rook = canCastle(board, king, offset);
                if (rook != null) {
                    Move castle = new Move(king, rook, king.square, board.board[king.square.row][king.square.col + offset]);
                    castle.isThreat = false;
                    castle.type = Move.CASTLE;
                    moves.add(castle);
                }
            }
            for (int[] offsets : KING_MOVES) {
                moves.add(offsetPath(board, king, offsets[0], offsets[1], includeThreatening));
            }
        }
        return moves;
    }

    public static Piece canCastle(Board board, King king, int direction) {
        int row = king.square.row;
        int col = 0;
        if (direction > 0) {
            col = 7;
        }
        if ((direction < 0 && !king.queenCastle) || (direction > 0 && !king.kingCastle)) {
            return null;
        }
        if (!king.hasMoved && board.board[row][col].isOccupied()) {
            Piece rook = board.board[row][col].piece;
            int offset = direction > 0 ? 1 : -1;
            if (rook.type == PieceType.ROOK && !rook.hasMoved) {
                int testCol = king.square.col + offset;
                while (testCol != col) {
                    if (board.board[row][testCol].isOccupied()) {
                        return null;
                    }
                    testCol += offset;
                }
                for (int i = 0; i <= Math.abs(direction); i++) {
                    if (i != 0 && board.board[row][king.square.col + i * offset].isOccupied()) {
                        return null;
                    }
                    if (board.threatening.contains(board.board[row][king.square.col + i * offset])) {
                        return null;
                    }
                }
                return rook;
            }
        }
        return null;
    }

    public static ArrayList<Move> generatePawnMoves(Board board, Piece p, boolean includeThreats) {
        ArrayList<Move> moves = new ArrayList<>();
        if (p.type == PieceType.PAWN) {
            Pawn pawn = (Pawn) p;
            int direction = p.color == Piece.BLACK ? 1 : -1;

            int row = p.square.row;
            int col = p.square.col;
            // advancement
            if (row + direction < 8 && row + direction >= 0 && !board.board[row + direction][col].isOccupied()) {
                Move move = offsetPath(board, p, 0, direction, includeThreats);
                if (move != null) {
                    move.isThreat = false;
                }
                moves.add(move);
            }
            boolean canDouble = (pawn.color == Piece.BLACK && row == 1) || (pawn.color == Piece.WHITE && row == 6);
            if (canDouble && !board.board[row + direction * 2][col].isOccupied() && !board.board[row + direction][col].isOccupied()) {
                Move move = offsetPath(board, p, 0, direction * 2, includeThreats);
                if (move != null) {
                    move.isThreat = false;
                }
                moves.add(move);
            }

            if (row + direction >= 0 && row + direction < 8) {
                // captures
                if (col > 0 && (includeThreats || board.board[row + direction][col - 1].isOccupied())) {
                    moves.add(offsetPath(board, p, -1, direction, includeThreats));
                }
                if (col < 7 && (includeThreats || board.board[row + direction][col + 1].isOccupied())) {
                    moves.add(offsetPath(board, p, 1, direction, includeThreats));
                }
                // en passant
                moves.add(generateEnPassant(board, p, -1));
                moves.add(generateEnPassant(board, p, 1));
            }
        }
        if ((p.square.row == 1 && p.color == Piece.WHITE) || (p.square.row == 6 && p.color == Piece.BLACK)) {
            for (int i = moves.size() - 1; i >= 0; i--) {
                Move move = moves.get(i);
                if (move != null && move.actor != null) {
                    int color = move.actor.color;
                    int row = move.end.row;
                    if ((color == Piece.WHITE && row == 0) || (color == Piece.BLACK && row == 7)) {
                        for (PieceType type : PROMOTE_OPTIONS) {
                            Move newMove = new Move(move.actor, move.captured, move.start, move.end);
                            newMove.type = Move.PROMOTION;
                            newMove.promoteTo = type;
                            moves.add(newMove);
                        }
                        moves.remove(i);
                    }
                }
            }
        }
        return moves;
    }

    public static Move generateEnPassant(Board board, Piece p, int offset) {
        int row = p.square.row;
        int col = p.square.col;
        int direction = p.color == Piece.BLACK ? 1 : -1;
        if (col + offset > 0 && col + offset < 8) {
            if (board.board[row][col + offset].isOccupied() && board.board[row][col + offset].piece.type == PieceType.PAWN) {
                Pawn adjPawn = (Pawn) board.board[row][col + offset].piece;
                if (adjPawn.enPassantable) {
                    Move move = new Move(p, adjPawn, p.square, board.board[row + direction][col + offset]);
                    move.type = Move.EN_PASSANT;
                    for (int i = 1; i < 8; i++) {
                        int newCol = col + i * offset;
                        if (newCol > 0 && newCol <= 7) {
                            Square sq = board.board[row][newCol];
                            if (sq.isOccupied()) {
                                if ((sq.piece.color != p.color) && sq.piece.type == PieceType.ROOK || sq.piece.type == PieceType.QUEEN) {
                                    return null;
                                }
                            }
                        }
                    }
                    return move;
                }
            }
        }
        return null;
    }

    public static Move offsetPath(Board b, Piece p, int xDir, int yDir, boolean includeThreats) {
        int row = p.square.row + yDir;
        int col = p.square.col + xDir;
        if (col >= 0 && col < 8 && row >= 0 && row < 8) {
            if (b.board[row][col].isOccupied() && b.board[row][col].piece.color == p.color) {
                if (includeThreats) {
                    return new Move(p.square, b.board[row][col]);
                }
                return null;
            }
            return new Move(p.square, b.board[row][col]);
        }
        return null;
    }

    private static ArrayList<Move> tracePath(Board b, Piece p, int xDir, int yDir, boolean includeThreats) {
        ArrayList<Move> path = new ArrayList<>();
        int row = p.square.row + yDir;
        int col = p.square.col + xDir;
        while (col >= 0 && col < 8 && row >= 0 && row < 8) {
            if (b.board[row][col].isOccupied()) {
                if (b.board[row][col].piece.color != p.color) {
                    path.add(new Move(p.square, b.board[row][col]));
                } else if (includeThreats) {
                    path.add(new Move(p.square, b.board[row][col]));
                    break;
                }
                break;
            } else {
                path.add(new Move(p.square, b.board[row][col]));
                col += xDir;
                row += yDir;
            }
        }
        return path;
    }
}
