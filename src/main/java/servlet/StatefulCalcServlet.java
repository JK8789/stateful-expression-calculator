package servlet;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Scanner;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import bsh.EvalError;
import bsh.Interpreter;

class Lexer {
	enum State {
		initial, open_bracket, close_bracket, character, sign, digit, operation;
	}

	State state;

	public boolean checkExpression(String expression) {
		state = State.initial;
		int brackets = 0;
		for (int i = 0; i < expression.length(); i++) {
			if (state == State.initial) {
				if (expression.charAt(i) == '(') {
					brackets += 1;
					state = State.open_bracket;
				} else if ((expression.charAt(i) >= 'a') && (expression.charAt(i) <= 'z')) {
					state = State.character;
				} else if ((expression.charAt(i) >= '0') && (expression.charAt(i) <= '9')) {
					state = State.digit;
				} else if (expression.charAt(i) == '-') {
					state = State.sign;
				} else if (expression.charAt(i) == ' ') {
					continue;
				} else {
					// System.out.println("Error in " + expression);
					return false;
				}
			} else if (state == State.open_bracket) {
				if ((expression.charAt(i) >= 'a') && (expression.charAt(i) <= 'z')) {
					state = State.character;
				} else if ((expression.charAt(i) >= '0') && (expression.charAt(i) <= '9')) {
					state = State.digit;
				} else if (expression.charAt(i) == '-') {
					state = State.sign;
				} else if (expression.charAt(i) == ' ') {
					continue;
				} else {
					// System.out.println("Error in " + expression);
					return false;
				}
			} else if (state == State.character) {
				if ((expression.charAt(i) == '-') || (expression.charAt(i) == '+') || (expression.charAt(i) == '*')
						|| (expression.charAt(i) == '/')) {
					state = State.operation;
				} else if (expression.charAt(i) == ')') {
					brackets -= 1;
					state = State.close_bracket;
				} else if (expression.charAt(i) == ' ') {
					continue;
				} else {
					System.out.println("Error in " + expression);
					return false;
				}
			} else if (state == State.digit) {
				if (expression.charAt(i) == ')') {
					brackets -= 1;
					state = State.close_bracket;
				} else if ((expression.charAt(i) == '-') || (expression.charAt(i) == '+')
						|| (expression.charAt(i) == '*') || (expression.charAt(i) == '/')) {
					state = State.operation;
				} else if ((expression.charAt(i) >= '0') && (expression.charAt(i) <= '9')) {
					state = State.digit;
				} else if (expression.charAt(i) == ' ') {
					continue;
				} else {
					System.out.println("Error in " + expression);
					return false;
				}
			} else if (state == State.sign) {
				if ((expression.charAt(i) >= '0') && (expression.charAt(i) <= '9')) {
					state = State.digit;
				} else if (expression.charAt(i) == ' ') {
					continue;
				} else {
					System.out.println("Error in " + expression);
					return false;
				}
			} else if (state == State.operation) {
				if ((expression.charAt(i) >= '0') && (expression.charAt(i) <= '9')) {
					state = State.digit;
				} else if ((expression.charAt(i) >= 'a') && (expression.charAt(i) <= 'z')) {
					state = State.character;
				} else if (expression.charAt(i) == '(') {
					brackets += 1;
					state = State.open_bracket;
				} else if (expression.charAt(i) == '-') {
					state = State.sign;
				} else if (expression.charAt(i) == ' ') {
					continue;
				} else {
					System.out.println("Error in " + expression);
					return false;
				}
			} else if (state == State.close_bracket) {
				if ((expression.charAt(i) == '-') || (expression.charAt(i) == '+') || (expression.charAt(i) == '*')
						|| (expression.charAt(i) == '/')) {
					state = State.operation;
				} else if (expression.charAt(i) == ' ') {
					continue;
				} else {
					System.out.println("Error in " + expression);
					return false;
				}
			}
			if (i == expression.length() - 1) {
				if (brackets != 0) {
					System.out.println("Error in " + expression);
					return false;
				}
			}

		}
		if ((state == State.operation) || (state == State.open_bracket) || (state == State.sign)) {
			System.out.println("Error in " + expression);
			return false;
		}
		return true;
	}

}

//@WebServlet("/calc/*")
public class StatefulCalcServlet extends HttpServlet {

