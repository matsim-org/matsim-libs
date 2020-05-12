package org.matsim.contrib.emissions.utils;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.emissions.Pollutant;
import org.matsim.contrib.emissions.events.EmissionEventsReader;
import org.matsim.contrib.emissions.events.WarmEmissionEvent;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.nio.file.Paths;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class EmissionsToRasterHandlerTest {

    @Rule
    public MatsimTestUtils testUtils = new MatsimTestUtils();

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
    }

    /**
     * The following test was used during development for visually analyzing the output of the rastered emissions
     */
    @Test
    @Ignore
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
        PalmChemistryInput.writeToCsv(Paths.get(testUtils.getOutputDirectory() + "rastered-emissions.csv"), chemistryInput);
    }

    /**
     * The following test was used during development for visually analyzing the output of the rastered emissions
     */
    @Test
    @Ignore
    public void testWithBerlinEmissions() {

        var bounds = new GeometryFactory().createPolygon(new Coordinate[]{
                new Coordinate(4588789.5991483666002750, 5820179.6808353941887617), // top left
                new Coordinate(4590888.1844554375857115, 5820179.6808353941887617),
                new Coordinate(4590888.1844554375857115, 5821326.1153511889278889), // bottom right
                new Coordinate(4588789.5991483666002750, 5821326.1153511889278889),
                new Coordinate(4588789.5991483666002750, 5820179.6808353941887617), // close geometry at top left
        });

        var network = NetworkUtils.readNetwork("https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.4-1pct/output-berlin-v5.4-1pct/berlin-v5.4-1pct.output_network.xml.gz");

        var rasteredNetwork = new RasteredNetwork(network, bounds, 10);

        var handler = new EmissionsToRasterHandler(rasteredNetwork, 3600);
        var manager = EventsUtils.createEventsManager();
        manager.addHandler(handler);

        new EmissionEventsReader(manager).readFile("https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.4-1pct/output-berlin-v5.4-1pct/berlin-v5.4-1pct.emission.events.offline.xml.gz");

        var result = handler.getPalmChemistryInput();
        result.writeToFile(Paths.get(testUtils.getOutputDirectory() + "ernst-reuter_chemistry.nc"));
        PalmChemistryInput.writeToCsv(Paths.get(testUtils.getOutputDirectory() + "rastered-emissions.csv"), result);
    }
}