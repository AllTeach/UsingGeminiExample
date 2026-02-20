package com.example.usinggeminiexample;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

/**
 * Othello (Reversi) — Player (White) vs Gemini AI (Black).
 *
 * Architecture: MVP-lite
 *   Model      → OthelloBoard
 *   View       → OthelloBoardView  +  TextViews
 *   Presenter  → this Activity
 */
public class OthelloActivity extends AppCompatActivity {

    // ── Replace with your API key ─────────────────────────────────────────────
    private static final String API_KEY = "YOUR_API_KEY_HERE!!!!";

    private OthelloBoard        board;
    private OthelloBoardView    boardView;
    private TextView            tvStatus;
    private TextView            tvScore;
    private Button              btnRestart;

    private GeminiManager       geminiManager;
    private boolean             geminiThinking = false;

    // ── Lifecycle ─────────────────────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_othello);

        geminiManager = new GeminiManager(API_KEY);

        boardView  = findViewById(R.id.othelloBoardView);
        tvStatus   = findViewById(R.id.tvOthelloStatus);
        tvScore    = findViewById(R.id.tvOthelloScore);
        btnRestart = findViewById(R.id.btnOthelloRestart);

        board = new OthelloBoard();
        boardView.setBoard(board);

        boardView.setOnCellClickListener((row, col) -> onHumanMove(row, col));

        btnRestart.setOnClickListener(v -> startNewGame());

