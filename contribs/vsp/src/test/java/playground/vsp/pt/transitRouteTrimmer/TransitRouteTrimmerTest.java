/* *********************************************************************** *
 * project: org.matsim.*
 * EditRoutesTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2021 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.vsp.pt.transitRouteTrimmer;

import javafx.util.Pair;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.pt.transitSchedule.api.*;
import org.matsim.utils.gis.shp2matsim.ShpGeometryUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TODO: reduce size of input files: either cut files to only include relevant TransitLines or use different scenario
 * Maybe the latter would be better such that we can include those files on this repository / write them as Fixture in Java.
 *
 * @author jakobrehmann
 */
@Disabled // is ignored :-(
public class TransitRouteTrimmerTest {

    final String inZoneShpFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/projects/avoev/shp-files/shp-berlin-hundekopf-areas/berlin_hundekopf.shp";
    final String inScheduleFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-v5.5-transit-schedule.xml.gz";
    final String inVehiclesFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-v5.5-transit-vehicles.xml.gz";
    final String inNetworkFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-v5.5-network.xml.gz";

    private static Scenario inputScenario;

    /**
     * This test class examines three different types of routes:
     * allIn: all stops of transitRoute are within zone
     * halfIn: one end of route is within the zone, one outside of the zone
     * middleIn: both ends of route are outside of zone, but middle in within zone
     * <p>
     * The following enum stores the transitRoute and transitLine Ids for each type
     */
    public enum routeType {
        allIn(Id.create("265---17372_700", TransitLine.class),
                Id.create("265---17372_700_0", TransitRoute.class)),
        halfIn(Id.create("184---17340_700", TransitLine.class),
                Id.create("184---17340_700_15", TransitRoute.class)),
        middleIn(Id.create("161---17326_700", TransitLine.class),
                Id.create("161---17326_700_22", TransitRoute.class));

        public final Id<TransitLine> transitLineId;
        public final Id<TransitRoute> transitRouteId;

        routeType(Id<TransitLine> transitLineId, Id<TransitRoute> transitRouteId) {
            this.transitLineId = transitLineId;
            this.transitRouteId = transitRouteId;
        }
    }


	/*
    Part 1: these tests check whether the three route types are actually configured as described above
     */

	/**
	* In the allIn scenario, the transitRoute in question should have all stops within zone.
	*/
	@Disabled
	@Test
	void test_AllIn() {

        Scenario scenario = provideCopyOfScenario(this.inScheduleFile, this.inNetworkFile, this.inVehiclesFile);
        Set<Id<TransitStopFacility>> stopsInZone = getStopsInZone(scenario.getTransitSchedule(), inZoneShpFile);

        Id<TransitLine> transitLineId = routeType.allIn.transitLineId;
        Id<TransitRoute> transitRouteId = routeType.allIn.transitRouteId;

        TransitRoute transitRoute = scenario.getTransitSchedule().getTransitLines().get(transitLineId).getRoutes().get(transitRouteId);
        int stopsTotal = transitRoute.getStops().size();
        int stopsInZoneCnt = 0;
        int stopsOutsideZoneCnt = 0;
        for (TransitRouteStop stop : transitRoute.getStops()) {
            if (stopsInZone.contains(stop.getStopFacility().getId())) {
                stopsInZoneCnt++;
            } else {
                stopsOutsideZoneCnt++;
            }
        }

        assertEquals(0, stopsOutsideZoneCnt, "There should be no stops outside of zone");
        assertEquals(stopsTotal, stopsInZoneCnt, "All stops should be inside the zone");
    }

	/**
	* In the halfIn scenario, the transitRoute in question begins outside of the zone and
	* ends within the zone.
	*/
	@Disabled
	@Test
	void test_HalfIn() {

        Scenario scenario = provideCopyOfScenario(this.inScheduleFile, this.inNetworkFile, this.inVehiclesFile);
        Set<Id<TransitStopFacility>> stopsInZone = getStopsInZone(scenario.getTransitSchedule(), inZoneShpFile);

        Id<TransitLine> transitLineId = routeType.halfIn.transitLineId;
        Id<TransitRoute> transitRouteId = routeType.halfIn.transitRouteId;
        TransitRoute transitRoute = scenario.getTransitSchedule().getTransitLines().get(transitLineId).getRoutes().get(transitRouteId);
        int sizeOld = transitRoute.getStops().size();
        int inCnt = countStopsInZone(transitRoute, stopsInZone);
        int outCnt = countStopsOutsideZone(transitRoute, stopsInZone);

        Id<TransitStopFacility> firstStopId = transitRoute.getStops().get(0).getStopFacility().getId();
        Id<TransitStopFacility> lastStopId = transitRoute.getStops().get(sizeOld - 1).getStopFacility().getId();

        assertNotEquals(sizeOld, inCnt, "The Route should not be entirely inside of the zone");
        assertNotEquals(sizeOld, outCnt, "The Route should not be entirely outside of the zone");
        assertFalse(stopsInZone.contains(firstStopId));
        assertTrue(stopsInZone.contains(lastStopId));

    }

	/**
	* In the MiddleIn scenario, the transitRoute in question begins outside of the zone then dips
	* into the zone, and finally leaves the zone once again
	*/
	@Disabled
	@Test
	void test_MiddleIn() {

        Scenario scenario = provideCopyOfScenario(this.inScheduleFile, this.inNetworkFile, this.inVehiclesFile);
        Set<Id<TransitStopFacility>> stopsInZone = getStopsInZone(scenario.getTransitSchedule(), inZoneShpFile);

        Id<TransitLine> transitLineId = routeType.middleIn.transitLineId;
        Id<TransitRoute> transitRouteId = routeType.middleIn.transitRouteId;
        TransitRoute transitRoute = scenario.getTransitSchedule().getTransitLines().get(transitLineId).getRoutes().get(transitRouteId);
        int sizeOld = transitRoute.getStops().size();
        int inCnt = countStopsInZone(transitRoute, stopsInZone);
        int outCnt = countStopsOutsideZone(transitRoute, stopsInZone);

        Id<TransitStopFacility> firstStopId = transitRoute.getStops().get(0).getStopFacility().getId();
        Id<TransitStopFacility> lastStopId = transitRoute.getStops().get(sizeOld - 1).getStopFacility().getId();

        assertNotEquals(sizeOld, inCnt, "The Route should not be entirely inside of the zone");
        assertNotEquals(sizeOld, outCnt, "The Route should not be entirely outside of the zone");
        assertFalse(stopsInZone.contains(firstStopId));
        assertFalse(stopsInZone.contains(lastStopId));

    }

	/*
    Part 2: These tests check functionality of all four trimming methods. For each trimming
    method, all three route types are checked.
     */

	/**
	* trimming method: DeleteRoutesEntirelyInsideZone.
	* route scenario: AllIn
	* The testRoute should be deleted since all stops are within the zone.
	*/
	@Disabled
	@Test
	void testDeleteRoutesEntirelyInsideZone_AllIn() {

        Scenario scenario = provideCopyOfScenario(this.inScheduleFile, this.inNetworkFile, this.inVehiclesFile);
        Set<Id<TransitStopFacility>> stopsInZone = getStopsInZone(scenario.getTransitSchedule(), inZoneShpFile);

        Id<TransitLine> transitLineId = routeType.allIn.transitLineId;

        // Modification
        Set<Id<TransitLine>> linesToModify = Collections.singleton(transitLineId);

        Pair<TransitSchedule, Vehicles> results = TransitRouteTrimmer.deleteRoutesEntirelyInsideZone(scenario.getTransitSchedule(), scenario.getTransitVehicles(),
                stopsInZone, linesToModify, null, false);

        // After Trim
        TransitSchedule transitScheduleNew = results.getKey();
        assertTrue(transitScheduleNew.getTransitLines().containsKey(transitLineId),
                "Schedule should include empty transit line");
        assertEquals(transitScheduleNew.getTransitLines().get(transitLineId).getRoutes().size(), 0, "transitLine should no longer contain any routes");

        // Test vehicles
        Set<Id<Vehicle>> vehiclesUsedInTransitSchedule = getVehiclesUsedInTransitSchedule(transitScheduleNew);
        Set<Id<Vehicle>> vehiclesInVehiclesNew = results.getValue().getVehicles().keySet();
        Assertions.assertTrue(vehiclesInVehiclesNew.containsAll(vehiclesUsedInTransitSchedule),
                "TransitVehicles should contain all vehicles used in new TransitSchedule");

    }

