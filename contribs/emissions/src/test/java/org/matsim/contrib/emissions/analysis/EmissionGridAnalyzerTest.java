package org.matsim.contrib.emissions.analysis;

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
                eventsManager.processEvent(createEmissionEvent(i, link, Pollutant.CO, pollutionPerEvent));
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
            bin.getValue().getCells().forEach(cell -> assertTrue(cell.getValue().get(pollutant) > pollutionPerEvent));
        });
    }

    @Test
    public void process_singleLinkAndSingleEvent() {

        final Pollutant pollutant = Pollutant.NO2;
        final double pollutionPerEvent = 1;
        final int time = 1;
        final Path eventsFile = Paths.get(testUtils.getOutputDirectory()).resolve(UUID.randomUUID().toString());
        final Network network = createRandomNetwork(1, 1000, 1000);
        writeEventsToFile(eventsFile, network, pollutant, pollutionPerEvent, time, time);

        EmissionGridAnalyzer analyzer = new EmissionGridAnalyzer.Builder()
                .withSmoothingRadius(1)
                .withNetwork(network)
                .withTimeBinSize(10)
                .withGridSize(100)
                .build();

        TimeBinMap<Grid<Map<Pollutant, Double>>> timeBinMap = analyzer.process(eventsFile.toString());

        assertEquals(1, timeBinMap.getTimeBins().size());

        TimeBinMap.TimeBin<Grid<Map<Pollutant, Double>>> bin = timeBinMap.getTimeBin(time);
        assertTrue(bin.hasValue());

        // we parse 1 event for 1 link with an emission value of 1. Each grid cell should have an emission of 1 if the link
        // directly crosses the cell. Or less but greater 0 if just adjacent.
        bin.getValue().getCells().forEach(cell -> assertTrue(0 < cell.getValue().get(pollutant) && cell.getValue().get(pollutant) <= 1.0));
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
                .withSmoothingRadius(1)
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
                .withSmoothingRadius(1)
                .withNetwork(network)
                .withTimeBinSize(1)
                .withGridSize(100)
                .build();

        analyzer.processToJsonFile(eventsFile.toString(), jsonFile.toString());

        byte[] jsonFileData = Files.readAllBytes(jsonFile);

        assertTrue(jsonFileData.length > 0);
    }
}
