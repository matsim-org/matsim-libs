package org.matsim.contrib.emissions.utils;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.emissions.Pollutant;
import org.matsim.contrib.emissions.events.EmissionEventsReader;
import org.matsim.contrib.emissions.events.WarmEmissionEvent;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class EmissionsToRasterHandlerTest {

    @Rule
    public MatsimTestUtils testUtils = new MatsimTestUtils();

    private static void writeToCsv(Path file, PalmChemistryInput chemistryInput) {

        try (var writer = Files.newBufferedWriter(file)) {
            try (var printer = new CSVPrinter(writer, CSVFormat.DEFAULT)) {

                // print header
                printer.print("x");
                printer.print("y");
                printer.print("time");
                for (Pollutant observedPollutant : chemistryInput.getObservedPollutants()) {
                    printer.print(observedPollutant);
                }
                printer.println();

                for (var bin : chemistryInput.getData().getTimeBins()) {

                    var time = bin.getStartTime();

                    for (var coordCellEntry : bin.getValue().entrySet()) {

                        var coord = coordCellEntry.getKey();
                        printer.print(coord.getX());
                        printer.print(coord.getY());
                        printer.print(time);

                        for (Pollutant observedPollutant : chemistryInput.getObservedPollutants()) {
                            var value = coordCellEntry.getValue().getEmissions().get(observedPollutant);
                            if (value == null)
                                value = 0.0;

                            printer.print(value);
                        }
                        printer.println();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void singleLinkWithSingleEmissionEvent() {

        final double emissionValue = 10;
        final double cellSize = 10;

        // create a simple network
        final var network = NetworkUtils.createNetwork();
        var from = network.getFactory().createNode(Id.createNodeId("from"), new Coord(0, 0));
        network.addNode(from);
        var to = network.getFactory().createNode(Id.createNodeId("to"), new Coord(cellSize * emissionValue, 0));
        network.addNode(to);
        network.addLink(network.getFactory().createLink(Id.createLinkId("link"), from, to));

        var rasteredNetwork = new RasteredNetwork(network, cellSize);
        var handler = new EmissionsToRasterHandler(rasteredNetwork, 1);

        var emissions = Map.of(Pollutant.NOx, emissionValue);

        handler.handleEvent(new WarmEmissionEvent(0, Id.createLinkId("link"), Id.createVehicleId("some-vehicle"), emissions));

        var result = handler.getPalmChemistryInput();
        for (PalmChemistryInput.Cell cell : result.getData().getTimeBin(0).getValue().values()) {

            // the emission value (10) should be evenly distributed over cells covered by the link
            assertEquals(emissionValue / cellSize, cell.getEmissions().get(Pollutant.NOx), Double.MIN_VALUE);
        }
    }

    @Test
    public void twoIntersectingLinksWithSingleEventForEach() {

        final double emissionValue = 10;
        final double cellSize = 10;

        // create a simple network
        final var network = NetworkUtils.createNetwork();
        var from = network.getFactory().createNode(Id.createNodeId("from"), new Coord(0, 0));
        network.addNode(from);
        var to1 = network.getFactory().createNode(Id.createNodeId("to1"), new Coord(cellSize * emissionValue, 0));
        network.addNode(to1);
        var to2 = network.getFactory().createNode(Id.createNodeId("to2"), new Coord(0, cellSize * emissionValue));
        network.addNode(to2);
        network.addLink(network.getFactory().createLink(Id.createLinkId("link1"), from, to1));
        network.addLink(network.getFactory().createLink(Id.createLinkId("link2"), from, to2));

        var rasteredNetwork = new RasteredNetwork(network, cellSize);
        var handler = new EmissionsToRasterHandler(rasteredNetwork, 1);

        var emissions = Map.of(Pollutant.NOx, emissionValue);

        handler.handleEvent(new WarmEmissionEvent(0, Id.createLinkId("link1"), Id.createVehicleId("some-vehicle"), emissions));
        handler.handleEvent(new WarmEmissionEvent(0, Id.createLinkId("link2"), Id.createVehicleId("some-vehicle"), emissions));

        var result = handler.getPalmChemistryInput();
        var origin = new Coord(-5, -5); // raster starts with link as center point of cells
        for (var cellEntry : result.getData().getTimeBin(0).getValue().entrySet()) {

            if (cellEntry.getKey().equals(origin)) {
                // the cell where both links intersect should receive the pollution of both links
                assertEquals(emissionValue * 2 / cellSize, cellEntry.getValue().getEmissions().get(Pollutant.NOx), Double.MIN_VALUE);
            } else {
                // all other cell should receive the evenly distributed pollution of their corresponding link
                assertEquals(emissionValue / cellSize, cellEntry.getValue().getEmissions().get(Pollutant.NOx), Double.MIN_VALUE);
            }
        }

        writeToCsv(Paths.get("C:/Users/Janekdererste/Desktop/debug.csv"), result);
    }

    @Test
    public void test() {

        Network network = NetworkUtils.readNetwork("./scenarios/sampleScenario/sample_network.xml");

        var rasteredNetwork = new RasteredNetwork(network, 20);

        var handler = new EmissionsToRasterHandler(rasteredNetwork, 100);
        var manager = EventsUtils.createEventsManager();
        manager.addHandler(handler);
        new EmissionEventsReader(manager).readFile(testUtils.getInputDirectory() + "/output_events.emissions.xml.gz");

        // read emissions events file
        var chemistryInput = handler.getPalmChemistryInput();

        assertNotNull(chemistryInput);

        // do something with the output
        writeToCsv(Paths.get("C:/Users/Janekdererste/Desktop/debug-2.csv"), chemistryInput);

    }
}