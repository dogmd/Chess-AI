import java.util.ArrayList;
import java.util.Stack;

public class Game {
    static final String DEFAULT_TIME = "30:00/2|30:00/2";

    Board board;
    long blackMillis, whiteMillis, blackAdd, whiteAdd, turnStart;
    int halfMoves, fullMoves;
    GameState gameState;
    ArrayList<Move> moveHistory;
    Stack<Long> repeatHistory;
    Stack<Integer> halfMoveHistory;

    public Game() {
        this(Board.DEFAULT_FEN, Game.DEFAULT_TIME);
    }

    public Game(String fen, String timeFormat) {
        moveHistory = new ArrayList<>();
        repeatHistory = new Stack<>();
        halfMoveHistory = new Stack<>();
        try {
            board = new Board(fen, this);
            repeatHistory.push(board.zobristKey);
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
        this.board = new Board(game.board);
        this.board.game = this;
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
        for (Move move : game.moveHistory) {
            this.moveHistory.add(new Move(move));
        }
        this.repeatHistory = new Stack<>();
        this.repeatHistory.addAll(game.repeatHistory);
        halfMoveHistory = new Stack<>();
        this.halfMoveHistory.addAll(game.halfMoveHistory);
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

    public void makeMove(Move move) {
        if (move != null) {
            if (Piece.getType(move.actor) == Piece.PAWN || move.isCapture()) {
                halfMoves = 0;
                repeatHistory.clear();
            } else {
                halfMoves++;
            }
            if (board.activeColor == Piece.BLACK) {
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

    public boolean hasBeenSeen(long key) {
        // enhanced for can cause concurrent mod error
        for (int i = 0; i < repeatHistory.size(); i++) {
            if (repeatHistory.get(i) == key) {
                return true;
            }
        }
        return false;
    }

    public void checkSetEndState() {
        if (halfMoves >= 100) {
            gameState = GameState.DRAW;
            return;
        }
        int repeatCount = 0;
        for (long zobristKey : repeatHistory) {
            if (zobristKey == board.zobristKey) {
                repeatCount++;
            }
        }
        if (repeatCount >= 3) {
            gameState = GameState.DRAW;
            return;
        }
        if (board.isStalemate()) {
            gameState = GameState.DRAW;
        } else if (board.isCheckmate()) {
            setWinner(getActiveColor());
        }
    }

    public ArrayList<Move> getMoves() {
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
    public void unmakeMove(Move move) {
        if (Piece.getColor(move.actor) == Piece.BLACK) {
            fullMoves--;
        }
        board.unmakeMove(move);
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
        String negative = "";
        if (time < 0) {
            setWinner(Piece.getOpposite(color));
            negative = "-";
        }
        return String.format("%s%02d:%02d", negative, Math.abs(min), Math.abs(sec));
    }

    public String toFEN() {
        return board.toString() + " " +  halfMoves + " " + fullMoves;
    }

    public String toString() {
        return board.toString() + "\n" + getRemainingTime(Piece.WHITE) + " / " + getRemainingTime(Piece.BLACK);
    }

    public int getActiveColor() {
        return board.activeColor;
    }

    public String toPGN() {
        StringBuilder pgn = new StringBuilder();
        Game copy = new Game();

        for (int i = 0; i < this.moveHistory.size(); i++) {
            Move move = this.moveHistory.get(i);
            if (i % 2 == 0) {
                pgn.append((i / 2) + 1);
                pgn.append(". ");
            }
            pgn.append(move.getAlgebraic(copy.board));
            pgn.append(" ");
            copy.makeMove(move);
        }
        return pgn.toString();
    }
}
