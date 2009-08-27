/*
 * Copyright Harry Liu
 * 
 * http://code.google.com/p/g-proxy/
 * 
 * This is an free proxy based on Google App Engine.
 * 
 */
package com.harry.g_proxy;

import java.io.File;
import java.io.FileInputStream;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;

import javax.servlet.ServletContext;
import javax.servlet.http.*;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;

import java.util.logging.Logger;

@SuppressWarnings("serial")
public class g_proxyServlet extends HttpServlet {
	
	private static final Logger log = Logger.getLogger(g_proxyServlet.class.getName());
	private final static String proxyHost = "https://g-proxy.appspot.com/";
	private final static byte[] prefixForHttp = {104, 116, 116, 112, 115, 58, 47, 47, 103, 45, 112, 114, 111, 120, 121, 46, 97, 112, 112, 115, 112, 111, 116, 46, 99, 111, 109, 47, 104, 116, 116, 112, 58, 47, 47}; // https://g-proxy.appspot.com/http://
	private final static byte[] prefixForHttps = {104, 116, 116, 112, 115, 58, 47, 47, 103, 45, 112, 114, 111, 120, 121, 46, 97, 112, 112, 115, 112, 111, 116, 46, 99, 111, 109, 47, 104, 116, 116, 112, 115, 58, 47, 47}; // https://g-proxy.appspot.com/https://
	private final static byte[] prefixForHrefDQ = {104, 114, 101, 102, 61, 34, 47 }; //href="/
	private final static byte[] prefixForHrefSQ = {104, 114, 101, 102, 61, 39, 47 }; //href='/
	private final static byte[] prefixForslash = {47}; //href='/

	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
        // Get the absolute path of the image
        ServletContext sc = getServletContext();
        
