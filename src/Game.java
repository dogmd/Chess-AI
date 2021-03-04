import java.util.ArrayList;
import java.util.List;

public class Game {
    Board board;
    long blackMillis, whiteMillis, blackAdd, whiteAdd, turnStart;
    int halfMoves, fullMoves;
    GameState gameState;
    ArrayList<Move> moveHistory;

    public Game(String fen, String timeFormat) {
        moveHistory = new ArrayList<>();
        try {
            board = new Board(fen);
            String[] fields = fen.split(" ");
            halfMoves = Integer.parseInt(fields[4]);
            fullMoves = Integer.parseInt(fields[5]);
        } catch (Exception e) {
            System.out.println("Error in FEN String: ");
            e.printStackTrace();
        }
        String[] times = timeFormat.split("\\|");
        String[] time = times[0].split("/");
        int min = Integer.parseInt(time[0].split(":")[0]);
        int sec = Integer.parseInt(time[0].split(":")[1]);
        whiteMillis = min * 60 * 1000 + sec * 1000;
        whiteAdd = Integer.parseInt(time[1]) * 1000;
        time = times[1].split("/");
        min = Integer.parseInt(time[0].split(":")[0]);
        sec = Integer.parseInt(time[0].split(":")[1]);
        blackMillis = min * 60 * 1000 + sec * 1000;
        blackAdd = Integer.parseInt(time[1]) * 1000;
        turnStart = System.currentTimeMillis();
        gameState = GameState.ACTIVE;
        board.updateInfo();
        checkSetEndState();
    }

    public Game(Game game) {
        this.board = new Board(game.board);
        board.updateInfo();
        this.blackMillis = game.blackMillis;
        this.whiteMillis = game.whiteMillis;
        this.blackAdd = game.blackAdd;
        this.whiteAdd = game.whiteAdd;
        this.turnStart = game.turnStart;
        this.halfMoves = game.halfMoves;
        this.fullMoves = game.fullMoves;
        this.gameState = game.gameState;
        this.moveHistory = new ArrayList<>(game.moveHistory);
    }

    public int getMaterialScore() {
        int total = 0;
        for (PieceType type : PieceType.values()) {
            int whiteNum = board.pieces.get(type).get(Piece.WHITE).size();
            int blackNum = board.pieces.get(type).get(Piece.BLACK).size();
            int weight = Piece.getWeight(type);
            if (whiteNum + blackNum > 0) {
                total += weight * (whiteNum - blackNum);
            }
        }
        return total;
    }

    public int getMobilityDiff() {
        return getMoves(Piece.WHITE).size() - getMoves(Piece.BLACK).size();
    }

    public void makeMove(Move move) {
        if (Main.gameWindow != null) {
            Main.gameWindow.gameView.selectedSquare = null;
        }
        if (move != null) {
            if (move.actor.type == PieceType.PAWN || move.isCapture()) {
                halfMoves = 0;
            } else {
                halfMoves++;
                if (halfMoves >= 100) {
                    //System.out.println("Fifty-move draw rule available.");
                    gameState = GameState.DRAW;
                }
            }
            if (getActiveColor() == Piece.BLACK) {
                fullMoves++;
                blackMillis += blackAdd - (System.currentTimeMillis() - turnStart);
            } else {
                whiteMillis += whiteAdd - (System.currentTimeMillis() - turnStart);
            }
            board.makeMove(move);
            moveHistory.add(move);
            checkSetEndState();
            turnStart = System.currentTimeMillis();
        }
    }

    public void checkSetEndState() {
        if (isStalemate(getActiveColor())) {
            gameState = GameState.DRAW;
        } else if (isCheckmate()) {
            setWinner(getActiveColor());
        }
    }

    public ArrayList<Move> getMoves() {
        return board.moves;
    }

    public ArrayList<Move> getMoves(int color) {
        if (board.activeColor == color) {
            return board.moves;
        } else {
            return board.getMoves(color);
        }
    }

    public void setWinner(int color) {
        gameState = color == Piece.WHITE ? GameState.BLACK_WIN : GameState.WHITE_WIN;
    }

    // Can be used to continue games (feature not bug)
    public void togglePause() {
        if (gameState == GameState.PAUSED) {
            gameState = GameState.ACTIVE;
            turnStart = System.currentTimeMillis();
        } else {
            gameState = GameState.PAUSED;
            if (getActiveColor() == Piece.BLACK) {
                blackMillis -= System.currentTimeMillis() - turnStart;
            } else {
                whiteMillis -= System.currentTimeMillis() - turnStart;
            }
        }
    }

    // Not a perfect reset
    public void unmakeMove(Move move) {
        if (move.actor.color == Piece.BLACK) {
            fullMoves--;
        }
        board.unmakeMove(move);
        moveHistory.remove(moveHistory.size() - 1);
    }

    public boolean isCheckmate() {
        int color = board.activeColor;
        List<Piece> kings = board.pieces.get(PieceType.KING).get(color);
        if (kings.size() > 0) {
            Piece king = kings.get(0);
            if (board.threatening.contains(king.square)) {
                return getMoves().size() == 0;
            } else {
                return false;
            }
        }
        return true;
    }

    public boolean isStalemate(int color) {
        List<Piece> kings = board.pieces.get(PieceType.KING).get(color);
        boolean onlyKings = true;
        for (List<List<Piece>> pieces : board.pieces.values()) {
            for (List<Piece> pieceColor : pieces) {
                for (Piece p : pieceColor) {
                    if (p.type != PieceType.KING) {
                        onlyKings = false;
                        break;
                    }
                }
            }
        }
        if (onlyKings) {
            return true;
        }
        if (kings.size() > 0) {
            Piece king = kings.get(0);
            if (!board.threatening.contains(king.square)) {
                return getMoves(king.color).size() == 0;
            } else {
                return false;
            }
        }
        return false;
    }

    public long getRemainingMillis(int color) {
        long time;
        if (color == Piece.BLACK) {
            time = blackMillis;
            if (gameState == GameState.ACTIVE && getActiveColor() == Piece.BLACK) {
                time -= System.currentTimeMillis() - turnStart;
            }
        } else {
            time = whiteMillis;
            if (gameState == GameState.ACTIVE && getActiveColor() == Piece.WHITE) {
                time -= System.currentTimeMillis() - turnStart;
            }
        }
        return time;
    }


    public String getRemainingTime(int color) {
        long time = getRemainingMillis(color);
        long min = (int)(time / 60.0 / 1000);
        long sec = (time - (min * 60 * 1000)) / 1000;
        long millis = time - min * 60 * 1000 - sec * 1000;
        String negative = "";
        if (time < 0) {
            setWinner(Piece.getOpposite(color));
            negative = "-";
        }
        return String.format("%s%02d:%02d.%02d", negative, Math.abs(min), Math.abs(sec), Math.abs(millis / 10));
    }

    public String toFen() {
        return board.toFen() + " " +  halfMoves + " " + fullMoves;
    }

    public String toString() {
        return board.toString() + "\n" + getRemainingTime(Piece.WHITE) + " / " + getRemainingTime(Piece.BLACK);
    }

    public int getActiveColor() {
        return board.activeColor;
    }
}
