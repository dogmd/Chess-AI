import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.image.PNGTranscoder;

import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
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
            gameView.selectedSquare = null;
            gameView.highlightedSquares.clear();
            gameView.alertedSquares.clear();
            game.unmakeMove(undone);
            gameView.selectedSquare = undone.start;
        } else if (e.getKeyCode() == KeyEvent.VK_T) {
            gameView.showThreatened = !gameView.showThreatened;
        } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            game.togglePause();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {}

    public void playNotify() {
        playSound("sounds/GenericNotify.wav");
    }

    public void playSound(String path) {
        AudioInputStream audioInputStream = null;
        Clip clip = null;
        try {
            audioInputStream = AudioSystem.getAudioInputStream(new File(path));
            clip = AudioSystem.getClip();
            clip.open(audioInputStream);
            clip.start();
        } catch (Exception e) {
            e.printStackTrace();
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
            alertedSquares.clear();
            mouseX = e.getX();
            mouseY = e.getY();
            dragging = true;
            Square clicked = getSelectedSquare(e.getX(), e.getY());
            if (clicked != null) {
                if (selectedSquare != null) {
                    handleMove(clicked, e.getX(), e.getY());
                }
                if (clicked.isOccupied() && clicked.piece.color == game.getActiveColor()) {
                    selectedSquare = clicked;
                    options = MoveGenerator.generateMoves(game.board, selectedSquare.piece);
                } else {
                    selectedSquare = null;
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
            Square sq = getSelectedSquare(e.getX(), e.getY());
            handleMove(sq, e.getX(), e.getY());
            repaint();
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            super.mouseDragged(e);
            if (selectedSquare != null && selectedSquare.isOccupied()) {
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
    Square selectedSquare;
    Map<String, BufferedImage> cachedIcons;
    ArrayList<Square> highlightedSquares;
    ArrayList<Square> alertedSquares;
    ArrayList<Move> options;
    int margins, boardWidth, squareWidth, smallOff;

    public GameView(Game game) {
        super(true);
        this.game = game;
        options = new ArrayList<>();
        highlightedSquares = new ArrayList<>();
        alertedSquares = new ArrayList<>();
        cachedIcons = new HashMap<>();
        try {
            robotoBold = Font.createFont(Font.TRUETYPE_FONT, new File("fonts/Roboto-Bold.ttf"));
            robotoBlack = Font.createFont(Font.TRUETYPE_FONT, new File("fonts/Roboto-Black.ttf"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);
        timer.start();
    }

    public void handleMove(Square clicked, int x, int y) {
        if (selectedSquare != null && selectedSquare != clicked && selectedSquare.isOccupied()) {
            PieceType selected;
            for (Move move : options) {
                if (clicked == move.end) {
                    if (move.type == Move.PROMOTION) {
                        selected = getSelectedPromotion(clicked, x, y);
                        if (selected == move.promoteTo) {
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
        selectedSquare = null;
        options.clear();
        Main.makeMove("HUMAN", game, move);
    }

    public PieceType getSelectedPromotion(Square sq, int x, int y) {
        double sqWidth = squareWidth;
        int topLeftX = margins + (int)(sqWidth * sq.col);
        int topLeftY = margins + (int)(sqWidth * sq.row);
        if (x < topLeftX + sqWidth / 2 && y < topLeftY + sqWidth / 2) {
            return PieceType.KNIGHT;
        } else if (x >= topLeftX + sqWidth / 2 && y >= topLeftY + sqWidth / 2) {
            return PieceType.ROOK;
        } else if (x < topLeftX + sqWidth / 2 && y >= topLeftY + sqWidth / 2) {
            return PieceType.BISHOP;
        } else {
            return PieceType.QUEEN;
        }
    }

    public Square getSelectedSquare(int x, int y) {
        Square sq = null;
        int minX = margins;
        int minY = margins;
        int maxX = margins + boardWidth;
        int maxY = margins + boardWidth;
        if(x > minX && x < maxX && y > minY && y < maxY) {
            int col = (x - minX) / squareWidth;
            int row = (y - minY) / squareWidth;
            sq = game.board.board[row][col];
        }
        return sq;
    }

    public void generateIcons() {
        cachedIcons.clear();
        squareWidth = squareWidth <= 0 ? 1 : squareWidth;
        for (int i = 0; i <= 1; i++) {
            for (PieceType p : PieceType.values()) {
                Piece piece = new Piece(p, i);
                String fileName = "icons/" + piece.toString().toLowerCase() + (piece.isWhite() ? "w" : "b") + ".svg";
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

        TranscoderInput input = new TranscoderInput(svgFile);
        try {
            imageTranscoder.transcode(input, null);
        } catch (TranscoderException e) {
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
        Square[][] board = game.board.board;
        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board[i].length; j++) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setStroke(new BasicStroke(2));
                g.setColor(Color.BLACK);
                g.drawRect(margins + (j * squareWidth), margins + (i * squareWidth), squareWidth, squareWidth);
                Square sq = board[i][j];
                if (sq.isLight()) {
                    g.setColor(LIGHT_GREY);
                } else {
                    g.setColor(DARK_GREY);
                }
                if (highlightedSquares.contains(sq)) {
                    g.setColor(BLUE);
                }
                if (alertedSquares.contains(sq)) {
                    g.setColor(YELLOW);
                }
                if (selectedSquare == sq) {
                    g.setColor(GREEN);
                }
                g.fillRect(margins + (j * squareWidth), margins + (i * squareWidth), squareWidth, squareWidth);
            }
        }
        drawOptions(g);
        if (showThreatened) {
            for (Square sq : game.board.threatening) {
                g.setColor(BLUE);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setStroke(new BasicStroke(4));
                g2d.fillRect(margins + sq.col * squareWidth, margins + sq.row * squareWidth, squareWidth, squareWidth);
            }
            for (Square sq : game.board.checkPath) {
                g.setColor(YELLOW);
                g.fillRect(margins + sq.col * squareWidth, margins + sq.row * squareWidth, squareWidth, squareWidth);
            }
            for (Pin pin : game.board.pins) {
                g.setColor(new Color((float)Math.random(), (float)Math.random(), (float)Math.random()));
                for (Square sq : pin.path) {
                    g.fillRect(margins + sq.col * squareWidth, margins + sq.row * squareWidth, squareWidth, squareWidth);
                }
            }
        }
        drawPieces(g);

        for (int i = 0; i < board.length; i++) {
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
        for (java.util.List<java.util.List<Piece>> allColors : game.board.pieces.values()) {
            for (java.util.List<Piece> color : allColors) {
                for (int i = color.size() - 1; i >= 0; i--) {
                    Piece p = color.get(i);
                    Square sq = p.square;
                    if (sq != null && sq.isOccupied() && (sq != selectedSquare || !dragging)) {
                        String fileName = "icons/" + Character.toLowerCase(sq.piece.getChar()) + (sq.piece.isWhite() ? "w" : "b") + ".svg";
                        g.drawImage(cachedIcons.get(fileName), margins + (sq.col * squareWidth), margins + (sq.row * squareWidth), this);
                    }
                }
            }
        }
    }

    public void drawOptions(Graphics g) {
        if (selectedSquare != null && selectedSquare.isOccupied()) {
            if (options.size() == 0) {
                options = MoveGenerator.generateMoves(game.board, selectedSquare.piece);
            }
            for (Move move : options) {
                if (move.type == Move.PROMOTION) {
                    String color = move.actor.color == Piece.WHITE ? "w" : "b";
                    String fileName = "icons/promote" + color + ".svg";
                    g.drawImage(cachedIcons.get(fileName), margins + (move.end.col * squareWidth), margins + (move.end.row * squareWidth), this.getParent());
                } else {
                    if (move.isCapture()) {
                        Graphics2D g2 = (Graphics2D) g;
                        g2.setStroke(new BasicStroke(2));
                        g.setColor(Color.BLACK);
                        g.drawRect(margins + (squareWidth * move.end.col), margins + (squareWidth * move.end.row), squareWidth, squareWidth);
                        g.setColor(RED);
                        g.fillRect(margins + (squareWidth * move.end.col), margins + (squareWidth * move.end.row), squareWidth, squareWidth);
                    } else {
                        g.setColor((move.end.isLight() ? LIGHT_GREY : DARK_GREY).darker());
                        int x = margins + (squareWidth * move.end.col) + squareWidth / 4;
                        int y = margins + (squareWidth * move.end.row) + squareWidth / 4;
                        int diam = squareWidth / 2;
                        g.drawOval(x, y, diam, diam);
                        g.fillOval(x, y, diam, diam);
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

    public void dragSelected(Graphics g) {
        String fileName = "icons/" + Character.toLowerCase(selectedSquare.piece.getChar()) + (selectedSquare.piece.isWhite() ? "w" : "b") + ".svg";
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

        if (selectedSquare != null && selectedSquare.isOccupied() && dragging) {
            dragSelected(g);
        }

        drawGameState(g);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        repaint();
    }
}
