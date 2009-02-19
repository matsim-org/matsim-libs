/**
 * 
 */
package playground.yu.utils.vis;

import org.matsim.gbl.Gbl;
import org.matsim.mobsim.queuesim.QueueNetwork;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.utils.vis.otfvis.executables.OTFEvent2MVI;

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
		Gbl.createConfig(null);
		Gbl.startMeasurement();

		NetworkLayer net = new NetworkLayer();
		new MatsimNetworkReader(net).readFile(args[0]);

		new OTFEvent2MVI(new QueueNetwork(net), args[1], args[2], Integer
				.parseInt(args[3])).convert();
	}

}