	/**
	* trimming method: DeleteRoutesEntirelyInsideZone.
	* route scenario: HalfIn
	* The testRoute should be retained and left unmodified,
	* since some stops are outside the zone.
	*/
	@Disabled
	@Test
	void testDeleteRoutesEntirelyInsideZone_HalfIn() {

        Scenario scenario = provideCopyOfScenario(this.inScheduleFile, this.inNetworkFile, this.inVehiclesFile);
        Set<Id<TransitStopFacility>> stopsInZone = getStopsInZone(scenario.getTransitSchedule(), inZoneShpFile);

        Id<TransitLine> transitLineId = routeType.halfIn.transitLineId;
        Id<TransitRoute> transitRouteId = routeType.halfIn.transitRouteId;

        // Before trim
        TransitRoute transitRouteOld = scenario.getTransitSchedule().getTransitLines().get(transitLineId).getRoutes().get(transitRouteId);
        int stopCntOld = transitRouteOld.getStops().size();

        // Modification
        Set<Id<TransitLine>> linesToModify = Collections.singleton(transitLineId);
        Pair<TransitSchedule, Vehicles> results = TransitRouteTrimmer.deleteRoutesEntirelyInsideZone(scenario.getTransitSchedule(), scenario.getTransitVehicles(),
                stopsInZone, linesToModify, null, false);

        // After trim
        TransitSchedule transitScheduleNew = results.getKey();

        assertTrue(transitScheduleNew.getTransitLines().containsKey(transitLineId),
                "Schedule should include transit line");

        TransitLine transitLine = transitScheduleNew.getTransitLines().get(transitLineId);
        assertTrue(transitLine.getRoutes().containsKey(transitRouteId),
                "Schedule should include transit route");

        TransitRoute transitRoute = transitLine.getRoutes().get(transitRouteId);
        int stopCntNew = transitRoute.getStops().size();
        assertEquals(stopCntOld, stopCntNew, "transitRoute should contain same number of stops as before modification");

        // Test vehicles
        Set<Id<Vehicle>> vehiclesUsedInTransitSchedule = getVehiclesUsedInTransitSchedule(transitScheduleNew);
        Set<Id<Vehicle>> vehiclesInVehiclesNew = results.getValue().getVehicles().keySet();
        Assertions.assertTrue(vehiclesInVehiclesNew.containsAll(vehiclesUsedInTransitSchedule),
                "TransitVehicles should contain all vehicles used in new TransitSchedule");

    }

	/**
	* trimming method: DeleteRoutesEntirelyInsideZone.
	* route scenario: MiddleIn
	* The testRoute should be retained and left unmodified,
	* since some stops are outside the zone.
	*/
	@Disabled
	@Test
	void testDeleteRoutesEntirelyInsideZone_MiddleIn() {
        Scenario scenario = provideCopyOfScenario(this.inScheduleFile, this.inNetworkFile, this.inVehiclesFile);
        Set<Id<TransitStopFacility>> stopsInZone = getStopsInZone(scenario.getTransitSchedule(), inZoneShpFile);

        Id<TransitLine> transitLineId = routeType.middleIn.transitLineId;
        Id<TransitRoute> transitRouteId = routeType.middleIn.transitRouteId;


        // Before trim
        TransitRoute transitRouteOld = scenario.getTransitSchedule().getTransitLines().get(transitLineId).getRoutes().get(transitRouteId);
        int stopCntOld = transitRouteOld.getStops().size();

        // Modification
        Set<Id<TransitLine>> linesToModify = Collections.singleton(transitLineId);
        Pair<TransitSchedule, Vehicles> results = TransitRouteTrimmer.deleteRoutesEntirelyInsideZone(scenario.getTransitSchedule(), scenario.getTransitVehicles(),
                stopsInZone, linesToModify, null, false);

        // After trim
        TransitSchedule transitScheduleNew = results.getKey();

        assertTrue(transitScheduleNew.getTransitLines().containsKey(transitLineId),
                "Schedule should include transit line");

        TransitLine transitLine = transitScheduleNew.getTransitLines().get(transitLineId);
        assertTrue(transitLine.getRoutes().containsKey(transitRouteId),
                "Schedule should include transit route");

        TransitRoute transitRoute = transitLine.getRoutes().get(transitRouteId);
        int stopCntNew = transitRoute.getStops().size();
        assertEquals(stopCntOld, stopCntNew, "transitRoute should contain same number of stops as before modification");

        // Test vehicles
        Set<Id<Vehicle>> vehiclesUsedInTransitSchedule = getVehiclesUsedInTransitSchedule(transitScheduleNew);
        Set<Id<Vehicle>> vehiclesInVehiclesNew = results.getValue().getVehicles().keySet();
        Assertions.assertTrue(vehiclesInVehiclesNew.containsAll(vehiclesUsedInTransitSchedule),
                "TransitVehicles should contain all vehicles used in new TransitSchedule");

    }

	/**
	* trimming method: TrimEnds.
	* route scenario: AllIn
	* The testRoute should be deleted since all stops are within the zone.
	*/
	@Disabled
	@Test
	void testTrimEnds_AllIn() {

        Scenario scenario = provideCopyOfScenario(this.inScheduleFile, this.inNetworkFile, this.inVehiclesFile);
        Set<Id<TransitStopFacility>> stopsInZone = getStopsInZone(scenario.getTransitSchedule(), inZoneShpFile);

        Id<TransitLine> transitLineId = routeType.allIn.transitLineId;

        // Modification
        Set<Id<TransitLine>> linesToModify = Collections.singleton(transitLineId);
        Pair<TransitSchedule, Vehicles> results = TransitRouteTrimmer.trimEnds(scenario.getTransitSchedule(), scenario.getTransitVehicles(),
                stopsInZone, linesToModify, false, null, 3, true);

        // After trim
        TransitSchedule transitScheduleNew = results.getKey();

        assertTrue(transitScheduleNew.getTransitLines().containsKey(transitLineId),
                "schedule should include empty transit line");
        assertEquals(transitScheduleNew.getTransitLines().get(transitLineId).getRoutes().size(), 0, "transitLine should no longer contain any routes");

        // Test vehicles
        Set<Id<Vehicle>> vehiclesUsedInTransitSchedule = getVehiclesUsedInTransitSchedule(transitScheduleNew);
        Set<Id<Vehicle>> vehiclesInVehiclesNew = results.getValue().getVehicles().keySet();
        Assertions.assertTrue(vehiclesInVehiclesNew.containsAll(vehiclesUsedInTransitSchedule),
                "TransitVehicles should contain all vehicles used in new TransitSchedule");

    }

	/**
	* trimming method: TrimEnds.
	* route scenario: HalfIn
	* The second half of the route is outside the zone and should be trimmed
	*/
	@Disabled
	@Test
	void testTrimEnds_HalfIn() {

        Scenario scenario = provideCopyOfScenario(this.inScheduleFile, this.inNetworkFile, this.inVehiclesFile);
        Set<Id<TransitStopFacility>> stopsInZone = getStopsInZone(scenario.getTransitSchedule(), inZoneShpFile);

        Id<TransitLine> transitLineId = routeType.halfIn.transitLineId;
        Id<TransitRoute> transitRouteId = routeType.halfIn.transitRouteId;

        // Before trim
        TransitRoute transitRouteOld = scenario.getTransitSchedule().getTransitLines().get(Id.create("184---17340_700", TransitLine.class)).getRoutes().get(Id.create("184---17340_700_15", TransitRoute.class));
        int sizeOld = transitRouteOld.getStops().size();
        int outCntOld = countStopsOutsideZone(transitRouteOld, stopsInZone);
        Id<TransitStopFacility> firstStopOld = transitRouteOld.getStops().get(0).getStopFacility().getId();
        Id<TransitStopFacility> lastStopOld = transitRouteOld.getStops().get(sizeOld - 1).getStopFacility().getId();

        // Modification
        Set<Id<TransitLine>> linesToModify = Collections.singleton(transitLineId);

        Pair<TransitSchedule, Vehicles> results = TransitRouteTrimmer.trimEnds(scenario.getTransitSchedule(), scenario.getTransitVehicles(),
                stopsInZone, linesToModify, false, null, 3, false);


        // After trim
        TransitSchedule transitScheduleNew = results.getKey();
        TransitRoute transitRouteNew = transitScheduleNew.getTransitLines().get(transitLineId).getRoutes().get(transitRouteId);

        int sizeNew = transitRouteNew.getStops().size();
        int inCntNew = countStopsInZone(transitRouteNew, stopsInZone);
        int outCntNew = countStopsOutsideZone(transitRouteNew, stopsInZone);
        Id<TransitStopFacility> firstStopNew = transitRouteNew.getStops().get(0).getStopFacility().getId();
        Id<TransitStopFacility> lastStopNew = transitRouteNew.getStops().get(sizeNew - 1).getStopFacility().getId();

        Assertions.assertTrue(sizeOld > sizeNew,
                "modified route should have less stops as original route");
        assertEquals(0, inCntNew, "there should be no stops within the zone");
        assertEquals(outCntOld, outCntNew, "number of stops outside of zone should remain same");
        assertEquals(firstStopOld, firstStopNew, "first stop of old and new route should be same");
        assertNotEquals(lastStopOld, lastStopNew, "last stop of old and new route should be different");

        // Test vehicles
        Set<Id<Vehicle>> vehiclesUsedInTransitSchedule = getVehiclesUsedInTransitSchedule(transitScheduleNew);
        Set<Id<Vehicle>> vehiclesInVehiclesNew = results.getValue().getVehicles().keySet();
        Assertions.assertTrue(vehiclesInVehiclesNew.containsAll(vehiclesUsedInTransitSchedule),
                "TransitVehicles should contain all vehicles used in new TransitSchedule");
    }

