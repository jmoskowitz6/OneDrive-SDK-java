package org.onedrive.network.legacy;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.SneakyThrows;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.onedrive.exceptions.InternalException;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * {@// TODO: Enhance javadoc}
 *
 * @author <a href="mailto:yoobyeonghun@gmail.com" target="_top">isac322</a>
 */
public class HttpsRequest {
	public static final String NETWORK_ERR_MSG = "Network connection error. Please retry later or contact API author.";
	private final HttpsURLConnection httpConnection;

	@SneakyThrows(MalformedURLException.class)
	public HttpsRequest(@NotNull String url) {
		URL url1 = new URL(url);
		try {
			httpConnection = (HttpsURLConnection) url1.openConnection();
		}
		catch (IOException e) {
			e.printStackTrace();
			// TODO: custom exception
			throw new RuntimeException(NETWORK_ERR_MSG);
		}
	}

	public HttpsRequest(@NotNull URL url) {
		try {
			httpConnection = (HttpsURLConnection) url.openConnection();
		}
		catch (IOException e) {
			e.printStackTrace();
			// TODO: custom exception
			throw new RuntimeException(NETWORK_ERR_MSG);
		}
	}

	/**
	 * Add {@code key}, {@code value} pair to http request's header.<br>
	 * Like: {@code key}: {@code value}.
	 *
	 * @param key   Key to add in request's header.
	 * @param value Value to add in request's header. It could be {@code null}.
	 */
	public void setHeader(@NotNull String key, String value) {
		httpConnection.setRequestProperty(key, value);
	}

	@NotNull
	public HttpsResponse doPost(String content) {
		byte[] bytes = content.getBytes(StandardCharsets.UTF_8);

		return doPost(bytes);
	}

	@NotNull
	public HttpsResponse doPost(byte[] content) {
		try {
			httpConnection.setRequestMethod("POST");
			return sendContent(content);
		}
		catch (ProtocolException e) {
			throw new UnsupportedOperationException("unsupported method POST. contact author with stacktrace", e);
		}
	}

	@NotNull
	public HttpsResponse doPatch(String content) {
		byte[] bytes = content.getBytes();
		return doPatch(bytes);
	}

	@NotNull
	public HttpsResponse doPatch(byte[] content) {
		try {
			httpConnection.setRequestMethod("PATCH");
			return sendContent(content);
		}
		catch (ProtocolException e) {
			throw new UnsupportedOperationException("unsupported method PATCH. contact author with stacktrace", e);
		}
	}

	public HttpsResponse sendContent(byte[] content) {
		try {
			httpConnection.setDoOutput(true);

			httpConnection.setFixedLengthStreamingMode(content.length);

			OutputStream out = httpConnection.getOutputStream();
			out.write(content);
			out.flush();
			out.close();

			return makeResponse();
		}
		catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(NETWORK_ERR_MSG);
		}
	}

	@NotNull
	public HttpsResponse doDelete() {
		try {
			httpConnection.setRequestMethod("DELETE");
			return makeResponse();
		}
		catch (ProtocolException e) {
			throw new UnsupportedOperationException("unsupported method DELETE. contact author with stacktrace", e);
		}
	}

	@NotNull
	public HttpsResponse doGet() {
		try {
			httpConnection.setRequestMethod("GET");
			return makeResponse();
		}
		catch (ProtocolException e) {
			throw new UnsupportedOperationException("unsupported method GET. contact author with stacktrace", e);
		}
	}

	/**
	 * @return Response object.
	 * @throws RuntimeException fail to network connection or fail to read response.
	 */
	@NotNull
	protected HttpsResponse makeResponse() {
		try {
			int code = httpConnection.getResponseCode();
			String message = httpConnection.getResponseMessage();
			Map<String, List<String>> header = httpConnection.getHeaderFields();
			URL url = httpConnection.getURL();

			ByteBuf byteStream = Unpooled.buffer(1024);
			InputStream body;

			if (code < 400)
				body = httpConnection.getInputStream();
			else
				body = httpConnection.getErrorStream();

			byte[] buffer = new byte[512];

			int readBytes;
			while ((readBytes = body.read(buffer)) > 0) {
				byteStream.writeBytes(buffer, 0, readBytes);
			}
			body.close();
			return new HttpsResponse(url, code, message, header, byteStream.array());

			// which one is better?
			/*
			val byteStream = new ByteArrayOutputStream();
			BufferedInputStream body;

			if (code < 400)
				body = new BufferedInputStream(httpConnection.getInputStream());
			else
				body = new BufferedInputStream(httpConnection.getErrorStream());

			int bytes;
			while ((bytes = body.read()) != -1) {
				byteStream.write(bytes);
			}

			byteStream.close();
			body.close();
			return new HttpsResponse(url, code, message, header, byteStream.toByteArray());
			*/
		}
		catch (IOException e) {
			e.printStackTrace();
			// TODO: custom exception
			throw new RuntimeException(NETWORK_ERR_MSG);
		}
		finally {
			httpConnection.disconnect();
		}
	}
}
