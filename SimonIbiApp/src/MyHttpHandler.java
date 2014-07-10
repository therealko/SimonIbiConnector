import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class MyHttpHandler implements HttpHandler {
	public void handle(HttpExchange exchange) throws IOException {
		
		URI request = exchange.getRequestURI();
		
		if(request.getPath().equals("/startRecognition")) {
	
			System.out.println("StartRecognition found!\nQuerydata: " + request.getQuery() + "\n");
	
			byte[] response = "startRecognition DONE".getBytes();
	
			exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
	
			//System.out.println(request.getQuery());
			OutputStream os = exchange.getResponseBody();
			os.write(response);
			os.close();
		
		} else if(request.getPath().equals("/cancelRecognition")) {
	
			System.out.println("CancelRecognition found!\nQuerydata: " + request.getQuery() + "\n");
		
			byte[] response = "cancelRecognition DONE".getBytes();
		
			exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
			OutputStream os = exchange.getResponseBody();
			os.write(response);
			os.close();
		
		} else if(request.getPath().equals("/getStatus")) {
	
			System.out.println("getStatus found!\n");
			
			byte[] response = "getStatus DONE".getBytes();
	
			exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
			OutputStream os = exchange.getResponseBody();
			os.write(response);
			os.close();
		
		} else {
	
			byte[] response = "nothing to fit\n".getBytes();
	      
      exchange.sendResponseHeaders(HttpURLConnection.HTTP_BAD_REQUEST, response.length);
      OutputStream os = exchange.getResponseBody();
      os.write(response);
      os.close();
  	}
  }
}



//static class MyHttpHandler implements HttpHandler {
//public void handle(HttpExchange exchange) throws IOException {
//	
//	URI request = exchange.getRequestURI();
//	
//	if(request.getPath().equals("/startRecognition")) {
//	
//		System.out.println("StartRecognition found!\nQuerydata: " + request.getQuery() + "\n");
//		
//		byte[] response = "startRecognition DONE".getBytes();
//		
//		exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
//		
//		//System.out.println(request.getQuery());
//		OutputStream os = exchange.getResponseBody();
//		os.write(response);
//		os.close();
//		
//	} else if(request.getPath().equals("/cancelRecognition")) {
//		
//		System.out.println("CancelRecognition found!\nQuerydata: " + request.getQuery() + "\n");
//		
//		byte[] response = "cancelRecognition DONE".getBytes();
//
//		exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
//		OutputStream os = exchange.getResponseBody();
//		os.write(response);
//		os.close();
//		
//	} else if(request.getPath().equals("/getStatus")) {
//		
//		System.out.println("getStatus found!\n");
//		
//		byte[] response = "getStatus DONE".getBytes();
//
//		exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
//		OutputStream os = exchange.getResponseBody();
//		os.write(response);
//		os.close();
//		
//	} else {
//		
//
//    byte[] response = "nothing to fit\n".getBytes();
//    
//    exchange.sendResponseHeaders(HttpURLConnection.HTTP_BAD_REQUEST, response.length);
//    OutputStream os = exchange.getResponseBody();
//    os.write(response);
//    os.close();
//	}
//}
//}

