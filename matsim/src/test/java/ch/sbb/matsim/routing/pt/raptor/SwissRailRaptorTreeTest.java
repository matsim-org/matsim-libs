/* *********************************************************************** *
 * project: org.matsim.* 												   *
 *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2023 by the members listed in the COPYING,        *
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
package ch.sbb.matsim.routing.pt.raptor;

import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorCore.TravelInfo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Tests for the tree-calculating functionality of SwissRailRaptor
 *
 * @author mrieser / SBB
 */
public class SwissRailRaptorTreeTest {

	@Test
	void testSingleStop_dep0740atN_optimized() {
        Fixture f = new Fixture();
        f.init();

        RaptorStaticConfig config = RaptorUtils.createStaticConfig(f.config);
        config.setOptimization(RaptorStaticConfig.RaptorOptimization.OneToAllRouting);
        SwissRailRaptorData data = SwissRailRaptorData.create(f.scenario.getTransitSchedule(), null, config, f.scenario.getNetwork(), null);
        SwissRailRaptor raptor = new SwissRailRaptor.Builder(data,f.scenario.getConfig()).build();

        RaptorParameters raptorParams = RaptorUtils.createParameters(f.config);

        // start with a stop on the green line
        TransitStopFacility fromStop = f.schedule.getFacilities().get(Id.create(23, TransitStopFacility.class));
        double depTime = 7*3600 + 40*60;
        Map<Id<TransitStopFacility>, TravelInfo> map = raptor.calcTree(fromStop, depTime, raptorParams, null);

        Assertions.assertEquals(f.schedule.getFacilities().size(), map.size(), "wrong number of reached stops.");

        assertTravelInfo(map, 0 , "23", 1, "07:41:00", "08:14:07"); // transfer at C, 7:50/8:02 blue, walk at A
        assertTravelInfo(map, 1 , "23", 1, "07:41:00", "08:14:00"); // transfer at C, 7:50/8:02 blue
        assertTravelInfo(map, 2 , "23", 1, "07:41:00", "08:09:07"); // transfer at C, 7:50/8:02 blue, walk at B
        assertTravelInfo(map, 3 , "23", 1, "07:41:00", "08:09:00"); // transfer at C, 7:50/8:02 blue
        assertTravelInfo(map, 4 , "23", 0, "07:41:00", "07:50:04"); // transfer at C, 7:50, walk 3.12 (-> 4) seconds (2.6 meters)
        assertTravelInfo(map, 5 , "23", 0, "07:41:00", "07:50:04"); // transfer at C, 7:50, walk 3.12 (-> 4) seconds (2.6 meters)
        assertTravelInfo(map, 6 , "23", 1, "07:41:00", "08:09:00"); // transfer at C, 7:50/8:02 blue
        assertTravelInfo(map, 7 , "23", 1, "07:41:00", "08:09:07"); // transfer at C, 7:50/8:02 blue, walk at D
        assertTravelInfo(map, 8 , "23", 1, "07:41:00", "08:16:00"); // transfer at C, 7:50/8:02 blue
        assertTravelInfo(map, 9 , "23", 1, "07:41:00", "08:16:07"); // transfer at C, 7:50/8:02 blue, walk at E
        assertTravelInfo(map, 10, "23", 1, "07:41:00", "08:23:00"); // transfer at C, 7:50/8:02 blue (travelling to 11 and transferring would be faster, but not cheaper!
        assertTravelInfo(map, 11, "23", 2, "07:41:00", "08:19:00"); // transfer at C, 7:50/8:00 red, transfer at G, 8:09/8:12
        assertTravelInfo(map, 12, "23", 1, "07:41:00", "08:09:00"); // transfer at C, 7:50/8:00 red
        assertTravelInfo(map, 13, "23", 1, "07:41:00", "08:09:07"); // transfer at C, 7:50/8:00 red, walk 4 meters / 7 seconds
        assertTravelInfo(map, 14, "23", 2, "07:41:00", "08:19:00"); // transfer at C, 7:50/8:00 red, transfer at G, 8:09/8:12
        assertTravelInfo(map, 15, "23", 2, "07:41:00", "08:19:07"); // same as [14], then walk
        assertTravelInfo(map, 16, "23", 2, "07:41:00", "08:24:00"); // transfer at C, 7:50/8:00 red, transfer at G, 8:09/8:12
        assertTravelInfo(map, 17, "23", 2, "07:41:00", "08:24:07"); // same as [16], then walk
        assertTravelInfo(map, 18, "23", 0, "07:41:00", "07:50:00"); // directly reachable
        assertTravelInfo(map, 19, "23", 1, "07:41:00", "08:01:00"); // transfer at C, 7:50/7:51 green
        assertTravelInfo(map, 20, "23", 1, "07:41:00", "08:11:00"); // transfer at C, 7:50/7:51 green
        assertTravelInfo(map, 21, "23", 1, "07:41:00", "08:09:04"); // transfer at C, 7:50/8:00 red, walk 2.6 meters / 4 seconds
        assertTravelInfo(map, 22, "23", 2, "07:41:00", "08:21:00"); // transfer at C, 7:50/8:00 red, transfer at G 8:09/8:11
        assertTravelInfo(map, 23, "23", 0, "07:40:00", "07:40:00"); // our start location
    }

