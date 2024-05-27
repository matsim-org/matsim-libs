package org.matsim.contrib.emissions.analysis;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.analysis.spatial.Grid;
import org.matsim.contrib.analysis.time.TimeBinMap;
import org.matsim.contrib.emissions.Pollutant;
import org.matsim.contrib.emissions.utils.TestUtils;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.matsim.contrib.emissions.Pollutant.*;

public class EmissionGridAnalyzerTest {

    @RegisterExtension
	public MatsimTestUtils testUtils = new MatsimTestUtils();

    private Geometry createRect(double maxX, double maxY) {
        return new GeometryFactory().createPolygon(new Coordinate[]{
                new Coordinate(0, 0),
                new Coordinate(maxX, 0),
                new Coordinate(maxX, maxY),
                new Coordinate(0, maxY),
                new Coordinate(0, 0)});
    }

	@Test
	void initialize_invalidGridSizeToSmoothingRadiusRatio_exception() {
		assertThrows(IllegalArgumentException.class, () -> {

			new EmissionGridAnalyzer.Builder()
					.withSmoothingRadius(1)
					.withGridSize(1000)
					.withNetwork(NetworkUtils.createNetwork())
					.withTimeBinSize(1)
					.build();

			fail("invalid grid size to smoothing radius ratio should cause exception");
		});
	}

