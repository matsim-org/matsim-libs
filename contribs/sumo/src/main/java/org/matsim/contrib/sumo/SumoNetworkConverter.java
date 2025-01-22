package org.matsim.contrib.sumo;

import com.google.common.collect.Sets;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.osm.networkReader.LinkProperties;
import org.matsim.contrib.osm.networkReader.OsmTags;
import org.matsim.core.network.turnRestrictions.DisallowedNextLinks;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.scenario.ProjectionUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.IOUtils;
import org.xml.sax.SAXException;
import picocli.CommandLine;

import javax.xml.parsers.ParserConfigurationException;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

/**
 * Converter for sumo networks
 *
 * @author rakow
 */
public class SumoNetworkConverter implements Callable<Integer> {

	private static final Logger log = LogManager.getLogger(SumoNetworkConverter.class);

	@CommandLine.Parameters(arity = "1..*", paramLabel = "INPUT", description = "Input file(s)")
	private List<Path> input;

	@CommandLine.Option(names = "--output", description = "Output xml file", required = true)
	private Path output;

	@CommandLine.Option(names = "--from-crs", description = "Coordinate system of input data", required = true)
	private String fromCRS;

	@CommandLine.Option(names = "--to-crs", description = "Desired output coordinate system", required = true)
	private String toCRS;

	@CommandLine.Option(names = "--free-speed-factor", description = "Free-speed reduction for urban links", defaultValue = "0.9")
	private double freeSpeedFactor = LinkProperties.DEFAULT_FREESPEED_FACTOR;

	@CommandLine.Option(names = "--lane-restrictions", description = "Define how restricted lanes are handled: ${COMPLETION-CANDIDATES}", defaultValue = "IGNORE")
	private LaneRestriction laneRestriction = LaneRestriction.IGNORE;

	private SumoNetworkConverter(List<Path> input, Path output, String fromCRS, String toCRS, double freeSpeedFactor,
								 LaneRestriction laneRestriction) {
		this.input = input;
		this.output = output;
		this.fromCRS = fromCRS;
		this.toCRS = toCRS;
		this.freeSpeedFactor = freeSpeedFactor;
		this.laneRestriction = laneRestriction;
	}

	private SumoNetworkConverter() {
	}

	/**
	 * Creates a new converter instance.
	 *
	 * @param input   List of input files, if multiple they will be merged
	 * @param output  output path
	 * @param fromCRS coordinate system of input data
	 * @param toCRS   desired coordinate system of network
	 */
	public static SumoNetworkConverter newInstance(List<Path> input, Path output, String fromCRS, String toCRS) {
		return new SumoNetworkConverter(input, output, fromCRS, toCRS, LinkProperties.DEFAULT_FREESPEED_FACTOR, LaneRestriction.IGNORE);
	}


	/**
	 * Creates a new instance.
	 *
	 * @see #newInstance(List, Path, String, String, double)
	 */
	public static SumoNetworkConverter newInstance(List<Path> input, Path output, String inputCRS, String targetCRS, double freeSpeedFactor) {
		return new SumoNetworkConverter(input, output, inputCRS, targetCRS, freeSpeedFactor, LaneRestriction.IGNORE);
	}

	/**
	 * Creates a new instance.
	 *
	 * @see #newInstance(List, Path, String, String, double)
	 */
	public static SumoNetworkConverter newInstance(List<Path> input, Path output, String inputCRS, String targetCRS,
												   double freeSpeedFactor, LaneRestriction laneRestriction) {
		return new SumoNetworkConverter(input, output, inputCRS, targetCRS, freeSpeedFactor, laneRestriction);
	}

	/**
	 * Reads network from input file.
	 */
	public static SumoNetworkHandler readNetwork(File input) throws IOException, SAXException, ParserConfigurationException {
		return SumoNetworkHandler.read(input);
	}

	public static void main(String[] args) {
		System.exit(new CommandLine(new SumoNetworkConverter()).execute(args));
	}

	/**
	 * Determine if a mode is allowed on a link.
	 */
	private static boolean isModeAllowed(String mode, SumoNetworkHandler.Edge edge, SumoNetworkHandler.Type type) {

		// Check edge attributes first
		if (edge.lanes.stream().anyMatch(l -> l.allow != null && l.allow.contains(mode)))
			return true;

		if (edge.lanes.stream().allMatch(l -> l.disallow != null && l.disallow.contains(mode)))
			return false;

		// Type allows this mode
		return type.allow.contains(mode) || (type.allow.isEmpty() && !type.disallow.contains(mode));
	}

	/**
	 * Execute the converter, which includes conversion and writing the files
	 *
	 * @see #convert(Network).
	 */
	@Override
	public Integer call() throws Exception {

		Network network = NetworkUtils.createNetwork();

		SumoNetworkHandler handler = convert(network);

		if (toCRS != null)
			ProjectionUtils.putCRS(network, toCRS);

		NetworkUtils.writeNetwork(network, output.toAbsolutePath().toString());

		writeGeometry(handler, output.toAbsolutePath().toString().replace(".xml", "-linkGeometries.csv"));

		writeFeatures(handler, output.toAbsolutePath().toString().replace(".xml", "-ft.csv"));

		return 0;
	}

