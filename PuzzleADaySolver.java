import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class PuzzleADaySolver {

    public static void main(String[] args) {
        new PuzzleADaySolver().run();
    }

    private static final int EMPTY = -1;
    private static final int BLOCKED = -2;

    private void run() {

        // Some 'positions' are outside the board:
        int[] illegal = new int[]{6, 13, 49, 50, 51, 52};

        // Pointers to a date/day:
        List<Integer> months = Arrays.asList(new Integer[]{ 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11});
        List<Integer> days = IntStream.range(14, 45).boxed().collect(Collectors.toList());
        List<Integer> weekdays = Arrays.asList(new Integer[]{45, 46, 47, 48, 53, 54, 55});

        // All unique puzzle pieces (with flipped/rotations):
        List<int[][]>[] pieceRotations = new List[10];
        pieceRotations[0] = getUniqueRotations(new int[][]{{1, 1, 1, 0}, {0, 0, 1, 1}});
        pieceRotations[1] = getUniqueRotations(new int[][]{{1, 1, 0}, {0, 1, 0}, {0, 1, 1}});
        pieceRotations[2] = getUniqueRotations(new int[][]{{1, 1}, {1, 1}, {0, 1}});
        pieceRotations[3] = getUniqueRotations(new int[][]{{1, 1}, {1, 0}, {1, 1}});
        pieceRotations[4] = getUniqueRotations(new int[][]{{0, 1}, {0, 1}, {1, 1}});
        pieceRotations[5] = getUniqueRotations(new int[][]{{0, 1, 1}, {1, 1, 0}});
        pieceRotations[6] = getUniqueRotations(new int[][]{{1, 0, 0, 0}, {1, 1, 1, 1}});
        pieceRotations[7] = getUniqueRotations(new int[][]{{1, 0, 0}, {1, 0, 0}, {1, 1, 1}});
        pieceRotations[8] = getUniqueRotations(new int[][]{{0, 0, 1}, {1, 1, 1}, {0, 0, 1}});
        pieceRotations[9] = getUniqueRotations(new int[][]{{1, 1, 1, 1}});


        for(int month : months) {
            for(int day : days) {
                for(int weekday : weekdays) {

                    int[] filledBoard = new int[7 * 8];
                    Arrays.fill(filledBoard, EMPTY);

                    for (int i : illegal) {
                        filledBoard[i] = BLOCKED;
                    }
                    // How to set up/read a day:
                    filledBoard[month] = BLOCKED;
                    filledBoard[day] = BLOCKED;
                    filledBoard[weekday] = BLOCKED;

                    String solution = fillBoard(filledBoard, pieceRotations, 0);

                    int[] board = Arrays.stream(solution.substring(1, solution.length()-1).split(",")).map(String::trim).mapToInt(Integer::parseInt).toArray();
                    System.out.println("Solution for: " + (days.indexOf(day)+1) + "/" + (months.indexOf(month)+1) +" [" + (weekdays.indexOf(weekday)+1)+"]:");
                    print(board);
                }
            }
        }
    }

    private String fillBoard(int[] board, List<int[][]>[] pieceRotations, int pieceToPlace) {
        if(pieceToPlace == 10) {
            // Done
            return Arrays.toString(board);
        }

        // Try this piece and its rotations on all positions: (yeah dumb/exhaustive)
        for(int[][] rotation : pieceRotations[pieceToPlace]) {
            for(int position = 0; position < board.length; position++) {

                // Try the fit:
                int ox = position % 7;
                int oy = position / 7;

                // skip if we go outside the board
                if(ox + rotation[0].length > 7) {
                    continue;
                }
                if(oy + rotation.length > 8) {
                    continue;
                }

                List<Integer> boardPositionsOfPiece = new ArrayList<>();
                boolean stillFits = true;
                for(int y = 0; stillFits && y < rotation.length; y++) {
                    for(int x = 0; stillFits && x < rotation[0].length; x++) {
                        if(rotation[y][x] == 1) {
                            int pos = position + y*7 + x;
                            if(board[pos] == EMPTY) {
                                boardPositionsOfPiece.add(pos);
                            } else {
                                stillFits = false;
                            }
                        }
                    }
                }

                // If this piece stillFits and we don't create a small disjoined group (which will be invalid anyway)
                if(stillFits) {
                    // Make the moves:
                    for(int i : boardPositionsOfPiece) board[i] = pieceToPlace;
                    if (pieceToPlace >= 8 || getSmallestGroup(board) > 4) {
                        String solution = fillBoard(board, pieceRotations, pieceToPlace + 1);
                        if (solution != null) {
                            return solution;
                        }
                    }
                    // Undo the moves:
                    for(int i : boardPositionsOfPiece) board[i] = EMPTY;
                }
            }
        }
        return null;
    }

    private List<int[][]> getUniqueRotations(final int[][] piece) {
        List<int[][]> pieceRotations = new ArrayList<>();
        pieceRotations.add(piece);
        // Add the three rotations:
        for(int i = 0; i < 3; i++) {
            pieceRotations.add(createRotation(pieceRotations.get(pieceRotations.size()-1)));
        }
        for(int i = 0; i < 4; i++) {
            pieceRotations.add(createFlip(pieceRotations.get(i)));
        }

        // Dedup:
        List<int[][]> uniqueRotations = new ArrayList<>();
        for(int[][] p:pieceRotations) {
            boolean isUnique = true;
            for(int[][] u : uniqueRotations) {
                if(isEqual(p,u)) {
                    isUnique = false;
                    break;
                }
            }
            if(isUnique) {
                uniqueRotations.add(p);
            }
        }
        return uniqueRotations;
    }

    /**
     * A LOT of invalid solutions can be removed if we don't allow small islands with <4 blocks
     * This makes the runtime managable, probably easier and better ways to do this, IDGAF
     */
    boolean[] filledIn = new boolean[7*8];
    private int getSmallestGroup(final int[] board) {
        // Fill every spot and count group-size:
        Arrays.fill(filledIn, false);

        int smallestGroup = Integer.MAX_VALUE;
        for(int i = 0; i < board.length; i++) {
            if(!filledIn[i] && board[i] == EMPTY) {
                smallestGroup = Math.min(smallestGroup, floodfill(filledIn, board, i));
            }
        }
        return smallestGroup;
    }

    private int floodfill(boolean[] filledIn, int[] board, int p) {
        int addedToGroup = 1;
        filledIn[p] = true;
        // Check all the neighbors:
        if(p-7 > 0 && !filledIn[p-7] && board[p-7] == EMPTY) {
            addedToGroup += floodfill(filledIn, board, p-7);
        }
        if(p+7 < board.length && !filledIn[p+7] && board[p+7] == EMPTY) {
            addedToGroup += floodfill(filledIn, board, p+7);
        }
        if(p%7 > 0 && !filledIn[p-1] && board[p-1] == EMPTY) {
            addedToGroup += floodfill(filledIn, board, p-1);
        }
        if(p%7 < 6 && !filledIn[p+1] && board[p+1] == EMPTY) {
            addedToGroup += floodfill(filledIn, board, p+1);
        }
        return addedToGroup;
    }

    private boolean isEqual(int[][] p1, int[][] p2) {
        if(p1.length != p2.length) {
            return false;
        }
        if(p1[0].length != p2[0].length) {
            return false;
        }

        for(int i = 0; i < p1.length; i++) {
            if(!Arrays.equals(p1[i],p2[i])) {
                return false;
            }
        }
        return true;
    }

    private int[][] createFlip(final int[][] piece) {
        int[][] rotation = new int[piece.length][piece[0].length];
        for(int x = 0; x < piece.length; x++) {
            for(int y = 0; y < piece[0].length; y++) {
                rotation[piece.length-1-x][y] = piece[x][y];
            }
        }
        return rotation;
    }

    private int[][] createRotation(final int[][] piece) {
        int[][] rotation = new int[piece[0].length][piece.length];
        for(int x = 0; x < piece.length; x++) {
            for(int y = 0; y < piece[0].length; y++) {
                rotation[y][piece.length-1-x] = piece[x][y];
            }
        }
        return rotation;
    }

    void print(int[] board) {
        for(int y = 0; y < 8; y++) {
            for(int x = 0; x < 7; x++) {

                if (board[y * 7 + x] >= 0) {
                    System.out.print(board[y * 7 + x] + " ");
                } else {
                    if (board[y * 7 + x] == EMPTY) {
                        System.out.print("_ ");
                    } else {
                        System.out.print("X ");
                    }
                }
            }
            System.out.println();
        }
    }
}
