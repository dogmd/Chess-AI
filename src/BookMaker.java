import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.*;

public class BookMaker {
    static final String FILE_NAMES = "abcdefgh";
    static final int MAX_PLY = 12;
    private static Map<Long, Set<Move>> loadedBook;

    public static Map<Long, Set<Move>> getBook(String path) {
        readBook();
        if (loadedBook != null) {
            return loadedBook;
        }
        Scanner bookFile = new Scanner(ClassLoader.getSystemResourceAsStream(path));
        Map<Long, Set<Move>> book = new HashMap<>();

        while (bookFile.hasNextLine()) {
            String[] moveList = bookFile.nextLine().split(" ");
            Game game = new Game();
            int ply = 0;
            for (String alMove : moveList) {
                if (ply > MAX_PLY) {
                    break;
                }
                if (!alMove.contains(".") && alMove.length() > 0) {
                    Move move = getMove(alMove, game.board);

                    if (!book.containsKey(game.board.zobristKey)) {
                        book.put(game.board.zobristKey, new HashSet<>());
                    }
                    book.get(game.board.zobristKey).add(move);

                    game.makeMove(move);
                    ply++;
                }
            }
        }

        loadedBook = book;
        writeBook(book);
        return book;
    }

    private static void readBook() {
        try {
            if (loadedBook == null) {
                Scanner bookFile = new Scanner(ClassLoader.getSystemResourceAsStream("book.txt"));
                loadedBook = new HashMap<>();
                while (bookFile.hasNextLine()) {
                    String line = bookFile.nextLine();
                    long key = Long.parseLong(line.substring(0, line.indexOf(':')));
                    if (!loadedBook.containsKey(key)) {
                        loadedBook.put(key, new HashSet<>());
                    }
                    String[] moves = line.substring(line.indexOf(':') + 1).split(", ");
                    for (String move : moves) {
                        loadedBook.get(key).add(new Move(Integer.parseInt(move.trim())));
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    private static void writeBook(Map<Long, Set<Move>> book) {
        PrintWriter pw;
        try {
            pw = new PrintWriter("book.txt");
            for (long key : book.keySet()) {
                StringBuilder moves = new StringBuilder();
                for (Move move : book.get(key)) {
                    moves.append(move.hashCode());
                    moves.append(", ");
                }
                pw.println(key + ": " + moves.toString().substring(0, moves.length() - 2));
            }
            pw.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static Move getMove(String al, Board board) {
        al = al.replaceAll("[+#x-]", "");
        Move move = null;
        for (Move testMove : board.moves) {
            move = testMove;
            int type = Piece.getType(move.actor);
            int color = Piece.getColor(move.actor);
            int startRow = move.start / 8;
            int startCol = move.start - startRow * 8;
            int endRow = move.end / 8;
            int endCol = move.end - endRow * 8;

            if (al.equals("OO")) {
                // king side castle
                if (type == Piece.KING && move.end - move.start == 2) {
                    return move;
                }
            } else if (al.equals("OOO")) {
                // queen side castle
                if (type == Piece.KING && move.end - move.start == -2) {
                    return move;
                }
            } else if (FILE_NAMES.contains(al.charAt(0) + "")) {
                // pawn move
                if (type != Piece.PAWN) {
                    continue;
                }
                if (FILE_NAMES.indexOf(al.charAt(0)) == startCol) {
                    if (al.contains("=")) {
                        // promotion
                        if (endRow == 0 || endRow == 7) {
                            if (al.length() == 5) {
                                // capture promotion
                                if (FILE_NAMES.indexOf(al.charAt(1)) != endCol) {
                                    continue;
                                }

                                int promoteTo = color << 3 | getPieceFromChar(al.charAt(al.length() - 1));
                                if (move.promoteTo != promoteTo) {
                                    continue;
                                }

                                return move;
                            }
                        }
                    } else {
                        // regular pawn move
                        int targetRow = '8' - al.charAt(al.length() - 1);
                        int targetCol = al.charAt(al.length() - 2) - 'a';
                        if (endRow == targetRow && endCol == targetCol) {
                            break;
                        }
                    }
                }
            } else {
                // regular piece move
                if (getPieceFromChar(al.charAt(0)) != type) {
                    continue;
                }

                int targetRow = '8' - al.charAt(al.length() - 1);
                int targetCol = al.charAt(al.length() - 2) - 'a';
                if (endRow == targetRow && endCol == targetCol) {
                    if (al.length() == 4) {
                        // al contains disambiguation
                        char disambiguationChar = al.charAt(1);
                        if (FILE_NAMES.contains(disambiguationChar + "")) {
                            if (FILE_NAMES.indexOf(disambiguationChar) != startCol) {
                                continue;
                            }
                        } else {
                            int disambiguationRow = '8' - disambiguationChar;
                            if (disambiguationRow != startRow) {
                                continue;
                            }
                        }
                    }
                    break;
                }
            }
        }
        return move;
    }

    public static int getPieceFromChar(char sym) {
        switch (sym) {
            case 'N': return Piece.KNIGHT;
            case 'B': return Piece.BISHOP;
            case 'R': return Piece.ROOK;
            case 'Q': return Piece.QUEEN;
            case 'K': return Piece.KING;
            default: return -1;
        }
    }
}
