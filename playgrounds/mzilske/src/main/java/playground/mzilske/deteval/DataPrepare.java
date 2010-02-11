package playground.mzilske.deteval;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.config.Config;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NetworkWriter;
import org.matsim.visum.VisumNetwork;
import org.matsim.visum.VisumNetworkReader;
import org.matsim.visum.VisumNetwork.EdgeType;

public class DataPrepare {
	
	private static final Logger log = Logger.getLogger(DataPrepare.class);

	private static final Collection<String> irrelevantIds = Arrays.asList("0", "1", "2", "3", "88", "89", "90", "91", "92", "93", "94", "95", "96", "97", "98", "99");

	private static String OutPath = "../detailedEval/net/";
	private static String InVisumNetFile = "../detailedEval/net/Analyse2005_Netz.net";

	// OUTPUT FILES
	private static String OutNetworkFile = OutPath + "network.xml";

	private final ScenarioImpl scenario;
	private final Config config;
	private VisumNetwork visumNetwork;

	public DataPrepare() {
		this.scenario = new ScenarioImpl();
		this.config = this.scenario.getConfig();
	}

	private void prepareConfig() {
		this.config.network().setOutputFile(OutNetworkFile);
	}

	private void convertNetwork() {
		NetworkLayer network = scenario.getNetwork();
		for (VisumNetwork.Node visumNode : visumNetwork.nodes.values()) {
			network.createAndAddNode(visumNode.id, visumNode.coord);
		}
		for (VisumNetwork.Edge visumEdge : visumNetwork.edges.values()) {
			double length = visumEdge.length * 1000;
			double freespeed = getFreespeedTravelTime(visumEdge.edgeTypeId);
			double capacity = getCapacity(visumEdge.edgeTypeId);
			if (isEdgeTypeRelevant(visumEdge.edgeTypeId)) {
				network.createAndAddLink(visumEdge.id, network.getNodes().get(visumEdge.fromNode), network.getNodes().get(visumEdge.toNode), length, freespeed, capacity, 1);
			}
		}
		network.setCapacityPeriod(24*3600);
	}

	private boolean isEdgeTypeRelevant(Id edgeTypeId) {
		String idString = edgeTypeId.toString();
		if (irrelevantIds.contains(idString)) {
			return false;
		} else {
			return true;
		}
	}

	private double getCapacity(Id edgeTypeId) {
		VisumNetwork.EdgeType edgeType = findEdgeType(edgeTypeId);
		double capacity = Double.parseDouble(edgeType.kapIV);
		return capacity;
	}

	private double getFreespeedTravelTime(Id edgeTypeId) {
		VisumNetwork.EdgeType edgeType = findEdgeType(edgeTypeId);
		double v0 = Double.parseDouble(edgeType.v0IV) / 3.6;
		return v0;
	}

	private EdgeType findEdgeType(Id edgeTypeId) {
		return visumNetwork.edgeTypes.get(edgeTypeId);
	}

	private void readVisumNetwork() {
		visumNetwork = new VisumNetwork();
		log.info("reading visum network.");
		try {
			new VisumNetworkReader(visumNetwork).read(InVisumNetFile);
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
		app.cleanNetwork();
		try {
			app.writeNetwork();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		log.info("done.");
	}

	private void cleanNetwork() {
		new org.matsim.core.network.algorithms.NetworkCleaner().run(scenario.getNetwork());
	}

}
