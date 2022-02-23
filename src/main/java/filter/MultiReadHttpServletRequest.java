package filter;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

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

		return new CachedServletInputStream(cachedBytes);
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
		private boolean finished;

		public CachedServletInputStream(ByteArrayOutputStream cachedBytes) {
			finished = true;
			/* create a new input stream from the cached request body */
			input = new ByteArrayInputStream(cachedBytes.toByteArray());
		}

		@Override
		public int read() throws IOException {
			finished = true;
			return input.read();
		}

		@Override
		public boolean isFinished() {
			return finished;
		}

		@Override
		public boolean isReady() {
			return !finished;
		}

		@Override
		public void setReadListener(ReadListener readListener_) {
			throw new IllegalStateException();
		}
	}
}