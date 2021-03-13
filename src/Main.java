import java.lang.reflect.Constructor;
import java.util.ConcurrentModificationException;

public class Main {
    static GameWindow gameWindow;
    static String board = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
    static String time = "30:00/2|30:00/2";
    static String agent1Class = null, agent2Class = null;
    static String agent1Name = "WHITE", agent2Name = "BLACK";
    static Agent agent1 = null, agent2 = null;
    static Agent evaluator;
    static FastMove suggestedMove;
    static String eval = "Eval: 0.00";
    static boolean displayEnabled = true;
    static boolean soundsEnabled = true;
    static boolean assistEnabled = false;
    static long delayMillis = 0;
    static int runCount = 1;

    public static void main(String[] args) {
        if (args.length == 0) {
            args = new String[]{"-agent2", "ScottAgent", "-name2", "3,true"};
        }
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.equals("-time")) {
                time = args[i + 1];
                i++;
            } else if (arg.equals("-board")) {
                board = args[i + 1];
                i++;
            } else if (arg.equals("-agent1")) {
                agent1Class = args[i + 1];
                i++;
            } else if (arg.equals("-agent2")) {
                agent2Class = args[i + 1];
                i++;
            } else if (arg.equals("-display")) {
                displayEnabled = Boolean.parseBoolean(args[i + 1]);
                i++;
            } else if (arg.equals("-delay")) {
                delayMillis = Long.parseLong(args[i + 1]);
                i++;
            } else if (arg.equals("-name1")) {
                agent1Name = args[i + 1];
                i++;
            } else if (arg.equals("-name2")) {
                agent2Name = args[i + 1];
                i++;
            } else if (arg.equals("-runcount")) {
                runCount = Integer.parseInt(args[i + 1]);
                i++;
            } else if (arg.equals("-sound")) {
                soundsEnabled = Boolean.parseBoolean(args[i + 1]);
                i++;
            } else if (arg.equals("-assist")) {
                assistEnabled = Boolean.parseBoolean(args[i + 1]);
                i++;
            }
        }
        PrecomputedMoveData.calculate();
        Game game = new Game(board, time);
        evaluator = new ScottAgent("3,true", game, Piece.WHITE);
        System.out.println(game);
        gameWindow = new GameWindow(game);

        if (agent1Class != null) {
            agent1 = getAgent(agent1Name, agent1Class, game);
            agent1.color = Piece.WHITE;
        }
        if (agent2Class != null) {
            agent2 = getAgent(agent2Name, agent2Class, game);
            agent2.color = Piece.BLACK;
        }

        if (agent1 != null) {
            new Thread(new EvalUpdater(new ScottAgent("3,true", game, game.getActiveColor()), game)).start();
        }

        int numGames = 0;
        while ((agent1 != null || agent2 != null) && numGames < runCount) {
            if (!makeAgentMove(agent1, game)) break;
            if (!makeAgentMove(agent2, game)) break;
            if (game.gameState != GameState.ACTIVE && game.gameState != GameState.PAUSED) {
                System.out.println(game.gameState);
                game = new Game(board, time);
                if (agent1 != null) {
                    agent1.game = game;
                }
                if (agent2 != null) {
                    agent2.game = game;
                }
                numGames++;
                if (numGames < runCount) {
                    gameWindow.game = game;
                    gameWindow.gameView.game = game;
                }
            }
        }
    }

    public static boolean makeAgentMove(Agent agent, Game game) {
        try {
            if (delayMillis > 0) {
                try {
                    Thread.sleep(delayMillis);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if (game.gameState == GameState.PAUSED) {
                return true;
            }
            if (game.gameState == GameState.ACTIVE && agent != null && game.getActiveColor() == agent.color) {
                makeMove(agent.name, game, agent.getMove(game, agent.color));
            }
        } catch (ConcurrentModificationException e) {
            System.out.println("Concurrent modification error.");
            return true;
        }
        return true;
    }

    public static void makeMove(String source, Game game, FastMove move) {
        if (Piece.getColor(move.actor) == game.board.activeColor) {
            if (Main.gameWindow != null) {
                Main.gameWindow.gameView.selectedSquare = FastBoard.EMPTY;
            }
            System.out.println(source + " plays " + move.toString().replace("\n", ""));
            GameState oldState = game.gameState;
            game.makeMove(move);
            new Thread(new EvalUpdater(new ScottAgent("3,true", game, game.getActiveColor()), game)).start();
            System.out.println(game.getMaterialScore() + "\n");
            if (Main.displayEnabled) {
                if (oldState != game.gameState) {
                    Main.gameWindow.playNotify();
                } else {
                    gameWindow.playSound(move);
                    gameWindow.gameView.highlightedSquares.clear();
                    gameWindow.gameView.highlightedSquares.add(move.start);
                    gameWindow.gameView.highlightedSquares.add(move.end);
                }
            }
        }
    }

    public static Agent getAgent(String agentName, String agentClassname, Game game) {
        try {
            Class agentClass = Class.forName(agentClassname);
            Class[] types = new Class[]{String.class, Game.class, int.class};
            Constructor constructor = agentClass.getConstructor(types);
            Object[] parameters = new Object[]{agentName, game, Piece.WHITE};
            return (Agent) constructor.newInstance(parameters);
        } catch (Exception e) {
            System.out.println("Error loading agent " + agentName);
            e.printStackTrace();
            return null;
        }
    }
}
