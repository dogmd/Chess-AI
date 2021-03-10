import java.util.ArrayList;

// Translated from https://github.com/SebLague/Chess-AI/blob/main/Assets/Scripts/Core/PrecomputedMoveData.cs

public class PrecomputedMoveData {
    public static final int[] ALL_OFFSETS = new int[]{8, -8, -1, 1, 7, -7, 9, -9};
    public static final int[] KNIGHT_OFFSETS = new int[]{17, -17, 15, -15, 10, -10, 6, -6};

    public static int[][] distToEdge; // distance to edge with offset at index
    public static int[][] knightMoves; // valid next indices for a knight at index
    public static int[][] kingMoves; // valid next indices for a king at index
    public static int[][] whitePawnAttacks; // valid white pawn attack indices at index
    public static int[][] blackPawnAttacks; // valid black pawn attack indices at index
    public static int[] directionLookup;
    public static int[] distToCenter;
    public static int[][] distBetween;

    public static void calculate() {
        whitePawnAttacks = new int[64][];
        blackPawnAttacks = new int[64][];
        knightMoves = new int[64][];
        kingMoves = new int[64][];
        distToEdge = new int[64][];

        for (int ind = 0; ind < 64; ind++) {
            int y = ind / 8;
            int x = ind - y * 8;

            int north = y;
            int south = 7 - y;
            int east = x;
            int west = 7 - x;
            distToEdge[ind] = new int[8];
            distToEdge[ind][0] = south;
            distToEdge[ind][1] = north;
            distToEdge[ind][2] = east;
            distToEdge[ind][3] = west;
            distToEdge[ind][4] = Math.min(south, east);
            distToEdge[ind][5] = Math.min(north, west);
            distToEdge[ind][6] = Math.min(south, west);
            distToEdge[ind][7] = Math.min(north, east);

            ArrayList<Integer> validKnightJumps = new ArrayList<>();
            for (int offset : KNIGHT_OFFSETS) {
                int newInd = ind + offset;
                if (newInd >= 0 && newInd < 64) {
                    int newY = newInd / 8;
                    int newX = newInd - newY * 8;
                    int maxOff = Math.max(Math.abs(x - newX), Math.abs(y - newY));
                    if (maxOff == 2) {
                        validKnightJumps.add(newInd);
                    }
                }
            }
            knightMoves[ind] = validKnightJumps.stream().mapToInt(i -> i).toArray();

            ArrayList<Integer> validKingMoves = new ArrayList<>();
            for (int offset : ALL_OFFSETS) {
                int newInd = ind + offset;
                if (newInd >= 0 && newInd < 64) {
                    int newY = newInd / 8;
                    int newX = newInd - newY * 8;
                    int maxOff = Math.max(Math.abs(x - newX), Math.abs(y - newY));
                    if (maxOff == 1) {
                        validKingMoves.add(newInd);
                    }
                }
            }
            kingMoves[ind] = validKingMoves.stream().mapToInt(i -> i).toArray();

            ArrayList<Integer> validWhiteCaptures = new ArrayList<>();
            ArrayList<Integer> validBlackCaptures = new ArrayList<>();
            if (x > 0) {
                if (y < 7) {
                    validBlackCaptures.add(ind + 7);
                }
                if (y > 0) {
                    validWhiteCaptures.add(ind - 9);
                }
            }
            if (x < 7) {
                if (y < 7) {
                    validBlackCaptures.add(ind + 9);
                }
                if (y > 0) {
                    validWhiteCaptures.add(ind - 7);
                }
            }
            whitePawnAttacks[ind] = validWhiteCaptures.stream().mapToInt(i -> i).toArray();
            blackPawnAttacks[ind] = validBlackCaptures.stream().mapToInt(i -> i).toArray();
        }

        directionLookup = new int[127];
        for (int i = 0; i < 127; i++) {
            int offset = i - 63;
            int absOff = Math.abs(offset);
            int absDir = 1;
            if (absOff % 9 == 0) {
                absDir = 9;
            } else if (absOff % 8 == 0) {
                absDir = 8;
            } else if (absOff % 7 == 0) {
                absDir = 7;
            }
            directionLookup[i] = absDir * (offset >= 0 ? 1 : -1);
        }

        distToCenter = new int[64];
        distBetween = new int[64][64];
        for (int start = 0; start < 64; start++) {
            int startRow = start / 8;
            int startCol = start - startRow * 8;
            distToCenter[start] = Math.max(3 - startCol, startCol - 4) + Math.max(3 - startRow, startRow - 4);
            for (int end = 0; end < 64; end++) {
                int endRow = end / 8;
                int endCol = end - endRow * 8;
                distBetween[start][end] = Math.abs(startRow - endRow) + Math.abs(startCol - endCol);
            }
        }
    }
}
