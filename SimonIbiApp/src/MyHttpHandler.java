import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;

import sun.reflect.ReflectionFactory.GetReflectionFactoryAction;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class MyHttpHandler implements HttpHandler {
	
	private IbiConnector ibicon_;
	
	public MyHttpHandler(IbiConnector ibicon)
	{
		ibicon_ = ibicon;
	}
	
	public void handle(HttpExchange exchange) throws IOException {
		
		URI request = exchange.getRequestURI();
		
		if(ibicon_.getRegistered() == true) {
			
///////////
			if(request.getPath().equals("/startRecognition")) {
		
				String query = request.getQuery().replace("\"", "");
				System.out.println("StartRecognition found!\nQuerydata: " + query + "\n");
		
				String[] queryparts = query.split("&");
				
				for(int i = 0; i< queryparts.length; i++)
				{
					System.out.println(queryparts[i]);
					String[] tmp = queryparts[i].replace("{", "").replace("}", "").split("=");
					
					for(int j = 0; j < tmp.length; j++)
					{
						if(tmp[j].equals("data"))
						{
							//{"options":["rauf","runter","rechts","links","eins","zwei","drei","vier","fünf","sechs","sieben","acht","neun"]}
						} else if(tmp[j].equals("requestId")) {
							//save
						} else if(tmp[j].equals("dialgoId")) {
							//save
						} else if(tmp[j].equals("modalityId")) {
							//check if this is my modalityID
						}
					}
				}
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
		
				byte[] response;
				System.out.println("getStatus found!");
				
					if(ibicon_.getRecognitionRunning())
						response = ("status=\"status\":\"processing\"&\"modalityId\"=" + ibicon_.getModalityId()).getBytes();
					else		
						response = ("status=\"status\":\"idle\"&\"modalityId\"=" + ibicon_.getModalityId()).getBytes();
					
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

		} else {
			byte[] response;
			response = "Simon is not registered".getBytes();
			System.out.println("Simon is not registered. Sending Bad Request Response");
			exchange.sendResponseHeaders(HttpURLConnection.HTTP_BAD_REQUEST, response.length);
			OutputStream os = exchange.getResponseBody();
			os.write(response);
			os.close();
		}
			/*
			 * resp->end("status=\"status\":\"idle\"&"
            "\"modalityId\"=\"" + QString::number(manager_->getModalityID()).toAscii() + "\"");
			 */

  }
}


