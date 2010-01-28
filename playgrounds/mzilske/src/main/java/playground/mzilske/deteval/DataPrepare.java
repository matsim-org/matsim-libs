package playground.mzilske.deteval;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.config.Config;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NetworkWriter;
import org.matsim.visum.VisumNetwork;
import org.matsim.visum.VisumNetworkReader;

public class DataPrepare {
	
	private static final Logger log = Logger.getLogger(DataPrepare.class);

	private static String OutPath = "../detailedEval/net/";
	private static String InVisumNetFile = "../detailedEval/Analyse2005_Netz.net";

	// OUTPUT FILES
	private static String OutNetworkFile = OutPath + "network.xml";

	private final ScenarioImpl scenario;
	private final Config config;
	private VisumNetwork vNetwork;

	public DataPrepare() {
		this.scenario = new ScenarioImpl();
		this.config = this.scenario.getConfig();
	}

	private void prepareConfig() {
		this.config.network().setOutputFile(OutNetworkFile);
	}

	private void convertNetwork() {
		NetworkLayer network = scenario.getNetwork();
		for (VisumNetwork.Node visumNode : vNetwork.nodes.values()) {
			network.createAndAddNode(visumNode.id, visumNode.coord);
		}
		for (VisumNetwork.Edge visumEdge : vNetwork.edges.values()) {
			network.createAndAddLink(visumEdge.id, network.getNodes().get(visumEdge.fromNode), network.getNodes().get(visumEdge.toNode), visumEdge.length * 1000, 14, 2000, 1);
		}
	}

	private void readVisumNetwork()  {
		vNetwork = new VisumNetwork();
		log.info("reading visum network.");
		try {
			new VisumNetworkReader(vNetwork).read(InVisumNetFile);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void writeNetwork() throws IOException,
			FileNotFoundException {
		NetworkLayer network = scenario.getNetwork();
		log.info("writing network to file.");
		new NetworkWriter(network).writeFile(OutNetworkFile);
	}

	public static void main(final String[] args) {
		convertVisumNetwork();
	}

	private static void convertVisumNetwork() {
		DataPrepare app = new DataPrepare();
		app.prepareConfig();
		app.readVisumNetwork();
		app.convertNetwork();
		try {
			app.writeNetwork();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		log.info("done.");
	}

}
