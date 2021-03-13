import java.util.ArrayList;

public class Game {
    FastBoard board;
    long blackMillis, whiteMillis, blackAdd, whiteAdd, turnStart;
    int halfMoves, fullMoves;
    GameState gameState;
    ArrayList<FastMove> moveHistory;

    public Game(String fen, String timeFormat) {
        moveHistory = new ArrayList<>();
        try {
            board = new FastBoard(fen, this);
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
        checkSetEndState();
    }

    public Game(Game game) {
        this.board = new FastBoard(game.board);
        board.updateInfo();
        this.blackMillis = game.blackMillis;
        this.whiteMillis = game.whiteMillis;
        this.blackAdd = game.blackAdd;
        this.whiteAdd = game.whiteAdd;
        this.turnStart = game.turnStart;
        this.halfMoves = game.halfMoves;
        this.fullMoves = game.fullMoves;
        this.gameState = game.gameState;
        this.moveHistory = new ArrayList<>(game.moveHistory.size());
        for (FastMove move : game.moveHistory) {
            this.moveHistory.add(new FastMove(move));
        }
    }

    public int getMaterialScore() {
        return board.getMaterialScore();
    }

    public boolean isEndgame() {
        return board.isEndgame();
    }

    public int getMobilityDiff() {
        return board.getMobilityDiff();
    }

    public void makeMove(FastMove move) {
        if (move != null) {
            if (Piece.getType(move.actor) == Piece.PAWN || move.isCapture()) {
                halfMoves = 0;
            } else {
                halfMoves++;
                if (halfMoves >= 100) {
                    gameState = GameState.DRAW;
                }
            }
            if (board.activeColor == Piece.BLACK) {
                fullMoves++;
                blackMillis += blackAdd - (System.currentTimeMillis() - turnStart);
            } else {
                whiteMillis += whiteAdd - (System.currentTimeMillis() - turnStart);
            }
            FastBoard fastBoard = (FastBoard) board;
            fastBoard.makeMove(move);
            moveHistory.add(move);
            checkSetEndState();
            turnStart = System.currentTimeMillis();
        }
    }

    public void checkSetEndState() {
        if (board.isStalemate()) {
            gameState = GameState.DRAW;
        } else if (board.isCheckmate()) {
            setWinner(getActiveColor());
        }
    }

    public ArrayList<FastMove> getMoves() {
        return board.moves;
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
    public void unmakeMove(FastMove move) {
        if (Piece.getColor(move.actor) == Piece.BLACK) {
            fullMoves--;
        }
        FastBoard fastBoard = (FastBoard) board;
        fastBoard.unmakeMove(move);
        moveHistory.remove(moveHistory.size() - 1);
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
        return board.toString() + " " +  halfMoves + " " + fullMoves;
    }

    public String toString() {
        return board.toString() + "\n" + getRemainingTime(Piece.WHITE) + " / " + getRemainingTime(Piece.BLACK);
    }

    public int getActiveColor() {
        return board.activeColor;
    }
}
