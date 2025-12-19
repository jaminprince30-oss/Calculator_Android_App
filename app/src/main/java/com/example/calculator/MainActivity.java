package com.example.calculator;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

public class MainActivity extends AppCompatActivity {

    private TextView dataTV, outputTV;
    private boolean lastNumeric = false;
    private boolean stateError = false;
    private boolean lastDot = false;
    private int openBrackets = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dataTV = findViewById(R.id.data);
        outputTV = findViewById(R.id.output);

        // Digits 0-9
        setNumeric(R.id.number0, "0");
        setNumeric(R.id.number1, "1");
        setNumeric(R.id.number2, "2");
        setNumeric(R.id.number3, "3");
        setNumeric(R.id.number4, "4");
        setNumeric(R.id.number5, "5");
        setNumeric(R.id.number6, "6");
        setNumeric(R.id.number7, "7");
        setNumeric(R.id.number8, "8");
        setNumeric(R.id.number9, "9");

        // Basic Operators
        setOperator(R.id.addition, "+");
        setOperator(R.id.substracation, "-");
        setOperator(R.id.multiplication, "*");
        // setOperator(R.id.division, "/"); // Ensure ID exists in XML

        findViewById(R.id.percentage).setOnClickListener(v -> addOperator("%"));

        // Powers and Functions
        findViewById(R.id.power2).setOnClickListener(v -> {
            if (lastNumeric && !stateError) { dataTV.append("^2"); lastNumeric = false; lastDot = false; }
        });
        // xⁿ button: insert ^ so user can type exponent
        findViewById(R.id.power3).setOnClickListener(v -> {
            if (lastNumeric && !stateError) {
                dataTV.append("^");
                lastNumeric = false;
                lastDot = false;
            }
        });

        findViewById(R.id.exp_e_power_x).setOnClickListener(v -> wrapLastNumberWithToken("exp"));
        findViewById(R.id.one_by_x).setOnClickListener(v -> wrapLastNumberWithToken("inv"));
        findViewById(R.id.sqrt).setOnClickListener(v -> appendFunc("sqrt("));

        // Trig Buttons
        findViewById(R.id.sin).setOnClickListener(v -> appendFunc("sin("));
        findViewById(R.id.cos).setOnClickListener(v -> appendFunc("cos("));
        findViewById(R.id.tan).setOnClickListener(v -> appendFunc("tan("));
        findViewById(R.id.asin).setOnClickListener(v -> appendFunc("asin("));
        findViewById(R.id.acos).setOnClickListener(v -> appendFunc("acos("));
        findViewById(R.id.atan).setOnClickListener(v -> appendFunc("atan("));

