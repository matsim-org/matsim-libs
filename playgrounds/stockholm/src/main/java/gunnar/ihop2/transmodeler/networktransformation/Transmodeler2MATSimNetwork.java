package gunnar.ihop2.transmodeler.networktransformation;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.network.algorithms.NetworkExpandNode;
import org.matsim.core.network.algorithms.NetworkExpandNode.TurnInfo;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.utils.gis.matsim2esri.network.FeatureGeneratorBuilderImpl;
import org.matsim.utils.gis.matsim2esri.network.Links2ESRIShape;
import org.matsim.utils.gis.matsim2esri.network.PolygonFeatureGenerator;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

import saleem.stockholmscenario.utils.StockholmTransformationFactory;
import floetteroed.utilities.Units;

/**
 * Turns a mesoscopic Transmodeler network (defined through a set of files in
 * csv format) into a MATSim network (in xml format). Creates for visual
 * comparison purposes also a shape file representation (in shp format) of the
 * MATSim network.
 * 
 * @author Gunnar Flötteröd
 *
 */
public class Transmodeler2MATSimNetwork {

	public static final String TMPATHID_ATTR = "TMPathID";

	// public static final String TMPATHDIR_ATTR = "TMPathDir";

	public static final String TMFROMNODEID_ATTR = "TMFromNodeID";

	// public static final String TMTONODEID_ATTR = "TMToNodeID";

	// -------------------- STATIC PACKAGE HELPERS --------------------

	static String unquote(final String original) {
		String result = original;
		if ((result.length() > 0) && "\"".equals(result.substring(0, 1))) {
			result = result.substring(1, result.length());
		}
		if ((result.length() > 0)
				&& "\"".equals(result.substring(result.length() - 1,
						result.length()))) {
			result = result.substring(0, result.length() - 1);
		}
		return result;
	}

	static enum DIR {
		AB, BA
	};

	static String newUnidirectionalId(final String bidirectionalId,
			final DIR dir) {
		return (bidirectionalId + "_" + dir);
	}

	// -------------------- MEMBERS --------------------

	private final String tmNodesFileName;

	private final String tmLinksFileName;

	private final String tmSegmentsFileName;

	private final String tmLanesFileName;

	private final String tmLaneConnectorsFileName;

	private final String matsimNetworkFileName;

	private final String linkAttributesFileName;

	private final String nodesShapeFileName1;

	private final String nodesShapeFileName2;

	private final String linksShapeFileName1;

	private final String linksShapeFileName2;

	// -------------------- CONSTRUCTION --------------------

	public Transmodeler2MATSimNetwork(final String tmNodesFileName,
			final String tmLinksFileName, final String tmSegmentsFileName,
			final String tmLanesFileName,
			final String tmLaneConnectorsFileName,
			final String matsimNetworkFileName,
			final String linkAttributesFileName,
			final String nodesShapeFileName1, final String linksShapeFileName1,
			final String nodesShapeFileName2, final String linksShapeFileName2) {
		this.tmNodesFileName = tmNodesFileName;
		this.tmLinksFileName = tmLinksFileName;
		this.tmSegmentsFileName = tmSegmentsFileName;
		this.tmLanesFileName = tmLanesFileName;
		this.tmLaneConnectorsFileName = tmLaneConnectorsFileName;
		this.matsimNetworkFileName = matsimNetworkFileName;
		this.linkAttributesFileName = linkAttributesFileName;
		this.nodesShapeFileName1 = nodesShapeFileName1;
		this.linksShapeFileName1 = linksShapeFileName1;
		this.nodesShapeFileName2 = nodesShapeFileName2;
		this.linksShapeFileName2 = linksShapeFileName2;
	}

	// -------------------- IMPLEMENTATION --------------------

