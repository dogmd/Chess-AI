public class EvalUpdater implements Runnable {
    Agent evaluator;
    Game game;
    public EvalUpdater(Agent evaluator, Game game) {
        this.evaluator = evaluator;
        this.game = game;
    }

    public void run() {
        Main.eval = "Eval: ...";
        Main.suggestedMove = null;
        Main.gameWindow.gameView.assistSquares.clear();
        int color = game.getActiveColor();
        Main.suggestedMove = evaluator.getMove(game, color);
        if (Main.assistEnabled && color == Main.gameWindow.game.getActiveColor()) {
            Main.gameWindow.gameView.assistSquares.add(Main.suggestedMove.start);
            Main.gameWindow.gameView.assistSquares.add(Main.suggestedMove.end);
        }
        double eval = ((ScottAgent)evaluator).bestScore * (game.board.activeColor == Piece.WHITE ? 1 : -1);
        Main.eval = String.format("Eval: %.2f", eval);
    }
}