		resp.setContentType("text/plain");
		try{
			//find the URL
			String realUrl = getUrl(req);
			if (realUrl.startsWith("/")) realUrl = realUrl.substring(1);
			
			//save the request host
			//http://aaa.com/bbb  => http://aaa.com
			String requestHost = null;
			byte[] requestHostBytes = null;
			if (realUrl.length()>10){
				int k = realUrl.indexOf("/", 8);
				if (k>0){
					k+=8;
					requestHost = realUrl.substring(0,k);
				}
				else{
					requestHost = realUrl;
				}
			}
			if (requestHost == null){
	            sc.log("don't have request URL "+realUrl);
	            returnHome(resp);
	            return;
			}
			else{
				char[] requestHostChars = requestHost.toCharArray();
				requestHostBytes = new byte[requestHostChars.length];
				for(int i=0;i<requestHostChars.length;i++){
					requestHostBytes[i]=(byte)requestHostChars[i];
				}
			}

			//whether is the Home page
			if (realUrl.length()==0 || realUrl.equalsIgnoreCase("index.html")) {
				returnHome(resp);
				return;
			}
			
			// log.info("--------The inputed URI:"+realUrl);
			
			//form the URL
			URL url = new URL(realUrl);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			String contentType = connection.getContentType();

			if (connection.getResponseCode() == HttpURLConnection.HTTP_OK && contentType.toLowerCase().contains("text")) {
                // is text file, replace every links inside
				replaceLinkAndReturnContentByBytes(connection, resp, contentType, requestHostBytes);
            } else {
            	//visit the URL
    			retrieveAndReturnUrlContent(url.openStream(), resp.getOutputStream());
            }

		}
		catch(Exception e){
			e.printStackTrace();
			e.printStackTrace(resp.getWriter());
		}
	}
	
	// /mywebapp/servlet/MyServlet/a/b;c=123?d=789
	private String getUrl(HttpServletRequest req) throws UnsupportedEncodingException {
        String reqUri = req.getRequestURI().toString();
        String queryString = req.getQueryString();   // d=789
        if (queryString != null) {
            reqUri += "?"+queryString;
        }
        return URLDecoder.decode(reqUri, "UTF-8");
    }
	
	private void returnHome(HttpServletResponse resp) throws Exception{
        // Get the absolute path of the image
        ServletContext sc = getServletContext();
        String filename = sc.getRealPath("index.html");
    
        // Get the MIME type of the image
        String mimeType = sc.getMimeType(filename);
        if (mimeType == null) {
            sc.log("Could not get MIME type of "+filename);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
    
        // Set content type
        resp.setContentType(mimeType);
    
        // Set content size
        File file = new File(filename);
        resp.setContentLength((int)file.length());
    
        // Open the file and output streams
        FileInputStream in = new FileInputStream(file);
        OutputStream out = resp.getOutputStream();
    
        // Copy the contents of the file to the output stream
        byte[] buf = new byte[1024];
        int count = 0;
        while ((count = in.read(buf)) >= 0) {
            out.write(buf, 0, count);
        }
        in.close();
        out.close();
	}
	
	private void retrieveAndReturnUrlContent(InputStream in, OutputStream out) throws Exception{
        // Copy the contents of the file to the output stream
        byte[] buf = new byte[1024];
        int count = 0;
        while ((count = in.read(buf)) >= 0) {
            out.write(buf, 0, count);
        }
        in.close();
        out.close();
	}

	private void replaceLinkAndReturnContentByBytes(HttpURLConnection connection, HttpServletResponse resp, String contentType, byte[] requestHostBytes) throws Exception{
		InputStream in = connection.getInputStream();
		OutputStream out = resp.getOutputStream();
		byte[] buf = new byte[1024];
        int count = 0;
        while ((count = in.read(buf)) >= 0) {
        	int leftCount = count-7;
        	int toWrite = 0;

        	for(int i=0;i<count;i++){
        		if (i<leftCount && buf[i]==104 && buf[i+1]==116 && buf[i+2]==116 && buf[i+3]==112 && buf[i+4]==58 && buf[i+5]==47 && buf[i+6]==47){
		        	//http://
		        	//104 116 116 112 58 47 47
        			out.write(buf, toWrite, i-toWrite);
        			out.write(prefixForHttp);
        			i += 7;
        			toWrite = i;
        		}
        		else if  (i<leftCount && buf[i]==104 && buf[i+1]==116 && buf[i+2]==116 && buf[i+3]==112 && buf[i+4]==115 && buf[i+5]==58 && buf[i+6]==47 && buf[i+7]==47){
	 	        	//https://
		        	//104 116 116 112 115 58 47 47
        			out.write(buf, toWrite, i-toWrite);
        			out.write(prefixForHttps);
        			i += 8;
        			toWrite = i;
        		}
        		else if (i<leftCount && buf[i]==104 && buf[i+1]==114 && buf[i+2]==101 && buf[i+3]==102 && buf[i+4]==61 && buf[i+5]==34 && buf[i+6]==47){
        			// href="/
		        	// 104, 114, 101, 102, 61, 34, 47
        			// =>
        			// href="/requstHost/
        			out.write(buf, toWrite, i-toWrite);
        			out.write(prefixForHrefDQ);
        			out.write(requestHostBytes);
        			out.write(prefixForslash);
        			i += 7;
        			toWrite = i;
        		}
        		else if (i<leftCount && buf[i]==104 && buf[i+1]==114 && buf[i+2]==101 && buf[i+3]==102 && buf[i+4]==61 && buf[i+5]==39 && buf[i+6]==47){
        			// href='/
		        	// 104, 114, 101, 102, 61, 39, 47
        			// =>
        			// href='/requstHost/
        			out.write(buf, toWrite, i-toWrite);
        			out.write(prefixForHrefSQ);
        			out.write(requestHostBytes);
        			out.write(prefixForslash);
        			i += 7;
        			toWrite = i;
        		}
        	}
        	if (toWrite<count){
        		out.write(buf, toWrite, count-toWrite);
        	}
        }
        in.close();
        resp.setContentType(contentType);
        out.flush();
        out.close();
	}
	
	private void replaceLinkAndReturnContent(HttpURLConnection connection, HttpServletResponse resp) throws Exception{
		BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
		StringBuffer sBuffer = new StringBuffer();
        String line;
        while ((line = reader.readLine()) != null) {
        	sBuffer.append(line);
        }
        reader.close();
        String content = sBuffer.toString();

        //replace
        content = content.replaceAll("src=\"https://", "src=\"https://g-proxy.appspot.com/https://");
        content = content.replaceAll("src=\"http://", "src=\"https://g-proxy.appspot.com/http://");
        content = content.replaceAll("href=\"https://", "href=\"https://g-proxy.appspot.com/https://");        
        content = content.replaceAll("href=\"http://", "href=\"https://g-proxy.appspot.com/http://");
        content = content.replaceAll("src='https://", "src='https://g-proxy.appspot.com/https://");
        content = content.replaceAll("src='http://", "src='https://g-proxy.appspot.com/http://");
        content = content.replaceAll("href='https://", "href='https://g-proxy.appspot.com/https://");
        content = content.replaceAll("href='http://", "href='https://g-proxy.appspot.com/http://");
        
		resp.setContentType(connection.getContentType());
		OutputStream out = resp.getOutputStream();
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out));

		writer.write(content, 0, content.length());
		writer.flush();
		writer.close();
	}
}