	@Test
	void testSingleStop_dep0740atN_unoptimized() {
        Fixture f = new Fixture();
        f.init();

        SwissRailRaptorData data = SwissRailRaptorData.create(f.scenario.getTransitSchedule(), null, RaptorUtils.createStaticConfig(f.config), f.scenario.getNetwork(), null);
        SwissRailRaptor raptor = new SwissRailRaptor.Builder(data, f.scenario.getConfig()).build();

        RaptorParameters raptorParams = RaptorUtils.createParameters(f.config);

        // start with a stop on the green line
        TransitStopFacility fromStop = f.schedule.getFacilities().get(Id.create(23, TransitStopFacility.class));
        double depTime = 7*3600 + 40*60;
        Map<Id<TransitStopFacility>, TravelInfo> map = raptor.calcTree(fromStop, depTime, raptorParams, null);

        Assertions.assertEquals(20, map.size(), "wrong number of reached stops.");

        Assertions.assertNull(map.get(Id.create(0, TransitStopFacility.class))); // unreachable
        assertTravelInfo(map, 1 , "23", 1, "07:41:00", "08:14:00"); // transfer at C, 7:50/8:02 blue
        Assertions.assertNull(map.get(Id.create(2, TransitStopFacility.class))); // unreachable
        assertTravelInfo(map, 3 , "23", 1, "07:41:00", "08:09:00"); // transfer at C, 7:50/8:02 blue
        assertTravelInfo(map, 4 , "23", 0, "07:41:00", "07:50:04"); // transfer at C, 7:50, walk 3.12 (-> 4) seconds (2.6 meters)
        assertTravelInfo(map, 5 , "23", 0, "07:41:00", "07:50:04"); // transfer at C, 7:50, walk 3.12 (-> 4) seconds (2.6 meters)
        assertTravelInfo(map, 6 , "23", 1, "07:41:00", "08:09:00"); // transfer at C, 7:50/8:02 blue
        assertTravelInfo(map, 7 , "23", 2, "07:41:00", "08:33:00"); // transfer at C, 7:50/8:00 red, transfer at G 8:09/8.12
        assertTravelInfo(map, 8 , "23", 1, "07:41:00", "08:16:00"); // transfer at C, 7:50/8:02 blue
        assertTravelInfo(map, 9 , "23", 2, "07:41:00", "08:26:00"); // transfer at C, 7:50/8:00 red, transfer at G 8:09/8.12
        assertTravelInfo(map, 10, "23", 1, "07:41:00", "08:23:00"); // transfer at C, 7:50/8:02 blue (travelling to 11 and transferring would be faster, but not cheaper!
        assertTravelInfo(map, 11, "23", 2, "07:41:00", "08:19:00"); // transfer at C, 7:50/8:00 red, transfer at G, 8:09/8:12
        assertTravelInfo(map, 12, "23", 1, "07:41:00", "08:09:00"); // transfer at C, 7:50/8:00 red
        assertTravelInfo(map, 13, "23", 1, "07:41:00", "08:09:07"); // transfer at C, 7:50/8:00 red, walk 4 meters / 7 seconds
        assertTravelInfo(map, 14, "23", 2, "07:41:00", "08:19:00"); // transfer at C, 7:50/8:00 red, transfer at G, 8:09/8:12
        Assertions.assertNull(map.get(Id.create(15, TransitStopFacility.class))); // unreachable
        assertTravelInfo(map, 16, "23", 2, "07:41:00", "08:24:00"); // transfer at C, 7:50/8:00 red, transfer at G, 8:09/8:12
        Assertions.assertNull(map.get(Id.create(17, TransitStopFacility.class))); // unreachable
        assertTravelInfo(map, 18, "23", 0, "07:41:00", "07:50:00"); // directly reachable
        assertTravelInfo(map, 19, "23", 1, "07:41:00", "08:01:00"); // transfer at C, 7:50/7:51 green
        assertTravelInfo(map, 20, "23", 1, "07:41:00", "08:11:00"); // transfer at C, 7:50/7:51 green
        assertTravelInfo(map, 21, "23", 1, "07:41:00", "08:09:04"); // transfer at C, 7:50/8:00 red, walk 2.6 meters / 3.12 (-> 4) seconds
        assertTravelInfo(map, 22, "23", 2, "07:41:00", "08:21:00"); // transfer at C, 7:50/8:00 red, transfer at G 8:09/8:11
        assertTravelInfo(map, 23, "23", 0, "07:40:00", "07:40:00"); // our start location
    }