	/**
	* trimming method: TrimEnds.
	* route scenario: MiddleIn
	* Since the ends are both outside of zone, route should not be modified
	*/
	@Disabled
	@Test
	void testTrimEnds_MiddleIn() {

        Scenario scenario = provideCopyOfScenario(this.inScheduleFile, this.inNetworkFile, this.inVehiclesFile);
        Set<Id<TransitStopFacility>> stopsInZone = getStopsInZone(scenario.getTransitSchedule(), inZoneShpFile);

        Id<TransitLine> transitLineId = routeType.middleIn.transitLineId;
        Id<TransitRoute> transitRouteId = routeType.middleIn.transitRouteId;


        // Before Trim
        TransitRoute transitRouteOld = scenario.getTransitSchedule().getTransitLines().get(transitLineId).getRoutes().get(transitRouteId);
        int numStopsOld = transitRouteOld.getStops().size();
        int numLinksOld = transitRouteOld.getRoute().getLinkIds().size();


        // Modification
        Set<Id<TransitLine>> linesToModify = Collections.singleton(transitLineId);
        Pair<TransitSchedule, Vehicles> results = TransitRouteTrimmer.trimEnds(scenario.getTransitSchedule(), scenario.getTransitVehicles(),
                stopsInZone, linesToModify, false, null, 3, true);

        // After trim
        TransitSchedule transitScheduleNew = results.getKey();
        TransitRoute routeNew = transitScheduleNew.getTransitLines().get(transitLineId).getRoutes().get(transitRouteId);
        int numStopsNew = routeNew.getStops().size();
        int numLinksNew = routeNew.getRoute().getLinkIds().size();

        Assertions.assertTrue(transitScheduleNew.getTransitLines().containsKey(transitLineId),
                "line should still exist");
        Assertions.assertEquals(numStopsOld, numStopsNew, "new route should contain same number of stops as old one");
        Assertions.assertEquals(numLinksOld, numLinksNew, "new route should contain same number of links as old one");

        // Test vehicles
        Set<Id<Vehicle>> vehiclesUsedInTransitSchedule = getVehiclesUsedInTransitSchedule(transitScheduleNew);
        Set<Id<Vehicle>> vehiclesInVehiclesNew = results.getValue().getVehicles().keySet();
        Assertions.assertTrue(vehiclesInVehiclesNew.containsAll(vehiclesUsedInTransitSchedule),
                "TransitVehicles should contain all vehicles used in new TransitSchedule");
    }


	/**
	* trimming method: SkipStops.
	* route scenario: AllIn
	* New route should be empty
	*/
	@Disabled
	@Test
	void testSkipStops_AllIn() {

        Scenario scenario = provideCopyOfScenario(this.inScheduleFile, this.inNetworkFile, this.inVehiclesFile);
        Set<Id<TransitStopFacility>> stopsInZone = getStopsInZone(scenario.getTransitSchedule(), inZoneShpFile);

        Id<TransitLine> transitLineId = routeType.allIn.transitLineId;

        // Modification
        Set<Id<TransitLine>> linesToModify = Collections.singleton(transitLineId);
        Pair<TransitSchedule, Vehicles> results = TransitRouteTrimmer.skipStops(scenario.getTransitSchedule(), scenario.getTransitVehicles(),
                stopsInZone, linesToModify, false, null, 3, true);

        // After trim
        TransitSchedule transitScheduleNew = results.getKey();
        assertTrue(transitScheduleNew.getTransitLines().containsKey(transitLineId), "sched should include empty transit line");
        assertEquals(transitScheduleNew.getTransitLines().get(transitLineId).getRoutes().size(), 0, "transitLine should not longer contain any routes");

        // Test vehicles
        Set<Id<Vehicle>> vehiclesUsedInTransitSchedule = getVehiclesUsedInTransitSchedule(transitScheduleNew);
        Set<Id<Vehicle>> vehiclesInVehiclesNew = results.getValue().getVehicles().keySet();
        Assertions.assertTrue(vehiclesInVehiclesNew.containsAll(vehiclesUsedInTransitSchedule),
                "TransitVehicles should contain all vehicles used in new TransitSchedule");

    }

	/**
	* trimming method: SkipStops.
	* route scenario: HalfIn
	* Stops outside zone should be skipped
	*/
	@Disabled
	@Test
	void testSkipStops_HalfIn() {

        Scenario scenario = provideCopyOfScenario(this.inScheduleFile, this.inNetworkFile, this.inVehiclesFile);
        Set<Id<TransitStopFacility>> stopsInZone = getStopsInZone(scenario.getTransitSchedule(), inZoneShpFile);

        // Before trim
        Id<TransitLine> transitLineId = routeType.halfIn.transitLineId;
        Id<TransitRoute> transitRouteId = routeType.halfIn.transitRouteId;

        TransitRoute transitRouteOld = scenario.getTransitSchedule().getTransitLines().get(transitLineId).getRoutes().get(transitRouteId);
        int sizeOld = transitRouteOld.getStops().size();
        int outCntOld = countStopsOutsideZone(transitRouteOld, stopsInZone);
        int numLinksOld = transitRouteOld.getRoute().getLinkIds().size();

        // Modification
        Set<Id<TransitLine>> linesToModify = Collections.singleton(transitLineId);
        Pair<TransitSchedule, Vehicles> results = TransitRouteTrimmer.skipStops(scenario.getTransitSchedule(), scenario.getTransitVehicles(),
                stopsInZone, linesToModify, true, null, 3, true);

        // After trim
        TransitSchedule transitScheduleNew = results.getKey();
        TransitRoute transitRouteNew = transitScheduleNew.getTransitLines().get(transitLineId).getRoutes().get(transitRouteId);
        int sizeNew = transitRouteNew.getStops().size();
        int inCntNew = countStopsInZone(transitRouteNew, stopsInZone);
        int outCntNew = countStopsOutsideZone(transitRouteNew, stopsInZone);
        int numLinksNew = transitRouteNew.getRoute().getLinkIds().size();

        Assertions.assertTrue(sizeOld > sizeNew,
                "there should be less stops after the modification");
        assertTrue(numLinksNew < numLinksOld,
                "new route should have less links than old route");
        assertEquals(1, inCntNew, "there should only be one stop within the zone");
        assertEquals(outCntOld, outCntNew, "the number of stops outside of zone should remain same");

        // Test vehicles
        Set<Id<Vehicle>> vehiclesUsedInTransitSchedule = getVehiclesUsedInTransitSchedule(transitScheduleNew);
        Set<Id<Vehicle>> vehiclesInVehiclesNew = results.getValue().getVehicles().keySet();
        Assertions.assertTrue(vehiclesInVehiclesNew.containsAll(vehiclesUsedInTransitSchedule),
                "TransitVehicles should contain all vehicles used in new TransitSchedule");

    }


