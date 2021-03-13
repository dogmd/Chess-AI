import java.util.ArrayList;

// Translates some parts from https://github.com/SebLague/Chess-AI/blob/d0832f8f1d32ddfb95525d1f1e5b772a367f272e/Assets/Scripts/Core/MoveGenerator.cs

public class MoveGenerator {
    public static final int[] CASTLE_DIRECTIONS = new int[]{-2, 2};
    public static final int[] ALL_OFFSETS = new int[]{8, -8, -1, 1, 7, -7, 9, -9};
    public static final int[][] PAWN_ATTACKS = new int[][]{new int[]{7, 5}, new int[]{4, 6}};
    public static final int[] PROMOTE_OPTIONS = new int[]{Piece.KNIGHT, Piece.QUEEN, Piece.ROOK, Piece.BISHOP};
    Board board;
    boolean onlyThreats, inCheck;

    public ArrayList<Move> generateThreats(Board b) {
        return generateMoves(b, Piece.getOpposite(b.activeColor));
    }

    public ArrayList<Move> generateMoves(Board b) {
        return generateMoves(b, b.activeColor);
    }

    public ArrayList<Move> generateMoves(Board b, int color) {
        onlyThreats = b.activeColor != color;
        inCheck = b.isChecked();
        board = b;
        ArrayList<Move> moves = new ArrayList<>(64);

        if (!onlyThreats) {
            addKingMoves(board.kingInd, moves);
        }
        if (board.multiCheck && !onlyThreats) {
            return moves;
        }

        for (int i = 0; i < board.pieceCounts[color]; i++) {
            int ind = board.pieces[color][i];
            int type = Piece.getType(board.board[ind]);
            switch(type) {
                case Piece.ROOK: addSlideMoves(ind, moves, 0, 4); break;
                case Piece.BISHOP: addSlideMoves(ind, moves, 4, 8); break;
                case Piece.QUEEN: addSlideMoves(ind, moves, 0, 8); break;
                case Piece.KNIGHT:
                    if (!onlyThreats) {
                        addKnightMoves(ind, moves);
                    }
                    break;
                case Piece.PAWN:
                    if (!onlyThreats) {
                        addPawnMoves(ind, moves);
                    }
                    break;
            }
        }
        return moves;
    }

    public void addKingMoves(int start, ArrayList<Move> moves) {
        int color = Piece.getColor(board.board[start]);
        for (int i = 0; i < PrecomputedMoveData.kingMoves[start].length; i++) {
            int currInd = PrecomputedMoveData.kingMoves[start][i];
            int currPiece = board.board[currInd];
            int currColor = Piece.getColor(currPiece);

            if (currColor == color) {
                if (onlyThreats) {
                    moves.add(new Move(board.board[start], currPiece, start, currInd, board));
                }
                continue;
            }

            boolean isCapture = color != currColor;
            if (!isCapture) {
                if (!onlyThreats && blocksCheck(currInd)) {
                    continue;
                }
            }

            if (onlyThreats || !board.threatening[currInd]) {
                int oppositeInd = start - (currInd - start);
                if (oppositeInd > 0 && oppositeInd <= 63) {
                    if (!board.checkPath[oppositeInd]) {
                        moves.add(new Move(board.board[start], currPiece, start, currInd, board));
                    }
                } else {
                    moves.add(new Move(board.board[start], currPiece, start, currInd, board));
                }
            }
        }
        if (!inCheck) {
            for (int direction : CASTLE_DIRECTIONS) {
                int canCastle = canCastle(start, direction, board);
                if (!onlyThreats && canCastle != -1) {
                    Move move = new Move(board.board[start], canCastle, start, start + direction, board);
                    move.type = Move.CASTLE;
                    moves.add(move);
                }
            }
        }
    }

    public static int canCastle(int start, int direction, Board board) {
        if (board.hasMoved[start] || Piece.getType(board.board[start]) != Piece.KING) {
            return -1;
        }
        int row = start / 8;
        int col = 0;
        if (direction > 0) {
            col = 7;
        }
        int ind = row * 8 + col;

        if (board.board[ind] != Board.EMPTY) {
            int rook = board.board[ind];
            if (Piece.getType(rook) == Piece.ROOK && !board.hasMoved[ind]) {
                int offset = direction > 0 ? 1 : -1;
                int testInd = start + offset;
                while (testInd != ind) {
                    if (board.board[testInd] != Board.EMPTY || board.threatening[testInd]) {
                        return -1;
                    }
                    testInd += offset;
                }
                return ind;
            }
        }
        return -1;
    }

    public void addSlideMoves(int start, ArrayList<Move> moves, int startOffInd, int endOffInd) {
        boolean isPinned = board.pins[start];
        int color = Piece.getColor(board.board[start]);

        // cannot possibly have valid moves
        if (!onlyThreats && board.singleCheck && isPinned) {
            return;
        }

        for (int offInd = startOffInd; offInd < endOffInd; offInd++) {
            int offset = ALL_OFFSETS[offInd];

            if (isPinned && !movingAlongRay(offset, board.kingInd, start)) {
                if (!onlyThreats) {
                    continue;
                }
            }

            for (int i = 0; i < PrecomputedMoveData.distToEdge[start][offInd]; i++) {
                int currInd = start + offset * (i + 1);
                int currPiece = board.board[currInd];
                int currColor = Piece.getColor(currPiece);

                if (color == currColor) {
                    if (onlyThreats) {
                        moves.add(new Move(board.board[start], currPiece, start, currInd, board));
                    }
                    break;
                }
                boolean isCapture = currPiece != Board.EMPTY;
                boolean blocksCheck = blocksCheck(currInd);
                if (blocksCheck || !inCheck) {
                    moves.add(new Move(board.board[start], currPiece, start, currInd, board));
                }

                // if not empty or a move blocks check, it is not possible to continue
                if (isCapture || blocksCheck) {
                    if (onlyThreats) {
                        moves.add(new Move(board.board[start], currPiece, start, currInd, board));
                    }
                    break;
                }
            }
        }
    }

