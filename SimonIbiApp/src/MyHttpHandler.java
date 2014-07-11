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
				
				boolean gotData = false;
				boolean gotReqId = false;
				boolean gotDiaId = false;
				boolean rightModId = false;
				
				String data = "";
				String requestId = "";
				String dialogId = "";
//				int modalityId = 0;
				
				for(int i = 0; i< queryparts.length; i++)
				{
					String[] tmp = queryparts[i].replace("{", "").replace("}", "").split("=");
					
//					for(int j = 0; j < tmp.length; j++)
					if(tmp.length == 2)
					{
						if(tmp[0].equals("data"))
						{
							//{"options":["rauf","runter","rechts","links","eins","zwei","drei","vier","fünf","sechs","sieben","acht","neun"]}
							if(tmp[1].contains("options:[")) {
								gotData = true;
								tmp[1]  = tmp[1].replace("options:[", "");
								tmp[1]  = tmp[1].replace("+", " ");
								tmp[1]  = tmp[1].replace("]", " ");
								data = tmp[1];
//								ibicon_.setData(tmp[1].split(","));
								
							}
							
						} else if(tmp[0].equals("requestId")) {
							//save
							gotReqId = true;
							requestId = tmp[1];
							
						} else if(tmp[0].equals("dialogId")) {
							//save
							gotDiaId = true;
							dialogId = tmp[1];
							
						} else if(tmp[0].equals("modalityId")) {
							//check if this is my modalityID
							
							if(Integer.parseInt(tmp[1]) == ibicon_.getModalityId())
								rightModId = true;
						}
					}
				}
				
				if(gotData && gotReqId && gotDiaId && rightModId)
				{
					System.out.println("startRecognition data seems fine");
					ibicon_.setData(data.split(","));
					ibicon_.setRequestId(requestId);
					ibicon_.setDialogId(dialogId);
					
					ibicon_.setRecognitionRunning(true);
					byte[] response = "startRecognition DONE".getBytes();
					
					exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
			
					//System.out.println(request.getQuery());
					OutputStream os = exchange.getResponseBody();
					os.write(response);
					os.close();

				}
				else {
					byte[] response = ("startRecognition data error! commands: " + gotData + ", RequestId: " + gotReqId + 
							", DialogId: " + gotDiaId + ", matched modalityId: " + rightModId).getBytes();
					System.out.println(new String(response, "UTF-8"));
					
					exchange.sendResponseHeaders(HttpURLConnection.HTTP_BAD_REQUEST, response.length);
			
					//System.out.println(request.getQuery());
					OutputStream os = exchange.getResponseBody();
					os.write(response);
					os.close();

				}

			} else if(request.getPath().equals("/cancelRecognition")) {
		
				String query = request.getQuery();
				System.out.println("CancelRecognition found!\nQuerydata: " + query + "\n");
				if(ibicon_.getRecognitionRunning()) {
						
					//requestId=c4d5aa42-3e93-4f1e-9d5d-20dc84f93e2e&dialogId=5
					
					String[] queryparts = query.split("&");
					
					boolean gotReqId = false;
					boolean gotDiaId = false;
					
					String requestId = "";
					String dialogId = "";
					
					for(int i = 0; i< queryparts.length; i++)
					{
						String[] tmp = queryparts[i].replace("\"", "").split("=");
						
						if(tmp.length == 2) {
							if(tmp[0].equals("requestId")) {
								if(tmp[1].equals(ibicon_.getRequestId()))
									gotReqId = true;
							} else if(tmp[0].equals("dialogId")) {
								if(tmp[1].equals(ibicon_.getDialogId()))
										gotDiaId = true;
							}
						}
					}
					
					if(gotDiaId && gotReqId) {
						
						System.out.println("canceled running recognition!");
						byte[] response = "cancelRecognition DONE".getBytes();
						
						exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
						OutputStream os = exchange.getResponseBody();
						os.write(response);
						os.close();
						
						ibicon_.setRecognitionRunning(false);
						ibicon_.setDialogId("");
						ibicon_.setRequestId("");	
						//TODO call cancel Recognition -> deactivate simon
					} else {
						
						System.out.println("wrong cancelRecognition data!");
						byte[] response = ("cancelRecognition data error. matched requestId: " + gotReqId + ", matched dialogId: " + gotDiaId).getBytes();
						
						exchange.sendResponseHeaders(HttpURLConnection.HTTP_BAD_REQUEST, response.length);
						OutputStream os = exchange.getResponseBody();
						os.write(response);
						os.close();
						
					}
				} else {
					
					System.out.println("no running recognition!");
					byte[] response = "no running recognition".getBytes();
					
					exchange.sendResponseHeaders(HttpURLConnection.HTTP_BAD_REQUEST, response.length);
					OutputStream os = exchange.getResponseBody();
					os.write(response);
					os.close();
					
				}
				
			
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