	/**
	* trimming method: SkipStops.
	* route scenario: MiddleIn
	* New route should have less stops than old route, but same amount of links
	*/
	@Disabled
	@Test
	void testSkipStops_MiddleIn() {

        Scenario scenario = provideCopyOfScenario(this.inScheduleFile, this.inNetworkFile, this.inVehiclesFile);
        Set<Id<TransitStopFacility>> stopsInZone = getStopsInZone(scenario.getTransitSchedule(), inZoneShpFile);


        Id<TransitLine> transitLineId = routeType.middleIn.transitLineId;
        Id<TransitRoute> transitRouteId = routeType.middleIn.transitRouteId;

        // Before trim
        TransitRoute transitRouteOld = scenario.getTransitSchedule().getTransitLines().get(transitLineId).getRoutes().get(transitRouteId);
        int numStopsOld = transitRouteOld.getStops().size();
        int numLinksOld = transitRouteOld.getRoute().getLinkIds().size();

        // Modification
        Set<Id<TransitLine>> linesToModify = Collections.singleton(transitLineId);
        Pair<TransitSchedule, Vehicles> results = TransitRouteTrimmer.skipStops(scenario.getTransitSchedule(), scenario.getTransitVehicles(),
                stopsInZone, linesToModify, true, null, 3, true);

        // After trim
        TransitSchedule transitScheduleNew = results.getKey();
        TransitRoute transitRouteNew = transitScheduleNew.getTransitLines().get(transitLineId).getRoutes().get(transitRouteId);
        int numStopsNew = transitRouteNew.getStops().size();
        int numLinksNew = transitRouteNew.getRoute().getLinkIds().size();
        int inCntNew = countStopsInZone(transitRouteNew, stopsInZone);

        Assertions.assertTrue(transitScheduleNew.getTransitLines().containsKey(transitLineId),
                "line should still exist");
        Assertions.assertNotEquals(numStopsOld, numStopsNew, "new route should NOT contain same number of stops as old one");
        Assertions.assertEquals(numLinksOld, numLinksNew, "new route should contain same number of links as old one");
        Assertions.assertEquals(2, inCntNew, "new route should only have two stops within zone, one per zone entrance/exit");


        // Test vehicles
        Set<Id<Vehicle>> vehiclesUsedInTransitSchedule = getVehiclesUsedInTransitSchedule(transitScheduleNew);
        Set<Id<Vehicle>> vehiclesInVehiclesNew = results.getValue().getVehicles().keySet();
        Assertions.assertTrue(vehiclesInVehiclesNew.containsAll(vehiclesUsedInTransitSchedule),
                "TransitVehicles should contain all vehicles used in new TransitSchedule");
    }

	/**
	* trimming method: SplitRoutes.
	* route scenario: AllIn
	* route will be deleted
	*/
	@Disabled
	@Test
	void testSplitRoutes_AllIn() {

        Scenario scenario = provideCopyOfScenario(this.inScheduleFile, this.inNetworkFile, this.inVehiclesFile);
        Set<Id<TransitStopFacility>> stopsInZone = getStopsInZone(scenario.getTransitSchedule(), inZoneShpFile);

        Id<TransitLine> transitLineId = routeType.allIn.transitLineId;

        // Modification
        Set<Id<TransitLine>> linesToModify = Collections.singleton(transitLineId);
        Pair<TransitSchedule, Vehicles> results = TransitRouteTrimmer.splitRoute(scenario.getTransitSchedule(), scenario.getTransitVehicles(),
                stopsInZone, linesToModify, false, null, 3,
                true, false, false, 0);


        // After trim
        TransitSchedule transitScheduleNew = results.getKey();
        assertTrue(transitScheduleNew.getTransitLines().containsKey(transitLineId),
                "schedule should include empty transit line");
        assertEquals(0, transitScheduleNew.getTransitLines().get(transitLineId).getRoutes().size(), "transitLine should not longer contain any routes");

        // Test vehicles
        Set<Id<Vehicle>> vehiclesUsedInTransitSchedule = getVehiclesUsedInTransitSchedule(transitScheduleNew);
        Set<Id<Vehicle>> vehiclesInVehiclesNew = results.getValue().getVehicles().keySet();
        Assertions.assertTrue(vehiclesInVehiclesNew.containsAll(vehiclesUsedInTransitSchedule),
                "TransitVehicles should contain all vehicles used in new TransitSchedule");
    }

	/**
	* trimming method: SplitRoutes.
	* route scenario: HalfIn
	* New route should have less stops than old route
	*/
	@Disabled
	@Test
	void testSplitRoutes_HalfIn() {
        Scenario scenario = provideCopyOfScenario(this.inScheduleFile, this.inNetworkFile, this.inVehiclesFile);
        Set<Id<TransitStopFacility>> stopsInZone = getStopsInZone(scenario.getTransitSchedule(), inZoneShpFile);

        Id<TransitLine> transitLineId = routeType.halfIn.transitLineId;

        // Before trim
        TransitRoute transitRouteOld = scenario.getTransitSchedule().getTransitLines().get(transitLineId).getRoutes().get(Id.create("184---17340_700_15", TransitRoute.class));
        int sizeOld = transitRouteOld.getStops().size();
        int outCntOld = countStopsOutsideZone(transitRouteOld, stopsInZone);

        // Modification
        Set<Id<TransitLine>> linesToModify = Collections.singleton(transitLineId);
        Pair<TransitSchedule, Vehicles> results = TransitRouteTrimmer.splitRoute(scenario.getTransitSchedule(), scenario.getTransitVehicles(),
                stopsInZone, linesToModify, true, null, 3,
                true, false, false, 0);


        // After trim
        TransitSchedule transitScheduleNew = results.getKey();

        assertTrue(transitScheduleNew.getTransitLines().containsKey(transitLineId));
        TransitLine transitLine = transitScheduleNew.getTransitLines().get(transitLineId);
        assertTrue(transitLine.getRoutes().containsKey(Id.create("184---17340_700_15_split1", TransitRoute.class)));

        TransitRoute transitRouteNew = transitScheduleNew.getTransitLines().get(Id.create("184---17340_700", TransitLine.class)).getRoutes().get(Id.create("184---17340_700_15_split1", TransitRoute.class));
        int sizeNew = transitRouteNew.getStops().size();
        int inCntNew = countStopsInZone(transitRouteNew, stopsInZone);
        int outCntNew = countStopsOutsideZone(transitRouteNew, stopsInZone);

        assertTrue(sizeOld > sizeNew,
                "new route should have less stops than old route");
        assertEquals(1, inCntNew, "there should only be one stop within the zone");
        assertEquals(outCntOld, outCntNew, "# of stops outside of zone should remain same");

        // Test vehicles
        Set<Id<Vehicle>> vehiclesUsedInTransitSchedule = getVehiclesUsedInTransitSchedule(transitScheduleNew);
        Set<Id<Vehicle>> vehiclesInVehiclesNew = results.getValue().getVehicles().keySet();
        Assertions.assertTrue(vehiclesInVehiclesNew.containsAll(vehiclesUsedInTransitSchedule),
                "TransitVehicles should contain all vehicles used in new TransitSchedule");
    }

	/**
	* trimming method: SplitRoutes.
	* route scenario: MiddleIn
	* Two routes should be created, each with only one stop within zone
	*/
	@Disabled
	@Test
	void testSplitRoutes_MiddleIn() {
        Scenario scenario = provideCopyOfScenario(this.inScheduleFile, this.inNetworkFile, this.inVehiclesFile);
        Set<Id<TransitStopFacility>> stopsInZone = getStopsInZone(scenario.getTransitSchedule(), inZoneShpFile);

        Id<TransitLine> transitLineId = routeType.middleIn.transitLineId;
        Id<TransitRoute> transitRouteId = routeType.middleIn.transitRouteId;

        // Before trim
        TransitRoute transitRouteOld = scenario.getTransitSchedule().getTransitLines().get(transitLineId).getRoutes().get(transitRouteId);

        // Modification
        Set<Id<TransitLine>> linesToModify = Collections.singleton(transitLineId);
        Pair<TransitSchedule, Vehicles> results = TransitRouteTrimmer.splitRoute(scenario.getTransitSchedule(), scenario.getTransitVehicles(),
                stopsInZone, linesToModify, true, null, 3,
                true, false, false, 0);


        // After trim
        TransitSchedule transitScheduleNew = results.getKey();

        assertTrue(transitScheduleNew.getTransitLines().containsKey(transitLineId), "line should still exist");
        TransitLine transitLineNew = transitScheduleNew.getTransitLines().get(transitLineId);

        assertTrue(transitLineNew.getRoutes().containsKey(Id.create("161---17326_700_22_split1", TransitRoute.class)));
        assertTrue(transitLineNew.getRoutes().containsKey(Id.create("161---17326_700_22_split2", TransitRoute.class)));
        assertFalse(transitLineNew.getRoutes().containsKey(Id.create("161---17326_700_22_split0", TransitRoute.class)));
        assertFalse(transitLineNew.getRoutes().containsKey(Id.create("161---17326_700_22_split3", TransitRoute.class)));
        assertFalse(transitLineNew.getRoutes().containsKey(Id.create("161---17326_700_22", TransitRoute.class)));


        TransitRoute transitRouteNew1 = transitLineNew.getRoutes().get(Id.create("161---17326_700_22_split1", TransitRoute.class));
        TransitRoute transitRouteNew2 = transitLineNew.getRoutes().get(Id.create("161---17326_700_22_split2", TransitRoute.class));

        assertNotEquals(transitRouteOld.getStops().size(), transitRouteNew1.getStops().size() + transitRouteNew2.getStops().size());
        assertNotEquals(transitRouteNew1.getStops().size(), transitRouteNew2.getStops().size());

        int inCntNew1 = countStopsInZone(transitRouteNew1, stopsInZone);
        int inCntNew2 = countStopsInZone(transitRouteNew2, stopsInZone);

        Assertions.assertEquals(1, inCntNew1, "new route #1 should only have one stop within zone");
        Assertions.assertEquals(1, inCntNew2, "new route #2 should only have one stop within zone");

        // Test vehicles
        Set<Id<Vehicle>> vehiclesUsedInTransitSchedule = getVehiclesUsedInTransitSchedule(transitScheduleNew);
        Set<Id<Vehicle>> vehiclesInVehiclesNew = results.getValue().getVehicles().keySet();
        Assertions.assertTrue(vehiclesInVehiclesNew.containsAll(vehiclesUsedInTransitSchedule),
                "TransitVehicles should contain all vehicles used in new TransitSchedule");

    }


