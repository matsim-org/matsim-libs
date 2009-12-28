/**
 * 
 */
package playground.yu.utils;

import playground.yu.utils.io.SimpleReader;
import playground.yu.utils.io.SimpleWriter;

/**
 * @author yu
 * 
 */
public class EventsReducer {
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		final String eventsFilename = "../matsimTests/scoringTest/output/ITERS/it.100/100.events.txt.gz";

		SimpleReader sr = new SimpleReader(eventsFilename);
		SimpleWriter sw = new SimpleWriter(eventsFilename.replaceFirst(
				"events", "events4mvi"));

		String line = sr.readLine();
		sw.writeln(line);
		// after filehead
		double time = 0;
		while (line != null && time < 86400.0) {
			line = sr.readLine();
			if (line != null) {
				sw.writeln(line);
				time = Double.parseDouble(line.split("\t")[0]);
			}
		}
		try {
			sr.close();
			sw.close();
		} catch (Exception e) {
			System.err.println(e);
		}
	}

}
