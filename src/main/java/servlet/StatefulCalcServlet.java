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

public class StatefulCalcServlet extends HttpServlet {

	private static String Calc(HttpServletRequest req, HttpServletResponse resp) throws IOException {

		HttpSession session = req.getSession();
		String expr_origin = (String) session.getAttribute("expression");
		System.out.println("       expr_origin " + expr_origin);
		StringBuilder expression = new StringBuilder();
		for (int i = 0; i < expr_origin.length(); ++i) {
			char c = expr_origin.charAt(i);
			if ((c >= 'a') && (c <= 'z')) {
				String attribute_value = (String) session.getAttribute(String.valueOf(c));
				System.out.println("проверка atr_value : " + attribute_value);
				if (attribute_value == null) {
					resp.sendError(409, "Conflict: lack of data");
					return null;
				}
				char value = attribute_value.charAt(0);
				System.out.println("value of " + String.valueOf(c) + " is " + String.valueOf(value));

				if ((value >= 'a') && (value <= 'z')) {         // check if value is name of another variable
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
		if (session.getAttribute("expression") == null) {
			resp.setStatus(201);
			resp.addHeader("Location: ", "/calc/expression");
		} else {
			resp.setStatus(200);
		}
		session.setAttribute("expression", expression);
		System.out.println("Expression \"" + expression + "\" IS PUTED in session.");
	}

	private static void processVariable(HttpServletRequest req, HttpServletResponse resp, char variable_name)
			throws IOException {
		HttpSession session = req.getSession();
		String variable_value = inputStreamToString(req.getInputStream());
//		try {
//			int var_value = Integer.parseInt(variable_value);
//			System.out.println(" проверяем на диапазон var_value: " + var_value);
//			if ((var_value < -10000) || (var_value > 10000)) {
//				System.out.println("var_value in processVariable TOO BIG or too small  " + var_value);
//				resp.sendError(403, "Forbidden: too big or too small");
//				return;
//			}
//		} catch (Exception e) {
//			System.out.println("Bad parsing string  " + variable_value + "  to int");
//		}

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
		String uri = req.getRequestURI();
		String paramOfUri = uri.substring(6);
		if (paramOfUri.equals("expression")) {
			processExpression(req, resp);
		} else if ((paramOfUri.length() == 1) && ((paramOfUri.charAt(0) >= 'a') && (paramOfUri.charAt(0) <= 'z'))) {
			processVariable(req, resp, paramOfUri.charAt(0));
		}

	}

	private static String inputStreamToString(InputStream inputStream) {
		try (Scanner scanner = new Scanner(inputStream, "UTF-8")) {
			return scanner.hasNext() ? scanner.useDelimiter("\\A").next() : "";
		}
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
		String res = Calc(req, resp);
		if (res == null) {
			return;
		}
		PrintWriter writer = resp.getWriter();
		writer.write(res);
	}
}