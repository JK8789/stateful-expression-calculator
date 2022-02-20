package filter;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Scanner;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ReadListener;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
//import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import org.apache.tomcat.util.http.fileupload.IOUtils;

class MultiReadHttpServletRequest extends HttpServletRequestWrapper {
	private ByteArrayOutputStream cachedBytes;

	public MultiReadHttpServletRequest(HttpServletRequest request) {
		super(request);
	}

	@Override
	public ServletInputStream getInputStream() throws IOException {
		if (cachedBytes == null)
			cacheInputStream();

		return new CachedServletInputStream();
	}

	@Override
	public BufferedReader getReader() throws IOException {
		return new BufferedReader(new InputStreamReader(getInputStream()));
	}

	private void cacheInputStream() throws IOException {
		/*
		 * Cache the inputstream in order to read it multiple times. For convenience, I
		 * use apache.commons IOUtils
		 */
		cachedBytes = new ByteArrayOutputStream();
		IOUtils.copy(super.getInputStream(), cachedBytes);
	}

	/* An inputstream which reads the cached request body */
	public class CachedServletInputStream extends ServletInputStream {
		private ByteArrayInputStream input;

		public CachedServletInputStream() {
			/* create a new input stream from the cached request body */
			input = new ByteArrayInputStream(cachedBytes.toByteArray());
		}

		@Override
		public int read() throws IOException {
			return input.read();
		}

		@Override
		public boolean isFinished() {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean isReady() {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public void setReadListener(ReadListener readListener) {
			// TODO Auto-generated method stub

		}
	}
}

//@WebFilter(filterName="CheckFilter", urlPatterns="/*")
public class CheckFilter implements Filter {

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		// TODO Auto-generated method stub

	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		System.out.println();

		// pre-processing
		// 1. check URI
		HttpServletRequest req = (HttpServletRequest) request;
		HttpServletResponse resp = (HttpServletResponse) response;
		MultiReadHttpServletRequest wrappedRequest = new MultiReadHttpServletRequest(req);
		String uri = req.getRequestURI();
		System.out.println("This is pre-processing! " + uri);
		if (uri.length() < 7) {
			resp.sendError(400, "Incorrect value of URI1: " + uri);
			return;
		}
		if (!uri.substring(0, 6).equals("/calc/")) {
			resp.sendError(400, "Incorrect value of URI2: " + uri);
			return;
		}
		String paramOfUri = uri.substring(6);
		boolean trueUri = false;
		if (paramOfUri.equals("expression")) {
			trueUri = true;
		} else if ((paramOfUri.length() == 1) && ((paramOfUri.charAt(0) >= 'a') && (paramOfUri.charAt(0) <= 'z'))) {
			trueUri = true;
		} else if (paramOfUri.equals("result")) {
			trueUri = true;
		}
		if (trueUri == false) {
			resp.sendError(400, "Incorrect value of URI3: " + uri);
			return;
		}

		// 2.check expression
		if (paramOfUri.equals("expression")) {
			boolean check_Expression = processExpression(wrappedRequest, resp);
			if (check_Expression == false) {
				return;
			}
		}

		// 3.check variable
		if ((paramOfUri.length() == 1) && ((paramOfUri.charAt(0) >= 'a') && (paramOfUri.charAt(0) <= 'z'))) {
			if (!processVariable(wrappedRequest, resp, paramOfUri.charAt(0))) {
				return;
			}
		}

		System.out.println("Pre-processing is finished!");
		chain.doFilter(wrappedRequest, response);
	}

	// checking expression method
	private static boolean processExpression(ServletRequest request, ServletResponse response) throws IOException {
		HttpServletRequest req = (HttpServletRequest) request;
		HttpServletResponse resp = (HttpServletResponse) response;
		String expression = inputStreamToString(req.getInputStream());
		System.out.println("Expression IN Pre-processing: " + expression);
		final boolean checking = checkExpression(expression);
		if (checking == false) {
			resp.sendError(400, "BAD format");
		}
		return checking;

	}

