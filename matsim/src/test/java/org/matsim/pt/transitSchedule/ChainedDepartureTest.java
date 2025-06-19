package org.matsim.pt.transitSchedule;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.ChainedDeparture;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;

/**
 * Tests for {@link ChainedDeparture} implementations and XML reader/writer.
 *
 * @author rakow
 */
public class ChainedDepartureTest {

    @RegisterExtension
    private MatsimTestUtils utils = new MatsimTestUtils();

    @Test
    void testChainedDepartureImpl() {
        // Prepare test data
        Id<TransitLine> transitLineId = Id.create("line1", TransitLine.class);
        Id<TransitRoute> transitRouteId = Id.create("route1", TransitRoute.class);
        Id<Departure> departureId = Id.create("dep1", Departure.class);

        // Create the chained departure
        ChainedDeparture chainedDeparture = new ChainedDepartureImpl(transitLineId, transitRouteId, departureId);

        // Test the getters
        assertEquals(transitLineId, chainedDeparture.getChainedTransitLineId(), "Transit line ID should match");
        assertEquals(transitRouteId, chainedDeparture.getChainedRouteId(), "Transit route ID should match");
        assertEquals(departureId, chainedDeparture.getChainedDepartureId(), "Departure ID should match");
    }

    @Test
    void testDepartureWithChainedDepartures() {
        // Create departure
        Id<Departure> departureId = Id.create("dep1", Departure.class);
        double departureTime = 8.0 * 3600; // 8:00 AM
        DepartureImpl departure = new DepartureImpl(departureId, departureTime);

        // Create chained departures
        Id<TransitLine> transitLineId1 = Id.create("line1", TransitLine.class);
        Id<TransitRoute> transitRouteId1 = Id.create("route1", TransitRoute.class);
        Id<Departure> chainedDepartureId1 = Id.create("chained1", Departure.class);

        Id<TransitLine> transitLineId2 = Id.create("line2", TransitLine.class);
        Id<TransitRoute> transitRouteId2 = Id.create("route2", TransitRoute.class);
        Id<Departure> chainedDepartureId2 = Id.create("chained2", Departure.class);

        ChainedDeparture chainedDep1 = new ChainedDepartureImpl(transitLineId1, transitRouteId1, chainedDepartureId1);
        ChainedDeparture chainedDep2 = new ChainedDepartureImpl(transitLineId2, transitRouteId2, chainedDepartureId2);

        // Add chained departures to the departure
        departure.setChainedDepartures(List.of(chainedDep1, chainedDep2));

        // Test the chained departures
        assertEquals(2, departure.getChainedDepartures().size(), "There should be two chained departures");

        ChainedDeparture retrievedChainedDep1 = departure.getChainedDepartures().get(0);
        ChainedDeparture retrievedChainedDep2 = departure.getChainedDepartures().get(1);

        // Test first chained departure
        assertEquals(transitLineId1, retrievedChainedDep1.getChainedTransitLineId(), "First chained departure transit line ID should match");
        assertEquals(transitRouteId1, retrievedChainedDep1.getChainedRouteId(), "First chained departure transit route ID should match");
        assertEquals(chainedDepartureId1, retrievedChainedDep1.getChainedDepartureId(), "First chained departure ID should match");

        // Test second chained departure
        assertEquals(transitLineId2, retrievedChainedDep2.getChainedTransitLineId(), "Second chained departure transit line ID should match");
        assertEquals(transitRouteId2, retrievedChainedDep2.getChainedRouteId(), "Second chained departure transit route ID should match");
        assertEquals(chainedDepartureId2, retrievedChainedDep2.getChainedDepartureId(), "Second chained departure ID should match");
    }

