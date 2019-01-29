package org.matsim.contrib.emissions.analysis;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import org.apache.log4j.Logger;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.analysis.spatial.Grid;
import org.matsim.contrib.analysis.time.TimeBinMap;
import org.matsim.contrib.emissions.events.WarmEmissionEvent;
import org.matsim.contrib.emissions.types.Pollutant;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.algorithms.EventWriter;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.network.NetworkUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.*;

public class EmissionGridAnalyzerTest {

    private static Logger logger = Logger.getLogger(EmissionGridAnalyzerTest.class);
    @Rule
    public MatsimTestUtils testUtils = new MatsimTestUtils();

    private static double getRandomValue(double upperBounds) {
        return Math.random() * upperBounds;
    }

    private void writeEventsToFile(Path eventsFile, Network network, Pollutant pollutant, double pollutionPerEvent, int fromTime, int toTime) {

        EventsManager eventsManager = EventsUtils.createEventsManager();
        EventWriter writer = new EventWriterXML(eventsFile.toString());
        eventsManager.addHandler(writer);

        network.getLinks().values().forEach(link -> {
            for (int i = fromTime; i <= toTime; i++) {
                eventsManager.processEvent(createEmissionEvent(i, link, pollutant, pollutionPerEvent));
            }
        });
        writer.closeFile();
    }


    private static Network createRandomNetwork(int numberOfLinks, double maxX, double maxY) {

        Network network = NetworkUtils.createNetwork();

        for (long i = 0; i < numberOfLinks; i++) {

            Link link = createRandomLink(network.getFactory(), maxX, maxY);
            network.addNode(link.getFromNode());
            network.addNode(link.getToNode());
            network.addLink(link);
        }
        return network;
    }

    private static Link createRandomLink(NetworkFactory factory, double maxX, double maxY) {
        Node fromNode = createRandomNode(factory, maxX, maxY);
        Node toNode = createRandomNode(factory, maxX, maxY);
        return factory.createLink(Id.createLinkId(UUID.randomUUID().toString()), fromNode, toNode);
    }

    private static Node createRandomNode(NetworkFactory factory, double maxX, double maxY) {
        Coord coord = new Coord(getRandomValue(maxX), getRandomValue(maxY));
        return factory.createNode(Id.createNodeId(UUID.randomUUID().toString()), coord);
    }

    private WarmEmissionEvent createEmissionEvent(double time, Link link, Pollutant pollutant, double pollutionPerEvent) {

        Map<String, Double> emissions = new HashMap<>();
        emissions.put(pollutant.getText(), pollutionPerEvent);
        return new WarmEmissionEvent(time, link.getId(), Id.createVehicleId(UUID.randomUUID().toString()), emissions);
    }

    private Geometry createRect(double maxX, double maxY) {
        return new GeometryFactory().createPolygon(new Coordinate[]{
                new Coordinate(0, 0),
                new Coordinate(maxX, 0),
                new Coordinate(maxX, maxY),
                new Coordinate(0, maxY),
                new Coordinate(0, 0)});
    }

    @Test(expected = IllegalArgumentException.class)
    public void initialize_invalidGridSizeToSmoothingRadiusRatio_exception() {

        new EmissionGridAnalyzer.Builder()
                .withSmoothingRadius(1)
                .withGridSize(1000)
                .withNetwork(NetworkUtils.createNetwork())
                .withTimeBinSize(1)
                .build();

        fail("invalid grid size to smoothing radius ratio should cause exception");
    }

    @Test
    public void process() {

        final Pollutant pollutant = Pollutant.HC;
        final double pollutionPerEvent = 1;
        Path eventsFile = Paths.get(testUtils.getOutputDirectory()).resolve(UUID.randomUUID().toString());
        Network network = createRandomNetwork(100, 1000, 1000);
        writeEventsToFile(eventsFile, network, pollutant, pollutionPerEvent, 1, 99);

        EmissionGridAnalyzer analyzer = new EmissionGridAnalyzer.Builder()
                .withGridSize(100)
                .withTimeBinSize(10)
                .withNetwork(network)
                .withSmoothingRadius(200)
                .build();

        TimeBinMap<Grid<Map<Pollutant, Double>>> timeBins = analyzer.process(eventsFile.toString());

        assertEquals(10, timeBins.getTimeBins().size());
        timeBins.getTimeBins().forEach(bin -> {
            assertTrue(bin.hasValue());
            bin.getValue().getCells().forEach(cell -> assertTrue(cell.getValue().containsKey(pollutant)));
        });
    }

