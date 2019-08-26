package org.rixon;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;

public class Main {

    public static void main(String[] args) throws IOException {
        final String filename = args[1];
        int n = Integer.parseInt(args[0]);
        final int steps = 5;

        int[][] board = readBoard(filename, n);

        printBoard(n, board);

        for(int i = 0; i < steps; i++) {
            board = embiggenBoard(n, board);
            n+=2;
            int[][] previous = prev(n, board);
            if (previous == null) {
                System.out.println("No previous board found.");
                return;
            }
            printBoard(n, previous);
            board = previous;
        }

        System.out.println("--------------------------------");

        for(int i = 0; i < steps; i++) {
            board = step(board);
            printBoard(n, board);
        }

    }

    private static int[][] prev(int n, int[][] board) {
        Model model = new Model("efil");

        BoolVar[][] vars = new BoolVar[n][];
        for (int y = 0; y < n; y++) {
            vars[y] = new BoolVar[n];
            for (int x = 0; x < n; x++) {
                vars[y][x] = model.boolVar("E" + y + x);
            }
        }

        for (int y = 0; y < n; y++) {
            for (int x = 0; x < n; x++) {
                BoolVar[] neighbours = getNeighbours(y, x, n, vars);
                Constraint neighbourSumIs2 = model.sum(neighbours, "=", 2);
                Constraint neighbourSumIs3 = model.sum(neighbours, "=", 3);
                Constraint neighbourSumIs2or3 = model.or(neighbourSumIs2, neighbourSumIs3);
                if (board[y][x] > 0) {
                    model.ifThenElse(vars[y][x], neighbourSumIs2or3, neighbourSumIs3);
                } else {
                    model.ifThenElse(vars[y][x], model.not(neighbourSumIs2or3), model.not(neighbourSumIs3));
                }
            }
        }

        IntVar sum = getBorderOptimisation(n, model, vars);


        Solution solution = model.getSolver().findOptimalSolution(sum, false);
        System.out.println("Solution found. Score (minimised) = " + solution.getIntVal(sum));

        if (solution == null) {
            return null;
        }

        int[][] prevBoard = newBoard(n);
        for (int y = 0; y < n; y++) {
            for (int x = 0; x < n; x++) {
                prevBoard[y][x] = solution.getIntVal(vars[y][x]);
            }
        }
        return prevBoard;
    }

    private static IntVar getBorderOptimisation(int n, Model model, BoolVar[][] vars) {
        BoolVar[] border = new BoolVar[(n-1)*4];
        for (int i = 0; i < n-1; i++) {
            border[i*4  ] = vars[  0][  i];
            border[i*4+1] = vars[n-1][i+1];
            border[i*4+2] = vars[i+1][  0];
            border[i*4+3] = vars[  i][n-1];
        }

        int[] coefficients = new int[border.length];
        Arrays.fill(coefficients, 1);
        IntVar sum = model.intVar("sum", 0, border.length+1);
        model.scalar(border, coefficients, "=", sum).post();
        return sum;
    }

    private static IntVar getFullOptimisation(int n, Model model, BoolVar[][] vars) {
        BoolVar[] border = new BoolVar[(n-1)*4];
        for (int i = 0; i < n-1; i++) {
            border[i*4  ] = vars[  0][  i];
            border[i*4+1] = vars[n-1][i+1];
            border[i*4+2] = vars[i+1][  0];
            border[i*4+3] = vars[  i][n-1];
        }

        int[] coefficients = new int[border.length];
        Arrays.fill(coefficients, 1);
        IntVar sum = model.intVar("sum", 0, border.length+1);
        model.scalar(border, coefficients, "=", sum).post();
        return sum;
    }

    private static BoolVar[] getNeighbours(int y, int x, int n, BoolVar[][] vars) {
        int a = 0;
        for (int yy = y-1; yy <= y+1; yy++) {
            for (int xx = x-1; xx <= x+1; xx++) {
                if (xx >= 0 && xx < n && yy >= 0 && yy < n && !(xx == x && yy == y)) {
                    a++;
                }
            }
        }

        BoolVar[] neighbours = new BoolVar[a];
        int m = 0;
        for (int yy = y-1; yy <= y+1; yy++) {
            for (int xx = x-1; xx <= x+1; xx++) {
                if (xx >= 0 && xx < n && yy >= 0 && yy < n && !(xx == x && yy == y)) {
                    neighbours[m] = vars[yy][xx];
                    m++;
                }
            }
        }
        return neighbours;
    }

    private static int[][] step(int[][] board) {
        int h = board.length;
        int[][] next = new int[h][];
        for(int y = 0; y < h; y++) {
            int w = board[y].length;
            next[y] = new int[w];
            for(int x = 0; x < w; x++) {
                int sum = 0;
                for (int xx = x == 0 ? 0 : x - 1; xx < x + 2 && xx < w; xx++) {
                    for (int yy = y == 0 ? 0 : y - 1; yy < y + 2 && yy < h; yy++) {
                        if (xx!=x || yy!=y) {
                            sum += board[yy][xx];
                        }
                    }
                }
                next[y][x] = board[y][x] == 0 ? sum == 3 ? 1 : 0 : sum == 2 || sum == 3 ? 1 : 0;
            }
        }
        return next;
    }

    private static void printBoard(int n, int[][] board) {
        System.out.println();
        for (int y = 0; y < n; y++) {
            StringBuilder line = new StringBuilder();
            for(int x = 0; x < n; x++) {
                line.append(board[y][x] == 1 ? 'O' : '.');
            }
            System.out.println(line);
        }
        System.out.println();
    }

    private static int[][] readBoard(String filename, int n) throws IOException {
        BufferedReader reader = new BufferedReader( new FileReader(filename));

        int[][] board = newBoard(n);

        for (int y = 0; y < n; y++) {
            String line = reader.readLine();
            for (int x = 0; x < n; x++) {
                board[y][x] = line.charAt(x) == 'O' ? 1 : 0;
            }
        }
        return board;
    }

    private static int[][] newBoard(int n) {
        int[][] board = new int[n][];
        for (int y = 0; y < n; y++) {
            board[y] = new int[n];
        }
        return board;
    }

    private static int[][] embiggenBoard(int n, int[][] board) {
        int[][] next = new int[n+2][];
        for(int y = 0; y < n+2; y++) {
            next[y] = new int[n+2];
        }

        for(int y = 0; y < n; y++) {
            System.arraycopy(board[y], 0, next[y + 1], 1, n);
        }
        return next;
    }

}
