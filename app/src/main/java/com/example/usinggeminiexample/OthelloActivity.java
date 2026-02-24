package com.example.usinggeminiexample;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

/**
 * OthelloActivity - MVP "Presenter + View" layer.
 *
 * Human  = WHITE discs  (plays by tapping highlighted squares)
 * Gemini = BLACK discs  (plays via Gemini API with JSON-schema response)
 *
 * Architecture:
 *   Model     -> OthelloModel  (pure Java, no Android deps)
 *   View      -> this Activity (renders board from model state)
 *   Presenter -> logic inside this Activity (thin; could be extracted)
 */
public class OthelloActivity extends AppCompatActivity {

    private static final String API_KEY = "YOUR_API_KEY_HERE!!!!";

    private OthelloModel  model;
    private GeminiManager geminiManager;

    private GridLayout gridBoard;
    private TextView   textViewStatus;
    private TextView   textViewBlackScore;
    private TextView   textViewWhiteScore;
    private TextView   textViewGeminiThinking;
    private View[][]   cellViews;

    private static final int COLOR_EMPTY  = Color.parseColor("#2E7D32");
    private static final int COLOR_HINT   = Color.parseColor("#A5D6A7");
    private static final int COLOR_BLACK  = Color.parseColor("#212121");
    private static final int COLOR_WHITE  = Color.parseColor("#FAFAFA");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_othello);

        model         = new OthelloModel();
        geminiManager = new GeminiManager(API_KEY);
        cellViews     = new View[OthelloModel.SIZE][OthelloModel.SIZE];

        bindViews();
        buildGrid();
        renderBoard();

        Button btnNew = findViewById(R.id.buttonNewGame);
        btnNew.setOnClickListener(v -> {
            model.reset();
            renderBoard();
            if (model.getCurrentPlayer() == OthelloModel.BLACK) {
                triggerGeminiMove();
            }
        });

        triggerGeminiMove();
    }

    private void bindViews() {
        gridBoard              = findViewById(R.id.gridBoard);
        textViewStatus         = findViewById(R.id.textViewStatus);
        textViewBlackScore     = findViewById(R.id.textViewBlackScore);
        textViewWhiteScore     = findViewById(R.id.textViewWhiteScore);
        textViewGeminiThinking = findViewById(R.id.textViewGeminiThinking);
    }

    private void buildGrid() {
        gridBoard.removeAllViews();
        for (int r = 0; r < OthelloModel.SIZE; r++) {
            for (int c = 0; c < OthelloModel.SIZE; c++) {
                View cell = new View(this);
                cell.setBackgroundColor(COLOR_EMPTY);

                GridLayout.LayoutParams params = new GridLayout.LayoutParams(
                        GridLayout.spec(r, 1f),
                        GridLayout.spec(c, 1f)
                );
                params.width  = 0;
                params.height = 0;
                params.setMargins(2, 2, 2, 2);
                cell.setLayoutParams(params);

                final int row = r, col = c;
                cell.setOnClickListener(v -> onCellClicked(row, col));

                cellViews[r][c] = cell;
                gridBoard.addView(cell);
            }
        }
    }

    private void renderBoard() {
        List<int[]> legalMoves = model.getLegalMoves();

        boolean[][] legal = new boolean[OthelloModel.SIZE][OthelloModel.SIZE];
        for (int[] m : legalMoves) legal[m[0]][m[1]] = true;

        for (int r = 0; r < OthelloModel.SIZE; r++) {
            for (int c = 0; c < OthelloModel.SIZE; c++) {
                View cell = cellViews[r][c];
                int  val  = model.getCell(r, c);

                if (val == OthelloModel.BLACK) {
                    drawDisc(cell, COLOR_BLACK);
                } else if (val == OthelloModel.WHITE) {
                    drawDisc(cell, COLOR_WHITE);
                } else if (legal[r][c] && model.getCurrentPlayer() == OthelloModel.WHITE) {
                    cell.setBackgroundColor(COLOR_HINT);
                } else {
                    cell.setBackgroundColor(COLOR_EMPTY);
                }
            }
        }
        updateStatusText();
    }

    private void drawDisc(View cell, int color) {
        android.graphics.drawable.GradientDrawable circle =
                new android.graphics.drawable.GradientDrawable();
        circle.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        circle.setColor(color);
        circle.setStroke(3, Color.parseColor("#424242"));
        cell.setBackground(circle);
    }

    private void updateStatusText() {
        int black = model.countDiscs(OthelloModel.BLACK);
        int white = model.countDiscs(OthelloModel.WHITE);
        textViewBlackScore.setText("\u26AB Black (Gemini): " + black);
        textViewWhiteScore.setText("\u26AA White (You): "    + white);

        if (model.isGameOver()) {
            int winner = model.getWinner();
            String msg;
            if      (winner == OthelloModel.WHITE) msg = "You win! (" + white + " vs " + black + ")";
            else if (winner == OthelloModel.BLACK) msg = "Gemini wins! (" + black + " vs " + white + ")";
            else                                   msg = "It's a draw! (" + white + " each)";
            textViewStatus.setText(msg);
        } else {
            String turn = (model.getCurrentPlayer() == OthelloModel.WHITE)
                    ? "Your turn (White)"
                    : "Gemini's turn (Black)";
            textViewStatus.setText(turn);
        }
    }

    private void onCellClicked(int row, int col) {
        if (model.getCurrentPlayer() != OthelloModel.WHITE) return;
        if (model.isGameOver()) return;

        List<int[]> flipped = model.applyMove(row, col);
        if (flipped.isEmpty()) return;

        renderBoard();

        if (!model.isGameOver() && model.getCurrentPlayer() == OthelloModel.BLACK) {
            triggerGeminiMove();
        }
    }

    private void triggerGeminiMove() {
        if (model.isGameOver()) return;

        List<int[]> legalMoves = model.getLegalMoves();
        if (legalMoves.isEmpty()) return;

        textViewGeminiThinking.setVisibility(View.VISIBLE);

        String     prompt = buildOthelloPrompt();
        JSONObject schema = buildResponseSchema();

        geminiManager.sendTextWithSchema(prompt, schema, response -> {
            runOnUiThread(() -> {
                textViewGeminiThinking.setVisibility(View.GONE);
                handleGeminiResponse(response, legalMoves);
            });
        });
    }

    private String buildOthelloPrompt() {
        int[][] board = model.getBoardCopy();
        List<int[]> legalMoves = model.getLegalMoves();

        StringBuilder sb = new StringBuilder();
        sb.append("You are playing Othello (Reversi) as the BLACK player.\n");
        sb.append("Board encoding: 0=empty, 1=WHITE(opponent), -1=BLACK(you).\n\n");
        sb.append("Current board (row 0 = top, col 0 = left):\n");

        for (int r = 0; r < OthelloModel.SIZE; r++) {
            sb.append("Row ").append(r).append(": [");
            for (int c = 0; c < OthelloModel.SIZE; c++) {
                sb.append(board[r][c]);
                if (c < OthelloModel.SIZE - 1) sb.append(", ");
            }
            sb.append("]\n");
        }

        sb.append("\nYour legal moves (row, col):\n");
        for (int[] move : legalMoves)
            sb.append("  row=").append(move[0]).append(", col=").append(move[1]).append("\n");

        sb.append("\nChoose the BEST strategic move for BLACK. ");
        sb.append("Reply ONLY with the JSON object {\"row\": <0-7>, \"col\": <0-7>}.\n");
        return sb.toString();
    }

    private JSONObject buildResponseSchema() {
        try {
            JSONObject schema = new JSONObject();
            schema.put("type", "object");

            JSONObject properties = new JSONObject();
            JSONObject rowProp = new JSONObject();
            rowProp.put("type", "integer");
            properties.put("row", rowProp);

            JSONObject colProp = new JSONObject();
            colProp.put("type", "integer");
            properties.put("col", colProp);

            schema.put("properties", properties);

            JSONArray required = new JSONArray();
            required.put("row");
            required.put("col");
            schema.put("required", required);

            return schema;
        } catch (Exception e) {
            return null;
        }
    }

    private void handleGeminiResponse(String rawResponse, List<int[]> legalMoves) {
        try {
            JSONObject root       = new JSONObject(rawResponse);
            JSONArray  candidates = root.getJSONArray("candidates");
            JSONObject content    = candidates.getJSONObject(0).getJSONObject("content");
            JSONArray  parts      = content.getJSONArray("parts");
            String     text       = parts.getJSONObject(0).getString("text").trim();

            JSONObject move = new JSONObject(text);
            int row = move.getInt("row");
            int col = move.getInt("col");

            boolean valid = false;
            for (int[] m : legalMoves)
                if (m[0] == row && m[1] == col) { valid = true; break; }

            if (!valid) {
                row = legalMoves.get(0)[0];
                col = legalMoves.get(0)[1];
            }

            model.applyMove(row, col);
            renderBoard();

        } catch (Exception e) {
            if (!legalMoves.isEmpty()) {
                model.applyMove(legalMoves.get(0)[0], legalMoves.get(0)[1]);
                renderBoard();
            }
        }
    }
}