	@Test
	void testSingleStop_dep0750atN_optimized() {
        Fixture f = new Fixture();
        f.init();

        RaptorStaticConfig config = RaptorUtils.createStaticConfig(f.config);
        config.setOptimization(RaptorStaticConfig.RaptorOptimization.OneToAllRouting);
        SwissRailRaptorData data = SwissRailRaptorData.create(f.scenario.getTransitSchedule(), null, config, f.scenario.getNetwork(), null);
        SwissRailRaptor raptor = new SwissRailRaptor.Builder(data, f.scenario.getConfig()).build();

        RaptorParameters raptorParams = RaptorUtils.createParameters(f.config);

        // start with a stop on the green line
        TransitStopFacility fromStop = f.schedule.getFacilities().get(Id.create(23, TransitStopFacility.class));
        double depTime = 7*3600 + 50*60;
        Map<Id<TransitStopFacility>, TravelInfo> map = raptor.calcTree(fromStop, depTime, raptorParams, null);

        // latest departure on green line is at 07:51, so we'll miss some stops!
        Assertions.assertEquals(21, map.size(), "wrong number of reached stops.");

        assertTravelInfo(map, 0 , "23", 1, "07:51:00", "08:14:07"); // same as [1], then walk
        assertTravelInfo(map, 1 , "23", 1, "07:51:00", "08:14:00"); // transfer at C, 8:00/8:02 blue
        assertTravelInfo(map, 2 , "23", 1, "07:51:00", "08:09:07"); // same as [3], then walk
        assertTravelInfo(map, 3 , "23", 1, "07:51:00", "08:09:00"); // transfer at C, 8:00/8:02 blue
        assertTravelInfo(map, 4 , "23", 0, "07:51:00", "08:00:04"); // transfer at C, 8:00, walk 3.12 (-> 4) seconds (2 meters)
        assertTravelInfo(map, 5 , "23", 0, "07:51:00", "08:00:04"); // transfer at C, 8:00, walk 3.12 (-> 4) seconds (2 meters)
        assertTravelInfo(map, 6 , "23", 1, "07:51:00", "08:09:00"); // transfer at C, 8:00/8:02 blue
        assertTravelInfo(map, 7 , "23", 1, "07:51:00", "08:09:07"); // same as [6], then walk
        assertTravelInfo(map, 8 , "23", 1, "07:51:00", "08:16:00"); // transfer at C, 8:00/8:02 blue
        assertTravelInfo(map, 9 , "23", 1, "07:51:00", "08:16:07"); // same as [8], then walk
        assertTravelInfo(map, 10, "23", 1, "07:51:00", "08:23:00"); // transfer at C, 8:00/8:02 blue
        assertTravelInfo(map, 11, "23", 1, "07:51:00", "08:23:07"); // same as [10], then walk
        assertTravelInfo(map, 12, "23", 1, "07:51:00", "08:28:00"); // transfer at C, 8:00/8:02 blue
        assertTravelInfo(map, 13, "23", 1, "07:51:00", "08:28:07"); // transfer at C, 8:00/8:02 blue, walk (4 meters) (transfer to red line is allowed)
        assertTravelInfo(map, 14, "23", 1, "07:51:00", "08:39:00"); // transfer at C, 8:00/8:02 blue
        assertTravelInfo(map, 15, "23", 1, "07:51:00", "08:39:07"); // same as [14], then walk
        assertTravelInfo(map, 16, "23", 1, "07:51:00", "08:44:00"); // transfer at C, 8:00/8:02 blue
        assertTravelInfo(map, 17, "23", 1, "07:51:00", "08:44:07"); // same as [16], then walk
        assertTravelInfo(map, 18, "23", 0, "07:51:00", "08:00:00"); // directly reachable
        Assertions.assertNull(map.get(Id.create(19, TransitStopFacility.class))); // unreachable
        Assertions.assertNull(map.get(Id.create(20, TransitStopFacility.class)));
        assertTravelInfo(map, 21, "23", 1, "07:51:00", "08:28:04"); // transfer at C, 8:00/8:01 green, walk 2 meters / 3.12 (-> 4) seconds
        Assertions.assertNull(map.get(Id.create(22, TransitStopFacility.class)));
        assertTravelInfo(map, 23, "23", 0, "07:50:00", "07:50:00"); // our start location
    }

