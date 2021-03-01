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


    public static ArrayList<Move> generateMoves(Board board, Piece p, boolean includeCovering) {
        ArrayList<Move> moves = new ArrayList<>();
        switch (p.type) {
            case BISHOP:
                moves = generateTracedMoves(board, p, BISHOP_MOVES, includeCovering);
                break;
            case ROOK:
                moves = generateTracedMoves(board, p, ROOK_MOVES, includeCovering);
                break;
            case QUEEN:
                moves = generateTracedMoves(board, p, QUEEN_MOVES, includeCovering);
                break;
            case PAWN:
                moves = generatePawnMoves(board, p, includeCovering);
                break;
            case KNIGHT:
                moves = generateKnightMoves(board, p, includeCovering);
                break;
            case KING:
                moves = generateKingMoves(board, p, includeCovering);
                break;
        }
        moves.removeIf(Objects::isNull);
        if (!includeCovering && (board.isChecked(p.color) || p.isPinned())) {
            for (int i = moves.size() - 1; i >= 0; i--) {
                if (resultsInCheck(board, moves.get(i), p.color)) {
                    moves.remove(i);
                }
            }
        }
        return moves;
    }

    public static boolean resultsInCheck(Board board, Move move, int color) {
        Board test = new Board(board);
        Move newMove = test.translateMove(move);
        test.makeMove(newMove, true);
        return test.isChecked(color);
    }

    public static ArrayList<Move> generateMoves(Board board, Piece p) {
        return generateMoves(board, p, false);
    }

    public static ArrayList<Move> generateTracedMoves(Board board, Piece p, int[][] moveList, boolean includeCovering) {
        ArrayList<Move> moves = new ArrayList<>();
        for (int[] offsets : moveList) {
            moves.addAll(tracePath(board, p, offsets[0], offsets[1], includeCovering));
        }
        return moves;
    }

    public static ArrayList<Move> generateKnightMoves(Board board, Piece p, boolean includeCovering) {
        ArrayList<Move> moves = new ArrayList<>();
        for (int[] offsets : KNIGHT_MOVES) {
            moves.add(offsetPath(board, p, offsets[0], offsets[1], includeCovering));
        }
        return moves;
    }

    public static ArrayList<Move> generateKingMoves(Board board, Piece p, boolean includeCovering) {
        ArrayList<Move> moves = new ArrayList<>();
        if (p.type == PieceType.KING) {
            King king = (King) p;
            for (int offset : CASTLE_DIRECTIONS) {
                Piece rook = canCastle(board, king, offset);
                if (rook != null) {
                    Move castle = new Move(king, rook, king.square, board.board[king.square.row][king.square.col + offset]);
                    castle.type = Move.CASTLE;
                    moves.add(castle);
                }
            }
            for (int[] offsets : KING_MOVES) {
                Move move = offsetPath(board, king, offsets[0], offsets[1], includeCovering);
                if (move != null && !includeCovering) {
                    for (Piece piece : move.end.threatenedBy) {
                        if (piece.color != p.color) {
                            move = null;
                            break;
                        }
                    }
                }
                moves.add(move);
            }
        }
        return moves;
    }

    public static Piece canCastle(Board board, King king, int direction) {
        int color = king.color;
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
                for (int i = 0; i <= Math.abs(direction); i++) {
                    if (i != 0 && board.board[row][king.square.col + i * offset].isOccupied()) {
                        return null;
                    }
                    Set<Piece> threats = board.board[row][king.square.col + i * offset].threatenedBy;
                    for (Piece piece : threats) {
                        if (piece.color != color) {
                            return null;
                        }
                    }
                }
                return rook;
            }
        }
        return null;
    }

    public static ArrayList<Move> generatePawnMoves(Board board, Piece p, boolean includeCovering) {
        ArrayList<Move> moves = new ArrayList<>();
        if (p.type == PieceType.PAWN) {
            Pawn pawn = (Pawn) p;
            int direction = p.color == Piece.BLACK ? 1 : -1;

            List<int[]> allMoves = new ArrayList<>();
            int row = p.square.row;
            int col = p.square.col;
            // advancement
            if (!includeCovering) {
                if (row + direction < 8 && row + direction >= 0 && !board.board[row + direction][col].isOccupied()) {
                    allMoves.add(new int[]{0, direction});
                }
                boolean canDouble = (pawn.color == Piece.BLACK && row == 1) || (pawn.color == Piece.WHITE && row == 6);
                if (canDouble && !board.board[row + direction * 2][col].isOccupied() && !board.board[row + direction][col].isOccupied()) {
                    allMoves.add(new int[]{0, direction * 2});
                }
            }

            if (row + direction >= 0 && row + direction < 8) {
                // captures
                if (col > 0 && (board.board[row + direction][col - 1].isOccupied() || includeCovering)) {
                    allMoves.add(new int[]{-1, direction});
                }
                if (col < 7 && (board.board[row + direction][col + 1].isOccupied() || includeCovering)) {
                    allMoves.add(new int[]{1, direction});
                }
                // en passant
                if (col > 0 && board.board[row][col - 1].isOccupied() && board.board[row][col - 1].piece.type == PieceType.PAWN) {
                    Pawn adjPawn = (Pawn) board.board[row][col - 1].piece;
                    if (adjPawn.enPassantable) {
                        Move move = new Move(p, adjPawn, p.square, board.board[row + direction][col - 1]);
                        move.type = Move.EN_PASSANT;
                        moves.add(move);
                    }
                }
                if (col < 7 && board.board[row][col + 1].isOccupied() && board.board[row][col + 1].piece.type == PieceType.PAWN) {
                    Pawn adjPawn = (Pawn) board.board[row][col + 1].piece;
                    if (adjPawn.enPassantable) {
                        Move move = new Move(p, adjPawn, p.square, board.board[row + direction][col + 1]);
                        move.type = Move.EN_PASSANT;
                        moves.add(move);
                    }
                }
            }

            for (int[] offsets : allMoves) {
                moves.add(offsetPath(board, p, offsets[0], offsets[1], includeCovering));
            }
        }
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
        return moves;
    }

    public static Move offsetPath(Board b, Piece p, int xDir, int yDir, boolean includeCovering) {
        int row = p.square.row + yDir;
        int col = p.square.col + xDir;
        if (col >= 0 && col < 8 && row >= 0 && row < 8) {
            if (b.board[row][col].isOccupied() && b.board[row][col].piece.color == p.color) {
                if (includeCovering) {
                    return new Move(p.square, b.board[row][col]);
                }
                return null;
            }
            Move move = new Move(p.square, b.board[row][col]);
            return move;
        }
        return null;
    }

    private static ArrayList<Move> tracePath(Board b, Piece p, int xDir, int yDir, boolean includeCovering) {
        ArrayList<Move> path = new ArrayList<>();
        int row = p.square.row + yDir;
        int col = p.square.col + xDir;
        boolean pinning = false;
        while (col >= 0 && col < 8 && row >= 0 && row < 8) {
            if (b.board[row][col].isOccupied()) {
                if (b.board[row][col].piece.color != p.color && !pinning) {
                    path.add(new Move(p.square, b.board[row][col]));
                    pinning = true;
                } else if (includeCovering) {
                    path.add(new Move(p.square, b.board[row][col]));
                    break;
                } else {
                    break;
                }
            } else if (!pinning) {
                path.add(new Move(p.square, b.board[row][col]));
                col += xDir;
                row += yDir;
            }
        }
        return path;
    }

    public static Piece getPinned(Board b, Piece p) {
        int[][] moveList = null;
        switch (p.type) {
            case BISHOP:
                moveList = BISHOP_MOVES;
                break;
            case ROOK:
                moveList = ROOK_MOVES;
                break;
            case QUEEN:
                moveList = QUEEN_MOVES;
                break;
            default:
                return null;
        }
        for (int[] offsets : moveList) {
            int row = p.square.row + offsets[1];
            int col = p.square.col + offsets[0];
            Piece pinned = null;
            while (col >= 0 && col < 8 && row >= 0 && row < 8) {
                Square sq = b.board[row][col];
                if (sq.isOccupied()) {
                    if (sq.piece.color != p.color && pinned == null) {
                        pinned = sq.piece;
                    } else if (pinned != null && sq.piece.color != p.color && sq.piece.type == PieceType.KING) {
                        return pinned;
                    } else if (pinned != null) {
                        break;
                    }
                }
                col += offsets[0];
                row += offsets[1];
            }
        }
        return null;
    }
}
