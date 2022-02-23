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

	private String calc(HttpServletRequest req, HttpServletResponse resp) throws IOException {

		HttpSession session = req.getSession();
		String exprOrigin = (String) session.getAttribute("expression");
		StringBuilder expression = new StringBuilder();
		for (int i = 0; i < exprOrigin.length(); ++i) {
			char c = exprOrigin.charAt(i);
			if ((c >= 'a') && (c <= 'z')) {
				String attributeValue = (String) session.getAttribute(String.valueOf(c));
				if (attributeValue == null) {
					resp.sendError(409, "Conflict: lack of data");
					return null;
				}
				char value = attributeValue.charAt(0);
				if ((value >= 'a') && (value <= 'z')) {
					expression.append(session.getAttribute(attributeValue));
				} else {
					expression.append(attributeValue);
				}
			} else {
				expression.append(c);
			}
		}
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
	}

	private static void processVariable(HttpServletRequest req, HttpServletResponse resp, char variableName)
			throws IOException {
		HttpSession session = req.getSession();
		String variableValue = inputStreamToString(req.getInputStream());
		if (session.getAttribute(String.valueOf(variableName)) == null) {
			resp.setStatus(201);
			resp.addHeader("Location: ", "/calc/" + variableName);
		} else {
			resp.setStatus(200);
		}
		session.setAttribute(String.valueOf(variableName), variableValue);
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
		String res = calc(req, resp);
		if (res == null) {
			return;
		}
		PrintWriter writer = resp.getWriter();
		writer.write(res);
	}
}