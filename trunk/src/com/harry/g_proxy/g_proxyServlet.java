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
	
	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		
		resp.setContentType("text/plain");
		try{
			//find the URL
			String realUrl = getUrl(req);
			if (realUrl.startsWith("/")) realUrl = realUrl.substring(1);
			

			//whether is the Home page
			if (realUrl.length()==0 || realUrl.equalsIgnoreCase("index.html")) {
				returnHome(resp);
				return;
			}
			
			log.info("The inputed URI:"+realUrl);
			
			//form the URL
			URL url = new URL(realUrl);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			String contentType = connection.getContentType();
			if (connection.getResponseCode() == HttpURLConnection.HTTP_OK && contentType.toLowerCase().contains("text")) {
                // is text file, replace every links inside
    			replaceLinkAndReturnContent(connection, resp);
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
//        System.out.println("print out the stream:");
        while ((count = in.read(buf)) >= 0) {
//        	System.out.println(count);
            out.write(buf, 0, count);
        }
        in.close();
        out.close();
	}
	
	private void replaceLinkAndReturnContent(HttpURLConnection connection, HttpServletResponse resp) throws Exception{
		BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
		StringBuffer sBuffer = new StringBuffer();
        String line;
        log.info("--------------in------------:");
        while ((line = reader.readLine()) != null) {
        	sBuffer.append(line);
        	log.info(line);
        }
        reader.close();
        String content = sBuffer.toString();
        log.info("--------------out("+content.length()+")------------:");
        log.info(content);
        
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