        findViewById(R.id.factorial_n).setOnClickListener(v -> addOperator("!"));
        findViewById(R.id.pi).setOnClickListener(v -> addConstant(String.valueOf(Math.PI)));
        findViewById(R.id.brackets).setOnClickListener(v -> onBrackets());
        findViewById(R.id.dot).setOnClickListener(v -> onDecimalPoint());
        findViewById(R.id.clear).setOnClickListener(v -> onClear());
        findViewById(R.id.all_clear).setOnClickListener(v -> onClear());
        findViewById(R.id.backspace).setOnClickListener(v -> onBackspace());
        findViewById(R.id.equal).setOnClickListener(v -> onEqual());
    }

    private void setNumeric(int id, String value) {
        Button b = findViewById(id);
        if (b != null) b.setOnClickListener(v -> {
            if (stateError) { dataTV.setText(value); stateError = false; }
            else { dataTV.append(value); }
            lastNumeric = true;
        });
    }

    private void setOperator(int id, String op) {
        Button b = findViewById(id);
        if (b != null) b.setOnClickListener(v -> addOperator(op));
    }

    private void addOperator(String op) {
        if (lastNumeric && !stateError) {
            dataTV.append(op);
            lastNumeric = op.equals("!");
            lastDot = false;
        }
    }

    private void appendFunc(String f) {
        if (stateError) return;
        dataTV.append(f);
        openBrackets++;
        lastNumeric = false;
        lastDot = false;
    }

    private void wrapLastNumberWithToken(String token) {
        if (stateError) return;
        String txt = dataTV.getText().toString();
        if (!lastNumeric || txt.isEmpty()) return;
        int i = txt.length() - 1;
        while (i >= 0 && (Character.isDigit(txt.charAt(i)) || txt.charAt(i) == '.')) i--;
        int start = i + 1;
        String num = txt.substring(start);
        String before = txt.substring(0, start);
        dataTV.setText(before + token + "(" + num + ")");
        lastNumeric = true; lastDot = false;
    }

    private void addConstant(String c) {
        if (stateError) { dataTV.setText(c); stateError = false; }
        else { dataTV.append(c); }
        lastNumeric = true;
    }

    private void onBrackets() {
        if (stateError) return;
        if (!lastNumeric || openBrackets == 0) { dataTV.append("("); openBrackets++; lastNumeric = false; }
        else { dataTV.append(")"); openBrackets--; lastNumeric = true; }
        lastDot = false;
    }

    private void onDecimalPoint() {
        if (lastNumeric && !stateError && !lastDot) { dataTV.append("."); lastNumeric = false; lastDot = true; }
    }

    private void onClear() {
        dataTV.setText(""); outputTV.setText("");
        lastNumeric = false; lastDot = false; stateError = false; openBrackets = 0;
    }

    private void onBackspace() {
        if (stateError) { onClear(); return; }
        String txt = dataTV.getText().toString();
        if (txt.isEmpty()) return;
        char last = txt.charAt(txt.length() - 1);
        if (last == '(') openBrackets--;
        else if (last == ')') openBrackets++;
        txt = txt.substring(0, txt.length() - 1);
        dataTV.setText(txt);
        if (!txt.isEmpty()) {
            char c = txt.charAt(txt.length() - 1);
            lastNumeric = Character.isDigit(c) || c == ')' || c == '!';
            lastDot = (c == '.');
        } else { lastNumeric = false; lastDot = false; }
    }

    private void onEqual() {
        if (stateError) return;

        String expr = dataTV.getText().toString();

        while (openBrackets > 0) {
            expr += ")";
            openBrackets--;
        }

        // Power a^b
        expr = expr.replaceAll("(\\d+(?:\\.\\d+)?)\\^(\\d+(?:\\.\\d+)?)", "Math.pow($1,$2)");

        // Trig (degrees) – avoid matching inside asin/acos/atan
        expr = expr.replaceAll("(^|[^a-zA-Z])sin\\(([^)]+)\\)", "$1Math.sin(($2)*Math.PI/180)");
        expr = expr.replaceAll("(^|[^a-zA-Z])cos\\(([^)]+)\\)", "$1Math.cos(($2)*Math.PI/180)");
        expr = expr.replaceAll("(^|[^a-zA-Z])tan\\(([^)]+)\\)", "$1Math.tan(($2)*Math.PI/180)");

// Inverse trig (degrees)
        expr = expr.replaceAll("asin\\(([^)]+)\\)", "(Math.asin($1)*180/Math.PI)");
        expr = expr.replaceAll("acos\\(([^)]+)\\)", "(Math.acos($1)*180/Math.PI)");
        expr = expr.replaceAll("atan\\(([^)]+)\\)", "(Math.atan($1)*180/Math.PI)");

        // Extra functions
        expr = expr.replaceAll("pow2\\(([^)]+)\\)", "Math.pow($1,2)");
        expr = expr.replaceAll("pow3\\(([^)]+)\\)", "Math.pow($1,3)");
        expr = expr.replaceAll("exp\\(([^)]+)\\)", "Math.exp($1)");
        expr = expr.replaceAll("inv\\(([^)]+)\\)", "1/($1)");
        expr = expr.replaceAll("sqrt\\(([^)]+)\\)", "Math.sqrt($1)");

        // Factorial
        expr = handleFactorial(expr);

        // Optional debug: see final JS string
        // outputTV.setText(expr);

        try {
            String result = evaluate(expr);
            outputTV.setText(result);
            lastDot = result.contains(".");
        } catch (Exception e) {
            outputTV.setText("Error");
            stateError = true;
            lastNumeric = false;
        }
    }


    private String handleFactorial(String expression) {
        while (expression.contains("!")) {
            int pos = expression.indexOf("!");
            int start = pos - 1;
            while (start >= 0 && Character.isDigit(expression.charAt(start))) start--;
            start++;
            String numStr = expression.substring(start, pos);
            if (numStr.isEmpty()) break;
            try {
                long n = Long.parseLong(numStr);
                double fact = 1;
                for (int i = 2; i <= n; i++) fact *= i;
                expression = expression.substring(0, start) + fact + expression.substring(pos + 1);
            } catch (Exception e) { break; }
        }
        return expression;
    }

    private String evaluate(String expr) {
        Context rhino = Context.enter();
        rhino.setOptimizationLevel(-1);
        try {
            Scriptable scope = rhino.initStandardObjects();
            Object res = rhino.evaluateString(scope, expr, "calc", 1, null);
            String result = res.toString();
            if (result.endsWith(".0")) result = result.substring(0, result.length() - 2);
            return result;
        } finally {
            Context.exit();
        }
    }
}