	@Test
	void testSingleStop_dep0750atN_unoptimized() {
        Fixture f = new Fixture();
        f.init();

        SwissRailRaptorData data = SwissRailRaptorData.create(f.scenario.getTransitSchedule(), null, RaptorUtils.createStaticConfig(f.config), f.scenario.getNetwork(), null);
        SwissRailRaptor raptor = new SwissRailRaptor.Builder(data, f.scenario.getConfig()).build();

        RaptorParameters raptorParams = RaptorUtils.createParameters(f.config);

        // start with a stop on the green line
        TransitStopFacility fromStop = f.schedule.getFacilities().get(Id.create(23, TransitStopFacility.class));
        double depTime = 7*3600 + 50*60;
        Map<Id<TransitStopFacility>, TravelInfo> map = raptor.calcTree(fromStop, depTime, raptorParams, null);

        // latest departure on green line is at 07:51, so we'll miss some stops!
        Assertions.assertEquals(14, map.size(), "wrong number of reached stops.");

        Assertions.assertNull(map.get(Id.create(0, TransitStopFacility.class))); // unreachable
        assertTravelInfo(map, 1 , "23", 1, "07:51:00", "08:14:00"); // transfer at C, 8:00/8:02 blue
        Assertions.assertNull(map.get(Id.create(2, TransitStopFacility.class))); // unreachable
        assertTravelInfo(map, 3 , "23", 1, "07:51:00", "08:09:00"); // transfer at C, 8:00/8:02 blue
        assertTravelInfo(map, 4 , "23", 0, "07:51:00", "08:00:04"); // transfer at C, 8:00, walk 3.12 (-> 4) seconds (2.6 meters)
        assertTravelInfo(map, 5 , "23", 0, "07:51:00", "08:00:04"); // transfer at C, 8:00, walk 3.12 (-> 4) seconds (2.6 meters)
        assertTravelInfo(map, 6 , "23", 1, "07:51:00", "08:09:00"); // transfer at C, 8:00/8:02 blue
        Assertions.assertNull(map.get(Id.create(7, TransitStopFacility.class))); // unreachable, no more departures at C(red) or G(blue)
        assertTravelInfo(map, 8 , "23", 1, "07:51:00", "08:16:00"); // transfer at C, 8:00/8:02 blue
        Assertions.assertNull(map.get(Id.create(9, TransitStopFacility.class))); // unreachable, no more departures at C(red) or G(blue)
        assertTravelInfo(map, 10, "23", 1, "07:51:00", "08:23:00"); // transfer at C, 8:00/8:02 blue
        Assertions.assertNull(map.get(Id.create(11, TransitStopFacility.class))); // unreachable, no more departures at C(red) or G(blue)
        assertTravelInfo(map, 12, "23", 1, "07:51:00", "08:28:00"); // transfer at C, 8:00/8:02 blue
        assertTravelInfo(map, 13, "23", 1, "07:51:00", "08:28:07"); // transfer at C, 8:00/8:02 blue, walk (4 meters) (transfer to red line is allowed)
        assertTravelInfo(map, 14, "23", 1, "07:51:00", "08:39:00"); // transfer at C, 8:00/8:02 blue
        Assertions.assertNull(map.get(Id.create(15, TransitStopFacility.class))); // unreachable
        assertTravelInfo(map, 16, "23", 1, "07:51:00", "08:44:00"); // transfer at C, 8:00/8:02 blue
        Assertions.assertNull(map.get(Id.create(17, TransitStopFacility.class))); // unreachable
        assertTravelInfo(map, 18, "23", 0, "07:51:00", "08:00:00"); // directly reachable
        Assertions.assertNull(map.get(Id.create(19, TransitStopFacility.class))); // unreachable
        Assertions.assertNull(map.get(Id.create(20, TransitStopFacility.class)));
        assertTravelInfo(map, 21, "23", 1, "07:51:00", "08:28:04"); // transfer at C, 8:00/8:01 green, walk 2.6 meters / 4 seconds
        Assertions.assertNull(map.get(Id.create(22, TransitStopFacility.class)));
        assertTravelInfo(map, 23, "23", 0, "07:50:00", "07:50:00"); // our start location
    }