	/*
    Part 3: tests hub functionality for SplitRoutes trimming method (using route type "middleIn")
    Hubs allow route to extend into zone to reach a import transit stop (like a major transfer point)
    */


	/**
	 * Test Hub functionality
	 * trimming method: SplitRoutes.
	 * route scenario: MiddleIn
	 * tests reach of hubs. Left hub should be included in route 1, while right hub should not be
	 * included in route 2, due to lacking reach
	 */
	@Disabled
	@Test
	void testSplitRoutes_MiddleIn_Hub_ValidateReach() {
        Scenario scenario = provideCopyOfScenario(this.inScheduleFile, this.inNetworkFile, this.inVehiclesFile);
        Set<Id<TransitStopFacility>> stopsInZone = getStopsInZone(scenario.getTransitSchedule(), inZoneShpFile);

        Id<TransitLine> transitLineId = routeType.middleIn.transitLineId;
        Id<TransitRoute> transitRouteId = routeType.middleIn.transitRouteId;

        // Before trim
        TransitRoute transitRouteOld = scenario.getTransitSchedule().getTransitLines().get(transitLineId).getRoutes().get(transitRouteId);

        // add hub attributes
        // Stop 070101005700 is a hub with reach of 3. This stop (as well as the intermediary stops)
        // should be included in route1
        Id<TransitStopFacility> facIdLeft = Id.create("070101005700", TransitStopFacility.class);
        scenario.getTransitSchedule().getFacilities().get(facIdLeft).getAttributes().putAttribute("hub-reach", 3);

        // Stop 070101006207 is a hub with reach of 3. This stop is therefore just out of range for route 2
        // Therefore it should not be included.
        Id<TransitStopFacility> facIdRight = Id.create("070101006207", TransitStopFacility.class);
        scenario.getTransitSchedule().getFacilities().get(facIdRight).getAttributes().putAttribute("hub-reach", 3);


        // Modification
        Set<Id<TransitLine>> linesToModify = Collections.singleton(transitLineId);

        Pair<TransitSchedule, Vehicles> results = TransitRouteTrimmer.splitRoute(scenario.getTransitSchedule(), scenario.getTransitVehicles(),
                stopsInZone, linesToModify, true, null, 3,
                true, true, false, 0);


        // After trim
        TransitSchedule transitScheduleNew = results.getKey();
        assertTrue(transitScheduleNew.getTransitLines().containsKey(transitLineId), "line should still exist");
        TransitLine transitLineNew = transitScheduleNew.getTransitLines().get(transitLineId);

        assertTrue(transitLineNew.getRoutes().containsKey(Id.create("161---17326_700_22_split1", TransitRoute.class)));
        assertTrue(transitLineNew.getRoutes().containsKey(Id.create("161---17326_700_22_split2", TransitRoute.class)));
        assertFalse(transitLineNew.getRoutes().containsKey(Id.create("161---17326_700_22_split0", TransitRoute.class)));
        assertFalse(transitLineNew.getRoutes().containsKey(Id.create("161---17326_700_22_split3", TransitRoute.class)));
        assertFalse(transitLineNew.getRoutes().containsKey(Id.create("161---17326_700_22", TransitRoute.class)));


        TransitRoute transitRouteNew1 = transitLineNew.getRoutes().get(Id.create("161---17326_700_22_split1", TransitRoute.class));
        TransitRoute transitRouteNew2 = transitLineNew.getRoutes().get(Id.create("161---17326_700_22_split2", TransitRoute.class));

        assertNotEquals(transitRouteOld.getStops().size(), transitRouteNew1.getStops().size() + transitRouteNew2.getStops().size());
        assertNotEquals(transitRouteNew1.getStops().size(), transitRouteNew2.getStops().size());

        int inCntNew1 = countStopsInZone(transitRouteNew1, stopsInZone);
        int inCntNew2 = countStopsInZone(transitRouteNew2, stopsInZone);

        assertEquals(3, inCntNew1, "new route #1 should have three stop within zone");
        assertEquals(1, inCntNew2, "new route #2 should have one stop within zone");

        // Test vehicles
        Set<Id<Vehicle>> vehiclesUsedInTransitSchedule = getVehiclesUsedInTransitSchedule(transitScheduleNew);
        Set<Id<Vehicle>> vehiclesInVehiclesNew = results.getValue().getVehicles().keySet();
        Assertions.assertTrue(vehiclesInVehiclesNew.containsAll(vehiclesUsedInTransitSchedule),
                "TransitVehicles should contain all vehicles used in new TransitSchedule");
    }

	/**
	* Test Hub functionality
	* trimming method: SplitRoutes.
	* route scenario: MiddleIn
	* tests parameter to include first nearest hub, even if reach is insufficient.
	* Right hub should be included, even though reach is too low.
	*/
	@Disabled
	@Test
	void testSplitRoutes_MiddleIn_Hub_IncludeFirstHubInZone() {
        Scenario scenario = provideCopyOfScenario(this.inScheduleFile, this.inNetworkFile, this.inVehiclesFile);
        Set<Id<TransitStopFacility>> stopsInZone = getStopsInZone(scenario.getTransitSchedule(), inZoneShpFile);
        Id<TransitLine> transitLineId = routeType.middleIn.transitLineId;
        Id<TransitRoute> transitRouteId = routeType.middleIn.transitRouteId;

        // Before trim
        TransitRoute transitRouteOld = scenario.getTransitSchedule().getTransitLines().get(transitLineId).getRoutes().get(transitRouteId);

        // Stop 070101005700 is a hub with reach of 3. This stop (as well as the intermediary stops)
        // should be included in route1
        Id<TransitStopFacility> facIdLeft = Id.create("070101005700", TransitStopFacility.class);
        scenario.getTransitSchedule().getFacilities().get(facIdLeft).getAttributes().putAttribute("hub-reach", 3);

        // Stop 070101006207 is a hub with reach of 3. This stop is therefore just out of range for route 2
        // However, since includeFirstHubInZone is true, it should be included anyway.
        Id<TransitStopFacility> facIdRight = Id.create("070101006207", TransitStopFacility.class);
        scenario.getTransitSchedule().getFacilities().get(facIdRight).getAttributes().putAttribute("hub-reach", 3);


        // Modification
        Set<Id<TransitLine>> linesToModify = Collections.singleton(transitLineId);
        Pair<TransitSchedule, Vehicles> results = TransitRouteTrimmer.splitRoute(scenario.getTransitSchedule(), scenario.getTransitVehicles(),
                stopsInZone, linesToModify, true, null, 3,
                true, true, true, 0);


        // After trim
        TransitSchedule transitScheduleNew = results.getKey();

        assertTrue(transitScheduleNew.getTransitLines().containsKey(transitLineId), "line should still exist");
        TransitLine transitLineNew = transitScheduleNew.getTransitLines().get(transitLineId);

        assertTrue(transitLineNew.getRoutes().containsKey(Id.create("161---17326_700_22_split1", TransitRoute.class)));
        assertTrue(transitLineNew.getRoutes().containsKey(Id.create("161---17326_700_22_split2", TransitRoute.class)));
        assertFalse(transitLineNew.getRoutes().containsKey(Id.create("161---17326_700_22_split0", TransitRoute.class)));
        assertFalse(transitLineNew.getRoutes().containsKey(Id.create("161---17326_700_22_split3", TransitRoute.class)));
        assertFalse(transitLineNew.getRoutes().containsKey(Id.create("161---17326_700_22", TransitRoute.class)));


        TransitRoute transitRouteNew1 = transitLineNew.getRoutes().get(Id.create("161---17326_700_22_split1", TransitRoute.class));
        TransitRoute transitRouteNew2 = transitLineNew.getRoutes().get(Id.create("161---17326_700_22_split2", TransitRoute.class));

        assertNotEquals(transitRouteOld.getStops().size(), transitRouteNew1.getStops().size() + transitRouteNew2.getStops().size());
        assertNotEquals(transitRouteNew1.getStops().size(), transitRouteNew2.getStops().size());

        int inCntNew1 = countStopsInZone(transitRouteNew1, stopsInZone);
        int inCntNew2 = countStopsInZone(transitRouteNew2, stopsInZone);


        assertEquals(3, inCntNew1, "new route #1 should have three stops within zone");
        assertEquals(4, inCntNew2, "new route #2 should have four stops within zone");
        Id<TransitStopFacility> idRoute1 = transitRouteNew1.getStops().get(transitRouteNew1.getStops().size() - 1).getStopFacility().getId();
        assertEquals(facIdLeft, idRoute1, "last stop of route #1 should be the left hub");

        Id<TransitStopFacility> idRoute2 = transitRouteNew2.getStops().get(0).getStopFacility().getId();
        assertEquals(facIdRight, idRoute2, "first stop of route #2 should be the right hub");

        // Test vehicles
        Set<Id<Vehicle>> vehiclesUsedInTransitSchedule = getVehiclesUsedInTransitSchedule(transitScheduleNew);
        Set<Id<Vehicle>> vehiclesInVehiclesNew = results.getValue().getVehicles().keySet();
        Assertions.assertTrue(vehiclesInVehiclesNew.containsAll(vehiclesUsedInTransitSchedule),
                "TransitVehicles should contain all vehicles used in new TransitSchedule");
    }

