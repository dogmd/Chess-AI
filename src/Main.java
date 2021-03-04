import java.lang.reflect.Constructor;
import java.util.ConcurrentModificationException;

public class Main {
    static GameWindow gameWindow;
    static String board = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
    static String time = "5:00/2|5:00/2";
    static String agent1Class = null, agent2Class = null;
    static String agent1Name = "WHITE", agent2Name = "BLACK";
    static Agent agent1 = null, agent2 = null;
    static boolean displayEnabled = true;
    static long delayMillis = 0;
    static int runCount = 1;

    public static void main(String[] args) {
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
            }
        }
        Game game = new Game(board, time);
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
                Move move = agent.getMove(game, agent.color);
                System.out.println(agent.name + " plays " + move.toString().replace("\n", ""));
                if (move.isCapture() && move.captured.type == PieceType.KING) {
                    System.out.println("OH NO");
                    game.unmakeMove(game.moveHistory.get(game.moveHistory.size() - 1));
                    return false;
                }
                game.makeMove(move);
                gameWindow.gameView.highlightedSquares.clear();
                gameWindow.gameView.highlightedSquares.add(move.start);
                gameWindow.gameView.highlightedSquares.add(move.end);
                System.out.println(game.toFen());
                System.out.println(game.getMaterialScore() + "\n");
            }
        } catch (ConcurrentModificationException e) {
            System.out.println("Concurrent modification error.");
            return true;
        }
        return true;
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
