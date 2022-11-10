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
import java.util.ArrayList;

public class Server implements Runnable {
	
	private int port = 10101;
	private ServerSocket server;
	private ArrayList<ConnectionHandler> connections;
	private boolean finished;
	
	/*
	 * Constructor
	 */
	public Server() {
		connections = new ArrayList<>();
		finished = false;
	}
	
	@Override
	public void run() {
		/*
		 * Listens for incoming connections on specified port.
		 * Incoming connections are handled by instances of ConnectionHandler.
		 */
		try {
			server = new ServerSocket(port);
			while (!finished) {
				Socket client = server.accept();
				ConnectionHandler handler = new ConnectionHandler(client);
				connections.add(handler);
			}
		} catch (IOException e) {
			e.printStackTrace();
			shutDownServer();
		}
	}
	
	public void shutDownServer() {
		finished = true;
		if (!server.isClosed()) {
			try {
				server.close();
			} catch (IOException e) {
				// We can not handle an IOException in this instance, so we are going to ignore it.
			}
		for (ConnectionHandler h : connections) {
			h.shutDownConnectionHandler();
		}
		}
	}
	
	public void broadcast(String s) {
		/*
		 * Broadcast a String s to all currently connected clients
		 */
		for (ConnectionHandler c : connections) {
			if (c != null) {
				c.messageToClient(s);
			}
		}
	}
	
	public void log(String s) {
		/*
		 * Print String s to the CLI
		 */
		System.out.println(s);
	}
	
	/*
	 * Connection Handler sub class
	 */	
	class ConnectionHandler implements Runnable {
		/*
		 * Handles client connections.
		 */
		private Socket client;
		private BufferedReader input; /*Data sent BY client*/
		private PrintWriter output; /*Data sent TO client*/
		private String nickname;
		
		/*
		 * Constructor
		 */
		public ConnectionHandler(Socket client) {
			this.client = client;
		}

		@Override
		public void run() {
			/*
			 * Establishes in- and output. Asks for a nickname. Finally, awaits user input.
			 */
			try {
				output = new PrintWriter(client.getOutputStream(), true);
				input = new BufferedReader(new InputStreamReader(client.getInputStream()));
				askForNickname();
				messageToClient("Hi, " + nickname + "! Type in a message or enter ':help' for a list of commands.\n");
				String userInput;
				while ((userInput = input.readLine()) != null) {
					if (userInput.startsWith(":")) {
						if (userInput.equals(":help"))
							printHelp();
						if (userInput.equals(":nick"))
							// TODO Implement nickname changing function
							log("Nick is not yet implemented.");
							//changeNickname();
						if (userInput.equals(":quit"))
							broadcast(nickname+" has left the chat.");
							shutDownConnectionHandler();
					} else {
						broadcast(userInput);
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
				shutDownConnectionHandler();
			}
		}
		
		public void shutDownConnectionHandler() {
			try {
				input.close();
				output.close();
				if (!client.isClosed()) {
					client.close();
				}
			} catch(IOException e) {
				// We can not handle an IOException in this instance, so we are going to ignore it.
			}
		}
		
		private void askForNickname( ) {
			output.println("Please enter a nickname to be used for the duration of this session:\n");
			try {
				nickname = input.readLine();
				log(nickname + " just connected!");
				broadcast(nickname + "just joined the chat. Say hello!");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		public void messageToClient(String s) {
			/*
			 * Output String s on Client CLI
			 */
			output.println(s);
		}
		
		private void printHelp() {
			messageToClient("This is a list of available commands:\n"
					+ ":help - Shows a list of available commands.\n"
					+ ":nick - Change your nickname.\n"
					+ ":quit - Disconnects your from the server.\n");
		}
	}

}
