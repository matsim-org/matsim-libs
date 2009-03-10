/**
 * 
 */
package playground.yu.test;

import org.matsim.events.Events;
import org.matsim.events.MatsimEventsReader;

/**
 * @author yu
 *
 */
public class ReadEventsTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String eventsFilename="../runs_SVN/run709/it.100/100.events.txt.gz";
		new MatsimEventsReader(new Events()).readFile(eventsFilename);
	}

}
