package gunnar.ihop2.transmodeler.networktransformation;

import java.io.IOException;
import java.util.LinkedHashSet;
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
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

import saleem.stockholmscenario.utils.StockholmTransformationFactory;
import floetteroed.utilities.Units;

/**
 * Turns a mesoscopic Transmodeler network (in csv format) into a MATSim network
 * (in xml format). Creates for visual comparison purposes also a shape file
 * representation (in shp format) of the MATSim network.
 * 
 * @author Gunnar Flötteröd
 *
 */
public class Transmodeler2MATSimNetwork {

	public static final String TMPATHID_ATTR = "TMPathID";
	
	public static final String TMPATHDIR_ATTR = "TMPathDir";

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

	static String newUnidirectionalLinkId(final String bidirectionalLinkId,
			final String direction) {
		return bidirectionalLinkId + "_" + direction;
	}

	// -------------------- MEMBERS --------------------

	private final String tmNodesFileName;

	private final String tmLinksFileName;

	private final String tmSegmentsFileName;

	private final String matsimNetworkFileName;

	private final String linkAttributesFileName;

	private final String shapeFileName;

	// -------------------- CONSTRUCTION --------------------

	public Transmodeler2MATSimNetwork(final String tmNodesFileName,
			final String tmLinksFileName, final String tmSegmentsFileName,
			final String matsimNetworkFileName,
			final String linkAttributesFileName, final String shapeFileName) {
		this.tmNodesFileName = tmNodesFileName;
		this.tmLinksFileName = tmLinksFileName;
		this.tmSegmentsFileName = tmSegmentsFileName;
		this.matsimNetworkFileName = matsimNetworkFileName;
		this.linkAttributesFileName = linkAttributesFileName;
		this.shapeFileName = shapeFileName;
	}

	// -------------------- IMPLEMENTATION --------------------

	public void run() throws IOException {

		/*
		 * (1) Read all Transmodeler data.
		 */

		final TransmodelerNodesReader nodesReader = new TransmodelerNodesReader(
				this.tmNodesFileName);
		final TransmodelerLinksReader linksReader = new TransmodelerLinksReader(
				this.tmLinksFileName, nodesReader.getNodes());
		final TransmodelerSegmentsReader segmentsReader = new TransmodelerSegmentsReader(
				this.tmSegmentsFileName, linksReader.getLinks());

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

		for (TransmodelerNode transmodelerNode : nodesReader.getNodes()
				.values()) {

//<<<<<<< HEAD
//=======
//			// final Coord coord = new CoordImpl(
//			// 1e-6 * transmodelerNode.getLongitude(),
//			// 1e-6 * transmodelerNode.getLatitude());
//>>>>>>> branch 'master' of https://github.com/matsim-org/matsim.git
	
			final Coord coord = coordinateTransform.transform(new Coord(1e-6 * transmodelerNode.getLongitude(), 1e-6 * transmodelerNode.getLatitude()));

			final Node matsimNode = matsimNetworkFactory.createNode(
					Id.create(transmodelerNode.getId(), Node.class), coord);
			matsimNetwork.addNode(matsimNode);
		}

		/*
		 * (3) Create and add all MATSim links.
		 */

		final Set<String> unknownLinkTypes = new LinkedHashSet<String>();

		for (TransmodelerLink transmodelerLink : linksReader.getLinks()
				.values()) {

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
				final SortedSet<TransmodelerSegment> segments = segmentsReader
						.getSegments().get(transmodelerLink);
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

			// TODO >>>>> New: memorize the underlying TM links >>>>>
			
			linkAttributes.putAttribute(matsimLink.getId().toString(),
					TMPATHID_ATTR, transmodelerLink.getPathId());
			
			// TODO <<<<< New: memorize the underlying TM links <<<<<
			
		}
		System.out.println("UNKNOWN LINK TYPES: " + unknownLinkTypes);

		/*
		 * (3c) Clean up the network and write it to file.
		 */

		final NetworkCleaner cleaner = new NetworkCleaner();
		cleaner.run(matsimNetwork);

		final NetworkWriter networkWriter = new NetworkWriter(matsimNetwork);
		networkWriter.write(this.matsimNetworkFileName);

		final ObjectAttributesXmlWriter linkAttributesWriter = new ObjectAttributesXmlWriter(
				linkAttributes);
		linkAttributesWriter.writeFile(this.linkAttributesFileName);

		/*
		 * (4) Create shape file.
		 */

		// final FeatureGeneratorBuilderImpl builder = new
		// FeatureGeneratorBuilderImpl(
		// matsimNetwork, StockholmTransformationFactory.WGS84_SWEREF99);
		// builder.setWidthCoefficient(0.01);
		// builder.setFeatureGeneratorPrototype(PolygonFeatureGenerator.class);
		// builder.setWidthCalculatorPrototype(CapacityBasedWidthCalculator.class);
		//
		// // new Nodes2ESRIShape(matsimNetwork, this.shapeFileName,
		// // TransformationFactory.WGS84).write();
		//
		// final Links2ESRIShape esriWriter = new Links2ESRIShape(matsimNetwork,
		// this.shapeFileName, builder);
		// esriWriter.write();

	}

	// -------------------- MAIN-FUNCTION --------------------

	public static void main(String[] args) throws IOException {

		final String path = "./data/transmodeler/";
		final String nodesFile = path + "Nodes.csv";
		final String segmentsFile = path + "Segments.csv";
		final String linksFile = path + "Links.csv";
		final String matsimFile = path + "network.xml";
		final String linkAttributesFile = path + "linkAttributes.xml";
		final String esriFile = path + "network.shp";

		System.out.println("STARTED ...");

		final Transmodeler2MATSimNetwork tm2MATSim = new Transmodeler2MATSimNetwork(
				nodesFile, linksFile, segmentsFile, matsimFile,
				linkAttributesFile, esriFile);
		tm2MATSim.run();

		System.out.println("... DONE");
	}
}
