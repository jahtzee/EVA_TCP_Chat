/**
 * @author Jan Zimmer
 * last modified 15.11.2022
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Client implements Runnable {
	
	private int port = 10101;
	private String ipadr = "127.0.0.1";
	private Socket client;
	private PrintWriter outputToServer;
	private BufferedReader inputFromServer;
	private boolean finished;
	
	public static void main(String[] args) {
			Client client = new Client();
			client.run();
	}
	
	@Override
	public void run() {
		/*
		 * Establishes a connection to the specified Server
		 */
		try {
			String inMsg;
			client = new Socket(ipadr, port);
			outputToServer = new PrintWriter(client.getOutputStream(), true);
			inputFromServer = new BufferedReader(new InputStreamReader(client.getInputStream()));
			InputHandler inputHandler = new InputHandler();
			Thread thread = new Thread(inputHandler); //We use just a single thread here, not a pool, since there is only one InputHandler per instance of Client
			thread.start(); //Open another thread with the inputHandler
			while ((inMsg = inputFromServer.readLine()) != null) {
				System.out.println(inMsg);
			}
		} catch (Exception e) {
			shutdownClient();
		}
	}
	
	private void shutdownClient() {
		/*
		 * Closes resources and client
		 */
		finished = true;
		try {
			inputFromServer.close();
			outputToServer.close();
			if (!client.isClosed()) {
				client.close();
			}
		} catch (IOException e) {
			// We can not handle an IOException in this instance, so we are going to ignore it.
		}
		
	}

	class InputHandler implements Runnable {
		/*
		 * Handles user input coming in through the command line, forwards it to the server client.inputStream. If a :quit command was entered, also invokes a shutdown of the client.
		 */
		@Override
		public void run() {
			try {
				BufferedReader cliInput = new BufferedReader(new InputStreamReader(System.in));
				while(!finished) {
					String msg = cliInput.readLine();
					if (msg.equals(":quit")) {
						outputToServer.println(msg); // sends :quit
						cliInput.close();
						shutdownClient();
					} else {
						outputToServer.println(msg); //output is sent to the server
					}
				}	
			} catch (IOException e) {
				shutdownClient();
			}
		}
	}
}
