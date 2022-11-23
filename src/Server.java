/**
 * 
 * @author Jan Zimmer
 * @mailto jazi1001@stud.hs-kl.de
 * @created 2022-10-16
 * 
 * Last modified 2022-11-23
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
import java.net.SocketException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class Server implements Runnable {
	
	private ExecutorService threadpool;
	private int port = 10101;
	private ServerSocket server;
	private ArrayList<ConnectionHandler> connections;
	private boolean finished;
	private File userFile = new File("userList.lst");
	private HashMap<String, String> userMap = new HashMap<String, String>();
	/*
	 * Main
	 */
	public static void main(String[] args) {
		Server server = new Server();
		server.run();
	}
	
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
		 * Initializes our cached thread pool and welcome socket.
		 * Listens for incoming connections on specified port.
		 * Incoming connections are handled by instances of ConnectionHandler
		 * and executed by the Executor Service of our thread pool.
		 */
		if (userFile.exists()) {
			loadUserMap();
		} else {
			checkForUserFile();
		}
		try {
			threadpool = Executors.newCachedThreadPool();
			server = new ServerSocket(port);
			while (!finished) {
				Socket client = server.accept();
				ConnectionHandler handler = new ConnectionHandler(client);
				connections.add(handler);
				threadpool.execute(handler);
			}
		} catch (SocketException e) {
			System.err.println("A socket exception occurred while running the server.");
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
			shutDownServer();
		}
	}
	
	/*
	 * Stops the main loop within the servers run() method,
	 * closes the server and shuts down every connection handler within the connections list.
	 */
	public void shutDownServer() {
		finished = true;
		threadpool.shutdown();
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
	
	public File checkForUserFile() {
		try {
			File userFile = new File("userList.lst");
			if (userFile.createNewFile()); {
				log("userList.lst created.");
			}
		} catch (IOException e) {
			System.err.println("An error occured while checking for the userFile.");
			e.printStackTrace();
		}
		return userFile;
	}
	
	public void loadUserMap() {
		Properties props = new Properties();
		try {
			props.load(new FileInputStream("userList.lst"));
		} catch (FileNotFoundException e) {
			System.err.println("userList.lst was not found while trying to load the user list.");
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println("An error occurred while attempting to load the user list file.");
			e.printStackTrace();
		}
		for (String key : props.stringPropertyNames()) {
			Server.this.userMap.put(key, props.get(key).toString());
		}
	}
	
	public void saveUserMap() {
		Properties props = new Properties();
		for (Map.Entry<String, String> entry : Server.this.userMap.entrySet()) {
			props.put(entry.getKey(), entry.getValue());
		}
		try {
			props.store(new FileOutputStream("userList.lst"), null);
		} catch (FileNotFoundException e) {
			System.err.println("The userList file could not be found while attempting to save.");
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println("An error occurred while saving the userList file.");
			e.printStackTrace();
		}
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
				if (!isKnownUser()) {
					askForNickname();
				} else {
					nickname = Server.this.userMap.get(this.client.getInetAddress().toString());
				}
				messageToClient("Hi, " + nickname + "! Type in a message or enter ':help' for a list of commands.\n");
				String userInput;
				while ((userInput = input.readLine()) != null) {
					handleCommand(userInput);
				}
			} catch (IOException e) {
				shutDownConnectionHandler();
			}
		}
		
		private void handleCommand(String userInput) {
			if (userInput.startsWith(":nick")) {
				changeNickname(userInput);
			} else if (userInput.startsWith(":quit")) {
				broadcast(nickname + " has left the chat.");
				log(nickname + " is disconnecting.");
				shutDownConnectionHandler();
			} else if (userInput.startsWith(":help")) {
				printHelp();
			} else {
				broadcast(nickname + ": " + userInput);
			}
			
		}

		private void changeNickname(String userInput) {
			String[] inputSplit = userInput.split(" ", 2);
			if (inputSplit.length == 2) {
				broadcast(nickname + " renamed themselves to " + inputSplit[1] + ".");
				log(nickname + " renamed themselves to " + inputSplit[1] + ".");
				nickname = inputSplit[1];
				messageToClient("Successfully changed nickname to " + nickname + ".");
				Server.this.userMap.remove(this.client.getInetAddress().toString());
				userMap.put(this.client.getInetAddress().toString(), nickname);
				Server.this.saveUserMap();
			} else {
				messageToClient("No nickname provided.");
			}
		}
		
		private boolean isKnownUser() {
			if (Server.this.userMap.containsKey(this.client.getInetAddress().toString())) {
				return true;
			} else {
				return false;
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
				broadcast(nickname + " just joined the chat. Say hello!");
				userMap.put(this.client.getInetAddress().toString(), nickname);
				Server.this.saveUserMap();
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
