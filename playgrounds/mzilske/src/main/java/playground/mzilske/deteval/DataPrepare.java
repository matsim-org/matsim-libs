package playground.mzilske.deteval;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.visum.VisumNetwork;
import org.matsim.visum.VisumNetworkReader;
import org.matsim.visum.VisumNetwork.EdgeType;

import playground.mzilske.bvg09.StreamingVisumNetworkReader;
import playground.mzilske.bvg09.VisumNetworkRowHandler;

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
		final NetworkLayer network = scenario.getNetwork();
		StreamingVisumNetworkReader streamingVisumNetworkReader = new StreamingVisumNetworkReader();
		
		VisumNetworkRowHandler nodeRowHandler = new VisumNetworkRowHandler() {
			
			@Override
			public void handleRow(Map<String, String> row) {
				Id id = new IdImpl(row.get("NR"));
				Coord coord = new CoordImpl(Double.parseDouble(row.get("XKOORD").replace(',', '.')), Double.parseDouble(row.get("YKOORD").replace(',', '.')));
				network.createAndAddNode(id, coord);
			}
			
		};
		streamingVisumNetworkReader.addRowHandler("KNOTEN", nodeRowHandler);
		
		VisumNetworkRowHandler edgeRowHandler = new VisumNetworkRowHandler() {
		
			@Override
			public void handleRow(Map<String, String> row) {	
				String nr = row.get("NR");
				IdImpl id = new IdImpl(nr);
				IdImpl fromNodeId = new IdImpl(row.get("VONKNOTNR"));
				IdImpl toNodeId = new IdImpl(row.get("NACHKNOTNR"));
				Link lastEdge = network.getLinks().get(id);
				if (lastEdge != null) {
					if (lastEdge.getFromNode().getId().equals(toNodeId) && lastEdge.getToNode().getId().equals(fromNodeId)) {
						id = new IdImpl(nr + 'R');
					} else {
						throw new RuntimeException("Duplicate edge.");
					}
				}
				double length = Double.parseDouble(row.get("LAENGE").replace(',', '.')) * 1000;
				String edgeTypeIdString = row.get("TYPNR");
				IdImpl edgeTypeId = new IdImpl(edgeTypeIdString);
				double freespeed = getFreespeedTravelTime(edgeTypeId);
				double capacity = getCapacity(edgeTypeId);
				if (isEdgeTypeRelevant(edgeTypeId)) {
					network.createAndAddLink(id, network.getNodes().get(fromNodeId), network.getNodes().get(toNodeId), length, freespeed, capacity, 1);
				}
			}
			
		};
		streamingVisumNetworkReader.addRowHandler("STRECKE", edgeRowHandler);
		streamingVisumNetworkReader.read(InVisumNetFile);
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
		new NetworkWriter(network).write(OutNetworkFile);
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
