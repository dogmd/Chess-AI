import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.image.PNGTranscoder;

import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class GameWindow extends JFrame implements KeyListener {
    Game game;
    GameView gameView;

    public GameWindow(Game game) {
        super("Chess");
        this.game = game;
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {System.exit(0);}
        });
        System.setProperty("awt.useSystemAAFontSettings","on");
        System.setProperty("swing.aatext", "true");
        setSize(1000, 825);
        this.gameView = new GameView(game);
        add("Center", this.gameView);
        addKeyListener(this);
        setVisible(true);
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_Z && game.moveHistory.size() > 0) {
            Move undone = game.moveHistory.get(game.moveHistory.size() - 1);
            gameView.options.clear();
            gameView.selectedSquare = Board.EMPTY;
            gameView.highlightedSquares.clear();
            gameView.assistSquares.clear();
            game.unmakeMove(undone);
            gameView.selectedSquare = undone.start;
        } else if (e.getKeyCode() == KeyEvent.VK_T) {
            gameView.showThreatened = !gameView.showThreatened;
        } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            game.togglePause();
        } else if (e.getKeyCode() == KeyEvent.VK_A) {
            Main.assistEnabled = !Main.assistEnabled;
            if (Main.assistEnabled && Main.suggestedMove != null) {
                gameView.assistSquares.clear();
                gameView.assistSquares.add(Main.suggestedMove.start);
                gameView.assistSquares.add(Main.suggestedMove.end);
            } else {
                gameView.assistSquares.clear();
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {}

    public void playNotify() {
        playSound("sounds/GenericNotify.wav");
    }

    public void playSound(String path) {
        if (Main.soundsEnabled) {
            AudioInputStream audioInputStream = null;
            Clip clip = null;
            try {
                InputStream bufferedIn = new BufferedInputStream(ClassLoader.getSystemResourceAsStream(path));
                audioInputStream = AudioSystem.getAudioInputStream(bufferedIn);
                clip = AudioSystem.getClip();
                clip.open(audioInputStream);
                clip.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void playSound(Move move) {
        if (move.isCapture()) {
            playSound("sounds/Capture.wav");
        } else {
            playSound("sounds/Move.wav");
        }
    }
}

class GameView extends JPanel implements ActionListener {
    static final double BOARD_SCALE = 0.8;
    static final double MARGINS_SCALE = 0.06;

    final Timer timer = new Timer(7, this);
    final MouseAdapter mouseHandler = new MouseAdapter() {
        @Override
        public void mousePressed(MouseEvent e) {
            super.mousePressed(e);
            highlightedSquares.clear();
            mouseX = e.getX();
            mouseY = e.getY();
            dragging = true;
            int clicked = getSelectedSquare(e.getX(), e.getY());
            if (clicked != -1) {
                if (selectedSquare != -1) {
                    handleMove(clicked, e.getX(), e.getY());
                }
                int piece = game.board.board[clicked];
                if (piece != -1 && Piece.getColor(piece) == game.getActiveColor()) {
                    if (!(Main.agent1 != null && Main.agent1.color == game.getActiveColor()) && !(Main.agent2 != null && Main.agent2.color == game.getActiveColor())) {
                        selectedSquare = clicked;
                        options.clear();
                        for (Move move : game.board.moves) {
                            if (move.start == selectedSquare) {
                                options.add(move);
                            }
                        }
                    }
                } else {
                    selectedSquare = -1;
                }
            }
            repaint();
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            super.mouseReleased(e);
            mouseX = e.getX();
            mouseY = e.getY();
            dragging = false;
            int sq = getSelectedSquare(e.getX(), e.getY());
            handleMove(sq, e.getX(), e.getY());
            repaint();
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            super.mouseDragged(e);
            if (selectedSquare != -1 && game.board.board[selectedSquare] != -1) {
                mouseX = e.getX();
                mouseY = e.getY();
            }
            repaint();
        }
    };

    static final Color LIGHT_GREY = new Color(0xBDBDBD);
    static final Color DARK_GREY = new Color(0x616161);
    static final Color RED = new Color(0xe57373);
    static final Color GREEN = new Color(0x81c784);
    static final Color BLUE = new Color(55, 0, 179);
    static final Color YELLOW = new Color(0xffeb3b);
    static final Color BACKGROUND = new Color(0x121212);

    Font robotoBold, robotoBlack;
    Game game;
    int height, width, mouseX, mouseY;
    boolean dragging, showThreatened;
    int selectedSquare;
    Map<String, BufferedImage> cachedIcons;
    ArrayList<Integer> highlightedSquares;
    ArrayList<Integer> assistSquares;
    ArrayList<Move> options;
    int margins, boardWidth, squareWidth, smallOff;

    public GameView(Game game) {
        super(true);
        this.game = game;
        options = new ArrayList<>();
        selectedSquare = -1;
        highlightedSquares = new ArrayList<>();
        assistSquares = new ArrayList<>();
        cachedIcons = new HashMap<>();
        try {
            robotoBold = Font.createFont(Font.TRUETYPE_FONT, ClassLoader.getSystemResourceAsStream("fonts/Roboto-Bold.ttf"));
            robotoBlack = Font.createFont(Font.TRUETYPE_FONT, ClassLoader.getSystemResourceAsStream("fonts/Roboto-Black.ttf"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);
        timer.start();
    }

    public void handleMove(int clicked, int x, int y) {
        if (selectedSquare != -1 && selectedSquare != clicked && game.board.board[selectedSquare] != -1) {
            int selectedPromotion;
            for (Move move : options) {
                if (clicked == move.end) {
                    if (move.type == Move.PROMOTION) {
                        selectedPromotion = getSelectedPromotion(clicked, x, y);
                        if (selectedPromotion == move.promoteTo) {
                            makeMove(move);
                            break;
                        }
                    } else {
                        makeMove(move);
                        break;
                    }
                }
            }
        }
    }

    public void makeMove(Move move) {
        selectedSquare = -1;
        options.clear();
        Main.makeMove("HUMAN", game, move);
    }

    public int getSelectedPromotion(int sq, int x, int y) {
        double sqWidth = squareWidth;
        int row = sq / 8;
        int col = sq - row * 8;
        int topLeftX = margins + (int)(sqWidth * col);
        int topLeftY = margins + (int)(sqWidth * row);
        int color = game.board.activeColor;
        int type;
        if (x < topLeftX + sqWidth / 2 && y < topLeftY + sqWidth / 2) {
            type = Piece.KNIGHT;
        } else if (x >= topLeftX + sqWidth / 2 && y >= topLeftY + sqWidth / 2) {
            type = Piece.ROOK;
        } else if (x < topLeftX + sqWidth / 2 && y >= topLeftY + sqWidth / 2) {
            type = Piece.BISHOP;
        } else {
            type = Piece.QUEEN;
        }
        return color << 3 | type;
    }

    public int getSelectedSquare(int x, int y) {
        int sq = -1;
        int minX = margins;
        int minY = margins;
        int maxX = margins + boardWidth;
        int maxY = margins + boardWidth;
        if(x > minX && x < maxX && y > minY && y < maxY) {
            int col = (x - minX) / squareWidth;
            int row = (y - minY) / squareWidth;
            sq = row * 8 + col;
        }
        return sq;
    }

    public void generateIcons() {
        cachedIcons.clear();
        squareWidth = squareWidth <= 0 ? 1 : squareWidth;
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 6; j++) {
                int piece = i << 3 | j;
                String fileName = "icons/" + Piece.getFenChar(piece).toLowerCase() + (Piece.getColor(piece) == Piece.WHITE ? "w" : "b") + ".svg";
                if (width != 0 && height != 0) {
                    cachedIcons.put(fileName, loadImage(fileName, squareWidth, squareWidth));
                }
            }
        }
        String fileName = "icons/promotew.svg";
        cachedIcons.put(fileName, loadImage(fileName, squareWidth, squareWidth));
        fileName = "icons/promoteb.svg";
        cachedIcons.put(fileName, loadImage(fileName, squareWidth, squareWidth));
    }

    // From https://stackoverflow.com/a/20664243/4352298
    public static BufferedImage loadImage(String svgFile, float width, float height) {
        BufferedImageTranscoder imageTranscoder = new BufferedImageTranscoder();

        imageTranscoder.addTranscodingHint(PNGTranscoder.KEY_WIDTH, width);
        imageTranscoder.addTranscodingHint(PNGTranscoder.KEY_HEIGHT, height);

        TranscoderInput input = null;
        try {
            input = new TranscoderInput(ClassLoader.getSystemResourceAsStream(svgFile));
            imageTranscoder.transcode(input, null);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return imageTranscoder.getBufferedImage();
    }

    public void setDimensions() {
        Dimension d = getSize();
        d = new Dimension(d.width - margins, d.height - margins);
        boardWidth = height;
        smallOff = (int)(0.02 * width);
        squareWidth = boardWidth / 8;
        height = d.height / BOARD_SCALE < d.width ? (d.height) : (int)(d.width * BOARD_SCALE);
        width = (int)(height / BOARD_SCALE);
    }

    public void drawBoard(Graphics g) {
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                int ind = i * 8 + j;
                Graphics2D g2 = (Graphics2D) g;
                g2.setStroke(new BasicStroke(smallOff / 5f));
                g.setColor(Color.BLACK);
                g.drawRect(margins + (j * squareWidth), margins + (i * squareWidth), squareWidth, squareWidth);
                if ((i + j) % 2 == 0) { // square is light
                    g.setColor(LIGHT_GREY);
                } else {
                    g.setColor(DARK_GREY);
                }
                if (highlightedSquares.contains(ind)) {
                    g.setColor(BLUE);
                }
                if (assistSquares.contains(ind)) {
                    g.setColor(YELLOW);
                }
                if (selectedSquare == ind) {
                    g.setColor(GREEN);
                }
                g.fillRect(margins + (j * squareWidth), margins + (i * squareWidth), squareWidth, squareWidth);
            }
        }
        if (showThreatened) {
            for (int i = 0; i < 64; i++) {
                int row = i / 8;
                int col = i - row * 8;
                if (game.board.threatening[i]) {
                    g.setColor(BLUE);
                    Graphics2D g2d = (Graphics2D) g;
                    g2d.setStroke(new BasicStroke(4));
                    g2d.fillRect(margins + col * squareWidth, margins + row * squareWidth, squareWidth, squareWidth);
                }
                if (game.board.pins[i]) {
                    g.setColor(new Color(0xef6c00));
                    g.fillRect(margins + col * squareWidth, margins + row * squareWidth, squareWidth, squareWidth);
                }
                if (game.board.checkPath[i]) {
                    g.setColor(YELLOW);
                    g.fillRect(margins + col * squareWidth, margins + row * squareWidth, squareWidth, squareWidth);
                }
            }
//            if (selectedSquare != -1) {
//                for (int i = 0; i < PrecomputedMoveData.whitePawnAttacks[selectedSquare].length; i++) {
//                    int ind = PrecomputedMoveData.whitePawnAttacks[selectedSquare][i];
//                    int row = ind / 8;
//                    int col = ind - row * 8;
//                    g.setColor(new Color(0x40c4ff));
//                    g.fillRect(margins + col * squareWidth, margins + row * squareWidth, squareWidth, squareWidth);
//                }
//            }
        }
        drawOptions(g);
        drawPieces(g);

        for (int i = 0; i < 8; i++) {
            g.setColor(Color.WHITE);
            g.setFont(robotoBlack.deriveFont(smallOff * 2.5f));
            String text = "" + (char)('A' + i);
            int textWidth = g.getFontMetrics().stringWidth(text);
            g.drawString(text, i * squareWidth + margins - textWidth / 2 + squareWidth / 2, margins - smallOff / 2);
            text = "" + (8 - i);
            int textHeight = g.getFont().getSize();
            textWidth = g.getFontMetrics().stringWidth(text);
            g.drawString(text, margins - smallOff / 2 - textWidth, i * squareWidth + margins + textHeight / 4 + squareWidth / 2);
        }
    }

    public void drawPieces(Graphics g) {
        for (int color = 0; color < 2; color++) {
            for (int i = 0; i < game.board.pieceCounts[color]; i++) {
                int ind = game.board.pieces[color][i];
                int piece = game.board.board[ind];
                if (piece != -1 && (ind != selectedSquare || !dragging)) {
                    int row = ind / 8;
                    int col = ind - row * 8;
                    String fileName = "icons/" + Piece.getFenChar(piece).toLowerCase() + (color == Piece.WHITE ? "w" : "b") + ".svg";
                    g.drawImage(cachedIcons.get(fileName), margins + (col * squareWidth), margins + (row * squareWidth), this);
                }
            }
        }
    }

    public void drawOptions(Graphics g) {
        if (selectedSquare != -1) {
            int piece = game.board.board[selectedSquare];
            if (piece != -1) {
                if (options.size() == 0) {
                    for (Move move : game.board.moves) {
                        if (move.start == selectedSquare) {
                            options.add(move);
                        }
                    }
                }
                for (Move move : options) {
                    int row = move.end / 8;
                    int col = move.end - row * 8;
                    if (move.type == Move.PROMOTION) {
                        String color = Piece.getColor(move.actor) == Piece.WHITE ? "w" : "b";
                        String fileName = "icons/promote" + color + ".svg";
                        g.drawImage(cachedIcons.get(fileName), margins + (col * squareWidth), margins + (row * squareWidth), this.getParent());
                    } else {
                        if (move.isCapture()) {
                            Graphics2D g2 = (Graphics2D) g;
                            g2.setStroke(new BasicStroke(2));
                            g.setColor(Color.BLACK);
                            g.drawRect(margins + (squareWidth * col), margins + (squareWidth * row), squareWidth, squareWidth);
                            g.setColor(RED);
                            g.fillRect(margins + (squareWidth * col), margins + (squareWidth * row), squareWidth, squareWidth);
                        } else {
                            g.setColor(((row + col) % 2 == 0 ? LIGHT_GREY : DARK_GREY).darker());
                            int x = margins + (squareWidth * col) + squareWidth / 4;
                            int y = margins + (squareWidth * row) + squareWidth / 4;
                            int diam = squareWidth / 2;
                            g.drawOval(x, y, diam, diam);
                            g.fillOval(x, y, diam, diam);
                        }
                    }
                }
            }
        }
    }

    public void drawTime(Graphics g) {
        // Dividing line
        g.setColor(Color.WHITE);
        Graphics2D g2 = (Graphics2D) g;
        g2.setStroke(new BasicStroke(4));
        g.drawLine(margins + boardWidth + smallOff, margins + (squareWidth * 4) - 2, width + margins - smallOff, margins + (squareWidth * 4) - 2);
        int lineWidth = (int)(width * (1 - BOARD_SCALE)) - smallOff;
        g.setFont(robotoBold.deriveFont(lineWidth * 0.2f));
        // Black's time
        String time = game.getRemainingTime(Piece.BLACK);
        int textWidth = g.getFontMetrics().stringWidth(time);
        g.drawString(time, margins + smallOff + boardWidth + (lineWidth - textWidth) / 4, (squareWidth * 4) + margins - smallOff);
        // White's time
        time = game.getRemainingTime(Piece.WHITE);
        textWidth = g.getFontMetrics().stringWidth(time);
        g.drawString(time, margins + smallOff + boardWidth + (lineWidth - textWidth) / 4, (squareWidth * 4) + g.getFont().getSize() + margins);
    }

    public void drawEval(Graphics g) {
        g.setColor(Color.WHITE);
        int lineWidth = (int)(width * (1 - BOARD_SCALE)) - smallOff;
        g.setFont(robotoBold.deriveFont(lineWidth * 0.1f));
        g.drawString(Main.eval, margins + smallOff + boardWidth, margins + boardWidth - smallOff);
        long evalCount = -1;
        if (Main.agent1 != null && Main.agent1.color == game.getActiveColor() && Main.agent1 instanceof ScottAgent) {
            evalCount = ((ScottAgent)Main.agent1).evalCount;
        } else if (Main.agent2 != null && Main.agent2.color == game.getActiveColor() && Main.agent2 instanceof ScottAgent) {
            evalCount = ((ScottAgent)Main.agent2).evalCount;
        }
        if (evalCount != -1) {
            g.drawString("Eval Count: " + evalCount, margins + smallOff + boardWidth, margins + boardWidth - g.getFont().getSize() - 2 * smallOff);
        }
    }

    public void dragSelected(Graphics g) {
        int piece = game.board.board[selectedSquare];
        String fileName = "icons/" + Piece.getFenChar(piece).toLowerCase() + (Piece.getColor(piece) == Piece.WHITE ? "w" : "b") + ".svg";
        int x = mouseX;
        int y = mouseY;
        int max = margins + boardWidth + squareWidth / 2;
        int min = margins - squareWidth / 2;
        int offset = squareWidth / 2;
        if (x + offset > max) {
            x = max - offset;
        } else if (x - offset < min) {
            x = min + offset;
        }
        if (y + offset > max) {
            y = max - offset;
        } else if (y - offset < min) {
            y = min + offset;
        }

        g.drawImage(cachedIcons.get(fileName), x - offset, y - offset, this.getParent());
    }

    public void drawGameState(Graphics g) {
        String text = game.gameState.toString();
        if (game.gameState != GameState.ACTIVE) {
            g.setColor(new Color(RED.getRed(), RED.getGreen(), RED.getBlue(), 127));
            g.setFont(robotoBold.deriveFont(smallOff * 3f));
            int textWidth = g.getFontMetrics().stringWidth(text);
            int textHeight = g.getFont().getSize();
            g.drawRect(squareWidth * 3, squareWidth * 3, (squareWidth * 2) + margins * 2, squareWidth + margins * 2);
            g.fillRect(squareWidth * 3, squareWidth * 3, (squareWidth * 2) + margins * 2, squareWidth + margins * 2);
            g.setColor(Color.BLACK);
            g.drawString(text, margins + boardWidth / 2 - textWidth / 2, margins + height / 2 - textHeight / 2);
        }
    }

    public void paint(Graphics g) {
        int oldWidth = width;
        int oldHeight = height;
        setDimensions();
        margins = (int)(MARGINS_SCALE * width);
        if (cachedIcons.size() == 0 || oldWidth != width || oldHeight != height) {
            generateIcons();
        }

        // Clear double buffered background
        g.setColor(BACKGROUND);
        g.fillRect(0, 0, getSize().width, getSize().height);
        g.setColor(Color.BLACK);

        drawBoard(g);
        drawTime(g);
        drawEval(g);

        if (selectedSquare != -1 && game.board.board[selectedSquare] != -1 && dragging) {
            dragSelected(g);
        }

        drawGameState(g);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        repaint();
    }
}