	@Test
	void testMultipleStops_optimized() {
        Fixture f = new Fixture();
        f.init();

        RaptorStaticConfig config = RaptorUtils.createStaticConfig(f.config);
        config.setOptimization(RaptorStaticConfig.RaptorOptimization.OneToAllRouting);
        SwissRailRaptorData data = SwissRailRaptorData.create(f.scenario.getTransitSchedule(), null, config, f.scenario.getNetwork(), null);
        SwissRailRaptor raptor = new SwissRailRaptor.Builder(data, f.scenario.getConfig()).build();

        RaptorParameters raptorParams = RaptorUtils.createParameters(f.config);

        // start at B and H
        TransitStopFacility fromStopB = f.schedule.getFacilities().get(Id.create(2, TransitStopFacility.class));
        TransitStopFacility fromStopH = f.schedule.getFacilities().get(Id.create(15, TransitStopFacility.class));
        double depTime = 7*3600 + 30*60;
        List<TransitStopFacility> fromStops = new ArrayList<>();
        fromStops.add(fromStopB);
        fromStops.add(fromStopH);
        Map<Id<TransitStopFacility>, TravelInfo> map = raptor.calcTree(fromStops, depTime, raptorParams, null);

        Assertions.assertEquals(f.schedule.getFacilities().size(), map.size(), "wrong number of reached stops.");

        assertTravelInfo(map, 0 ,  "2", 0, "07:49:00", "07:54:07"); // initial transfer at B (not counted), walk at A
        assertTravelInfo(map, 1 ,  "2", 0, "07:49:00", "07:54:00"); // initial transfer at B (not counted)
        assertTravelInfo(map, 2 ,  "2", 0, "07:30:00", "07:30:00"); // from B, we started here
        assertTravelInfo(map, 3 ,  "2", 0, "07:30:07", "07:30:07"); // from B, walk
        assertTravelInfo(map, 4 ,  "2", 0, "07:33:00", "07:38:00"); // from B, directly reachable
        assertTravelInfo(map, 5 ,  "2", 0, "07:33:00", "07:38:07"); // same as [4], then walk
        assertTravelInfo(map, 6 ,  "2", 0, "07:33:00", "07:49:00"); // from B, directly reachable
        assertTravelInfo(map, 7 ,  "2", 0, "07:33:00", "07:49:07"); // same as [6], then walk
        assertTravelInfo(map, 8 ,  "2", 0, "07:33:00", "07:56:00"); // from B, directly reachable
        assertTravelInfo(map, 9 ,  "2", 0, "07:33:00", "07:56:07"); // same as [8], then walk
        assertTravelInfo(map, 10,  "2", 0, "07:33:00", "08:03:00"); // from B, directly reachable (would be faster from H, but not cheaper due to the transfer penalty at the end)
        assertTravelInfo(map, 11, "15", 0, "07:43:00", "07:59:00"); // from H, directly reachable
        assertTravelInfo(map, 12, "15", 0, "07:43:00", "07:48:07"); // same as [13], then walk
        assertTravelInfo(map, 13, "15", 0, "07:43:00", "07:48:00"); // from H, directly reachable
        assertTravelInfo(map, 14, "15", 0, "07:30:07", "07:30:07"); // same as [15], then walk
        assertTravelInfo(map, 15, "15", 0, "07:30:00", "07:30:00"); // from H, we started here
        assertTravelInfo(map, 16, "15", 0, "07:39:00", "07:44:00"); // from H[14]
        assertTravelInfo(map, 17, "15", 0, "07:39:00", "07:44:07"); // same as [16], then walk
        assertTravelInfo(map, 18,  "2", 0, "07:33:00", "07:38:04"); // same as [2], then walk
        assertTravelInfo(map, 19,  "2", 1, "07:33:00", "07:51:00"); // from B, transfer at C, 7:38/7:41 green
        assertTravelInfo(map, 20,  "2", 1, "07:33:00", "08:01:00"); // from B, transfer at C, 7:38/7:41 green
        assertTravelInfo(map, 21, "15", 0, "07:43:00", "07:48:04"); // from H, transfer at G (walk)
        assertTravelInfo(map, 22, "15", 1, "07:43:00", "08:01:00"); // from H, transfer at G, 7:48/7:51 green
        assertTravelInfo(map, 23, "15", 1, "07:43:00", "08:11:00"); // from H, transfer at G, 7:48/7:51 green
    }

