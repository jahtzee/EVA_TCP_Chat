/**
 * 
 * @author Jan Zimmer
 * @mailto jazi1001@stud.hs-kl.de
 * @created 2022-10-16
 * 
 * Last modified 2022-11-25
 * 
 * Client class for the TCP Chat Application.
 * 
 **/

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketException;

public class Client implements Runnable {
	
	private int port = 10101;
	private String ipadr = "10.0.3.103"; //userver1
	private Socket client;
	private PrintWriter outputToServer;
	private BufferedReader inputFromServer;
	private boolean finished;
	
	public static void main(String[] args) {
		if (args.length == 2) {
			Client client = new Client(args[0], Integer.parseInt(args[1]));
			client.run();
		} else {
			Client client = new Client();
			client.run();
		}
	}
	
	public Client() {
	}
	
	public Client(String address, int port) {
		this.port = port;
		this.ipadr = address;
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
		} catch (ConnectException e) {
			System.err.println("Could not connect to server. Is it running?");	
		} catch (SocketException e) {
			System.err.println("Connection lost.");
		} catch (IOException e) {
			//ignore
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
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
