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

import java.io.ByteArrayInputStream;
import java.util.ArrayList;

public class TestScenario {

	public static void main(String[] args) {
		ArrayList<Client> clients = new ArrayList<Client>();
		ArrayList<Thread> threadfield = new ArrayList<Thread>();
		for (int i = 1; i<= 2; i++) {
			Client c = new Client();
			clients.add(c);
			Thread t = new Thread(c);
			threadfield.add(t);
			t.start();
		}
	}
}
