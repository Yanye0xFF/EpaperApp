package com.demo.epaper.network;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;

public class HttpUtil {
	
	public static final int URL_ERROR = -100;
	public static final int STATE_OK = 200;

	private int statusCode = 0;

	private String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/64.0.3282.140 Safari/537.36 Edge/18.17763";
	
	private boolean isRequestEncode = false;
	private boolean isResponseDecode= false;
	private boolean isUrlRedirect = false;
	
	private int timeout = 5000;
	private String recvDecode = "UTF-8";
	private String sendEncode = "UTF-8";
	
	private StringBuilder resultBuilder = null;
	private StringBuilder headerBuilder = null;
	private int headerLineCount = 0;

	public HttpUtil(){
	}
	
	private static HttpUtil instance = null;

	public static HttpUtil getInstance() {
		if(instance == null) {
			instance = new HttpUtil();
		}
		return instance;
	}


	public void init(int timeout, boolean redirect, String userAgent, String encodeType) {
		this.timeout = Math.max(timeout, 5000);
		this.isUrlRedirect = redirect;
		this.userAgent = userAgent;

		this.recvDecode = encodeType;
		this.sendEncode = encodeType;
	}

	public String getBody() {
		return resultBuilder.toString();
	}

	public int doGet(String url, String cookies) {
		String myUrl;

		if(url == null || url.isEmpty()) {
			return HttpUtil.URL_ERROR;
		}

		init();

		if(isRequestEncode && url.contains("?")){
			int start = url.indexOf("?") + 1;
			String dataArea = url.substring(start);
			String urlArea = url.substring(0, start);
			try {
				myUrl = urlArea + URLEncoder.encode(dataArea, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
				return HttpUtil.URL_ERROR;
			}
		}else {
			myUrl = url;
		}
		
		BufferedReader bufferedReader = null;
		
		try {
			URL getUrl = new URL(myUrl);
			
			HttpURLConnection conn = (HttpURLConnection) getUrl.openConnection();
            HttpURLConnection.setFollowRedirects(this.isUrlRedirect);
            
            conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
			conn.setRequestProperty("Connection", "keep-alive");
			conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
			if(strOk(cookies)){
				conn.setRequestProperty("Cookie", cookies);
			}
            conn.setRequestProperty("user-agent", userAgent);
			conn.setReadTimeout(timeout);
			conn.setConnectTimeout(timeout);
            conn.setDoOutput(false);
            conn.setDoInput(true);
            conn.connect();
            
			statusCode = conn.getResponseCode();
			for(headerLineCount = 0; conn.getHeaderField(headerLineCount) != null; headerLineCount++) {
				headerBuilder.append(conn.getHeaderField(headerLineCount));
				headerBuilder.append("\n");
			}
			String line;
			bufferedReader = new BufferedReader(new InputStreamReader(conn.getInputStream(), recvDecode));
            
            while (true) {
				line = bufferedReader.readLine();
            	if(line == null) {
            		break;
            	}
            	resultBuilder.append(line);
            	resultBuilder.append("\n");
            }

			bufferedReader.close();

            conn.disconnect();
            
		} catch (MalformedURLException e) {
			e.printStackTrace();
			statusCode = 502;
		} catch (IOException e) {
			e.printStackTrace();
		}finally{
            try{
                if(bufferedReader != null){
					bufferedReader.close();
                }
            }catch(IOException e){
                e.printStackTrace();
            }
        }
		if(isResponseDecode){
			try {
				String temp = URLDecoder.decode(resultBuilder.toString(), "UTF-8");
				resultBuilder.setLength(0);
				resultBuilder.append(temp);
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}
		
		return statusCode;
	}

	private void init() {
		if(resultBuilder == null) {
			resultBuilder = new StringBuilder(20480);
		}
		resultBuilder.setLength(0);

		if(headerBuilder == null) {
			headerBuilder = new StringBuilder(2048);
		}
		headerBuilder.setLength(0);
	}

	private boolean strOk(String args) {
		if(args != null) {
			return !args.isEmpty();
		}
		return false;
	}
}