    @Test
    void testReadWriteChainedDeparturesFromXML() {
        // Read the example schedule
        String inputFile = utils.getPackageInputDirectory() + "chained_departures_schedule.xml";
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        TransitSchedule schedule = scenario.getTransitSchedule();
        new TransitScheduleReader(scenario).readFile(inputFile);

        // Verify first line (one chained departure with only toDeparture attribute)
        TransitLine line1 = schedule.getTransitLines().get(Id.create("line1", TransitLine.class));
        assertNotNull(line1, "Transit line 'line1' should exist");

        TransitRoute route1 = line1.getRoutes().get(Id.create("line1_r1", TransitRoute.class));
        assertNotNull(route1, "Transit route 'line1_r1' should exist");

        Departure departure1 = route1.getDepartures().get(Id.create("d1", Departure.class));
        assertNotNull(departure1, "Departure 'd1' should exist");

        List<ChainedDeparture> chainedDeparturesLine1 = departure1.getChainedDepartures();
        assertEquals(1, chainedDeparturesLine1.size(), "Departure d1 should have one chained departure");

        ChainedDeparture chainedDep1 = chainedDeparturesLine1.get(0);
        assertEquals(Id.create("d2", Departure.class), chainedDep1.getChainedDepartureId(), "Chained departure should point to d2");
		assertEquals(chainedDep1.getChainedTransitLineId(), line1.getId());
		assertEquals(chainedDep1.getChainedRouteId(), route1.getId());

        // Verify second line (chained departure with all attributes)
        TransitLine line2 = schedule.getTransitLines().get(Id.create("line2", TransitLine.class));
        assertNotNull(line2, "Transit line 'line2' should exist");

        TransitRoute route2 = line2.getRoutes().get(Id.create("line2_r1", TransitRoute.class));
        assertNotNull(route2, "Transit route 'line2_r1' should exist");

        Departure departure2 = route2.getDepartures().get(Id.create("d1", Departure.class));
        assertNotNull(departure2, "Departure 'd1' should exist");

        List<ChainedDeparture> chainedDeparturesLine2 = departure2.getChainedDepartures();
        assertEquals(2, chainedDeparturesLine2.size(), "Departure d1 should have two chained departures");

        // Verify the first chained departure of line2
        ChainedDeparture lineTwo_firstChainedDep = chainedDeparturesLine2.get(0);
        assertEquals(Id.create("d2", Departure.class), lineTwo_firstChainedDep.getChainedDepartureId(), "First chained departure should point to d2");
        assertEquals(Id.create("line1", TransitLine.class), lineTwo_firstChainedDep.getChainedTransitLineId(), "Transit line ID should be 'line'");
        assertEquals(Id.create("line1_r1", TransitRoute.class), lineTwo_firstChainedDep.getChainedRouteId(), "Transit route ID should be 'line1_r1'");

        // Verify the second chained departure of line2
        ChainedDeparture lineTwo_secondChainedDep = chainedDeparturesLine2.get(1);
        assertEquals(Id.create("d3", Departure.class), lineTwo_secondChainedDep.getChainedDepartureId(), "Second chained departure should point to d3");
        assertEquals(Id.create("line1", TransitLine.class), lineTwo_secondChainedDep.getChainedTransitLineId(), "Transit line ID should be 'line'");
        assertEquals(Id.create("line1_r1", TransitRoute.class), lineTwo_secondChainedDep.getChainedRouteId(), "Transit route ID should be 'line1_r1'");

        // Test writing and reading back
        String outputFile = utils.getOutputDirectory() + "output_schedule.xml";
        new TransitScheduleWriter(schedule).writeFile(outputFile);

        // Read the written file back
        Scenario readScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        TransitSchedule readSchedule = readScenario.getTransitSchedule();
        new TransitScheduleReader(readScenario).readFile(outputFile);

        // Verify the data was preserved
        TransitLine readLine1 = readSchedule.getTransitLines().get(Id.create("line1", TransitLine.class));
        assertNotNull(readLine1, "Transit line 'line1' should exist in read schedule");

        TransitRoute readRoute1 = readLine1.getRoutes().get(Id.create("line1_r1", TransitRoute.class));
        assertNotNull(readRoute1, "Transit route 'line1_r1' should exist in read schedule");

        Departure readDeparture1 = readRoute1.getDepartures().get(Id.create("d1", Departure.class));
        assertNotNull(readDeparture1, "Departure 'd1' should exist in read schedule");

        List<ChainedDeparture> readChainedDeparturesLine1 = readDeparture1.getChainedDepartures();
        assertEquals(1, readChainedDeparturesLine1.size(), "Read departure d1 should have one chained departure");

        ChainedDeparture readChainedDep1 = readChainedDeparturesLine1.get(0);
        assertEquals(Id.create("d2", Departure.class), readChainedDep1.getChainedDepartureId(), "Read chained departure should point to d2");
        assertEquals(readChainedDep1.getChainedTransitLineId(), readLine1.getId());
        assertEquals(readChainedDep1.getChainedRouteId(), readRoute1.getId());
    }