	@Test
	void testMultipleStops_unoptimized() {
        Fixture f = new Fixture();
        f.init();

        SwissRailRaptorData data = SwissRailRaptorData.create(f.scenario.getTransitSchedule(), null, RaptorUtils.createStaticConfig(f.config), f.scenario.getNetwork(), null);
        SwissRailRaptor raptor = new SwissRailRaptor.Builder(data, f.scenario.getConfig()).build();

        RaptorParameters raptorParams = RaptorUtils.createParameters(f.config);

        // start at B and H
        TransitStopFacility fromStopB = f.schedule.getFacilities().get(Id.create(2, TransitStopFacility.class));
        TransitStopFacility fromStopH = f.schedule.getFacilities().get(Id.create(15, TransitStopFacility.class));
        double depTime = 7*3600 + 30*60;
        List<TransitStopFacility> fromStops = new ArrayList<>();
        fromStops.add(fromStopB);
        fromStops.add(fromStopH);
        Map<Id<TransitStopFacility>, TravelInfo> map = raptor.calcTree(fromStops, depTime, raptorParams, null);

        Assertions.assertEquals(22, map.size(), "wrong number of reached stops.");

        Assertions.assertNull(map.get(Id.create(0, TransitStopFacility.class))); // unreachable
        assertTravelInfo(map, 1 , "15", 0, "07:43:00", "08:34:00"); // from H, directly reachable
        assertTravelInfo(map, 2 ,  "2", 0, "07:30:00", "07:30:00"); // from B, we started here
        assertTravelInfo(map, 3 , "15", 0, "07:43:00", "08:29:00"); // from H, directly reachable
        assertTravelInfo(map, 4 ,  "2", 0, "07:33:00", "07:38:00"); // from B, directly reachable
        assertTravelInfo(map, 5 , "15", 0, "07:43:00", "08:18:00"); // from H, directly reachable
        assertTravelInfo(map, 6 ,  "2", 0, "07:33:00", "07:49:00"); // from B, directly reachable
        assertTravelInfo(map, 7 , "15", 0, "07:43:00", "08:13:00"); // from H, directly reachable
        assertTravelInfo(map, 8 ,  "2", 0, "07:33:00", "07:56:00"); // from B, directly reachable
        assertTravelInfo(map, 9 , "15", 0, "07:43:00", "08:06:00"); // from H, directly reachable
        assertTravelInfo(map, 10,  "2", 0, "07:33:00", "08:03:00"); // from B, directly reachable
        assertTravelInfo(map, 11, "15", 0, "07:43:00", "07:59:00"); // from H, directly reachable
        assertTravelInfo(map, 12,  "2", 0, "07:33:00", "08:08:00"); // from B, directly reachable
        assertTravelInfo(map, 13, "15", 0, "07:43:00", "07:48:00"); // from H, directly reachable
        assertTravelInfo(map, 14,  "2", 0, "07:33:00", "08:19:00"); // from B, directly reachable
        assertTravelInfo(map, 15, "15", 0, "07:30:00", "07:30:00"); // from H, we started here
        assertTravelInfo(map, 16,  "2", 0, "07:33:00", "08:24:00"); // from B, directly reachable
        Assertions.assertNull(map.get(Id.create(17, TransitStopFacility.class))); // unreachable
        assertTravelInfo(map, 18,  "2", 0, "07:33:00", "07:38:04"); // from B, transfer at C, 7:38 (walk 2.6m)
        assertTravelInfo(map, 19,  "2", 1, "07:33:00", "07:51:00"); // from B, transfer at C, 7:38/7:41 green
        assertTravelInfo(map, 20,  "2", 1, "07:33:00", "08:01:00"); // from B, transfer at C, 7:38/7:41 green
        assertTravelInfo(map, 21, "15", 0, "07:43:00", "07:48:04"); // from H, transfer at G (walk)
        assertTravelInfo(map, 22, "15", 1, "07:43:00", "08:01:00"); // from H, transfer at G, 7:48/7:51 green
        assertTravelInfo(map, 23, "15", 1, "07:43:00", "08:11:00"); // from H, transfer at G, 7:48/7:51 green
    }

