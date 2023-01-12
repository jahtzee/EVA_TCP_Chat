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
import java.net.UnknownHostException;

public class Client implements Runnable {

	private int port = 10101;
	private String ipadr = "10.0.3.103"; //userver1
	private Socket client;
	private PrintWriter outputToServer;
	private BufferedReader inputFromServer;
	private Boolean isDone = false;
	private Boolean isReconnectAttempt = false;
	private Thread inputHandlerThread; //Separate thread for the InputHandler

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
		try {
			connect();
		} catch (ConnectException e) {
			System.err.println("Unable to establish a connection.");
		} catch (UnknownHostException e) {
			System.err.println("Unable to establish a connection. Please check server address.");
		} catch (IOException e) {
			//We can't handle an IOException in this instance.
		}
	}

	private void connect() throws UnknownHostException, IOException {
		/*
		 * Establishes a connection to the specified Server. An InputHandler is instantiated and assigned to a new thread.
		 * The client continues listening for incoming input from the server until the connection is closed. If closed unexpectedly,
		 * we attempt reconnection. Reconnection in the finally block is not attempted if we are already in a reconnect attempt.
		 */
		try {
			client = new Socket(ipadr, port);
			outputToServer = new PrintWriter(client.getOutputStream(), true);
			inputFromServer = new BufferedReader(new InputStreamReader(client.getInputStream()));
			if (!isReconnectAttempt) {
				InputHandler inputHandler = new InputHandler();
				inputHandlerThread = new Thread(inputHandler); 
				inputHandlerThread.start(); 
			}
			String inMsg;
			while ((inMsg = inputFromServer.readLine()) != null) {
				isReconnectAttempt = false; //A successful connection ends the reconnection attempt.
				System.out.println(inMsg);
			}
		} finally {
			if (!isDone && !isReconnectAttempt) {
				System.err.println("Connection closed unexpectedly. Attempting to reconnect...");
				reconnect();
			}
		}
	}

	private void reconnect() {
		/*
		 * Up to 5 reconnection attempts are made before the client is closed.
		 */
		int reconnectionAttempts = 0;
		isReconnectAttempt = true;
		while(!isDone && reconnectionAttempts < 5) {
			try {
				Thread.sleep(1000);
				connect();
			} catch (ConnectException e) {
				reconnectionAttempts++;
			} catch (UnknownHostException e) {
				reconnectionAttempts++;
				e.printStackTrace();
			} catch (IOException e) {
				//We can't handle an IOException in this instance.
				e.printStackTrace();
			} catch (InterruptedException e) {
				//We can't handle an InterruptedException in this instance.
			}
		}
		if(reconnectionAttempts >=5){
			System.err.println("Max reconnection attempts reached. Shutting down...");
			shutdownClient();
		}	
	}

	private void shutdownClient() {
		/*
		 * Closes resources and client
		 */
		isDone = true;
		try {
			inputFromServer.close();
			outputToServer.close();
			if (!client.isClosed()) {
				client.close();
			}
			System.exit(0); //This is terrible, but reading a line from cliInput in the InputHandler blocks the entire thread
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
				while(!isDone && !Thread.currentThread().isInterrupted()) {
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
