/**
 * 
 * @author Jan Zimmer
 * @mailto jazi1001@stud.hs-kl.de
 * @created 2022-10-16
 * 
 * Last modified 2022-11-25
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
import java.util.concurrent.locks.ReentrantLock;
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
	private ReentrantLock userMapLock = new ReentrantLock();
	
	/*
	 * Main
	 */
	public static void main(String[] args) {
		if (args.length == 1) {
			Server server = new Server(Integer.parseInt(args[0]));
			server.run();
		} else {
			Server server = new Server();
			server.run();
		}
	}
	
	/*
	 * Constructor
	 */
	public Server() {
		connections = new ArrayList<>();
		finished = false;
	}
	
	public Server(int port) {
		connections = new ArrayList<>();
		finished = false;
		this.port = port;
	}
	
	@Override
	public void run() {
		/*
		 * Initializes our cached thread pool and welcome socket.
		 * Listens for incoming connections on specified port.
		 * Incoming connections are handled by instances of ConnectionHandler
		 * and executed by the Executor Service of our thread pool.
		 */
		log("Server is now running.");
		if (userFile.exists()) {
			loadUserMap();
		} else {
			checkForUserMapFile();
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
	private void shutDownServer() {
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
	
	private void broadcast(String s) {
		/*
		 * Broadcast a String s to all currently connected clients
		 */
		for (ConnectionHandler c : connections) {
			if (c != null) {
				c.messageToClient(s);
			}
		}
	}
	
	private void log(String s) {
		/*
		 * Print String s to the CLI
		 */
		System.out.println(s);
	}
	
	private File checkForUserMapFile() {
		try {
			File userFile = new File("userList.lst");
			if (userFile.createNewFile()); {
				log("userList.lst created.");
			}
		} catch (IOException e) {
			System.err.println("An error occured while checking for / creating the userFile.");
			e.printStackTrace();
		}
		return userFile;
	}
	
	private void loadUserMap() {
		Server.this.userMapLock.lock();
		Properties props = new Properties();
		try {
			props.load(new FileInputStream("userList.lst"));
			for (String key : props.stringPropertyNames()) {
				Server.this.userMap.put(key, props.get(key).toString());
			}
		} catch (FileNotFoundException e) {
			System.err.println("userList.lst was not found while trying to load the user list.");
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println("An error occurred while attempting to load the user list file.");
			e.printStackTrace();
		} finally {
			Server.this.userMapLock.unlock();
		}
	}
	
	private void saveUserMap() {
		Server.this.userMapLock.lock();
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
		} finally {
			Server.this.userMapLock.unlock();
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
			 * Establishes in- and output. Asks for a nickname if the clients address is not recognized. Finally, awaits user input.
			 */
			try {
				output = new PrintWriter(client.getOutputStream(), true);
				input = new BufferedReader(new InputStreamReader(client.getInputStream()));
				if (!isKnownUser()) {
					askForNickname();
				} else {
					nickname = Server.this.userMap.get(this.client.getInetAddress().toString());
					log(nickname + " just connected.");
					broadcast(nickname + " just joined the chat. Hi there!");
				}
				messageToClient("Hi, " + nickname + "! Type in a message or enter ':help' for a list of commands.\n");
				String userInput;
				while ((userInput = input.readLine()) != null && !client.isClosed()) {
					handleCommand(userInput);
				}
			} catch (IOException e) {
				shutDownConnectionHandler();
			}
		}
		
		private void handleCommand(String userInput) {
			/*
			 * Interprets user commands (:command)
			 */
			if (userInput.startsWith(":nick")) {
				changeNickname(userInput);
			} else if (userInput.startsWith(":quit")) {
				broadcast(nickname + " has left the chat.");
				log(nickname + " has disconnected.");
				Server.this.connections.remove(this);
				shutDownConnectionHandler();
			} else if (userInput.startsWith(":help")) {
				printHelp();
			} else if (userInput.startsWith(":users")) {
				printConnectedUsers();
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

		private void shutDownConnectionHandler() {
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
				log(nickname + " just connected.");
				broadcast(nickname + " just joined the chat. Hi there!");
				userMap.put(this.client.getInetAddress().toString(), nickname);
				Server.this.saveUserMap();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		private void messageToClient(String s) {
			/*
			 * Output String s on Client CLI
			 */
			output.println(s);
		}
		
		private void printConnectedUsers() {
			messageToClient("Currently connected:");
			for (ConnectionHandler ch :Server.this.connections) {
				if (ch.client.isConnected())
					messageToClient("- "+ch.nickname);
			}
		}
		
		private void printHelp() {
			messageToClient("This is a list of available commands:\n"
					+ ":help - Shows a list of available commands.\n"
					+ ":nick new_Nickname - Change your nickname.\n"
					+ ":users - Shows a list of connected users.\n"
					+ ":quit - Disconnects you from the server.\n");
		}
	}

}
