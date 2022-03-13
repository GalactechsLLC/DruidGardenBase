package garden.druid.base.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map.Entry;

public class HttpClient {
	
	public static byte[] get(String url, HashMap<String, String> headers) throws URISyntaxException, IOException {
		URI uri = new URI(url);
		HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
		connection.setRequestMethod("GET");
		if(headers != null) {
			for(Entry<String, String> header : headers.entrySet()) {
				connection.setRequestProperty(header.getKey(), header.getValue());
			}
		}
		connection.setUseCaches(false);
		connection.setDoInput(true);
		connection.setDoOutput(false);
		connection.connect();
		InputStream rd;
		try {
			rd = connection.getInputStream();
		} catch (Exception e) {
			rd = connection.getErrorStream();
		}
		ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
		byte[] buffer = new byte[1024];
		int read;
		while ((read = rd.read(buffer)) >= 0) {
			byteOutput.write(buffer, 0, read);
		}
		byte[] rtn = byteOutput.toByteArray();
		byteOutput.close();
		rd.close();
		return rtn;
	}
}
