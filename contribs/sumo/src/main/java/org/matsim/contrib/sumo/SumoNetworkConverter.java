package org.matsim.contrib.sumo;

import com.google.common.collect.Sets;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.*;
import org.matsim.contrib.osm.networkReader.LinkProperties;
import org.matsim.contrib.osm.networkReader.OsmTags;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.scenario.ProjectionUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.GeoFileReader;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.lanes.*;
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

import static org.matsim.lanes.LanesUtils.calculateAndSetCapacity;

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

    @CommandLine.Option(names = "--shp", description = "Optional shape file used for filtering")
    private Path shapeFile;

    @CommandLine.Option(names = "--from-crs", description = "Coordinate system of input data", required = true)
    private String fromCRS;

    @CommandLine.Option(names = "--to-crs", description = "Desired output coordinate system", required = true)
    private String toCRS;

    @CommandLine.Option(names = "--free-speed-factor", description = "Free-speed reduction for urban links", defaultValue = "0.9")
    private double freeSpeedFactor = LinkProperties.DEFAULT_FREESPEED_FACTOR;

    private SumoNetworkConverter(List<Path> input, Path output, Path shapeFile, String fromCRS, String toCRS, double freeSpeedFactor) {
        this.input = input;
        this.output = output;
        this.shapeFile = shapeFile;
        this.fromCRS = fromCRS;
        this.toCRS = toCRS;
        this.freeSpeedFactor = freeSpeedFactor;
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
        return new SumoNetworkConverter(input, output, null, fromCRS, toCRS, LinkProperties.DEFAULT_FREESPEED_FACTOR);
    }

    /**
     * Creates a new converter instance, with a shape file for filtering.
     *
     * @param shapeFile only include links in this shape file.
     * @see #newInstance(List, Path, String, String)
     */
    public static SumoNetworkConverter newInstance(List<Path> input, Path output, Path shapeFile, String fromCRS, String toCRS) {
        return new SumoNetworkConverter(input, output, shapeFile, fromCRS, toCRS, LinkProperties.DEFAULT_FREESPEED_FACTOR);
    }

    /**
     * Creates a new instance.
     * @see #newInstance(List, Path, Path, String, String, double)
     */
    public static SumoNetworkConverter newInstance(List<Path> input, Path output, Path shapeFile, String inputCRS, String targetCRS, double freeSpeedFactor) {
        return new SumoNetworkConverter(input, output, shapeFile, inputCRS, targetCRS, freeSpeedFactor);
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
     * Network area based on the cut-out.
     */
    private static Geometry calculateNetworkArea(Path shapeFile) {
        // only the first feature is used
        return ((Geometry) GeoFileReader.getAllFeatures(shapeFile.toString()).iterator().next().getDefaultGeometry());
    }

    /**
     * Execute the converter, which includes conversion and writing the files
     *
     * @see #convert(Network, Lanes) .
     */
    @Override
    public Integer call() throws Exception {

        Network network = NetworkUtils.createNetwork();
        Lanes lanes = LanesUtils.createLanesContainer();

        SumoNetworkHandler handler = convert(network, lanes);

        calculateLaneCapacities(network, lanes);

        // This needs to run without errors, otherwise network is broken
        network.getLinks().values().forEach(link -> {
            LanesToLinkAssignment l2l = lanes.getLanesToLinkAssignments().get(link.getId());
            if (l2l != null)
                LanesUtils.createLanes(link, l2l);
        });

		if (toCRS != null)
			ProjectionUtils.putCRS(network, toCRS);

		NetworkUtils.writeNetwork(network, output.toAbsolutePath().toString());

        new LanesWriter(lanes).write(output.toAbsolutePath().toString().replace(".xml", "-lanes.xml"));

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
			out.printRecord(props.getHeader());
			props.print(out);

		} catch (IOException e) {
			log.warn("Could not write property file.", e);
		}
	}

	/**
     * Calculates lane capacities, according to {@link LanesUtils}.
     */
    public void calculateLaneCapacities(Network network, Lanes lanes) {
        for (LanesToLinkAssignment l2l : lanes.getLanesToLinkAssignments().values()) {
            Link link = network.getLinks().get(l2l.getLinkId());
            for (Lane lane : l2l.getLanes().values()) {
                calculateAndSetCapacity(lane,
                        lane.getToLaneIds() == null || lane.getToLaneIds().isEmpty(), link, network);
            }
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
                            return String.format(Locale.US,"(%f,%f)", p.getX(), p.getY());
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
     * @param lanes   resulting lanes are added into this object.
     * @return internal handler used for conversion
     */
    public SumoNetworkHandler convert(Network network, Lanes lanes) throws ParserConfigurationException, SAXException, IOException {

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
        LanesFactory lf = lanes.getFactory();

        Map<String, LinkProperties> linkProperties = LinkProperties.createLinkProperties();

        // add additional service tag
        linkProperties.put(OsmTags.SERVICE, new LinkProperties(LinkProperties.LEVEL_LIVING_STREET, 1,15 / 3.6, 450, false));

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

            link.setNumberOfLanes(edge.lanes.size());
            Set<String> modes = Sets.newHashSet(TransportMode.car, TransportMode.ride);

            SumoNetworkHandler.Type type = sumoHandler.types.get(edge.type);

            if (type.allow.contains("bicycle") || (type.allow.isEmpty() && !type.disallow.contains("bicycle")))
                modes.add(TransportMode.bike);

            link.setAllowedModes(modes);
            link.setLength(edge.getLength());
            LanesToLinkAssignment l2l = lf.createLanesToLinkAssignment(link.getId());

            for (SumoNetworkHandler.Lane lane : edge.lanes) {
                Lane mLane = lf.createLane(Id.create(lane.id, Lane.class));
                mLane.setAlignment(lane.index);
                mLane.setStartsAtMeterFromLinkEnd(lane.length);
                l2l.addLane(mLane);
            }

            // set link prop based on MATSim defaults
            LinkProperties prop = linkProperties.get(type.highway);
            double speed = type.speed;

            // incoming lane connected to the others
            // this is needed by matsim for lanes to work properly
            if (edge.lanes.size() >= 1) {
                Lane inLane = lf.createLane(Id.create(link.getId() + "_in", Lane.class));
                inLane.setStartsAtMeterFromLinkEnd(link.getLength());
                inLane.setAlignment(0);
                l2l.getLanes().keySet().forEach(inLane::addToLaneId);
                l2l.addLane(inLane);

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

            lanes.addLanesToLinkAssignment(l2l);
            network.addLink(link);
        }

        if (shapeFile != null) {
            Geometry shp = calculateNetworkArea(shapeFile);

            // remove lanes outside survey area
            for (Node node : network.getNodes().values()) {
                if (!shp.contains(MGC.coord2Point(node.getCoord()))) {
                    node.getOutLinks().keySet().forEach(l -> lanes.getLanesToLinkAssignments().remove(l));
                    node.getInLinks().keySet().forEach(l -> lanes.getLanesToLinkAssignments().remove(l));
                }
            }
        }

        // clean up network
        new NetworkCleaner().run(network);

        // also clean lanes
        lanes.getLanesToLinkAssignments().keySet().removeIf(l2l -> !network.getLinks().containsKey(l2l));

        for (List<SumoNetworkHandler.Connection> connections : sumoHandler.connections.values()) {
            for (SumoNetworkHandler.Connection conn : connections) {

                Id<Link> fromLink = Id.createLinkId(conn.from);
                Id<Link> toLink = Id.createLinkId(conn.to);

                LanesToLinkAssignment l2l = lanes.getLanesToLinkAssignments().get(fromLink);

                // link was removed
                if (l2l == null)
                    continue;

                Lane lane = l2l.getLanes().values().stream().filter(l -> l.getAlignment() == conn.fromLane).findFirst().orElse(null);
                if (lane == null) {
                    log.warn("Could not find from lane in network for {}", conn);
                    continue;
                }

                lane.addToLinkId(toLink);
            }
        }

        int removed = 0;

        Iterator<LanesToLinkAssignment> it = lanes.getLanesToLinkAssignments().values().iterator();

        // lanes needs to have a target, if missing we need to chose one
        while (it.hasNext()) {
            LanesToLinkAssignment l2l = it.next();

            for (Lane lane : l2l.getLanes().values()) {
                if (lane.getToLinkIds() == null && lane.getToLaneIds() == null) {
                    // chose first reachable link from this lane
                    Collection<? extends Link> out = network.getLinks().get(l2l.getLinkId()).getToNode().getOutLinks().values();
                    out.forEach(l -> lane.addToLinkId(l.getId()));

                    log.warn("No target for lane {}, chosen {}", lane.getId(), out);
                }
            }

            Set<Id<Link>> targets = l2l.getLanes().values().stream()
                    .filter(l -> l.getToLinkIds() != null)
                    .map(Lane::getToLinkIds).flatMap(List::stream)
                    .collect(Collectors.toSet());

            // remove superfluous lanes (both pointing to same link with not alternative)
            if (targets.size() == 1 && network.getLinks().get(l2l.getLinkId()).getToNode().getOutLinks().size() <= 1) {
                it.remove();
                removed++;
            }
        }

        log.info("Removed {} superfluous lanes, total={}", removed, lanes.getLanesToLinkAssignments().size());
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

}