    @Test
    void testCreateScheduleWithChainedDepartures() {
        // Create a transit schedule from scratch with chained departures
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        TransitSchedule schedule = scenario.getTransitSchedule();
        TransitScheduleFactory factory = schedule.getFactory();

        // Create transit lines and routes
        TransitLine line1 = factory.createTransitLine(Id.create("line1", TransitLine.class));
        TransitLine line2 = factory.createTransitLine(Id.create("line2", TransitLine.class));

        TransitRoute route1 = factory.createTransitRoute(
                Id.create("route1", TransitRoute.class),
                null, // route
                new ArrayList<>(), // stops
                "bus"
        );

        TransitRoute route2 = factory.createTransitRoute(
                Id.create("route2", TransitRoute.class),
                null, // route
                new ArrayList<>(), // stops
                "bus"
        );

        // Create departures
        Id<Departure> depId1 = Id.create("dep1", Departure.class);
        Id<Departure> depId2 = Id.create("dep2", Departure.class);
        Id<Departure> depId3 = Id.create("dep3", Departure.class);

        DepartureImpl departure1 = new DepartureImpl(depId1, 8.0 * 3600);
        DepartureImpl departure2 = new DepartureImpl(depId2, 9.0 * 3600);
        DepartureImpl departure3 = new DepartureImpl(depId3, 10.0 * 3600);

        departure1.setVehicleId(Id.create("vehicle1", Vehicle.class));
        departure2.setVehicleId(Id.create("vehicle2", Vehicle.class));
        departure3.setVehicleId(Id.create("vehicle3", Vehicle.class));

        // Create chained departures
        ChainedDeparture chainedDep1 = new ChainedDepartureImpl(
                Id.create("line2", TransitLine.class),
                Id.create("route2", TransitRoute.class),
                depId3
        );

        departure1.setChainedDepartures(List.of(chainedDep1));

        // Add departures to routes
        route1.addDeparture(departure1);
        route1.addDeparture(departure2);
        route2.addDeparture(departure3);

        // Add routes to lines
        line1.addRoute(route1);
        line2.addRoute(route2);

        // Add lines to schedule
        schedule.addTransitLine(line1);
        schedule.addTransitLine(line2);

        // Write the schedule
        String outputFile = utils.getOutputDirectory() + "created_schedule.xml";
        new TransitScheduleWriter(schedule).writeFile(outputFile);

        // Read it back
        Scenario readScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        TransitSchedule readSchedule = readScenario.getTransitSchedule();
        new TransitScheduleReader(readScenario).readFile(outputFile);

        // Verify the read schedule
        TransitLine readLine1 = readSchedule.getTransitLines().get(Id.create("line1", TransitLine.class));
        assertNotNull(readLine1, "Transit line 'line1' should exist in read schedule");

        TransitRoute readRoute1 = readLine1.getRoutes().get(Id.create("route1", TransitRoute.class));
        assertNotNull(readRoute1, "Transit route 'route1' should exist in read schedule");

        Departure readDeparture1 = readRoute1.getDepartures().get(Id.create("dep1", Departure.class));
        assertNotNull(readDeparture1, "Departure 'dep1' should exist in read schedule");

        List<ChainedDeparture> readChainedDeps = readDeparture1.getChainedDepartures();
        assertEquals(1, readChainedDeps.size(), "Should have one chained departure");

        ChainedDeparture readChainedDep = readChainedDeps.get(0);
        assertEquals(Id.create("line2", TransitLine.class), readChainedDep.getChainedTransitLineId());
        assertEquals(Id.create("route2", TransitRoute.class), readChainedDep.getChainedRouteId());
        assertEquals(Id.create("dep3", Departure.class), readChainedDep.getChainedDepartureId());
    }
}