	/**
	* Test Hub functionality
	* trimming method: SplitRoutes.
	* route scenario: MiddleIn
	* if multiple hubs are within reach of route, the route should go to further one
	*/
	@Disabled
	@Test
	void testSplitRoutes_MiddleIn_Hub_MultipleHubs() {
        Scenario scenario = provideCopyOfScenario(this.inScheduleFile, this.inNetworkFile, this.inVehiclesFile);
        Set<Id<TransitStopFacility>> stopsInZone = getStopsInZone(scenario.getTransitSchedule(), inZoneShpFile);

        Id<TransitLine> transitLineId = routeType.middleIn.transitLineId;
        Id<TransitRoute> transitRouteId = routeType.middleIn.transitRouteId;

        // Before trim
        TransitRoute transitRouteOld = scenario.getTransitSchedule().getTransitLines().get(transitLineId).getRoutes().get(transitRouteId);
        int numStopsOld = transitRouteOld.getStops().size();

        assertFalse(stopsInZone.contains(transitRouteOld.getStops().get(0).getStopFacility().getId()));
        assertFalse(stopsInZone.contains(transitRouteOld.getStops().get(numStopsOld - 1).getStopFacility().getId()));


        // Stop 070101005700 is a hub with reach of 3. This stop (as well as the intermediary stops)
        // should be included in route1
        Id<TransitStopFacility> facIdLeft = Id.create("070101005700", TransitStopFacility.class);
        scenario.getTransitSchedule().getFacilities().get(facIdLeft).getAttributes().putAttribute("hub-reach", 3);

        // Stop 070101006207 is a hub with reach of 5. This stop is therefore in range for route 1
        // Therefore it should not be included.
        Id<TransitStopFacility> facIdRight = Id.create("070101005702", TransitStopFacility.class);
        scenario.getTransitSchedule().getFacilities().get(facIdRight).getAttributes().putAttribute("hub-reach", 5);


        // Modification
        Set<Id<TransitLine>> linesToModify = Collections.singleton(transitLineId);
        Pair<TransitSchedule, Vehicles> results = TransitRouteTrimmer.splitRoute(scenario.getTransitSchedule(), scenario.getTransitVehicles(),
                stopsInZone, linesToModify, true, null, 3,
                true, true, false, 0);


        // After trim
        TransitSchedule transitScheduleNew = results.getKey();

        assertTrue(transitScheduleNew.getTransitLines().containsKey(transitLineId), "line should still exist");
        TransitLine transitLineNew = transitScheduleNew.getTransitLines().get(transitLineId);

        assertTrue(transitLineNew.getRoutes().containsKey(Id.create("161---17326_700_22_split1", TransitRoute.class)));
        assertTrue(transitLineNew.getRoutes().containsKey(Id.create("161---17326_700_22_split2", TransitRoute.class)));
        assertFalse(transitLineNew.getRoutes().containsKey(Id.create("161---17326_700_22_split0", TransitRoute.class)));
        assertFalse(transitLineNew.getRoutes().containsKey(Id.create("161---17326_700_22_split3", TransitRoute.class)));
        assertFalse(transitLineNew.getRoutes().containsKey(Id.create("161---17326_700_22", TransitRoute.class)));


        TransitRoute transitRouteNew1 = transitLineNew.getRoutes().get(Id.create("161---17326_700_22_split1", TransitRoute.class));
        TransitRoute transitRouteNew2 = transitLineNew.getRoutes().get(Id.create("161---17326_700_22_split2", TransitRoute.class));

        assertNotEquals(transitRouteOld.getStops().size(), transitRouteNew1.getStops().size() + transitRouteNew2.getStops().size());
        assertNotEquals(transitRouteNew1.getStops().size(), transitRouteNew2.getStops().size());

        int inCntNew1 = countStopsInZone(transitRouteNew1, stopsInZone);
        int inCntNew2 = countStopsInZone(transitRouteNew2, stopsInZone);

        assertEquals(5, inCntNew1, "new route #1 should have five stop within zone");
        assertEquals(1, inCntNew2, "new route #2 should have one stop within zone");

    }

	/**
	* Test Hub functionality
	* trimming method: SplitRoutes.
	* route scenario: MiddleIn
	* if two new routes overlap (because they both reach to same hub)
	* then they should be combined into one route
	*/
	@Disabled
	@Test
	void testSplitRoutes_MiddleIn_Hub_OverlapRoutes() {
        Scenario scenario = provideCopyOfScenario(this.inScheduleFile, this.inNetworkFile, this.inVehiclesFile);
        Set<Id<TransitStopFacility>> stopsInZone = getStopsInZone(scenario.getTransitSchedule(), inZoneShpFile);

        Id<TransitLine> transitLineId = routeType.middleIn.transitLineId;
        Id<TransitRoute> transitRouteId = routeType.middleIn.transitRouteId;

        // Before trim
        TransitRoute transitRouteOld = scenario.getTransitSchedule().getTransitLines().get(transitLineId).getRoutes().get(transitRouteId);

        // Stop 070101005708 = S Wilhelmshagen (Berlin) - Hub
        Id<TransitStopFacility> facId = Id.create("070101005708", TransitStopFacility.class);
        scenario.getTransitSchedule().getFacilities().get(facId).getAttributes().putAttribute("hub-reach", 11);

        // Modification
        Set<Id<TransitLine>> linesToModify = Collections.singleton(transitLineId);
        Pair<TransitSchedule, Vehicles> results = TransitRouteTrimmer.splitRoute(scenario.getTransitSchedule(), scenario.getTransitVehicles(),
                stopsInZone, linesToModify, true, null, 3,
                true, true, false, 0);


        // After trim
        TransitSchedule transitScheduleNew = results.getKey();

        assertTrue(transitScheduleNew.getTransitLines().containsKey(transitLineId), "line should still exist");
        TransitLine transitLineNew = transitScheduleNew.getTransitLines().get(transitLineId);

        assertTrue(transitLineNew.getRoutes().containsKey(Id.create("161---17326_700_22_split1", TransitRoute.class)));
        assertFalse(transitLineNew.getRoutes().containsKey(Id.create("161---17326_700_22_split2", TransitRoute.class)));
        assertFalse(transitLineNew.getRoutes().containsKey(Id.create("161---17326_700_22_split0", TransitRoute.class)));
        assertFalse(transitLineNew.getRoutes().containsKey(Id.create("161---17326_700_22_split3", TransitRoute.class)));
        assertFalse(transitLineNew.getRoutes().containsKey(Id.create("161---17326_700_22", TransitRoute.class)));


        TransitRoute transitRouteNew1 = transitLineNew.getRoutes().get(Id.create("161---17326_700_22_split1", TransitRoute.class));
        assertEquals(transitRouteOld.getStops().size(), transitRouteNew1.getStops().size());

        int inCntNew1 = countStopsInZone(transitRouteNew1, stopsInZone);
        assertEquals(19, inCntNew1, "new route #1 should have 19 stops within zone");


    }

