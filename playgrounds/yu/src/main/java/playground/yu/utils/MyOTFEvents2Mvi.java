/**
 * 
 */
package playground.yu.utils;

import org.matsim.run.OTFVis;

import playground.yu.utils.io.SimpleReader;
import playground.yu.utils.io.SimpleWriter;

/**
 *quote from org.matsim.utils.vis.otfvis.executables.OTFEvent2MVI of David
 * Strippgen
 * 
 * @author yu
 * 
 */
public class MyOTFEvents2Mvi {
	private static void printUsage() {
		System.out.println();
		System.out.println("MyOTFEvents2Mvi:");
		System.out.println("----------------");
		System.out
				.println("Create a Converter from eventsfile to .mvi-file with 4 parameters");
		System.out.println();
		System.out.println("usage: MyOTFEvents2Mvi args");
		System.out.println(" args[0]: netFilename incl. path (.xml)(required)");
		System.out
				.println(" arg[1]: eventsFilename incl. path (.txt[.gz])(required)");
		System.out
				.println(" arg[2]: .mvi file incl. path (.mvi) to output (required)");
		System.out.println(" arg[3]: time-interval[s](required)");
		System.out.println("----------------");
	}

	/**
	 * @param args
	 *            [0] - netFilename;
	 * @param args
	 *            [1] - eventsFilename;
	 * @param args
	 *            [2] - .mvi-Filename;
	 * @param args
	 *            [3] - time-interval_s;
	 */
	public static void main(String[] args) {
		if (args.length < 3) {
			printUsage();
			System.exit(0);
		}
		// -----------WRITES A SHORT EVENTSFILE-----------------
		SimpleReader sr = new SimpleReader(args[1]);

		String eventsOutputFilename = args[1].replaceAll("events",
				"events_short");
		SimpleWriter sw2 = new SimpleWriter(eventsOutputFilename);

		String line = sr.readLine();
		sw2.writeln(line);
		// after filehead
		double time = 0, lastTime = 0;
		while (line != null && time < 108000.0) {
			line = sr.readLine();
			if (line != null) {
				sw2.writeln(line);
				time = Double.parseDouble(line.split("\t")[0]);
				sw2.flush();
				if (time % 3600 == 0 && time != lastTime) {
					System.out.println("write new short Events, time :\t"
							+ time);
					lastTime = time;
				}
			}
		}
		sr.close();
		sw2.close();
		// ----------------------------------------------------
		OTFVis
				.main(new String[] { "-convert", eventsOutputFilename,
						args[0]/* networkFilename */, args[2]/* mviFilename */,
						"300"/* snapshotPeriod */});

		System.out.println("done.");
	}
}
