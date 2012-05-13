/**
 * 
 */
package playground.qiuhan.sa;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.visum.VisumNetwork;
import org.matsim.visum.VisumNetworkReader;

import playground.mzilske.bvg09.StreamingVisumNetworkReader;
import playground.mzilske.bvg09.VisumNetworkRowHandler;

/**
 * @author Q. SUN
 * 
 */
public class NetVisum2MATSim {
	private Network network;
	private VisumNetwork visumNet;

	public NetVisum2MATSim() {
		this.network = ScenarioUtils.createScenario(ConfigUtils.createConfig())
				.getNetwork();
	}

	public void readVisumNets(String inputFilename) {
		visumNet = new VisumNetwork();
		System.out.println(">>>>>Visum network reading began!");
		new VisumNetworkReader(visumNet).read(inputFilename);
		System.out.println(">>>>>Visum network reading ended!");
	}

	public void convertNetwork(String inputFilename) {
		System.out.println(">>>>>Network converting began!");
		StreamingVisumNetworkReader streamingVisumNetworkReader = new StreamingVisumNetworkReader();

		// convert nodes
		VisumNetworkRowHandler nodeRowHandler = new VisumNodesRowHandler(
				(NetworkImpl) this.network);
		streamingVisumNetworkReader.addRowHandler("KNOTEN", nodeRowHandler);

		// convert links
		VisumNetworkRowHandler linkRowHandler = new VisumLinksRowHandler(
				(NetworkImpl) this.network, visumNet);
		streamingVisumNetworkReader.addRowHandler("STRECKE", linkRowHandler);

		streamingVisumNetworkReader.read(inputFilename);

		((NetworkImpl) network).setCapacityPeriod(24d * 3600d);

		System.out.println(">>>>>Network converting ended!");
	}

	public void writeMATSimNetwork(String outputFilename) {
		System.out.println(">>>>>This network has "
				+ this.network.getNodes().size() + " nodes and "
				+ this.network.getLinks().size() + " links!");
		System.out.println(">>>>>MATSim network writing began!");
		new NetworkWriter(network).write(outputFilename);
		System.out.println(">>>>>MATSim network writing ended!");
	}

	public void cleanNetwork() {
		new NetworkCleaner().run(this.network);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String outputMATSimNetworkFile = "output/matsimNetwork/testNetwork.xml";
		String inputVisumNetFile = "input/visumNet/testNet.net";

		NetVisum2MATSim n2m = new NetVisum2MATSim();

		n2m.readVisumNets(inputVisumNetFile);
		n2m.convertNetwork(inputVisumNetFile);
		// n2m.cleanNetwork();
		n2m.writeMATSimNetwork(outputMATSimNetworkFile);
	}

}
