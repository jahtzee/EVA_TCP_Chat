/**
 * 
 * @author Jan Zimmer
 * @mailto jazi1001@stud.hs-kl.de
 * @created 2022-10-16
 * 
 * Last modified 2022-10-16
 * 
 * Server class for the TCP Chat Application.
 * 
 **/

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class Server implements Runnable{
	
	private int port = 10101;

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

	@Override
	public void run() {
		
		/*
		 * Listens for incoming connections on specified port.
		 * Incoming connections are handled by instances of ConnectionHandler.
		 */
		
		try {
			ServerSocket server = new ServerSocket(port);
			Socket client = server.accept();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	class ConnectionHandler implements Runnable {
		
		/*
		 * Handles client connections.
		 */
		
		private Socket client;
		private BufferedReader input; /*Data sent BY client*/
		private PrintWriter output; /*Data sent TO client*/
		
		public ConnectionHandler(Socket client) {
			this.client = client;
		}
		
		@Override
		public void run() {
			try {
				output = new PrintWriter(client.getOutputStream(), true);
				input = new BufferedReader(new InputStreamReader(client.getInputStream()));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}

}