	/*
    Part 4: Tests individual user-defined parameters
     */

	/**
	 * Test parameter allowableStopsWithinZone
	 * trimming method: SplitRoutes.
	 * route scenario: MiddleIn
	 * route should not be split, since the parameter allowableStopsWithinZone is equal to the number
	 * of stops within zone
	 */
	@Disabled
	@Test
	void testSplitRoutes_MiddleIn_AllowableStopsWithin() {
        Scenario scenario = provideCopyOfScenario(this.inScheduleFile, this.inNetworkFile, this.inVehiclesFile);
        Set<Id<TransitStopFacility>> stopsInZone = getStopsInZone(scenario.getTransitSchedule(), inZoneShpFile);
        Id<TransitLine> transitLineId = routeType.middleIn.transitLineId;
        Id<TransitRoute> transitRouteId = routeType.middleIn.transitRouteId;

        // Before trim
        TransitRoute transitRouteOld = scenario.getTransitSchedule().getTransitLines().get(transitLineId).getRoutes().get(transitRouteId);

        // Modification
        Set<Id<TransitLine>> linesToModify = Collections.singleton(transitLineId);
        Pair<TransitSchedule, Vehicles> results = TransitRouteTrimmer.splitRoute(scenario.getTransitSchedule(), scenario.getTransitVehicles(),
                stopsInZone, linesToModify, true, null, 3,
                false, true, false, 19);


        // After trim
        TransitSchedule transitScheduleNew = results.getKey();

        assertTrue(transitScheduleNew.getTransitLines().containsKey(transitLineId), "line should still exist");
        TransitLine transitLineNew = transitScheduleNew.getTransitLines().get(transitLineId);

        assertTrue(transitLineNew.getRoutes().containsKey(Id.create("161---17326_700_22_split1", TransitRoute.class)));
        assertFalse(transitLineNew.getRoutes().containsKey(Id.create("161---17326_700_22_split2", TransitRoute.class)));
        assertFalse(transitLineNew.getRoutes().containsKey(Id.create("161---17326_700_22_split0", TransitRoute.class)));
        assertFalse(transitLineNew.getRoutes().containsKey(Id.create("161---17326_700_22_split3", TransitRoute.class)));
        assertFalse(transitLineNew.getRoutes().containsKey(Id.create("161---17326_700_22", TransitRoute.class)));


        TransitRoute routeNew1 = transitLineNew.getRoutes().get(Id.create("161---17326_700_22_split1", TransitRoute.class));

        assertEquals(transitRouteOld.getStops().size(), routeNew1.getStops().size());

        int inCntNew1 = countStopsInZone(routeNew1, stopsInZone);

        assertEquals(19, inCntNew1, "new route #1 should have 19 stops within zone");


    }

	@Disabled
	@Test
	void testDeparturesAndOffsetsAndDescription() {

        Scenario scenario = provideCopyOfScenario(this.inScheduleFile, this.inNetworkFile, this.inVehiclesFile);
        Set<Id<TransitStopFacility>> stopsInZone = getStopsInZone(scenario.getTransitSchedule(), inZoneShpFile);

        Id<TransitLine> transitLineId = routeType.middleIn.transitLineId;
        Id<TransitRoute> transitRouteId = routeType.middleIn.transitRouteId;

        // Before trim
        TransitRoute transitRouteOld = scenario.getTransitSchedule().getTransitLines().get(transitLineId).getRoutes().get(transitRouteId);

        // set description to check whether description is copied to new routes
        transitRouteOld.setDescription("test123");

        // Modification
        Set<Id<TransitLine>> linesToModify = Collections.singleton(transitLineId);
        Pair<TransitSchedule, Vehicles> results = TransitRouteTrimmer.splitRoute(scenario.getTransitSchedule(), scenario.getTransitVehicles(),
                stopsInZone, linesToModify, true, null, 3,
                true, false, false, 0);


        // After trim
        TransitSchedule transitScheduleNew = results.getKey();

        assertTrue(transitScheduleNew.getTransitLines().containsKey(transitLineId), "line should still exist");
        TransitLine transitLineNew = transitScheduleNew.getTransitLines().get(transitLineId);

        assertTrue(transitLineNew.getRoutes().containsKey(Id.create("161---17326_700_22_split1", TransitRoute.class)));
        assertTrue(transitLineNew.getRoutes().containsKey(Id.create("161---17326_700_22_split2", TransitRoute.class)));

        TransitRoute transitRouteNew1 = transitLineNew.getRoutes().get(Id.create("161---17326_700_22_split1", TransitRoute.class));
        TransitRoute transitRouteNew2 = transitLineNew.getRoutes().get(Id.create("161---17326_700_22_split2", TransitRoute.class));

        int routeOldSize = transitRouteOld.getStops().size();
        int routeNew1size = transitRouteNew1.getStops().size();
        int routeNew2Size = transitRouteNew2.getStops().size();


        //Start of left route
        TransitRouteStop routeNew1Start = transitRouteNew1.getStops().get(0);

        Assertions.assertEquals(0., routeNew1Start.getDepartureOffset().seconds(),0.);
        Assertions.assertEquals(0., routeNew1Start.getArrivalOffset().seconds(), 0.);

        //End of first route
        TransitRouteStop routeNew1End = transitRouteNew1.getStops().get(routeNew1size - 1);
        TransitRouteStop routeOld1End = transitRouteOld.getStops().get(routeNew1size - 1);

        Assertions.assertEquals(routeOld1End.getDepartureOffset(), routeNew1End.getDepartureOffset());
        Assertions.assertEquals(routeOld1End.getArrivalOffset(), routeNew1End.getArrivalOffset());

        //Start of second route
        TransitRouteStop routeNew2Start = transitRouteNew2.getStops().get(0);
        TransitRouteStop routeOld2Start = transitRouteOld.getStops().get(routeOldSize - routeNew2Size);
        double deltaSeconds = routeOld2Start.getArrivalOffset().seconds();


        Assertions.assertEquals(0., routeNew2Start.getDepartureOffset().seconds(), 0.);
        Assertions.assertEquals(0., routeNew2Start.getArrivalOffset().seconds(), 0.);


        //End of second route
        TransitRouteStop routeNew2End = transitRouteNew2.getStops().get(routeNew2Size - 1);
        TransitRouteStop routeOld2End = transitRouteOld.getStops().get(routeOldSize - 1);

        Assertions.assertEquals(routeOld2End.getDepartureOffset().seconds() - deltaSeconds, routeNew2End.getDepartureOffset().seconds(), 0.);
        Assertions.assertEquals(routeOld2End.getArrivalOffset().seconds() - deltaSeconds, routeNew2End.getArrivalOffset().seconds(), 0.);


        // Check Departures
        Assertions.assertEquals(transitRouteOld.getDepartures().size(), transitRouteNew1.getDepartures().size());
        Assertions.assertEquals(transitRouteOld.getDepartures().size(), transitRouteNew2.getDepartures().size());
        for (Departure departureOld : transitRouteOld.getDepartures().values()) {
            Id<Departure> idOld = departureOld.getId();
            double departureTimeOld = departureOld.getDepartureTime();
            Id<Vehicle> vehicleIdOld = departureOld.getVehicleId();
            Id<Departure> idNew1 = Id.create(idOld.toString() + "_split1", Departure.class);
            Assertions.assertTrue(transitRouteNew1.getDepartures().containsKey(idNew1));
            Departure departureNew1 = transitRouteNew1.getDepartures().get(idNew1);
            Assertions.assertEquals(vehicleIdOld.toString() + "_split1", departureNew1.getVehicleId().toString());
            Assertions.assertEquals(departureTimeOld, departureNew1.getDepartureTime(), 0.);

            Id<Departure> idNew2 = Id.create(idOld.toString() + "_split2", Departure.class);
            Assertions.assertTrue(transitRouteNew2.getDepartures().containsKey(idNew2));
            Departure departureNew2 = transitRouteNew2.getDepartures().get(idNew2);
            Assertions.assertEquals(vehicleIdOld.toString() + "_split2", departureNew2.getVehicleId().toString());
            Assertions.assertEquals(departureTimeOld + deltaSeconds, departureNew2.getDepartureTime(), 0.);

        }

        Assertions.assertEquals("test123", transitRouteNew1.getDescription());
        Assertions.assertEquals("test123", transitRouteNew2.getDescription());

        Assertions.assertEquals(transitRouteOld.getTransportMode(), transitRouteNew1.getTransportMode());
        Assertions.assertEquals(transitRouteOld.getTransportMode(), transitRouteNew2.getTransportMode());


//        transitScheduleNew.getAttributes();

    }


