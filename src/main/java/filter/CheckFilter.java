package filter;

import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class CheckFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // TODO Auto-generated method stub
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        MultiReadHttpServletRequest wrappedRequest = new MultiReadHttpServletRequest(req);
        final String uri = req.getRequestURI();
        if (uri.equals("/calc/expression")) {
            if (processExpression(wrappedRequest, resp)) {
                chain.doFilter(wrappedRequest, resp);
            }
            return;
        }
        Pattern pattern = Pattern.compile("/calc/([a-z])$");
        Matcher matcher = pattern.matcher(uri);
        if (matcher.find()) {
            final String variableName = matcher.group(1);
            if (processVariable(wrappedRequest, resp, variableName.charAt(0))) {
                chain.doFilter(wrappedRequest, resp);
            }
            return;
        }
        if (uri.equals("/calc/result")) {
            chain.doFilter(wrappedRequest, resp);
            return;
        }
        resp.sendError(400, "Incorrect value of URI1: " + uri);
    }

    private static boolean processExpression(ServletRequest request, ServletResponse response) throws IOException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        String expression = inputStreamToString(req.getInputStream());
        final boolean checking = checkExpression(expression);
        if (checking == false) {
            resp.sendError(400, "BAD format");
        }
        return checking;

    }

    private static String inputStreamToString(InputStream inputStream) {
        try (Scanner scanner = new Scanner(inputStream, "UTF-8")) {
            return scanner.hasNext() ? scanner.useDelimiter("\\A").next() : "";
        }
    }

    enum State {
        INITIAL, OPEN_BRACKET, CLOSE_BRACKET, CHARACTER, SIGN, DIGIT, OPERATION;
    }

    public static boolean checkExpression(String expression) {
        State state = State.INITIAL;
        int brackets = 0;
        for (int i = 0; i < expression.length(); i++) {

            char c = expression.charAt(i);
            if (state == State.INITIAL) {
                if (c == '(') {
                    brackets += 1;
                    state = State.OPEN_BRACKET;
                } else if ((c >= 'a') && (c <= 'z')) {
                    state = State.CHARACTER;
                } else if ((c >= '0') && (c <= '9')) {
                    state = State.DIGIT;
                } else if (c == '-') {
                    state = State.SIGN;
                } else if (c == ' ') {
                    continue;
                } else {
                    return false;
                }
            } else if (state == State.OPEN_BRACKET) {
                if ((c >= 'a') && (c <= 'z')) {
                    state = State.CHARACTER;
                } else if ((c >= '0') && (c <= '9')) {
                    state = State.DIGIT;
                } else if (c == '-') {
                    state = State.SIGN;
                } else if (c == ' ') {
                    continue;
                } else {
                    return false;
                }
            } else if (state == State.CHARACTER) {
                if ((c == '-') || (c == '+') || (c == '*') || (c == '/')) {
                    state = State.OPERATION;
                } else if (c == ')') {
                    brackets -= 1;
                    state = State.CLOSE_BRACKET;
                } else if (c == ' ') {
                    continue;
                } else {
                    return false;
                }
            } else if (state == State.DIGIT) {
                if (c == ')') {
                    brackets -= 1;
                    state = State.CLOSE_BRACKET;
                } else if ((c == '-') || (c == '+') || (c == '*') || (c == '/')) {
                    state = State.OPERATION;
                } else if ((c >= '0') && (c <= '9')) {
                    state = State.DIGIT;
                } else if (c == ' ') {
                    continue;
                } else {
                    return false;
                }
            } else if (state == State.SIGN) {
                if ((c >= '0') && (c <= '9')) {
                    state = State.DIGIT;
                } else if (c == ' ') {
                    continue;
                } else {
                    return false;
                }
            } else if (state == State.OPERATION) {
                if ((c >= '0') && (c <= '9')) {
                    state = State.DIGIT;
                } else if ((c >= 'a') && (c <= 'z')) {
                    state = State.CHARACTER;
                } else if (c == '(') {
                    brackets += 1;
                    state = State.OPEN_BRACKET;
                } else if (c == '-') {
                    state = State.SIGN;
                } else if (c == ' ') {
                    continue;
                } else {
                    return false;
                }
            } else if (state == State.CLOSE_BRACKET) {
                if ((c == '-') || (c == '+') || (c == '*') || (c == '/')) {
                    state = State.OPERATION;
                } else if (c == ' ') {
                    continue;
                } else {
                    return false;
                }
            }
            if (i == expression.length() - 1) {
                if (brackets != 0) {
                    return false;
                }
            }

        }
        if ((state == State.OPERATION) || (state == State.OPEN_BRACKET) || (state == State.SIGN)) {
            return false;
        }
        return true;
    }

    private static boolean processVariable(ServletRequest request, ServletResponse response, char variable_name)
            throws IOException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        String variableValue = inputStreamToString(req.getInputStream());
        if (req.getMethod().equals("DELETE")) {
            if (variableValue.isEmpty()) {
                return true;
            }
            resp.sendError(400, "Bad DELETE request");
            return false;
        }
        try {
            int varValue = Integer.parseInt(variableValue);
            if ((varValue < -10000) || (varValue > 10000)) {
                resp.sendError(403, "Forbidden: too big or too small");
                return false;
            }
        } catch (Exception e) {
            Pattern pattern = Pattern.compile("^([a-z])$");
            Matcher matcher = pattern.matcher(variableValue);
            if (matcher.find()) {
                HttpSession session = req.getSession();
                String variableValueOfValue = (String) session.getAttribute(variableValue);
                if (variableValueOfValue != null) {
                    return true;
                }
            }
            resp.sendError(400, "Incorrect value: " + variableValue);
            return false;
        }
        return true;
    }

    @Override
    public void destroy() {
        // TODO Auto-generated method stub

    }

}