    public void addKnightMoves(int start, ArrayList<Move> moves) {
        int color = Piece.getColor(board.board[start]);
        if (!onlyThreats && board.pins[start]) {
            return;
        }

        for (int i = 0; i < PrecomputedMoveData.knightMoves[start].length; i++) {
            int currInd = PrecomputedMoveData.knightMoves[start][i];
            int currPiece = board.board[currInd];
            int currColor = Piece.getColor(currPiece);

            boolean isCapture = currColor == Piece.getOpposite(color);
            if (!onlyThreats && (inCheck && !blocksCheck(currInd))) {
                continue;
            }
            if (isCapture) {
                moves.add(new Move(board.board[start], currPiece, start, currInd, board));
            } else if (onlyThreats || currColor == -1) {
                moves.add(new Move(board.board[start], currPiece, start, currInd, board));
            }
        }
    }

    public void addPawnMoves(int start, ArrayList<Move> moves) {
        int color = Piece.getColor(board.board[start]);
        int pawnOffset = color == Piece.WHITE ? -8 : 8;
        int startRow = board.activeColor == Piece.BLACK ? 1 : 6;
        int endRow = board.activeColor == Piece.BLACK ? 6 : 1;
        int row = start / 8;

        // advancements
        if (!onlyThreats) {
            int oneAdvance = start + pawnOffset;
            if (board.board[oneAdvance] == Board.EMPTY) {
                if (!board.pins[start] || movingAlongRay(pawnOffset, start, board.kingInd)) {
                    if (!inCheck || blocksCheck(oneAdvance)) {
                        Move move = new Move(board.board[start], Board.EMPTY, start, oneAdvance, board);
                        if (row == endRow) {
                            addPromotions(move, moves);
                        } else {
                            moves.add(move);
                        }
                    }
                    if (row == startRow) {
                        int twoAdvance = oneAdvance + pawnOffset;
                        if (board.board[twoAdvance] == Board.EMPTY) {
                            if (!inCheck || blocksCheck(twoAdvance)) {
                                moves.add(new Move(board.board[start], Board.EMPTY, start, twoAdvance, board));
                            }
                        }
                    }
                }
            }
        }

        // captures
        for (int i = 0; i < 2; i++) {
            if (PrecomputedMoveData.distToEdge[start][PAWN_ATTACKS[color][i]] > 0) {
                int offset = ALL_OFFSETS[PAWN_ATTACKS[color][i]];
                int currInd = start + offset;
                int currPiece = board.board[currInd];
                int currColor = Piece.getColor(currPiece);

                if (!onlyThreats && (board.pins[start] && !movingAlongRay(offset, board.kingInd, start))) {
                    continue;
                }

                if (onlyThreats) {
                    moves.add(new Move(board.board[start], currPiece, start, currInd, board));
                } else if (Piece.getOpposite(color) == currColor) {
                    if (inCheck && !blocksCheck(currInd)) {
                        continue;
                    }
                    Move move = new Move(board.board[start], currPiece, start, currInd, board);
                    if (row == endRow) {
                        addPromotions(move, moves);
                    } else {
                        moves.add(move);
                    }
                }

                if (currInd == board.enPassantable) {
                    if (!enPassantRevealsCheck(start, currInd)) {
                        int ind = (start / 8) * 8 + currInd % 8;
                        Move move = new Move(board.board[start], board.board[ind], start, currInd, board);
                        move.type = Move.EN_PASSANT;
                        moves.add(move);
                    }
                }
            }
        }
    }

    public boolean enPassantRevealsCheck(int start, int end) {
        int color = Piece.getColor(board.board[start]);
        int startRow = start / 8;
        int startCol = start % 8;
        int endCol = end % 8;
        int offset = endCol - startCol;
        boolean hasKing = false, hasThreat = false;
        for (int i = 1; i < 8; i++) {
            int threatCol = startCol + i * offset;
            int kingCol = startCol + i * -offset;
            if (threatCol >= 0 && threatCol <= 7) {
                int ind = startRow * 8 + threatCol;
                if (board.board[ind] != Board.EMPTY) {
                    int currColor = Piece.getColor(board.board[ind]);
                    int type = Piece.getType(board.board[ind]);
                    if ((currColor != color) && type == Piece.ROOK || type == Piece.QUEEN) {
                        hasThreat = true;
                    }
                }
            }
            if (kingCol >= 0 && kingCol <= 7) {
                int ind = startRow * 8 + kingCol;
                if (board.board[ind] != Board.EMPTY) {
                    int currColor = Piece.getColor(board.board[ind]);
                    int type = Piece.getType(board.board[ind]);
                    if ((currColor == color) && type == Piece.KING) {
                        hasKing = true;
                    }
                }
            }
        }
        return hasKing && hasThreat;
    }

    public void addPromotions(Move move, ArrayList<Move> moves) {
        int color = Piece.getColor(move.actor);
        for (int type : PROMOTE_OPTIONS) {
            Move newMove = new Move(move.actor, move.captured, move.start, move.end, board);
            newMove.promoteTo = color << 3 | type;
            newMove.type = Move.PROMOTION;
            moves.add(newMove);
        }
    }

    public boolean movingAlongRay(int offset, int start, int end) {
        int dir = PrecomputedMoveData.directionLookup[end - start + 63];
        return (dir == offset || -dir == offset);
    }

    public boolean blocksCheck(int ind) {
        return inCheck && board.checkPath[ind];
    }
}