	/**
	 * Write csv with link properties.
	 */
	public void writeFeatures(SumoNetworkHandler handler, String output) {

		SumoNetworkFeatureExtractor props = new SumoNetworkFeatureExtractor(handler);

		try (CSVPrinter out = new CSVPrinter(IOUtils.getBufferedWriter(output), CSVFormat.DEFAULT)) {
			List<String> header = new ArrayList<>(props.getHeader());
			header.addAll(handler.attributes);

			out.printRecord(header);
			props.print(out);

		} catch (IOException e) {
			log.warn("Could not write property file.", e);
		}
	}

	/**
	 * Writes link geometries.
	 */
	public void writeGeometry(SumoNetworkHandler handler, String path) {
		try (BufferedWriter out = IOUtils.getBufferedWriter(path)) {
			CSVPrinter printer = new CSVPrinter(out, CSVFormat.DEFAULT.withHeader("LinkId", "Geometry"));

			for (Map.Entry<String, SumoNetworkHandler.Edge> e : handler.getEdges().entrySet()) {

				SumoNetworkHandler.Edge edge = e.getValue();

				// Create straight line for edges without shape
				if (edge.shape.isEmpty()) {

					SumoNetworkHandler.Junction f = handler.getJunctions().get(edge.from);
					SumoNetworkHandler.Junction t = handler.getJunctions().get(edge.to);
					if (f == null || t == null)
						continue;

					edge.shape.add(f.coord);
					edge.shape.add(t.coord);
				}

				printer.printRecord(
					e.getKey(),
					edge.shape.stream().map(d -> {
						Coord p = handler.createCoord(d);
						return String.format(Locale.US, "(%f,%f)", p.getX(), p.getY());
					}).collect(Collectors.joining(","))
				);


			}

		} catch (IOException e) {
			log.error("Could not write link geometries", e);
		}
	}