	private static String Calc(HttpServletRequest req, HttpServletResponse resp) throws IOException {

		// String expr_origin = req.getParameter("expression");
		HttpSession session = req.getSession();
		String expr_origin = (String) session.getAttribute("expression");
		System.out.println("       expr_origin " + expr_origin);
		StringBuilder expression = new StringBuilder();
		for (int i = 0; i < expr_origin.length(); ++i) {
			char c = expr_origin.charAt(i);
			if ((c >= 'a') && (c <= 'z')) {
				String atr_value = (String) session.getAttribute(String.valueOf(c));
				System.out.println("проверка atr_value : " + atr_value);
				if (atr_value == null) {
					resp.setStatus(409);
					System.out.println("       setStatus 409 ");
					return null;
				}
				char value = atr_value.charAt(0);
				System.out.println("value of " + String.valueOf(c) + " is " + String.valueOf(value));

				if ((value >= 'a') && (value <= 'z')) { // check if value is name of another variable
					expression.append(session.getAttribute(String.valueOf(value)));
				} else {
					expression.append(session.getAttribute(String.valueOf(c)));
				}
			} else {
				expression.append(c);
			}
		}
		System.out.println("expr_result " + expression);

		Interpreter interpreter = new Interpreter();
		Integer integer = null;
		try {
			interpreter.eval("result = " + expression);
		} catch (EvalError e2) {
			return "Bad expression" + expression;
		}
		try {
			integer = (Integer) interpreter.get("result");
		} catch (EvalError e) {
			return "Bad expression" + expression;
		}
		return Integer.toString(integer);
	}

	private static void processExpression(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		HttpSession session = req.getSession();
		String expression = inputStreamToString(req.getInputStream());
		Lexer lexer = new Lexer();
		System.out.println("expression who checking " + expression);
		boolean checking = lexer.checkExpression(expression);
		if (checking == false) {
			resp.sendError(400, "BAD format");
			return;
		} else
			System.out.println("expression after checking is GOOD! " + expression);
		if (session.getAttribute("expression") == null) {
			resp.setStatus(201);
			resp.addHeader("Location: ", "/calc/expression");
		} else {
			resp.setStatus(200);
		}
		System.out.println("gooing to put expression " + expression);
		session.setAttribute("expression", expression);
		System.out.println("expression IS PUTED in session  " + expression);
	}

	private static void processVariable(HttpServletRequest req, HttpServletResponse resp, char variable_name)
			throws IOException {
		HttpSession session = req.getSession();
		String variable_value = inputStreamToString(req.getInputStream());
//		if ((variable_value.length() == 1) && (variable_value.charAt(0) >= 'a') && (variable_value.charAt(0) <= 'z')) {
//			variable_value = (String) session.getAttribute(variable_value);
//		} else {
//			int var_value = Integer.parseInt(variable_value);
//		}
		try {
			int var_value = Integer.parseInt(variable_value);
			System.out.println("проверяем на диапазон var_value: " + var_value);
			if ((var_value < -10000) || (var_value > 10000)) {
				System.out.println("var_value in processVariable TOO BIG or too smal  " + var_value);
				//resp.setStatus(403); // to add the reason
				resp.sendError(403, "Forbidden: too big or too smal");
				return;
			}
		} catch (Exception e) {
			System.out.println("Bad parsing string  " + variable_value + "  to int");
		}

		if (session.getAttribute(String.valueOf(variable_name)) == null) {
			resp.setStatus(201);
			resp.addHeader("Location: ", "/calc/" + variable_name);
		} else {
			resp.setStatus(200);
		}
		session.setAttribute(String.valueOf(variable_name), variable_value);
		System.out.println("variable_value IS PUTED in session  " + variable_value);
	}

	@Override
	protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		System.out.println();
		String uri = req.getRequestURI();
		String paramOfUri = uri.substring(6);
		if (paramOfUri.equals("expression")) {
			processExpression(req, resp);
		} else if ((paramOfUri.length() == 1) && ((paramOfUri.charAt(0) >= 'a') && (paramOfUri.charAt(0) <= 'z'))) {
			processVariable(req, resp, paramOfUri.charAt(0));
		} else {
			resp.sendError(400, "Incorrect value of URI: " + uri);
		}

	}

	private static String inputStreamToString(InputStream inputStream) {
		Scanner scanner = new Scanner(inputStream, "UTF-8");
		return scanner.hasNext() ? scanner.useDelimiter("\\A").next() : "";
	}

	@Override
	protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String uri = req.getRequestURI();
		String paramOfUri = uri.substring(6);
		HttpSession session = req.getSession();
		session.removeAttribute(paramOfUri);
		resp.setStatus(204);
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		// GET resp with value of evaluated expression to /calc/result URI.
		// return 200 status code and calculated integer value as a response body OR
		// return 409 status code and reason of this error.
		String res = Calc(req, resp);
		if (res == null) {
			resp.sendError(409, "Conflict: lack of data");
			return;
		}
		PrintWriter writer = resp.getWriter();
		writer.write(res);
	}

//	private void doFilter() {
//
//	}
}