    @Test
    public void process_singleLinkWithOneEvent() {

        final Pollutant pollutant = Pollutant.CO;
        final double pollutionPerEvent = 1000;
        final int time = 1;
        final Path eventsFile = Paths.get(testUtils.getOutputDirectory()).resolve(UUID.randomUUID().toString() + "xml");
        final Network network = NetworkUtils.createNetwork();
        Node from = network.getFactory().createNode(Id.createNodeId("from"), new Coord(5, 5));
        network.addNode(from);
        Node to = network.getFactory().createNode(Id.createNodeId("to"), new Coord(6, 6));
        network.addNode(to);
        network.addLink(network.getFactory().createLink(Id.createLinkId("link"), from, to));
        writeEventsToFile(eventsFile, network, pollutant, pollutionPerEvent, time, time);

        EmissionGridAnalyzer analyzer = new EmissionGridAnalyzer.Builder()
                .withSmoothingRadius(5)
                .withNetwork(network)
                .withTimeBinSize(10)
                .withGridSize(4)
                .withBounds(createRect(12, 12))
                .build();

        TimeBinMap<Grid<Map<Pollutant, Double>>> timeBinMap = analyzer.process(eventsFile.toString());

        assertEquals(1, timeBinMap.getTimeBins().size());
        TimeBinMap.TimeBin<Grid<Map<Pollutant, Double>>> bin = timeBinMap.getTimeBin(time);
        assertTrue(bin.hasValue());

        Grid.Cell<Map<Pollutant, Double>> fromCell = bin.getValue().getCell(new Coordinate(5, 5));
        double valueOfCellWithLink = fromCell.getValue().get(pollutant);

        // deterministic value assumed to be valid due to comparison with old implementation
        assertEquals(198.41377287035186, valueOfCellWithLink, 0.0001);

        // try to make sure that values decrease with increasing distance
        bin.getValue().getCells().forEach(cell -> assertTrue(cell.getValue().get(pollutant) <= valueOfCellWithLink));
    }

    @Test
    public void process_singleLinkWithTwoEvents() {

        final Pollutant pollutant = Pollutant.CO;
        final double pollutionPerEvent = 1000;
        final int time = 1;
        final Path eventsFile = Paths.get(testUtils.getOutputDirectory()).resolve(UUID.randomUUID().toString() + "xml");
        final Network network = NetworkUtils.createNetwork();
        Node from = network.getFactory().createNode(Id.createNodeId("from"), new Coord(5, 5));
        network.addNode(from);
        Node to = network.getFactory().createNode(Id.createNodeId("to"), new Coord(6, 6));
        network.addNode(to);
        network.addLink(network.getFactory().createLink(Id.createLinkId("link"), from, to));
        writeEventsToFile(eventsFile, network, pollutant, pollutionPerEvent, time, time + 1);

        EmissionGridAnalyzer analyzer = new EmissionGridAnalyzer.Builder()
                .withSmoothingRadius(5)
                .withNetwork(network)
                .withTimeBinSize(10)
                .withGridSize(4)
                .withBounds(createRect(12, 12))
                .build();

        TimeBinMap<Grid<Map<Pollutant, Double>>> timeBinMap = analyzer.process(eventsFile.toString());

        assertEquals(1, timeBinMap.getTimeBins().size());
        TimeBinMap.TimeBin<Grid<Map<Pollutant, Double>>> bin = timeBinMap.getTimeBin(time);
        assertTrue(bin.hasValue());

        Grid.Cell<Map<Pollutant, Double>> fromCell = bin.getValue().getCell(new Coordinate(5, 5));
        double valueOfCellWithLink = fromCell.getValue().get(pollutant);

        // deterministic value assumed to be valid due to comparison with old implementation
        assertEquals(396.8275457407037, valueOfCellWithLink, 0.0001);

        // try to make sure that values decrease with increasing distance
        bin.getValue().getCells().forEach(cell -> assertTrue(cell.getValue().get(pollutant) <= valueOfCellWithLink));
    }