	@Test
	void process() {

        final Pollutant pollutant = HC;
        final double pollutionPerEvent = 1;
        Path eventsFile = Paths.get(testUtils.getOutputDirectory()).resolve(UUID.randomUUID().toString() + ".xml");
        Network network = TestUtils.createRandomNetwork(100, 1000, 1000);
        TestUtils.writeWarmEventsToFile(eventsFile, network, pollutant, pollutionPerEvent, 1, 99);

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
	void process_singleLinkWithOneEvent() {

        final Pollutant pollutant = CO;
        final double pollutionPerEvent = 1000;
        final int time = 1;
        final Path eventsFile = Paths.get(testUtils.getOutputDirectory()).resolve(UUID.randomUUID().toString() + ".xml");
        final Network network = NetworkUtils.createNetwork();
        Node from = network.getFactory().createNode(Id.createNodeId("from"), new Coord(5, 5));
        network.addNode(from);
        Node to = network.getFactory().createNode(Id.createNodeId("to"), new Coord(6, 6));
        network.addNode(to);
        network.addLink(network.getFactory().createLink(Id.createLinkId("link"), from, to));
        TestUtils.writeWarmEventsToFile(eventsFile, network, pollutant, pollutionPerEvent, time, time);

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
	void process_singleLinkWithTwoEvents() {

        final Pollutant pollutant = CO;
        final double pollutionPerEvent = 1000;
        final int time = 1;
        final Path eventsFile = Paths.get(testUtils.getOutputDirectory()).resolve(UUID.randomUUID().toString() + ".xml");
        final Network network = NetworkUtils.createNetwork();
        Node from = network.getFactory().createNode(Id.createNodeId("from"), new Coord(5, 5));
        network.addNode(from);
        Node to = network.getFactory().createNode(Id.createNodeId("to"), new Coord(6, 6));
        network.addNode(to);
        network.addLink(network.getFactory().createLink(Id.createLinkId("link"), from, to));
        TestUtils.writeWarmEventsToFile(eventsFile, network, pollutant, pollutionPerEvent, time, time + 1);

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
	void process_twoLinksWithOneEventEach() {

        final Pollutant pollutant = CO;
        final double pollutionPerEvent = 1000;
        final int time = 1;
        final Path eventsFile = Paths.get(testUtils.getOutputDirectory()).resolve(UUID.randomUUID().toString() + ".xml");
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
        TestUtils.writeWarmEventsToFile(eventsFile, network, pollutant, pollutionPerEvent, time, time);

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
	void process_twoLinksWithTwoEventsEach() {

        final Pollutant pollutant = NOx;
        final double pollutionPerEvent = 1000;
        final int time = 1;
        final Path eventsFile = Paths.get(testUtils.getOutputDirectory()).resolve(UUID.randomUUID().toString() + ".xml");
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
        TestUtils.writeWarmEventsToFile(eventsFile, network, pollutant, pollutionPerEvent, time, time + 1);

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
	void process_withBoundaries() {

        final double pollutionPerEvent = 1;
        Path eventsFile = Paths.get(testUtils.getOutputDirectory()).resolve(UUID.randomUUID().toString() + ".xml");
        Network network = TestUtils.createRandomNetwork(100, 1000, 1000);
        TestUtils.writeWarmEventsToFile(eventsFile, network, NOx, pollutionPerEvent, 1, 99);

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
	void processToJson() {

        final double pollutionPerEvent = 1;
        final int time = 1;
        final Path eventsFile = Paths.get(testUtils.getOutputDirectory()).resolve(UUID.randomUUID().toString() + ".xml");
        final Network network = TestUtils.createRandomNetwork(1, 1000, 1000);
        TestUtils.writeWarmEventsToFile(eventsFile, network, NO2, pollutionPerEvent, time, time + 3);

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
	void processToJsonFile() throws IOException {

        final double pollutionPerEvent = 1;
        final int time = 1;
        final Path eventsFile = Paths.get(testUtils.getOutputDirectory()).resolve(UUID.randomUUID().toString() + ".xml");
        final Path jsonFile = Paths.get(testUtils.getOutputDirectory()).resolve(UUID.randomUUID().toString() + ".json");
        final Network network = TestUtils.createRandomNetwork(1, 1000, 1000);
        TestUtils.writeWarmEventsToFile(eventsFile, network, NO2, pollutionPerEvent, time, time + 3);

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

	@Test
	void process_regression() throws IOException {

        var scenarioUrl = ExamplesUtils.getTestScenarioURL( "emissions-sampleScenario" );
        var configUrl = IOUtils.extendUrl( scenarioUrl, "config_empty.xml" );
        var config = ConfigUtils.loadConfig( configUrl.toString() );
        config.network().setInputFile( "sample_network.xml" );
        var network = ScenarioUtils.loadScenario( config ).getNetwork(); // this is a bit overkill, but it is an easy way to get the directory context into the loading.
//        var network = NetworkUtils.readNetwork(config.get + "/sample_network.xml");
        var analyzer = new EmissionGridAnalyzer.Builder()
                .withGridSize(10)
                .withTimeBinSize(1000000) // aiming for single time bin
                .withNetwork(network)
                .withSmoothingRadius(50)
                .withGridType(EmissionGridAnalyzer.GridType.Square)
                .build();

        var map = analyzer.process(testUtils.getInputDirectory() + "output_events.emissions.xml.gz");
        writeGridToCSV(map, testUtils.getOutputDirectory() + "actual-pollution.csv");

        // have this complicated comparison here, since due to rounding errors some values are not exactly the same, thus
        // simply comparing the checksum of the file or the read in String is not sufficient
        assertCsvValuesAreSame(Paths.get(testUtils.getInputDirectory() + "expected-pollution.csv"), Paths.get(testUtils.getOutputDirectory() + "actual-pollution.csv"));
    }

    private void writeGridToCSV(TimeBinMap<Grid<Map<Pollutant, Double>>> bins, String outputPath) {

        var pollutants = Pollutant.values();

        try (CSVPrinter printer = new CSVPrinter(new FileWriter(outputPath), CSVFormat.TDF)) {

            //print header with all possible pollutants
            printer.print("timeBinStartTime");
            printer.print("x");
            printer.print("y");

            for (var p : pollutants) {
                printer.print(p.toString());
            }
            printer.println();

            //print values if pollutant was not present just print 0 instead
            for (TimeBinMap.TimeBin<Grid<Map<Pollutant, Double>>> bin : bins.getTimeBins()) {
                final double timeBinStartTime = bin.getStartTime();
                for (Grid.Cell<Map<Pollutant, Double>> cell : bin.getValue().getCells()) {

                    printer.print(timeBinStartTime);
                    printer.print(cell.getCoordinate().x);
                    printer.print(cell.getCoordinate().y);

                    for (var p : pollutants) {
                        if (cell.getValue().containsKey(p)) {
                            printer.print(cell.getValue().get(p));
                        } else {
                            printer.print(0);
                        }
                    }
                    printer.println();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void assertCsvValuesAreSame(Path expected, Path acutal) throws IOException {

        try (FileReader expectedReader = new FileReader(expected.toString()); var actualReader = new FileReader(acutal.toString())) {

            var actualIterator = CSVFormat.TDF.withFirstRecordAsHeader().parse(actualReader).iterator();

            for (CSVRecord expectedRecord : CSVFormat.TDF.withFirstRecordAsHeader().parse(expectedReader)) {
                var actualRecord = actualIterator.next();
                for (var i = 0; i < expectedRecord.size(); i++) {

                    var expectedNumber = Double.parseDouble(expectedRecord.get(i));
                    var actualNumber = Double.parseDouble(actualRecord.get(i));

                    assertEquals(expectedNumber, actualNumber, 0.000001);
                }
            }
        }
    }
}