	@Test
	void testSingleStop_costs_dep0740atN_optimized() {
        Fixture f = new Fixture();
        f.init();

        RaptorStaticConfig config = RaptorUtils.createStaticConfig(f.config);
        config.setOptimization(RaptorStaticConfig.RaptorOptimization.OneToAllRouting);
        SwissRailRaptorData data = SwissRailRaptorData.create(f.scenario.getTransitSchedule(), null, config, f.scenario.getNetwork(), null);
        SwissRailRaptor raptor = new SwissRailRaptor.Builder(data, f.scenario.getConfig()).build();

        RaptorParameters raptorParams = RaptorUtils.createParameters(f.config);

        // start with a stop on the green line
        TransitStopFacility fromStop = f.schedule.getFacilities().get(Id.create(23, TransitStopFacility.class));
        double depTime = 7*3600 + 40*60;
        Map<Id<TransitStopFacility>, TravelInfo> map = raptor.calcTree(fromStop, depTime, raptorParams, null);

        Id<TransitStopFacility> stop19id = Id.create(19, TransitStopFacility.class);
        assertTravelInfo(map, 19, "23", 1, "07:41:00", "08:01:00"); // transfer at C, 7:50/7:51 green
        TravelInfo info0740 = map.get(stop19id);
        TravelInfo info0739 = raptor.calcTree(fromStop, 7*3600 + 39*60, raptorParams, null).get(stop19id);

        Assertions.assertEquals(info0740.ptDepartureTime, info0739.ptDepartureTime, 0.0, "departure time should be the same.");
        Assertions.assertEquals(info0740.ptArrivalTime, info0739.ptArrivalTime, 0.0, "arrival time should be the same.");
        Assertions.assertEquals(info0740.ptTravelTime, info0739.ptTravelTime, 0.0, "travel time should be the same.");
        Assertions.assertEquals(info0740.accessTime, info0739.accessTime, 0.0, "access time should be independent of waiting time.");
        Assertions.assertEquals(info0740.accessCost, info0739.accessCost, 0.0, "access cost should be independent of waiting time.");
        Assertions.assertEquals(info0740.travelCost, info0739.travelCost, 0.0, "travel cost should be independent of waiting time.");

        Assertions.assertEquals(info0740.waitingTime, info0739.waitingTime - 60, 0.0, "waiting time should differ by 1 minute");
        Assertions.assertTrue(info0740.waitingCost < info0739.waitingCost, "waiting cost should differ");
    }