    @Test
    public void process_twoLinksWithOneEventEach() {

        final Pollutant pollutant = Pollutant.CO;
        final double pollutionPerEvent = 1000;
        final int time = 1;
        final Path eventsFile = Paths.get(testUtils.getOutputDirectory()).resolve(UUID.randomUUID().toString() + "xml");
        final Network network = NetworkUtils.createNetwork();
        Node from = network.getFactory().createNode(Id.createNodeId("from"), new Coord(5, 5));
        network.addNode(from);
        Node to = network.getFactory().createNode(Id.createNodeId("to"), new Coord(6, 6));
        network.addNode(to);
        network.addLink(network.getFactory().createLink(Id.createLinkId("link"), from, to));
        Node from2 = network.getFactory().createNode(Id.createNodeId("from2"), new Coord(2, 2));
        network.addNode(from2);
        Node to2 = network.getFactory().createNode(Id.createNodeId("to2"), new Coord(3, 3));
        network.addNode(to2);
        network.addLink(network.getFactory().createLink(Id.createLinkId("link2"), from2, to2));
        writeEventsToFile(eventsFile, network, pollutant, pollutionPerEvent, time, time);

        EmissionGridAnalyzer analyzer = new EmissionGridAnalyzer.Builder()
                .withSmoothingRadius(5)
                .withNetwork(network)
                .withTimeBinSize(10)
                .withGridSize(4)
                .withBounds(createRect(12, 12))
                .build();

        TimeBinMap<Grid<Map<Pollutant, Double>>> timeBinMap = analyzer.process(eventsFile.toString());

        assertEquals(1, timeBinMap.getTimeBins().size());
        TimeBinMap.TimeBin<Grid<Map<Pollutant, Double>>> bin = timeBinMap.getTimeBin(time);
        assertTrue(bin.hasValue());

        Grid.Cell<Map<Pollutant, Double>> fromCell = bin.getValue().getCell(new Coordinate(5, 5));
        double valueOfCellWithLink = fromCell.getValue().get(pollutant);

        assertEquals(275.3558600648614, valueOfCellWithLink, 0.0001);
        // try to make sure that values decrease with increasing distance. Give some padding to the value
        bin.getValue().getCells().forEach(cell -> assertTrue(cell.getValue().get(pollutant) <= valueOfCellWithLink + 0.1));
    }

    @Test
    public void process_twoLinksWithTwoEventsEach() {

        final Pollutant pollutant = Pollutant.CO;
        final double pollutionPerEvent = 1000;
        final int time = 1;
        final Path eventsFile = Paths.get(testUtils.getOutputDirectory()).resolve(UUID.randomUUID().toString() + "xml");
        final Network network = NetworkUtils.createNetwork();
        Node from = network.getFactory().createNode(Id.createNodeId("from"), new Coord(5, 5));
        network.addNode(from);
        Node to = network.getFactory().createNode(Id.createNodeId("to"), new Coord(6, 6));
        network.addNode(to);
        network.addLink(network.getFactory().createLink(Id.createLinkId("link"), from, to));
        Node from2 = network.getFactory().createNode(Id.createNodeId("from2"), new Coord(2, 2));
        network.addNode(from2);
        Node to2 = network.getFactory().createNode(Id.createNodeId("to2"), new Coord(3, 3));
        network.addNode(to2);
        network.addLink(network.getFactory().createLink(Id.createLinkId("link2"), from2, to2));
        writeEventsToFile(eventsFile, network, pollutant, pollutionPerEvent, time, time + 1);

        EmissionGridAnalyzer analyzer = new EmissionGridAnalyzer.Builder()
                .withSmoothingRadius(5)
                .withNetwork(network)
                .withTimeBinSize(10)
                .withGridSize(4)
                .withBounds(createRect(12, 12))
                .build();

        TimeBinMap<Grid<Map<Pollutant, Double>>> timeBinMap = analyzer.process(eventsFile.toString());

        assertEquals(1, timeBinMap.getTimeBins().size());
        TimeBinMap.TimeBin<Grid<Map<Pollutant, Double>>> bin = timeBinMap.getTimeBin(time);
        assertTrue(bin.hasValue());

        Grid.Cell<Map<Pollutant, Double>> fromCell = bin.getValue().getCell(new Coordinate(5, 5));
        double valueOfCellWithLink = fromCell.getValue().get(pollutant);

        assertEquals(550.7117201297228, valueOfCellWithLink, 0.0001);
        // try to make sure that values decrease with increasing distance. give some padding to the value
        bin.getValue().getCells().forEach(cell -> assertTrue(cell.getValue().get(pollutant) <= valueOfCellWithLink + 0.1));
    }

