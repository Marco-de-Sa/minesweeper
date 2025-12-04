package MinesweeperGUI;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Random;

public class MinesweeperGUI extends JFrame {
    private final int rows;
    private final int cols;
    private final int mines;
    private final CellButton[][] buttons;
    private final Cell[][] board;
    private boolean firstClick = true;
    private boolean gameOver = false;

    public MinesweeperGUI(int rows, int cols, int mines) {
        /*
        * this handles the GUI for the minesweeper game
        * */
        super("Minesweeper");
        this.rows = rows;
        this.cols = cols;
        this.mines = Math.min(mines, rows * cols - 1);
        this.buttons = new CellButton[rows][cols];
        this.board = new Cell[rows][cols];

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel grid = new JPanel(new GridLayout(rows, cols));
        grid.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));
        initBoard(grid);

        add(grid, BorderLayout.CENTER);
        pack();
        setResizable(false);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void initBoard(JPanel grid) {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                board[r][c] = new Cell();
                CellButton btn = new CellButton(r, c);
                btn.setPreferredSize(new Dimension(40, 40));
                btn.setFont(btn.getFont().deriveFont(Font.BOLD, 14f));
                btn.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        if (gameOver) return;
                        if (SwingUtilities.isRightMouseButton(e)) {
                            toggleFlag(btn);
                        } else if (SwingUtilities.isLeftMouseButton(e)) {
                            reveal(btn);
                        }
                    }
                });
                buttons[r][c] = btn;
                grid.add(btn);
            }
        }
    }

    private void placeMines(int safeR, int safeC) {
        Random rnd = new Random();
        int placed = 0;
        while (placed < mines) {
            int r = rnd.nextInt(rows);
            int c = rnd.nextInt(cols);
            // don't place on the first clicked cell or duplicate
            if ((r == safeR && c == safeC) || board[r][c].mine) continue;
            board[r][c].mine = true;
            placed++;
        }
        computeAdjacents();
    }

    private void computeAdjacents() {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (board[r][c].mine) continue;
                int count = 0;
                for (int dr = -1; dr <= 1; dr++) {
                    for (int dc = -1; dc <= 1; dc++) {
                        int nr = r + dr, nc = c + dc;
                        if (nr < 0 || nr >= rows || nc < 0 || nc >= cols) continue;
                        if (board[nr][nc].mine) count++;
                    }
                }
                board[r][c].adjacent = count;
            }
        }
    }

    private void toggleFlag(CellButton btn) {
        Cell cell = board[btn.r][btn.c];
        if (cell.revealed) return;
        cell.flagged = !cell.flagged;
        btn.setText(cell.flagged ? "F" : "");
        btn.setForeground(Color.RED);
    }

    private void reveal(CellButton btn) {
        int r = btn.r, c = btn.c;
        if (board[r][c].flagged || board[r][c].revealed) return;

        if (firstClick) {
            placeMines(r, c);
            firstClick = false;
        }

        if (board[r][c].mine) {
            // reveal all mines and end game
            btn.setText("B");
            btn.setBackground(Color.PINK);
            board[r][c].revealed = true;
            revealAllMines();
            endGame(false);
            return;
        }

        floodReveal(r, c);
        checkWin();
    }

    private void floodReveal(int r, int c) {
        if (r < 0 || r >= rows || c < 0 || c >= cols) return;
        Cell cell = board[r][c];
        CellButton btn = buttons[r][c];
        if (cell.revealed || cell.flagged) return;

        cell.revealed = true;
        btn.setEnabled(false);
        btn.setBackground(Color.LIGHT_GRAY);

        if (cell.adjacent > 0) {
            btn.setText(Integer.toString(cell.adjacent));
            btn.setForeground(colorForNumber(cell.adjacent));
            return;
        }

        // if zero adjacent, reveal neighbors
        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                if (dr == 0 && dc == 0) continue;
                floodReveal(r + dr, c + dc);
            }
        }
    }

    private Color colorForNumber(int n) {
        switch (n) {
            case 1: return Color.BLUE;
            case 2: return new Color(0, 128, 0); // green
            case 3: return Color.RED;
            case 4: return new Color(0, 0, 128);
            case 5: return new Color(128, 0, 0);
            case 6: return new Color(0, 128, 128);
            case 7: return Color.BLACK;
            case 8: return Color.GRAY;
            default: return Color.BLACK;
        }
    }

    private void revealAllMines() {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (board[r][c].mine) {
                    CellButton btn = buttons[r][c];
                    btn.setText("B");
                    btn.setEnabled(false);
                    btn.setBackground(Color.PINK);
                }
            }
        }
    }

    private void checkWin() {
        int unrevealed = 0;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (!board[r][c].revealed) unrevealed++;
            }
        }
        if (unrevealed == mines) {
            endGame(true);
        }
    }

    private void endGame(boolean won) {
        gameOver = true;
        String message = won ? "You win!" : "Game over.";
        JOptionPane.showMessageDialog(this, message);
    }

    // small helper classes
    private static class Cell {
        boolean mine = false;
        boolean revealed = false;
        boolean flagged = false;
        int adjacent = 0;
    }

    private static class CellButton extends JButton {
        final int r, c;
        CellButton(int r, int c) { super(); this.r = r; this.c = c; }
    }

    public static void main(String[] args) {
        // default 9x9 with 10 mines. allows args: rows, columns and mines count
        SwingUtilities.invokeLater(() -> {
            int r = 9, c = 9, m = 10;
            if (args.length >= 3) {
                try {
                    r = Math.max(5, Integer.parseInt(args[0]));
                    c = Math.max(5, Integer.parseInt(args[1]));
                    m = Math.max(1, Integer.parseInt(args[2]));
                } catch (NumberFormatException ignored) {}
            }
            new MinesweeperGUI(r, c, m);
        });
    }
}