	/**
	 * Perform the actual conversion on given input data.
	 *
	 * @param network results will be added into this network.
	 * @return internal handler used for conversion
	 */
	public SumoNetworkHandler convert(Network network) throws ParserConfigurationException, SAXException, IOException {

		log.info("Parsing SUMO network");

		SumoNetworkHandler sumoHandler = SumoNetworkHandler.read(input.get(0).toFile());
		log.info("Parsed {} edges with {} junctions", sumoHandler.edges.size(), sumoHandler.junctions.size());

		for (int i = 1; i < input.size(); i++) {

			CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(fromCRS, toCRS);

			File file = input.get(i).toFile();
			SumoNetworkHandler other = SumoNetworkHandler.read(file);

			writeInductionLoops(file, other);

			log.info("Merging {} edges with {} junctions from {} into base network", other.edges.size(), other.junctions.size(), file);
			sumoHandler.merge(other, ct);
		}

		NetworkFactory f = network.getFactory();

		Map<String, LinkProperties> linkProperties = LinkProperties.createLinkProperties();

		// add additional service tag
		linkProperties.put(OsmTags.SERVICE, new LinkProperties(LinkProperties.LEVEL_PATH, 1, 15 / 3.6, 450, false));
		linkProperties.put(OsmTags.PATH, new LinkProperties(LinkProperties.LEVEL_PATH, 1, 15 / 3.6, 300, false));

		// This is for bikes
		linkProperties.put(OsmTags.CYCLEWAY, new LinkProperties(LinkProperties.LEVEL_PATH, 1, 15 / 3.6, 300, false));

		for (SumoNetworkHandler.Edge edge : sumoHandler.edges.values()) {

			// skip railways and unknowns
			if (edge.type == null || !edge.type.startsWith("highway"))
				continue;

			Link link = f.createLink(Id.createLinkId(edge.id),
				createNode(network, sumoHandler, edge.from),
				createNode(network, sumoHandler, edge.to)
			);

			if (edge.name != null)
				link.getAttributes().putAttribute("name", edge.name);

			link.getAttributes().putAttribute(NetworkUtils.TYPE, edge.type);

			Set<String> modes = Sets.newHashSet(TransportMode.car, TransportMode.ride);

			SumoNetworkHandler.Type type = sumoHandler.types.get(edge.type);

			// Determine allowed modes
			if (!isModeAllowed("passenger", edge, type)) {
				modes.remove(TransportMode.car);
				modes.remove(TransportMode.ride);
			}

			if (isModeAllowed("bicycle", edge, type))
				modes.add(TransportMode.bike);

			if (isModeAllowed("truck", edge, type))
				modes.add(TransportMode.truck);

			link.setAllowedModes(modes);
			link.setLength(edge.getLength());

			if (laneRestriction == LaneRestriction.REDUCE_CAR_LANES) {

				int size = edge.lanes.size();

				SumoNetworkHandler.Lane lane = edge.lanes.get(0);
				edge.lanes.removeIf(l -> l.allow != null && !l.allow.contains("passenger"));

				// Keep at least one lane
				if (edge.lanes.isEmpty())
					edge.lanes.add(lane);

				int restricted = size - edge.lanes.size();
				if (restricted > 0) {
					link.getAttributes().putAttribute("restricted_lanes", restricted);
				}
			}

			link.setNumberOfLanes(edge.lanes.size());

			// set link prop based on MATSim defaults
			LinkProperties prop = linkProperties.get(type.highway);
			double speed = type.speed;

			// incoming lane connected to the others
			// this is needed by matsim for lanes to work properly
			if (!edge.lanes.isEmpty()) {
				double laneSpeed = edge.lanes.get(0).speed;
				if (!Double.isNaN(laneSpeed) && laneSpeed > 0) {
					// use speed info of first lane
					// in general lanes do not have different speeds
					speed = edge.lanes.get(0).speed;
				}
			}

			link.getAttributes().putAttribute(NetworkUtils.ALLOWED_SPEED, speed);

			if (prop == null) {
				log.warn("Skipping unknown link type: {}", type.highway);
				continue;
			}

			link.setFreespeed(LinkProperties.calculateSpeedIfSpeedTag(speed, freeSpeedFactor));
			link.setCapacity(LinkProperties.getLaneCapacity(link.getLength(), prop) * link.getNumberOfLanes());

			network.addLink(link);
		}

		// clean up network
		new NetworkCleaner().run(network);

		Set<Id<Link>> ignored = new HashSet<>();

		// map of link and source link that are restricted
		Map<Id<Link>, Set<Id<Link>>> restrictions = new HashMap<>();

		for (Map.Entry<String, List<SumoNetworkHandler.Connection>> kv : sumoHandler.connections.entrySet()) {

			Link link = network.getLinks().get(Id.createLinkId(kv.getKey()));

			if (link != null) {
				Set<Id<Link>> outLinks = link.getToNode().getOutLinks().keySet();
				Set<Id<Link>> allowed = kv.getValue().stream().map(c -> Id.createLinkId(c.to)).collect(Collectors.toSet());

				// Disallowed link ids
				Sets.SetView<Id<Link>> dis = Sets.difference(outLinks, allowed);

				if (outLinks.size() == dis.size()) {
					ignored.add(link.getId());
					continue;
				}

				if (!dis.isEmpty()) {
					DisallowedNextLinks disallowed = new DisallowedNextLinks();
					for (Id<Link> id : dis) {

						Set<Id<Link>> restricted = restrictions.computeIfAbsent(id, (k) -> new HashSet<>());

						Link targetLink = network.getLinks().get(id);
						Map<Id<Link>, ? extends Link> turnLinks = targetLink.getFromNode().getOutLinks();

						// Ensure that a link is always reachable from at least one other link
						if (turnLinks.size() - 1 <= restricted.size()) {
							ignored.add(id);
							continue;
						}

						restricted.add(link.getId());
						disallowed.addDisallowedLinkSequence(TransportMode.car, List.of(id));
					}

					NetworkUtils.setDisallowedNextLinks(link, disallowed);
				}
			}
		}

		// clean again (possibly with turn restrictions)
		new NetworkCleaner().run(network);

		if (!ignored.isEmpty()) {
			log.warn("Ignored turn restrictions for {} links with no connections: {}", ignored.size(), ignored);
		}

		return sumoHandler;
	}

	private void writeInductionLoops(File file, SumoNetworkHandler other) throws IOException {
		Path loops = Path.of(file.getAbsolutePath().replace(".xml", "_loops.xml"));
		BufferedWriter writer = Files.newBufferedWriter(loops);

		log.info("Writing induction loop definition {}", loops);
		writer.write("<additional>\n");

		// ignore duplicated
		Set<String> written = new HashSet<>();

		for (SumoNetworkHandler.Edge edge : other.edges.values()) {
			for (SumoNetworkHandler.Lane lane : edge.lanes) {
				if (!written.contains(lane.id)) {
					writer.write(String.format("    <inductionLoop id=\"mLoop_%s\" lane=\"%s\" pos=\"-0.1\" freq=\"900\" file=\"counts.xml\"/>\n", lane.id, lane.id));
					written.add(lane.id);
				}

			}
		}

		writer.write("</additional>");

		writer.close();
	}

	private Node createNode(Network network, SumoNetworkHandler sumoHandler, String nodeId) {

		Id<Node> id = Id.createNodeId(nodeId);
		Node node = network.getNodes().get(id);
		if (node != null)
			return node;

		SumoNetworkHandler.Junction junction = sumoHandler.junctions.get(nodeId);
		if (junction == null)
			throw new IllegalStateException("Junction not in network:" + nodeId);

		Coord coord = sumoHandler.createCoord(junction.coord);
		node = network.getFactory().createNode(id, coord);
		node.getAttributes().putAttribute("type", junction.type);

		network.addNode(node);
		return node;
	}

	/**
	 * How restricted lanes should be handled.
	 */
	public enum LaneRestriction {
		IGNORE, REDUCE_CAR_LANES
	}

}