	public void run() throws IOException {

		/*
		 * (1) Read all Transmodeler data.
		 */

		final TransmodelerNodesReader nodesReader = new TransmodelerNodesReader(
				this.tmNodesFileName);
		final TransmodelerLinksReader linksReader = new TransmodelerLinksReader(
				this.tmLinksFileName, nodesReader.id2node);
		final TransmodelerSegmentsReader segmentsReader = new TransmodelerSegmentsReader(
				this.tmSegmentsFileName, linksReader.id2link);
		final TransmodelerLaneReader lanesReader = new TransmodelerLaneReader(
				this.tmLanesFileName, segmentsReader.unidirSegmentId2link);
		final TransmodelerLaneConnectorReader connectorsReader = new TransmodelerLaneConnectorReader(
				this.tmLaneConnectorsFileName, lanesReader.upstrLaneId2link,
				lanesReader.downstrLaneId2link);

		System.out.println();
		System.out
				.println("------------------------------------------------------------");
		System.out.println("TRANSMODELER FILES SUMMARY");
		System.out.println("Loaded " + nodesReader.id2node.size() + " nodes.");
		System.out.println("Loaded " + linksReader.id2link.size()
				+ " links; ignored " + linksReader.getIgnoredLinksCnt()
				+ " links.");
		System.out.println("Loaded "
				+ segmentsReader.unidirSegmentId2link.size()
				+ " segments; ignored " + segmentsReader.getIgnoredSegmentCnt()
				+ " segments.");
		System.out
				.println("Loaded "
						+ (lanesReader.upstrLaneId2link.size() + lanesReader.downstrLaneId2link
								.size()) + " lanes; ignored "
						+ lanesReader.getIgnoredLaneCnt() + " lanes.");
		System.out.println("Loaded "
				+ (connectorsReader.getLoadedConnectionCnt())
				+ " lane connections; ignored "
				+ connectorsReader.getIgnoredConnectionCnt() + " connections.");
		System.out
				.println("------------------------------------------------------------");
		System.out.println();

		/*
		 * (2a) Create a MATSim network and additional object attributes.
		 */

		final Network matsimNetwork = NetworkUtils.createNetwork();
		final NetworkFactory matsimNetworkFactory = matsimNetwork.getFactory();
		final ObjectAttributes linkAttributes = new ObjectAttributes();

		/*
		 * (2b) Create and add all MATSim nodes.
		 */

		final CoordinateTransformation coordinateTransform = StockholmTransformationFactory
				.getCoordinateTransformation(
						StockholmTransformationFactory.WGS84,
						StockholmTransformationFactory.WGS84_SWEREF99);

		for (TransmodelerNode transmodelerNode : nodesReader.id2node.values()) {

			final Coord coord = coordinateTransform.transform(new Coord(
					1e-6 * transmodelerNode.getLongitude(),
					1e-6 * transmodelerNode.getLatitude()));

			final Node matsimNode = matsimNetworkFactory.createNode(
					Id.create(transmodelerNode.getId(), Node.class), coord);
			matsimNetwork.addNode(matsimNode);
		}

		/*
		 * (3) Create and add all MATSim links.
		 */

		final Set<String> unknownLinkTypes = new LinkedHashSet<String>();

		for (TransmodelerLink transmodelerLink : linksReader.id2link.values()) {

			final Node matsimFromNode = matsimNetwork.getNodes().get(
					Id.create(transmodelerLink.getFromNode().getId(),
							Node.class));
			final Node matsimToNode = matsimNetwork.getNodes()
					.get(Id.create(transmodelerLink.getToNode().getId(),
							Node.class));

			final Link matsimLink = matsimNetworkFactory.createLink(
					Id.create(transmodelerLink.getId(), Link.class),
					matsimFromNode, matsimToNode);
			final LinkTypeParameters parameters = LinkTypeParameters.TYPE2PARAMS
					.get(transmodelerLink.getType());
			if (parameters != null) {
				final SortedSet<TransmodelerSegment> segments = transmodelerLink.segments;
				double lanes = 0.0;
				double length = 0.0;
				for (TransmodelerSegment segment : segments) {
					lanes += segment.getLanes() * segment.getLength();
					length += segment.getLength();
				}
				lanes /= length;
				matsimLink.setNumberOfLanes(lanes);
				matsimLink.setLength(length * Units.M_PER_KM);
				matsimLink.setCapacity(parameters.flowCapacity_veh_hLane
						* lanes);
				matsimLink.setFreespeed(parameters.maxSpeed_km_h
						* Units.M_S_PER_KM_H);
				matsimNetwork.addLink(matsimLink);
			} else {
				unknownLinkTypes.add(transmodelerLink.getType());
			}

			linkAttributes.putAttribute(matsimLink.getId().toString(),
					TMPATHID_ATTR, transmodelerLink.getBidirectionalId());
			linkAttributes.putAttribute(matsimLink.getId().toString(),
					TMFROMNODEID_ATTR, transmodelerLink.getFromNode().getId());
			// linkAttributes.putAttribute(matsimLink.getId().toString(),
			// TMTONODEID_ATTR, transmodelerLink.getToNode().getId());
		}

		System.out.println();
		System.out
				.println("------------------------------------------------------------");
		System.out.println("RAW MATSIM NETWORK STATISTICS");
		System.out.println("Number of nodes: "
				+ matsimNetwork.getNodes().size());
		System.out.println("Number of links: "
				+ matsimNetwork.getLinks().size());
		System.out.println("Unknown (and ignored) link types: "
				+ unknownLinkTypes);
		System.out
				.println("------------------------------------------------------------");
		System.out.println();

		/*
		 * (3c) Clean up the network.
		 */

		NetworkCleaner cleaner = new NetworkCleaner();
		cleaner.run(matsimNetwork);

		System.out.println();
		System.out
				.println("------------------------------------------------------------");
		System.out.println("MATSIM NETWORK STATISTICS AFTER NETWORK CLEANING");
		System.out.println("Number of nodes: "
				+ matsimNetwork.getNodes().size());
		System.out.println("Number of links: "
				+ matsimNetwork.getLinks().size());
		System.out
				.println("------------------------------------------------------------");
		System.out.println();

		final double linkWidthCoefficient = 0.3;

		{
			final FeatureGeneratorBuilderImpl builder = new FeatureGeneratorBuilderImpl(
					matsimNetwork, StockholmTransformationFactory.WGS84);
			builder.setWidthCoefficient(linkWidthCoefficient);
			builder.setFeatureGeneratorPrototype(PolygonFeatureGenerator.class);
			final Links2ESRIShape esriWriter = new Links2ESRIShape(
					matsimNetwork, this.linksShapeFileName1, builder);
			esriWriter.write();

			final PrintWriter nodesWriter = new PrintWriter(
					this.nodesShapeFileName1);
			nodesWriter.println("x;y");
			for (Node node : matsimNetwork.getNodes().values()) {
				nodesWriter.println(node.getCoord().getX() + ";"
						+ node.getCoord().getY());
			}
			nodesWriter.flush();
			nodesWriter.close();
		}

		/*
		 * (3d) Expand networks to allow for turning moves.
		 * 
		 * TODO use meaningful parameters
		 * 
		 * TODO test!
		 */

		final Map<Id<Node>, Node> originalMATSimNodes = new LinkedHashMap<>();
		originalMATSimNodes.putAll(matsimNetwork.getNodes());
		final Map<Id<Link>, Link> originalMATSimLinks = new LinkedHashMap<>();
		originalMATSimLinks.putAll(matsimNetwork.getLinks());

		for (Map.Entry<Id<Node>, Node> matsimId2node : originalMATSimNodes
				.entrySet()) {
			if ((matsimId2node.getValue().getInLinks().size() > 1)
					|| (matsimId2node.getValue().getInLinks().size() > 1)) {
				final ArrayList<TurnInfo> turns = new ArrayList<TurnInfo>();
				double maxTurnLength = 5.0;

				for (Map.Entry<Id<Link>, ? extends Link> matsimId2inLink : matsimId2node
						.getValue().getInLinks().entrySet()) {
					TransmodelerLink tmInLink = linksReader.id2link
							.get(matsimId2inLink.getKey().toString());
					for (Map.Entry<TransmodelerLink, Double> tmOutLink2turnLength : tmInLink.downstreamLink2turnLength
							.entrySet()) {
						final Id<Link> matsimOutLinkId = Id.create(
								tmOutLink2turnLength.getKey().getId(),
								Link.class);
						if (matsimNetwork.getLinks().containsKey(
								matsimOutLinkId)) {
							turns.add(new TurnInfo(matsimId2inLink.getKey(),
									matsimOutLinkId));
							maxTurnLength = Math.max(maxTurnLength,
									tmOutLink2turnLength.getValue());
						}
					}
				}

				// for (TurnInfo turn : turns) {
				// System.out.print(turn.getFromLinkId() + " -> "
				// + turn.getToLinkId());
				// System.out.print(" ... upstrFromNode = ");
				// System.out.print(matsimNetwork.getLinks()
				// .get(turn.getFromLinkId()).getFromNode().getId());
				// System.out.print(", upstrToNode = ");
				// System.out.print(matsimNetwork.getLinks()
				// .get(turn.getFromLinkId()).getToNode().getId());
				// System.out.print(", downstrFromNode = ");
				// System.out.print(matsimNetwork.getLinks()
				// .get(turn.getToLinkId()).getFromNode().getId());
				// System.out.print(", downstrToNode = ");
				// System.out.print(matsimNetwork.getLinks()
				// .get(turn.getToLinkId()).getToNode().getId());
				// System.out.println();
				// }

				final double radius = 10.0;
				final double offset = 5.0;
				final NetworkExpandNode exp = new NetworkExpandNode(
						matsimNetwork, radius, offset);
				exp.expandNode(matsimId2node.getKey(), turns);
			}
		}

		System.out.println();
		System.out
				.println("------------------------------------------------------------");
		System.out
				.println("MATSIM NETWORK STATISTICS AFTER INTERSECTION EXPANSION");
		System.out.println("Number of nodes: "
				+ matsimNetwork.getNodes().size());
		System.out.println("Number of links: "
				+ matsimNetwork.getLinks().size());
		System.out
				.println("------------------------------------------------------------");
		System.out.println();

		/*
		 * Clean network once again.
		 */

		final boolean cleanAfterExpansion = true;

		if (cleanAfterExpansion) {

			cleaner = new NetworkCleaner();
			cleaner.run(matsimNetwork);

			System.out.println();
			System.out
					.println("------------------------------------------------------------");
			System.out
					.println("MATSIM NETWORK STATISTICS AFTER REPEATED NETWORK CLEANING");
			System.out.println("Number of nodes: "
					+ matsimNetwork.getNodes().size());
			System.out.println("Number of links: "
					+ matsimNetwork.getLinks().size());
			System.out
					.println("------------------------------------------------------------");
			System.out.println();

		}

		/*
		 * (3e) Write the network and its attributes to file.
		 */

		final NetworkWriter networkWriter = new NetworkWriter(matsimNetwork);
		networkWriter.write(this.matsimNetworkFileName);

		final ObjectAttributesXmlWriter linkAttributesWriter = new ObjectAttributesXmlWriter(
				linkAttributes);
		linkAttributesWriter.writeFile(this.linkAttributesFileName);

		/*
		 * (4) Create shape file.
		 */

		{
			final FeatureGeneratorBuilderImpl builder = new FeatureGeneratorBuilderImpl(
					matsimNetwork, TransformationFactory.WGS84);
			builder.setWidthCoefficient(linkWidthCoefficient);
			builder.setFeatureGeneratorPrototype(PolygonFeatureGenerator.class);
			final Links2ESRIShape esriWriter = new Links2ESRIShape(
					matsimNetwork, this.linksShapeFileName2, builder);
			esriWriter.write();

			final PrintWriter nodesWriter = new PrintWriter(
					this.nodesShapeFileName2);
			nodesWriter.println("x;y");
			for (Node node : matsimNetwork.getNodes().values()) {
				nodesWriter.println(node.getCoord().getX() + ";"
						+ node.getCoord().getY());
			}
			nodesWriter.flush();
			nodesWriter.close();
		}
	}

	// -------------------- MAIN-FUNCTION --------------------

	public static void main(String[] args) throws IOException {

		final String path = "./data/transmodeler/";
		final String nodesFile = path + "Nodes.csv";
		final String segmentsFile = path + "Segments.csv";
		final String lanesFile = path + "Lanes.csv";
		final String laneConnectorsFile = path + "Lane Connectors.csv";
		final String linksFile = path + "Links.csv";
		final String matsimFile = path + "network.xml";
		final String linkAttributesFile = path + "linkAttributes.xml";
		final String nodesFile1 = path + "nodes1.txt";
		final String nodesFile2 = path + "nodes2.txt";
		final String linksFile1 = path + "links1.shp";
		final String linksFile2 = path + "links2.shp";

		System.out.println("STARTED ...");

		final Transmodeler2MATSimNetwork tm2MATSim = new Transmodeler2MATSimNetwork(
				nodesFile, linksFile, segmentsFile, lanesFile,
				laneConnectorsFile, matsimFile, linkAttributesFile, nodesFile1,
				linksFile1, nodesFile2, linksFile2);
		tm2MATSim.run();

		System.out.println("... DONE");
	}
}