        startNewGame();
    }

    // ── Game flow ─────────────────────────────────────────────────────────────

    private void startNewGame() {
        board.reset();
        geminiThinking = false;
        refreshUI();

        // If Gemini (Black) moves first, kick it off automatically
        if (board.getCurrentTurn() == OthelloBoard.BLACK) {
            askGeminiForMove();
        }
    }

    /** Called when the human taps a cell on the board. */
    private void onHumanMove(int row, int col) {
        if (geminiThinking) return;
        if (board.getCurrentTurn() != OthelloBoard.WHITE) return;

        boolean moved = board.makeMove(row, col, OthelloBoard.WHITE);
        if (!moved) return;

        refreshUI();

        if (board.isGameOver()) {
            showGameOver();
            return;
        }

        if (board.getCurrentTurn() == OthelloBoard.BLACK) {
            askGeminiForMove();
        } else {
            // Black had no moves — human plays again
            refreshUI();
        }
    }

    // ── Gemini integration ────────────────────────────────────────────────────

    private void askGeminiForMove() {
        geminiThinking = true;
        tvStatus.setText("🤖 Gemini is thinking…");
        boardView.setLegalMoves(null); // hide hints while AI thinks

        List<int[]> legalMoves = board.getLegalMoves(OthelloBoard.BLACK);
        if (legalMoves.isEmpty()) {
            // Gemini must pass
            geminiThinking = false;
            refreshUI();
            return;
        }

        String prompt = buildGeminiPrompt(legalMoves);
        String schema = buildResponseSchema();

        geminiManager.sendTextWithSchema(prompt, schema, response -> {
            runOnUiThread(() -> {
                geminiThinking = false;
                handleGeminiResponse(response, legalMoves);
            });
        });
    }

    private String buildGeminiPrompt(List<int[]> legalMoves) {
        // Encode column as letter (A-H), row as number (1-8) for clarity
        StringBuilder legalStr = new StringBuilder();
        for (int[] m : legalMoves) {
            legalStr.append((char) ('A' + m[1]))
                    .append(m[0] + 1)
                    .append(", ");
        }
        if (legalStr.length() > 2)
            legalStr.setLength(legalStr.length() - 2); // trim trailing ", "

        return "You are playing Othello (Reversi) as the BLACK player.\n" +
               "The board uses coordinates: columns A-H (left to right), rows 1-8 (top to bottom).\n" +
               "B = Black (you), W = White (opponent), . = empty.\n\n" +
               "Current board:\n" + board.toBoardString() + "\n" +
               "Your legal moves are: " + legalStr + "\n\n" +
               "Choose the BEST move for Black. " +
               "Respond ONLY with a JSON object matching the provided schema. " +
               "The 'row' field is 0-indexed (0-7) and 'col' is 0-indexed (0-7). " +
               "For example, A1 = row:0, col:0.  D4 = row:3, col:3.";
    }

    /**
     * JSON Schema for Gemini's structured output.
     * Forces the model to reply with {"row": <int>, "col": <int>, "reasoning": "<string>"}
     */
    private String buildResponseSchema() {
        return "{\n" +
               "  \"type\": \"object\",\n" +
               "  \"properties\": {\n" +
               "    \"row\": {\n" +
               "      \"type\": \"integer\",\n" +
               "      \"description\": \"0-indexed row of the chosen move (0 = row 1, 7 = row 8)\",\n" +
               "      \"minimum\": 0,\n" +
               "      \"maximum\": 7\n" +
               "    },\n" +
               "    \"col\": {\n" +
               "      \"type\": \"integer\",\n" +
               "      \"description\": \"0-indexed column of the chosen move (0 = col A, 7 = col H)\",\n" +
               "      \"minimum\": 0,\n" +
               "      \"maximum\": 7\n" +
               "    },\n" +
               "    \"reasoning\": {\n" +
               "      \"type\": \"string\",\n" +
               "      \"description\": \"Short explanation of why this move was chosen\"\n" +
               "    }\n" +
               "  },\n" +
               "  \"required\": [\"row\", \"col\", \"reasoning\"]\n" +
               "}";
    }

    /** Parse the Gemini JSON response and apply the move. Fall back to first legal move on error. */
    private void handleGeminiResponse(String rawResponse, List<int[]> legalMoves) {
        int chosenRow = -1;
        int chosenCol = -1;
        String reasoning = "";

        try {
            if (rawResponse.startsWith("Error") || rawResponse.startsWith("Exception")) {
                tvStatus.setText("Gemini error: " + rawResponse);
            } else {
                // The structured response text is inside candidates[0].content.parts[0].text
                JSONObject root = new JSONObject(rawResponse);
                JSONArray candidates = root.getJSONArray("candidates");
                JSONObject content = candidates.getJSONObject(0).getJSONObject("content");
                String text = content.getJSONArray("parts").getJSONObject(0).getString("text");

                JSONObject move = new JSONObject(text);
                chosenRow = move.getInt("row");
                chosenCol = move.getInt("col");
                reasoning = move.optString("reasoning", "");
            }
        } catch (Exception e) {
            // JSON parse failure → fall back to first legal move
            chosenRow = -1;
        }

        // Validate that the returned move is actually legal
        boolean valid = false;
        if (chosenRow >= 0) {
            for (int[] m : legalMoves) {
                if (m[0] == chosenRow && m[1] == chosenCol) {
                    valid = true;
                    break;
                }
            }
        }

        // Fall back to first legal move if Gemini returned something invalid
        if (!valid) {
            chosenRow = legalMoves.get(0)[0];
            chosenCol = legalMoves.get(0)[1];
            reasoning = "(fallback — first legal move)";
        }

        board.makeMove(chosenRow, chosenCol, OthelloBoard.BLACK);

        String colLetter = String.valueOf((char) ('A' + chosenCol));
        tvStatus.setText("🤖 Gemini played " + colLetter + (chosenRow + 1) +
                         (reasoning.isEmpty() ? "" : "\n" + reasoning));

        refreshUI();

        if (board.isGameOver()) {
            showGameOver();
        }
    }

    // ── UI helpers ────────────────────────────────────────────────────────────

    private void refreshUI() {
        boardView.setBoard(board);

        int[] score = board.getScore();
        tvScore.setText("⬜ You (White): " + score[0] + "   ⬛ Gemini (Black): " + score[1]);

        if (board.isGameOver()) {
            showGameOver();
            return;
        }

        int turn = board.getCurrentTurn();
        if (turn == OthelloBoard.WHITE && !geminiThinking) {
            List<int[]> legalMoves = board.getLegalMoves(OthelloBoard.WHITE);
            boardView.setLegalMoves(legalMoves);
            tvStatus.setText("Your turn (White) — tap a highlighted square");
        } else if (turn == OthelloBoard.BLACK) {
            boardView.setLegalMoves(null);
            if (!geminiThinking) {
                tvStatus.setText("🤖 Gemini's turn (Black)…");
            }
        }
    }

    private void showGameOver() {
        boardView.setLegalMoves(null);
        int[] score = board.getScore();
        String result;
        if (score[0] > score[1])      result = "🎉 You win! " + score[0] + " – " + score[1];
        else if (score[1] > score[0]) result = "🤖 Gemini wins! " + score[1] + " – " + score[0];
        else                          result = "It's a draw! " + score[0] + " – " + score[1];
        tvStatus.setText("Game Over — " + result);
    }
}