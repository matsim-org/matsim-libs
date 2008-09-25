/**
 * 
 */
package playground.yu.utils.vis;

import org.matsim.gbl.Gbl;
import org.matsim.mobsim.queuesim.QueueNetwork;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.utils.vis.otfvis.executables.OTFEvent2MVI;
import org.matsim.world.World;

/**
 * @author yu
 * 
 */
public class MyOTFEvents2Mvi {

	/**
	 * quote from org.matsim.utils.vis.otfvis.executables.OTFEvent2MVI of David
	 * Strippgen
	 * 
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
		Gbl.createConfig(null);
		Gbl.startMeasurement();
		World world = Gbl.createWorld();

		NetworkLayer net = new NetworkLayer();
		new MatsimNetworkReader(net).readFile(args[0]);
		world.setNetworkLayer(net);

		new OTFEvent2MVI(new QueueNetwork(net), args[1], args[2], Integer
				.parseInt(args[3])).convert();
	}

}