	// conversion method: InputStream To String
	private static String inputStreamToString(InputStream inputStream) {
		try (Scanner scanner = new Scanner(inputStream, "UTF-8")) {
			return scanner.hasNext() ? scanner.useDelimiter("\\A").next() : "";
		}
	}

	// Lexer method for checking expression
	enum State {
		initial, open_bracket, close_bracket, character, sign, digit, operation;
	}

	public static boolean checkExpression(String expression) {
		System.out.println("This is checkExpression method: " + expression);
		State state = State.initial;
		int brackets = 0;
		for (int i = 0; i < expression.length(); i++) {
			System.out.print("state = ");
			System.out.println(state);
			char c = expression.charAt(i);
			if (state == State.initial) {
				if (c == '(') {
					brackets += 1;
					state = State.open_bracket;
				} else if ((c >= 'a') && (c <= 'z')) {
					state = State.character;
				} else if ((c >= '0') && (c <= '9')) {
					state = State.digit;
				} else if (c == '-') {
					state = State.sign;
				} else if (c == ' ') {
					continue;
				} else {
					return false;
				}
			} else if (state == State.open_bracket) {
				if ((c >= 'a') && (c <= 'z')) {
					state = State.character;
				} else if ((c >= '0') && (c <= '9')) {
					state = State.digit;
				} else if (c == '-') {
					state = State.sign;
				} else if (c == ' ') {
					continue;
				} else {
					return false;
				}
			} else if (state == State.character) {
				if ((c == '-') || (c == '+') || (c == '*') || (c == '/')) {
					state = State.operation;
				} else if (c == ')') {
					brackets -= 1;
					state = State.close_bracket;
				} else if (c == ' ') {
					continue;
				} else {
					System.out.println("return false");
					return false;
				}
			} else if (state == State.digit) {
				if (c == ')') {
					brackets -= 1;
					state = State.close_bracket;
				} else if ((c == '-') || (c == '+') || (c == '*') || (c == '/')) {
					state = State.operation;
				} else if ((c >= '0') && (c <= '9')) {
					state = State.digit;
				} else if (c == ' ') {
					continue;
				} else {
					return false;
				}
			} else if (state == State.sign) {
				if ((c >= '0') && (c <= '9')) {
					state = State.digit;
				} else if (c == ' ') {
					continue;
				} else {
					return false;
				}
			} else if (state == State.operation) {
				if ((c >= '0') && (c <= '9')) {
					state = State.digit;
				} else if ((c >= 'a') && (c <= 'z')) {
					state = State.character;
				} else if (c == '(') {
					brackets += 1;
					state = State.open_bracket;
				} else if (c == '-') {
					state = State.sign;
				} else if (c == ' ') {
					continue;
				} else {
					return false;
				}
			} else if (state == State.close_bracket) {
				if ((c == '-') || (c == '+') || (c == '*') || (c == '/')) {
					state = State.operation;
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
		if ((state == State.operation) || (state == State.open_bracket) || (state == State.sign)) {
			return false;
		}
		return true;
	}

	// checking Variable method
	private static boolean processVariable(ServletRequest request, ServletResponse response, char variable_name)
			throws IOException {
		HttpServletRequest req = (HttpServletRequest) request;
		HttpServletResponse resp = (HttpServletResponse) response;
		// HttpSession session = req.getSession();
		String variable_value = inputStreamToString(req.getInputStream());
		try {
			int var_value = Integer.parseInt(variable_value);
			System.out.println(" проверяем на диапазон var_value: " + var_value);
			if ((var_value < -10000) || (var_value > 10000)) {
				System.out.println("var_value in processVariable TOO BIG or too small  " + var_value);
				resp.sendError(403, "Forbidden: too big or too small");
				return false;
			}
		} catch (Exception e) {
			System.out.println("Bad parsing string  " + variable_value + "  to int");
		}
		return true;
	}

	@Override
	public void destroy() {
		// TODO Auto-generated method stub

	}

}
