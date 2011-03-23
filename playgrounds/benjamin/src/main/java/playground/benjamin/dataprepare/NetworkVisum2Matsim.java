package playground.benjamin.dataprepare;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.geotools.feature.Feature;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.visum.VisumNetwork;
import org.matsim.visum.VisumNetwork.EdgeType;
import org.matsim.visum.VisumNetworkReader;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

import playground.mzilske.bvg09.StreamingVisumNetworkReader;
import playground.mzilske.bvg09.VisumNetworkRowHandler;

public class NetworkVisum2Matsim {

	private static final Logger log = Logger.getLogger(NetworkVisum2Matsim.class);


	private static final Collection<String> usedIds = new ArrayList<String>();
	private static final Collection<String> irrelevantIds = Arrays.asList("0", "1", "2", "3", "4", "5", "6", "84", "85", "86", "87", "88", "89", "90", "91", "92", "93", "94", "95", "96", "97", "98", "99");
	private static final Collection<String> additionalIrrelevantIdsPeriphery = Arrays.asList(/*   /*"37","38","39","40","41","42","43",*/   /*"44", "45","46","47",*/   /*"48",*/   /*"49","50","51","52","53","54","55","56","57","58","59"*/   /*,"60","61","62","63","64","65","66","67","68","69","70","71","72","73"*/   /*,"74","75"*/   /*,"76","77","78","79"*/    /*,"80","81","82","83"*/);

	//some speed adaptions based on "OsmTransitMain"
	private static final Collection<String> innerCity30to40KmhIdsNeedingVmaxChange = Arrays.asList(/*"44",*/   /*"74", "75", "82", "83"*/   /*, "47", "50", "60", "61", "62" */);
	private static final Collection<String> innerCity45to60KmhIdsNeedingVmaxChange = Arrays.asList(/*"37", "38", "39", "40", "41", "42", "43",*/ /*  "45", "46", "48", "49"*/ /*"54", "55", "56", "57", "58",*/    /*"59", "72", "73", "80", "81"*/);
	private static final Collection<String> innerCity70to80KmhIdsNeedingVmaxChange = Arrays.asList();
	private static final Collection<String> innerCity100to140KmhIdsNeedingVmaxChange = Arrays.asList();

	private static String OutPath = "../../detailedEval/Net/";
	private static String InVisumNetFile = "../../detailedEval/Net/Analyse2005_Netz.net";
	private static String DetailedAreaShape = "../../detailedEval/Net/shapeFromVISUM/Landkreise_umMuenchen_Umrisse.shp";

	// OUTPUT FILES
	private static String OutNetworkFile = OutPath + "network-86-85-87-84_withLanes.xml";

	private final ScenarioImpl scenario;
	private final Config config;
	private VisumNetwork visumNetwork;

	public NetworkVisum2Matsim() {
		this.scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		this.config = this.scenario.getConfig();
	}

	private void prepareConfig() {
	}

