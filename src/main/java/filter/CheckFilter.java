package filter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
//import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

//@WebFilter(filterName="CheckFilter", urlPatterns="/*")
public class CheckFilter implements Filter {

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		// TODO Auto-generated method stub

	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		System.out.println("This is pre-processing!");

		// pre-processing
		HttpServletRequest req = (HttpServletRequest) request;
		HttpServletResponse resp = (HttpServletResponse) response;
		String uri = req.getRequestURI();
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
		System.out.println("This is pre-processing!");
		chain.doFilter(req, response);
	}

	@Override
	public void destroy() {
		// TODO Auto-generated method stub

	}

}
