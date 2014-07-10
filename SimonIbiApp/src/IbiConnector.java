import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.sun.net.httpserver.HttpServer;

public class IbiConnector
{

	//TODO read from config
		private static String protocol_ = "http";
		
		private String ibiHost_ = "127.0.0.1";
		private int ibiPort_ = 9002;
		private String simonIp_ = "127.0.0.1";
		private int simonPort_ = 8888;
		
		private IoHandler iohandler_;
		private HttpServer server_;
		
		private ArrayList<String> keywords;
		
		private ScheduledExecutorService exec_;
		
		private Socket socket_;
		private BufferedWriter writer_;
		private BufferedReader reader_;
		
		/*
		 * IBI Parameter
		 * ModalityID = SimonID by IBI
		 * RequestID 	= the request id from the ibi request
		 * DialogID 	= Id from the dialog at ibi
		 */
		private int modalityId_ = 0;
		public int getModalityId()
		{
			return modalityId_;
		}
		private String requestId_ = "";
		private String dialogId_ = "";
		
		private boolean registered_;
		public boolean getRegistered()
		{
			return registered_;
		}
		
		private boolean recognitionRunning_;
		public boolean getRecognitionRunning()
		{
			return recognitionRunning_;
		}
		
		public enum CMD_CODE {
			//provided Methods from Simon
			REGISTERMODALITY(10),
			GETMODALITYSTATUS(11),
			HANDLEMODALITYACTION(12),
			
			//provided Methods from IBI
			STARTRECOGNITION(13),
			CANCELRECOGNITION(14),
			GETSTATUS(15),
			
			//status or check codes
			EVERYTHINGISALLRIGHT(50),
			
			//http codes
			OKAY(200),
			BADREQUEST(400);
			
			 private int code;
			 
			 private CMD_CODE(int c) {
			   code = c;
			 }
			 
			 public int getCode() {
			   return code;
			 }
			 
		}
		
		public IbiConnector(String ibiIp, int ibiPort, String simonIp, int simonPort){
			ibiHost_ = ibiIp;
			ibiPort_ = ibiPort;
			simonIp_ = simonIp;
			simonPort_ = simonPort;
			registered_ = false;
			
			this.iohandler_ = new IoHandler();
			if(initConnection() == -1)
    	{
    		if(close() == 1)
    			initConnection();
    	}
			
		}
		
		public int initConnection()
		{
			System.out.println("#### starting Server! ####");
			try {
				server_ = HttpServer.create(new InetSocketAddress(8888),0);
				server_.createContext("/startRecognition", new MyHttpHandler(this));
				server_.createContext("/cancelRecognition", new MyHttpHandler(this));
				server_.createContext("/getStatus", new MyHttpHandler(this));
				server_.setExecutor(null);
				server_.start();
				System.out.println("Server is started");
				
				startGetModalityStatusThread();
				
				return 1;
			} catch (IOException e) {
				System.err.println("Couldn't start server:\n" + e);
				e.printStackTrace();
				return 0;
			}
		}

		public int close()
		{
			System.out.println("#### closing Server! ####");
			exec_.shutdownNow();
			server_.stop(0);
			System.out.println("everything is closed");
			return 1;
		}
		
