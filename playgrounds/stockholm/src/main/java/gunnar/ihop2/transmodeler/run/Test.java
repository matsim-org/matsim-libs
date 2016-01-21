package gunnar.ihop2.transmodeler.run;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

class Test {

	private Test() {
	}

	public static void main(String[] args) throws IOException {
		System.out.println("STARTED ...");
		int i = 0;

		final BufferedReader reader = new BufferedReader(new FileReader(
				"./ihop2-data/transmodeler-matsim/exchange/events.xml"));

		String line;
		while ((i++ < Integer.MAX_VALUE) && (line = reader.readLine()) != null) {
			if (line.contains("person=\"1773064\"")) {
				System.out.println(line);
				if (line.contains("actstart")) {
					System.out.println();
				}
			}
		}
		reader.close();
		System.out.println("... DONE");
	}

}
