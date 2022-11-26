import java.io.ByteArrayInputStream;
import java.util.ArrayList;

/**
 * 
 */

/**
 * @author Jan Zimmer
 * last modified 26.11.2022
 */
public class TestScenario {

	public static void main(String[] args) {
		byte[] array = {1, 2, 3, 4};
		System.setIn(new ByteArrayInputStream(array));
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