		//TODO Path unterscheidung wie handleModalityAction/registerModality/getModalityStatus
		public int sendRequest(String path, String parameters)
		{
			
			URL url;
	    HttpURLConnection connection = null; 
	    
			try {
				System.out.println("sending a request: ");
				String data = "";

				if(path.equals("/handleModalityAction")) {
					System.out.println("\n#### handleModalityAction! ####\n");
					data = "command=eins";
					
				} else if(path.equals("/registerModality")) {
					System.out.println("\n#### registerModality! ####\n");
					
					data = URLEncoder.encode("params", "UTF-8") + "=" + 
					URLEncoder.encode("{\"name\"", "UTF-8") + 		":" + URLEncoder.encode("\"simonListensClient\"", "UTF-8") + "," +
					URLEncoder.encode("\"direction\"", "UTF-8") + ":" + URLEncoder.encode("\"input\"", "UTF-8") + "," +
					URLEncoder.encode("\"type\"", "UTF-8") + 			":" + URLEncoder.encode("\"vocal\"", "UTF-8") + "," + 
					URLEncoder.encode("\"port\"", "UTF-8") + 			":" + URLEncoder.encode(Integer.toString(simonPort_) + "}", "UTF-8");

					System.out.println("registerModality params: " + data);  
					
					 url = new URL("http://" + ibiHost_ + ":" + ibiPort_ + path );
				    System.out.println("url: " + url.toString());
			      connection = (HttpURLConnection)url.openConnection();
			      connection.setRequestMethod("POST");
			      connection.setRequestProperty("Content-Type", 
			           "application/x-www-form-urlencoded");
						
			      connection.setRequestProperty("Content-Length", "" + 
			               Integer.toString(data.getBytes().length));
			      connection.setRequestProperty("Content-Language", "en-US");  
						
			      connection.setUseCaches (false);
			      connection.setDoInput(true);
			      connection.setDoOutput(true);

			      //Send request
			      DataOutputStream wr = new DataOutputStream (
			                  connection.getOutputStream ());
			      wr.writeBytes(data);
			      wr.flush ();
			      wr.close ();

				} else if(path.equals("/getModalityStatus")) {
					System.out.println("\n#### getModalityStatus! ####\n\tModalityID: " + modalityId_);
					data = URLEncoder.encode("params", "UTF-8") + "=" + URLEncoder.encode("{\"modalityId\"", "UTF-8") + ":" +
					URLEncoder.encode(Integer.toString(modalityId_) + "}", "UTF-8");
					
			    url = new URL("http://" + ibiHost_ + ":" + ibiPort_ + path );
			    System.out.println("url: " + url.toString());
		      connection = (HttpURLConnection)url.openConnection();
		      connection.setRequestMethod("POST");
		      connection.setRequestProperty("Content-Type", 
		           "application/x-www-form-urlencoded");
					
		      connection.setRequestProperty("Content-Length", "" + 
		               Integer.toString(data.getBytes().length));
		      connection.setRequestProperty("Content-Language", "en-US");  
					
		      connection.setUseCaches (false);
		      connection.setDoInput(true);
		      connection.setDoOutput(true);
	
		      //Send request
		      DataOutputStream wr = new DataOutputStream (
		                  connection.getOutputStream ());
		      wr.writeBytes(data);
		      wr.flush ();
		      wr.close ();
				}
				
				System.out.println("Sending done, waiting for an answer!");
	      System.out.println("try to read the response");
	      //Get Response	
	      InputStream is = connection.getInputStream();
	      BufferedReader rd = new BufferedReader(new InputStreamReader(is));
	      String line;
	      StringBuffer response = new StringBuffer(); 
	      
	      while((line = rd.readLine()) != null) {
	        response.append(line);
	        response.append('\r');
	      }
	      rd.close();
	      System.out.println("Response: " + response.toString());
	      
	      String body = response.toString();
	      String[] bodylist;
	      
	      if(path == "/getModalityStatus")
	      {
	      	//filter status:ok bzw status:nok
	      	
	      	body = body.replace("\"", "");
	      	//bodylist = body.split(",");
	      	
	      	if(body.contains("status:ok")) {
	      		return CMD_CODE.EVERYTHINGISALLRIGHT.getCode();
	      	}
	      	else if(body.contains("status:nok")) {
	      		return CMD_CODE.REGISTERMODALITY.getCode();
	      	}
	      	else {
	      		System.out.println("something is wrong with the getModalityStatus Response.");
	      		//new getModalityStatus in 30sec
	      		return CMD_CODE.GETMODALITYSTATUS.getCode();
	      	}

	      } else if (path == "/registerModality") {
	      	
	      	body = body.replace("\"", "");
	      	
	      	//System.out.println("body: " + body);
	      	if(body.contains("status:ok")) {
	      		bodylist = body.split(",");
	      		
	      		for(int i = 0; i< bodylist.length; i++)
	      		{
	      			String[] tmp = bodylist[i].split(":");
	      			//System.out.println("part: " + tmp[0]);
	      			if((tmp.length == 2) && (tmp[0].equals("modalityId"))) {
	      				modalityId_ = Integer.parseInt(tmp[1]);
	      				System.out.println("new ModalityID is #" + modalityId_);
	      			}
	      			
	      		}

	      		return CMD_CODE.EVERYTHINGISALLRIGHT.getCode();
	      	}
	      	else if(body.contains("status:nok")) {
	      		return CMD_CODE.REGISTERMODALITY.getCode();
	      	}
	      	else {
	      		System.out.println("something is wrong with the getModalityStatus Response.");
	      		return CMD_CODE.GETMODALITYSTATUS.getCode();
	      	}

	      	
	      }
	      
	      return 0;
	      
      } catch (UnknownHostException e) {
	      // TODO Auto-generated catch block
      	System.err.println("Couldn't find host\n" + e);
	      e.printStackTrace();
      } catch (IOException e) {
	      // TODO Auto-generated catch block
      	System.err.println("Couldn't send a request or read the response\n" + e);
	      e.printStackTrace();
			} finally {
				if(connection != null) {
					connection.disconnect(); 
      }
    }
			return -1;
		}
		