	private void convertNetwork() {
		final NetworkImpl network = scenario.getNetwork();
		StreamingVisumNetworkReader streamingVisumNetworkReader = new StreamingVisumNetworkReader();

		final Set<Feature> featuresInShape;
		try {
			featuresInShape = new ShapeFileReader().readFileAndInitialize(DetailedAreaShape);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

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
				Node fromNode = network.getNodes().get(fromNodeId);
				Node toNode = network.getNodes().get(toNodeId);
				Link lastEdge = network.getLinks().get(id);
				if (lastEdge != null) {
					if (lastEdge.getFromNode().getId().equals(toNodeId) && lastEdge.getToNode().getId().equals(fromNodeId)) {
						id = new IdImpl(nr + 'R');
					} else {
						throw new RuntimeException("Duplicate edge.");
					}
				}
				double length = Double.parseDouble(row.get("LAENGE").replace(',', '.')) * 1000;
				double freespeed = 0.0;
				String edgeTypeIdString = row.get("TYPNR");
				IdImpl edgeTypeId = new IdImpl(edgeTypeIdString);
				double capacity = getCapacity(edgeTypeId);
				int noOfLanes = getNoOfLanes(edgeTypeId);
				// kick out all irrelevant edge types
				if (isEdgeTypeRelevant(edgeTypeId)) {
					// take all edges in detailed area
					if(isEdgeInDetailedArea(fromNode, featuresInShape)){

						if(innerCity30to40KmhIdsNeedingVmaxChange.contains(edgeTypeIdString)){
							freespeed = getFreespeedTravelTime(edgeTypeId) / 2;
						}
						if(innerCity45to60KmhIdsNeedingVmaxChange.contains(edgeTypeIdString)){
							freespeed = getFreespeedTravelTime(edgeTypeId) / 1.5;
						}
						else{
							freespeed = getFreespeedTravelTime(edgeTypeId);
						}
						network.createAndAddLink(id, fromNode, toNode, length, freespeed, capacity, noOfLanes, null, edgeTypeIdString);
						usedIds.add(edgeTypeIdString);
					}
					// kick out all edges in periphery that are irrelevant only there
					else {
						if(isEdgeTypeRelevantForPeriphery(edgeTypeId)){
							freespeed = getFreespeedTravelTime(edgeTypeId);
							network.createAndAddLink(id, fromNode, toNode, length, freespeed, capacity, noOfLanes, null, edgeTypeIdString);
							usedIds.add(edgeTypeIdString);
						}
					}

				}
			}

		};
		streamingVisumNetworkReader.addRowHandler("STRECKE", edgeRowHandler);
		streamingVisumNetworkReader.read(InVisumNetFile);
		network.setCapacityPeriod(16*3600);
	}

	private boolean isEdgeTypeRelevant(Id edgeTypeId) {
		String idString = edgeTypeId.toString();
		if (irrelevantIds.contains(idString)) {
			return false;
		} else {
			return true;
		}
	}

	private boolean isEdgeInDetailedArea(Node fromNode, Set<Feature> featuresInShape) {
		boolean isInDetailedArea = false;
		GeometryFactory factory = new GeometryFactory();
		Geometry geo = factory.createPoint(new Coordinate(fromNode.getCoord().getX(), fromNode.getCoord().getY()));
		for (Feature ft : featuresInShape) {
			if (ft.getDefaultGeometry().contains(geo)){
				isInDetailedArea = true;
				break;
			}
		}
		return isInDetailedArea;
	}

	private boolean isEdgeTypeRelevantForPeriphery(Id edgeTypeId) {
		String idString = edgeTypeId.toString();
		if (additionalIrrelevantIdsPeriphery.contains(idString)) {
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

	private int getNoOfLanes(Id edgeTypeId) {
		VisumNetwork.EdgeType edgeType = findEdgeType(edgeTypeId);
		int noOfLanes = Integer.parseInt(edgeType.noOfLanes);
		return noOfLanes;
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
		NetworkImpl network = scenario.getNetwork();
		log.info("writing network to file.");
		new NetworkWriter(network).write(OutNetworkFile);
	}

	public static void main(final String[] args) {
		convertVisumNetwork();
	}

	private static void convertVisumNetwork() {
		NetworkVisum2Matsim app = new NetworkVisum2Matsim();
		app.prepareConfig();
		app.readVisumNetwork();
		app.convertNetwork();
		app.cleanNetwork();
		app.dumpEdgeTypes();
		try {
			app.writeNetwork();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		log.info("done.");
	}

	private void dumpEdgeTypes() {
		for (String usedEdgeId : usedIds) {
			System.out.print(usedEdgeId + " ");
		}
		for (EdgeType edgeType : visumNetwork.edgeTypes.values()) {
			if (usedIds.contains(edgeType.id.toString())) {
				System.out.println(edgeType.id + "   " + edgeType.v0IV + "  " + edgeType.kapIV);
			}
		}
	}

	private void cleanNetwork() {
		new org.matsim.core.network.algorithms.NetworkCleaner().run(scenario.getNetwork());
	}

}
