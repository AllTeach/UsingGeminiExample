package com.example.usinggeminiexample;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure-logic model for Othello (Reversi).
 *
 * Board encoding
 *   0  = empty
 *   1  = WHITE  (human player)
 *  -1  = BLACK  (Gemini / AI)
 *
 * Coordinate system: row 0..7 top-to-bottom, col 0..7 left-to-right.
 */
public class OthelloModel {

    public static final int EMPTY = 0;
    public static final int WHITE = 1;   // human
    public static final int BLACK = -1;  // Gemini

    public static final int SIZE = 8;

    // 8 directions: {dRow, dCol}
    private static final int[][] DIRS = {
            {-1, -1}, {-1, 0}, {-1, 1},
            { 0, -1},           { 0, 1},
            { 1, -1}, { 1, 0}, { 1, 1}
    };

    private final int[][] board = new int[SIZE][SIZE];
    private int currentPlayer; // WHITE or BLACK

    public OthelloModel() {
        reset();
    }

    /** Reset to standard Othello start position. */
    public void reset() {
        for (int r = 0; r < SIZE; r++)
            for (int c = 0; c < SIZE; c++)
                board[r][c] = EMPTY;

        // Classic start
        board[3][3] = WHITE;
        board[3][4] = BLACK;
        board[4][3] = BLACK;
        board[4][4] = WHITE;

        currentPlayer = BLACK; // Black always goes first in Othello
    }

    // ── Accessors ────────────────────────────────────────────────────────────

    public int getCell(int row, int col) {
        return board[row][col];
    }

    public int getCurrentPlayer() {
        return currentPlayer;
    }

    /** Returns a deep copy of the 8x8 board array. */
    public int[][] getBoardCopy() {
        int[][] copy = new int[SIZE][SIZE];
        for (int r = 0; r < SIZE; r++)
            System.arraycopy(board[r], 0, copy[r], 0, SIZE);
        return copy;
    }

    // ── Legal-move logic ─────────────────────────────────────────────────────

    /** All legal moves for the current player. */
    public List<int[]> getLegalMoves() {
        return getLegalMovesFor(currentPlayer);
    }

    /** All legal moves for a given player. */
    public List<int[]> getLegalMovesFor(int player) {
        List<int[]> moves = new ArrayList<>();
        for (int r = 0; r < SIZE; r++)
            for (int c = 0; c < SIZE; c++)
                if (isLegalMove(r, c, player))
                    moves.add(new int[]{r, c});
        return moves;
    }

    /** True if placing a disc at (row,col) for 'player' is legal. */
    public boolean isLegalMove(int row, int col, int player) {
        if (board[row][col] != EMPTY) return false;
        for (int[] d : DIRS)
            if (!flipsInDirection(row, col, d[0], d[1], player).isEmpty())
                return true;
        return false;
    }

    // ── Move application ─────────────────────────────────────────────────────

    /**
     * Apply a move for the current player.
     * Returns the list of flipped positions (non-empty if the move was legal).
     * After a successful move, the turn is switched (skipping if the opponent
     * has no legal moves).
     */
    public List<int[]> applyMove(int row, int col) {
        List<int[]> allFlipped = new ArrayList<>();
        if (!isLegalMove(row, col, currentPlayer)) return allFlipped;

        board[row][col] = currentPlayer;
        for (int[] d : DIRS) {
            List<int[]> flipped = flipsInDirection(row, col, d[0], d[1], currentPlayer);
            for (int[] pos : flipped)
                board[pos[0]][pos[1]] = currentPlayer;
            allFlipped.addAll(flipped);
        }

        switchTurn();
        return allFlipped;
    }

    /** Switch to the other player; skip that player if they have no legal moves. */
    private void switchTurn() {
        int opponent = -currentPlayer;
        if (!getLegalMovesFor(opponent).isEmpty()) {
            currentPlayer = opponent;
        } else if (getLegalMovesFor(currentPlayer).isEmpty()) {
            // Neither player can move - game over; keep currentPlayer for score check
        }
        // else currentPlayer keeps their turn
    }

    // ── Game-over and scoring ──────────────────────────────────────────────────

    /** True when neither player has any legal moves. */
    public boolean isGameOver() {
        return getLegalMovesFor(WHITE).isEmpty() && getLegalMovesFor(BLACK).isEmpty();
    }

    public int countDiscs(int player) {
        int count = 0;
        for (int r = 0; r < SIZE; r++)
            for (int c = 0; c < SIZE; c++)
                if (board[r][c] == player) count++;
        return count;
    }

    /**
     * Returns WHITE, BLACK, or EMPTY (draw).
     * Only meaningful when isGameOver() is true.
     */
    public int getWinner() {
        int w = countDiscs(WHITE);
        int b = countDiscs(BLACK);
        if (w > b) return WHITE;
        if (b > w) return BLACK;
        return EMPTY;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Positions that would be flipped along direction (dr,dc) if 'player'
     * places at (row,col).  Returns empty list if no flips occur.
     */
    private List<int[]> flipsInDirection(int row, int col, int dr, int dc, int player) {
        List<int[]> candidates = new ArrayList<>();
        int r = row + dr, c = col + dc;
        while (inBounds(r, c) && board[r][c] == -player) {
            candidates.add(new int[]{r, c});
            r += dr;
            c += dc;
        }
        if (!inBounds(r, c) || board[r][c] != player) return new ArrayList<>();
        return candidates;
    }

    private boolean inBounds(int r, int c) {
        return r >= 0 && r < SIZE && c >= 0 && c < SIZE;
    }
}