    @Test
    public void process_withBoundaries() {

        final Pollutant pollutant = Pollutant.HC;
        final double pollutionPerEvent = 1;
        Path eventsFile = Paths.get(testUtils.getOutputDirectory()).resolve(UUID.randomUUID().toString());
        Network network = createRandomNetwork(100, 1000, 1000);
        writeEventsToFile(eventsFile, network, pollutant, pollutionPerEvent, 1, 99);

        EmissionGridAnalyzer analyzer = new EmissionGridAnalyzer.Builder()
                .withBounds(createRect(100, 100))
                .withGridSize(20)
                .withTimeBinSize(100)
                .withNetwork(network)
                .withSmoothingRadius(100)
                .build();

        TimeBinMap<Grid<Map<Pollutant, Double>>> timeBinMap = analyzer.process(eventsFile.toString());

        assertEquals(1, timeBinMap.getTimeBins().size());
        TimeBinMap.TimeBin<Grid<Map<Pollutant, Double>>> bin = timeBinMap.getTimeBin(2);
        assertTrue(bin.hasValue());

        assertEquals(25, bin.getValue().getCells().size());
    }

    @Test
    public void processToJson() {

        final Pollutant pollutant = Pollutant.NO2;
        final double pollutionPerEvent = 1;
        final int time = 1;
        final Path eventsFile = Paths.get(testUtils.getOutputDirectory()).resolve(UUID.randomUUID().toString());
        final Network network = createRandomNetwork(1, 1000, 1000);
        writeEventsToFile(eventsFile, network, pollutant, pollutionPerEvent, time, time + 3);

        EmissionGridAnalyzer analyzer = new EmissionGridAnalyzer.Builder()
                .withSmoothingRadius(200)
                .withNetwork(network)
                .withTimeBinSize(1)
                .withGridSize(100)
                .build();

        String json = analyzer.processToJsonString(eventsFile.toString());

        assertNotNull(json);
    }

    @Test
    public void processToJsonFile() throws IOException {

        final Pollutant pollutant = Pollutant.NO2;
        final double pollutionPerEvent = 1;
        final int time = 1;
        final Path eventsFile = Paths.get(testUtils.getOutputDirectory()).resolve(UUID.randomUUID().toString());
        final Path jsonFile = Paths.get(testUtils.getOutputDirectory()).resolve(UUID.randomUUID().toString());
        final Network network = createRandomNetwork(1, 1000, 1000);
        writeEventsToFile(eventsFile, network, pollutant, pollutionPerEvent, time, time + 3);

        EmissionGridAnalyzer analyzer = new EmissionGridAnalyzer.Builder()
                .withSmoothingRadius(100)
                .withNetwork(network)
                .withTimeBinSize(1)
                .withGridSize(100)
                .build();

        analyzer.processToJsonFile(eventsFile.toString(), jsonFile.toString());

        byte[] jsonFileData = Files.readAllBytes(jsonFile);

        assertTrue(jsonFileData.length > 0);
    }
}