	@Test
	void testSingleStop_raptorroute_dep0740atN_optimized() {
        Fixture f = new Fixture();
        f.init();

        RaptorStaticConfig config = RaptorUtils.createStaticConfig(f.config);
        config.setOptimization(RaptorStaticConfig.RaptorOptimization.OneToAllRouting);
        SwissRailRaptorData data = SwissRailRaptorData.create(f.scenario.getTransitSchedule(), null, config, f.scenario.getNetwork(), null);
        SwissRailRaptor raptor = new SwissRailRaptor.Builder(data, f.scenario.getConfig()).build();

        RaptorParameters raptorParams = RaptorUtils.createParameters(f.config);

        // start with a stop on the green line
        TransitStopFacility fromStop = f.schedule.getFacilities().get(Id.create(23, TransitStopFacility.class));
        double depTime = 7*3600 + 40*60;
        Map<Id<TransitStopFacility>, TravelInfo> map = raptor.calcTree(fromStop, depTime, raptorParams, null);

        Id<TransitStopFacility> stop2id = Id.create(2, TransitStopFacility.class);
        assertTravelInfo(map, 2 , "23", 1, "07:41:00", "08:09:07"); // transfer at C, 7:50/8:02 blue, walk at B
        TravelInfo info = map.get(stop2id);

        RaptorRoute route = info.getRaptorRoute();
        Assertions.assertNotNull(route);
        List<RaptorRoute.RoutePart> parts = new ArrayList<>();
        for (RaptorRoute.RoutePart part : route.getParts()) {
            parts.add(part);
        }

        Assertions.assertEquals(5, parts.size());
        RaptorRoute.RoutePart stage1 = parts.get(0);
        RaptorRoute.RoutePart stage2 = parts.get(1);
        RaptorRoute.RoutePart stage3 = parts.get(2);
        RaptorRoute.RoutePart stage4 = parts.get(3);
        RaptorRoute.RoutePart stage5 = parts.get(4);


        Assertions.assertNull(stage1.line); // access walk
        Assertions.assertEquals(0, stage1.distance, 0.0);
        Assertions.assertEquals(0, stage1.arrivalTime - stage1.depTime, 0.0);

        Assertions.assertEquals("green", stage2.line.getId().toString());
        Assertions.assertEquals(TransportMode.pt, stage2.mode);
        Assertions.assertEquals(540, stage2.arrivalTime - stage2.boardingTime, 1e-7);
        Assertions.assertEquals(10000, stage2.distance, 1e-7);

        Assertions.assertNull(stage3.line); // transfer

        Assertions.assertEquals("blue", stage4.line.getId().toString());
        Assertions.assertEquals(TransportMode.pt, stage4.mode);
        Assertions.assertEquals(660, stage4.arrivalTime - stage4.boardingTime, 1e-7);
        Assertions.assertEquals(5000, stage4.distance, 1e-7);

        Assertions.assertNull(stage5.line); // egress_walk
    }

    private void assertTravelInfo(Map<Id<TransitStopFacility>, TravelInfo> map, int stopId, String expectedDepartureStop, int expectedTransfers, String expectedDepartureTime, String expectedArrivalTime) {
        TravelInfo info = map.get(Id.create(stopId, TransitStopFacility.class));
        Assertions.assertNotNull(info, "Stop " + stopId + " is not reachable.");
        Assertions.assertEquals(expectedDepartureStop, info.departureStop.toString(), "wrong departure stop");
        Assertions.assertEquals(expectedTransfers, info.transferCount, "wrong number of transfers");
        Assertions.assertEquals(Time.parseTime(expectedArrivalTime), Math.floor(info.ptArrivalTime), 0.0, "unexpected arrival time: " + Time.writeTime(info.ptArrivalTime));
        Assertions.assertEquals(Time.parseTime(expectedDepartureTime), Math.floor(info.ptDepartureTime), 0.0, "unexpected departure time: " + Time.writeTime(info.ptDepartureTime));
    }

}