		public int checkInput(String path, String header, String body)
    {
			List<String> headerlist = Arrays.asList(header.split("\n|\r|,|[|]"));
      //System.out.println("headerlist: " + headerlist);
      
      List<String> bodylist = Arrays.asList(body.split("\n|,"));
      System.out.println("bodylist: " + bodylist);
      
      
      if(path.equals("/getModalityStatus")) {
      	
      	Iterator itr= headerlist.iterator();

      	boolean okay = false;
      	while(itr.hasNext()) {
      		String row = (String) itr.next();
      	  //System.out.println("checking headerrow: " + row);
      		if(row.equals("HTTP/1.1 200 OK")) {
      			System.out.println("HTTP STATUS 200 OKAY");
      			okay = true;
      		}
      	}
      	
      	itr = bodylist.iterator();
      	while(itr.hasNext() && okay) {
      		String row = (String) itr.next();
      	  //System.out.println("bodylist: " + row);
      	  
      	  row = row.replace("\"", "");
      	  //System.out.println("checking bodyrow: " + row);
    	  	if(row.equals("status:ok"))
    	  	{
    	  		System.out.println("ModalityId is correct!");
    	  		return CMD_CODE.OKAY.getCode();
    	  		
    	  	} else if(row.equals("status:nok")) {
    	  		System.out.println("ModalityId is out of date, try new registerModalityID!s");
    	  		return CMD_CODE.REGISTERMODALITY.getCode();
    	  	}
    	  	else
    	  		System.out.println("something wrong with reading params from body!");
      	}
      	
      	return CMD_CODE.BADREQUEST.getCode();
      	
      } else if(path.equals("/registerModality")) {
      	Iterator itr= headerlist.iterator();

      	boolean okay = false;
      	while(itr.hasNext()) {
      		String row = (String) itr.next();
      	  //System.out.println("list: " + row);

      		if(row.equals("HTTP/1.1 200 OK")) {
      			System.out.println("HTTP STATUS 200 OKAY");
      			okay = true;
      		}
      	}
      	
      	itr = bodylist.iterator();
      	
      	while(itr.hasNext() && okay) {
      		String row = (String) itr.next();
      	  System.out.println("bodylist: " + row);
      	  
      	  row = row.replace("\"", "");
//    	  	if(row.contains("status") && (row.contains("ok")) && row.equals("status:ok"))
    	  	if(row.equals("status:ok"))
      	  {
    	  		System.out.println("ModalityId is correct!");
    	  		return CMD_CODE.OKAY.getCode();
    	  		
//    	  	} else if(row.contains("status") && (row.contains("nok")) && row.equals("status:nok")) {
      	  } else if(row.equals("status:nok")) {
    	  		System.out.println("ModalityId is out of date, try new registerModalityID!s");
    	  		return CMD_CODE.REGISTERMODALITY.getCode();
    	  	}
    	  	else
    	  		System.out.println("something wrong with reading params from body!");
      	}
      	
      	
      } else if(path.equals("/handleModalityAction")) {
      	
      }
      
    	return 0;
    }
		
		public boolean containsKeyword(String keyword)
		{
			//TODO
			if(keywords.contains(keyword)){
				if(sendRequest("/handeModalityAction", keyword) == CMD_CODE.OKAY.getCode())
					return true;
			}
			
			return false;
		}
		
		
		
		public void startGetModalityStatusThread()
		{
			System.out.println("trying to start getModalityStatusThread");
			try
			{
				exec_ = Executors.newSingleThreadScheduledExecutor();
				exec_.scheduleAtFixedRate(new Runnable() {
				  @Override
				  public void run() {
				  	DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
				  	Date date = new Date();
				    System.out.println("getModalityStatusThread at: " + dateFormat.format(date));
				    
				    if(sendRequest("/getModalityStatus", "") == CMD_CODE.REGISTERMODALITY.getCode()) {
				    	registered_ = false;
				    	if(sendRequest("/registerModality", "") != CMD_CODE.EVERYTHINGISALLRIGHT.getCode())
				    			System.out.println("Register failed. Waiting for next getModalityStatus");
				    	else
			    			registered_ = true;
				    }
			    	System.out.println("\nnext getModalityStatus in about 30secounds!\n");
			    	
				  }
				}, 5, 30, TimeUnit.SECONDS);
				System.out.println("getModalityStatusThread is now running");
			} catch (Exception e) {
	      // TODO Auto-generated catch block
      	System.err.println("Couldn't start GetModalityStatus Thread\n" + e);
	      e.printStackTrace();
      }
		}
   
		
   public static void main(String[] args) {

    IbiConnector ibicon = new IbiConnector("127.0.0.1", 9002, "127.0.0.1", 8888);
    if(ibicon.close() == 1)
    	ibicon.initConnection();
    else
    	System.out.println("couldn close the server");

   }
}
