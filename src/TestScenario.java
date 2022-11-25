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
		ArrayList<Client> clients = new ArrayList<Client>();
		for (int i = 1; i<= 100; i++) {
			clients.add(new Client());
		}
	}

}
