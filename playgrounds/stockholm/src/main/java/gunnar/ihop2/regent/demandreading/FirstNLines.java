package gunnar.ihop2.regent.demandreading;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class FirstNLines {

	public FirstNLines() {
	}

	public static void main(String[] args) throws IOException {

		final String file = "./test/regentmatsim/exchange/trips2.xml";
//		final String to = "./test/regentmatsim/exchange/trips2.xml";
		final int maxLines = 100; // Integer.MAX_VALUE;
		int lines = 0;

		final BufferedReader reader = new BufferedReader(new FileReader(file));
//		final PrintWriter writer = new PrintWriter(to);
		
		String line;
		int objects = 0;
		while ((line = reader.readLine()) != null && (lines++) < maxLines) {
			System.out.println(line);
//			if (objects % 1000 == 0) {
//				System.out.println(objects);
//			}
//			if (line.startsWith("  <object id=")) {
//				writer.println("  <object id=\"" + (objects++) + "\">");
//			} else {
//				writer.println(line);
//			}
		}

//		writer.flush();
//		writer.close();
		
		reader.close();
	}
}