    private Set<Id<TransitStopFacility>> getStopsInZone(TransitSchedule transitSchedule, String zoneShpFile) {
        List<PreparedGeometry> geometries;
        try {
            geometries = ShpGeometryUtils.loadPreparedGeometries(new URL(zoneShpFile));
        } catch (MalformedURLException e) {
            e.printStackTrace();
            throw new RuntimeException("Wrong Filename!");
        }
        Set<Id<TransitStopFacility>> stopsInZone = new HashSet<>();
        for (TransitStopFacility stop : transitSchedule.getFacilities().values()) {
            if (ShpGeometryUtils.isCoordInPreparedGeometries(stop.getCoord(), geometries)) {
                stopsInZone.add(stop.getId());
            }
        }

        return stopsInZone;
    }

    /*
     * TODO: copy this scenario cloning stuff somewhere else and test it.
     * TODO: There is some overlap with the copies the main class to be tested does, but maybe deep copies are less necessary there
     */
    private Scenario provideCopyOfScenario(String scheduleFile, String networkFile, String vehiclesFile) {

        if (inputScenario == null) {
            Config config = ConfigUtils.createConfig();
            config.transit().setTransitScheduleFile(scheduleFile);
            config.network().setInputFile(networkFile);
            config.transit().setVehiclesFile(vehiclesFile);

            inputScenario = ScenarioUtils.loadScenario(config);
        }

        // copy scenario
        Scenario copiedScenario = ScenarioUtils.loadScenario(ConfigUtils.createConfig());

        // copy Network
        for (Node node: inputScenario.getNetwork().getNodes().values()) {
            copiedScenario.getNetwork().addNode(
                    copiedScenario.getNetwork().getFactory().createNode(
                            node.getId(), CoordUtils.createCoord(node.getCoord().getX(), node.getCoord().getY())));
        }

        for (Link link: inputScenario.getNetwork().getLinks().values()) {
            copiedScenario.getNetwork().addLink(
                    copiedScenario.getNetwork().getFactory().createLink(
                            link.getId(),
                            copiedScenario.getNetwork().getNodes().get(link.getFromNode().getId()),
                            copiedScenario.getNetwork().getNodes().get(link.getToNode().getId())));
        }

        // copy TransitSchedule
        for (TransitStopFacility transitStopFacility: inputScenario.getTransitSchedule().getFacilities().values()) {
            TransitStopFacility copiedTransitStopFacility = copiedScenario.getTransitSchedule().getFactory().
                    createTransitStopFacility(                    transitStopFacility.getId(),
                    CoordUtils.createCoord(
                            transitStopFacility.getCoord().getX(), transitStopFacility.getCoord().getY()),
                    transitStopFacility.getIsBlockingLane());
            for (Map.Entry<String, Object> entry: transitStopFacility.getAttributes().getAsMap().entrySet()) {
                // TODO: real deep copy of value Object
                copiedTransitStopFacility.getAttributes().putAttribute(entry.getKey(), entry.getValue());
            }
            copiedTransitStopFacility.setLinkId(transitStopFacility.getLinkId());
            copiedScenario.getTransitSchedule().addStopFacility(copiedTransitStopFacility);
        }

        for (TransitLine transitLine: inputScenario.getTransitSchedule().getTransitLines().values()) {
            TransitLine copiedTransitLine = copiedScenario.getTransitSchedule().getFactory().createTransitLine(
                    transitLine.getId());
            for (Map.Entry<String, Object> entry: transitLine.getAttributes().getAsMap().entrySet()) {
                // TODO: real deep copy of value Object
                copiedTransitLine.getAttributes().putAttribute(entry.getKey(), entry.getValue());
            }
            if (transitLine.getName() != null) {
                copiedTransitLine.setName(new String(transitLine.getName()));
            }
            copiedScenario.getTransitSchedule().addTransitLine(copiedTransitLine);

            for (TransitRoute transitRoute: transitLine.getRoutes().values()) {
                List<TransitRouteStop> copiedTransitRouteStops = new ArrayList<>();
                for (TransitRouteStop transitRouteStop: transitRoute.getStops()) {
                    copiedTransitRouteStops.add(
                            copiedScenario.getTransitSchedule().getFactory().createTransitRouteStop(
                                    copiedScenario.getTransitSchedule().getFacilities().get(
                                            transitRouteStop.getStopFacility().getId()),
                                    transitRouteStop.getArrivalOffset(),
                                    transitRouteStop.getDepartureOffset()));
                }
                TransitRoute copiedTransitRoute = copiedScenario.getTransitSchedule().getFactory().createTransitRoute(
                        transitRoute.getId(), transitRoute.getRoute().clone(),
                        copiedTransitRouteStops,
                        new String(transitRoute.getTransportMode()));
                if (transitRoute.getDescription() != null) {
                    copiedTransitRoute.setDescription(transitRoute.getDescription());
                }
                copiedTransitLine.addRoute(copiedTransitRoute);

                for (Departure departure: transitRoute.getDepartures().values()) {
                    Departure copiedDeparture = copiedScenario.getTransitSchedule().getFactory().createDeparture(
                            departure.getId(), departure.getDepartureTime());
                    copiedDeparture.setVehicleId(departure.getVehicleId());
                    copiedTransitRoute.addDeparture(copiedDeparture);
                }
            }
        }

        // copy TransitVehicles
        for (VehicleType vehicleType: inputScenario.getTransitVehicles().getVehicleTypes().values()) {
            VehicleType copiedVehicleType = copiedScenario.getTransitVehicles().getFactory().createVehicleType(vehicleType.getId());
            copiedVehicleType.getCapacity().setSeats(vehicleType.getCapacity().getSeats());
            copiedVehicleType.getCapacity().setStandingRoom(vehicleType.getCapacity().getStandingRoom());
            VehicleUtils.setDoorOperationMode(copiedVehicleType, VehicleUtils.getDoorOperationMode(vehicleType));
            VehicleUtils.setAccessTime(copiedVehicleType, VehicleUtils.getAccessTime(vehicleType));
            VehicleUtils.setEgressTime(copiedVehicleType, VehicleUtils.getEgressTime(vehicleType));
            copiedScenario.getTransitVehicles().addVehicleType(copiedVehicleType);
        }

        for (Vehicle vehicle: inputScenario.getTransitVehicles().getVehicles().values()) {
            Vehicle copiedVehicle = copiedScenario.getTransitVehicles().getFactory().createVehicle(
                    Id.createVehicleId(vehicle.getId()),
                    copiedScenario.getTransitVehicles().getVehicleTypes().get(vehicle.getType().getId()));
            copiedScenario.getTransitVehicles().addVehicle(copiedVehicle);
        }

        return copiedScenario;
    }

    private Set<Id<Vehicle>> getVehiclesUsedInTransitSchedule(TransitSchedule transitScheduleNew) {
        return transitScheduleNew.getTransitLines().values().stream()
                .flatMap(x -> x.getRoutes().values().stream()
                        .flatMap(y -> y.getDepartures().values().stream()
                                .map(Departure::getVehicleId))).collect(Collectors.toSet());
    }


    private int countStopsInZone(TransitRoute transitRoute, Set<Id<TransitStopFacility>> stopsInZone) {
        int inCnt = 0;
        for (TransitRouteStop stop : transitRoute.getStops()) {
            if (stopsInZone.contains(stop.getStopFacility().getId())) {
                inCnt++;
            }
        }
        return inCnt;
    }

    private int countStopsOutsideZone(TransitRoute transitRoute, Set<Id<TransitStopFacility>> stopsInZone) {
        int outCnt = 0;
        for (TransitRouteStop stop : transitRoute.getStops()) {
            if (!stopsInZone.contains(stop.getStopFacility().getId())) {
                outCnt++;
            }
        }
        return outCnt;
    }
}
