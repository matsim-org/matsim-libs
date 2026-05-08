/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package org.matsim.contrib.bicycle.network;

import org.junit.jupiter.api.Test;
import org.matsim.contrib.osm.networkReader.SupersonicOsmNetworkReader.Direction;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.matsim.contrib.bicycle.network.BicycleInfraCategory.*;

/**
 * Tests for {@link BicycleInfraClassifier}. The tests are table-driven: each
 * case builds a small OSM-tag map, runs it through the classifier, and asserts
 * the expected {@link BicycleInfraCategory}.
 *
 * <p>The classifier's precedence (first match wins, top to bottom) is the spec.
 * The {@code precedence_*} tests pin down a few critical orderings: changing
 * the rule order in {@code classify()} would break those tests, which is the
 * point.
 */
public class BicycleInfraClassifierTest {

	private final BicycleInfraClassifier classifier = new BicycleInfraClassifier();


	// =========================================================================
	// 1. CYCLEWAY_ON_HIGHWAY_PROTECTED (PBL)
	// =========================================================================

	@Test
	void cyclewayOnHighwayProtected_byPhysicalSeparationLeft() {
		Map<String, String> tags = tags(
			"highway", "cycleway",
			"is_sidepath", "yes",
			"separation:left", "bollard"
		);
		assertEquals(CYCLEWAY_ON_HIGHWAY_PROTECTED, classifier.classify(tags, Direction.Forward));
	}

	@Test
	void cyclewayOnHighwayProtected_byParkingAsSeparation() {
		Map<String, String> tags = tags(
			"highway", "cycleway",
			"is_sidepath", "yes",
			"traffic_mode:left", "parking"
		);
		assertEquals(CYCLEWAY_ON_HIGHWAY_PROTECTED, classifier.classify(tags, Direction.Forward));
	}

	@Test
	void cyclewayOnHighwayProtected_notProtectedWithoutSidepath() {
		// Same tags, but is_sidepath missing → not classified as protected.
		Map<String, String> tags = tags(
			"highway", "cycleway",
			"separation:left", "bollard"
		);
		BicycleInfraCategory result = classifier.classify(tags, Direction.Forward);
		// Must NOT be PROTECTED. (Whatever else it ends up classified as is fine.)
		assertNotEquals(CYCLEWAY_ON_HIGHWAY_PROTECTED, result,
			"without is_sidepath=yes, separation:left alone should not yield PROTECTED, got " + result);
	}


	// =========================================================================
	// 2. CYCLEWAY_LINK
	// =========================================================================

	@Test
	void cyclewayLink_basicCase() {
		Map<String, String> tags = tags(
			"highway", "cycleway",
			"cycleway", "link"
		);
		assertEquals(CYCLEWAY_LINK, classifier.classify(tags, Direction.Forward));
	}


	// =========================================================================
	// 3. CROSSING
	// =========================================================================

	@Test
	void crossing_cyclewayCrossing() {
		Map<String, String> tags = tags(
			"highway", "cycleway",
			"cycleway", "crossing"
		);
		assertEquals(CROSSING, classifier.classify(tags, Direction.Forward));
	}

	@Test
	void crossing_pathWithBicycleDesignated() {
		Map<String, String> tags = tags(
			"highway", "path",
			"path", "crossing",
			"bicycle", "designated"
		);
		assertEquals(CROSSING, classifier.classify(tags, Direction.Forward));
	}

	@Test
	void crossing_footwayCrossingWithoutBicycle_isNotCrossing() {
		// footway=crossing without bicycle=yes/designated must not become CROSSING.
		Map<String, String> tags = tags(
			"highway", "footway",
			"footway", "crossing"
		);
		BicycleInfraCategory result = classifier.classify(tags, Direction.Forward);
		assertNotEquals(CROSSING, result,
			"footway crossing without bicycle access must not be CROSSING, got " + result);
	}


	// =========================================================================
	// 4. BICYCLE_ROAD / BICYCLE_ROAD_VEHICLE_DESTINATION
	// =========================================================================

	@Test
	void bicycleRoad_byTag() {
		Map<String, String> tags = tags(
			"highway", "residential",
			"bicycle_road", "yes"
		);
		assertEquals(BICYCLE_ROAD, classifier.classify(tags, Direction.Forward));
	}

	@Test
	void bicycleRoad_byTrafficSignDE244() {
		Map<String, String> tags = tags(
			"highway", "residential",
			"traffic_sign", "DE:244"
		);
		assertEquals(BICYCLE_ROAD, classifier.classify(tags, Direction.Forward));
	}

	@Test
	void bicycleRoadVehicleDestination_byVehicleDestination() {
		Map<String, String> tags = tags(
			"highway", "residential",
			"bicycle_road", "yes",
			"vehicle", "destination"
		);
		assertEquals(BICYCLE_ROAD_VEHICLE_DESTINATION, classifier.classify(tags, Direction.Forward));
	}

	@Test
	void bicycleRoadVehicleDestination_byTrafficSignKfzFrei() {
		Map<String, String> tags = tags(
			"highway", "residential",
			"bicycle_road", "yes",
			"traffic_sign", "DE:244, Kfz-Verkehr frei"
		);
		assertEquals(BICYCLE_ROAD_VEHICLE_DESTINATION, classifier.classify(tags, Direction.Forward));
	}


	// =========================================================================
	// 5. SHARED_BUS_LANE_*
	// =========================================================================

	@Test
	void sharedBusLane_busWithBike_byCyclewayShareBusway() {
		Map<String, String> tags = tags(
			"highway", "cycleway",
			"cycleway", "share_busway"
		);
		assertEquals(SHARED_BUS_LANE_BUS_WITH_BIKE, classifier.classify(tags, Direction.Forward));
	}

	@Test
	void sharedBusLane_bikeWithBus_byLaneShareBusway() {
		Map<String, String> tags = tags(
			"highway", "cycleway",
			"lane", "share_busway"
		);
		assertEquals(SHARED_BUS_LANE_BIKE_WITH_BUS, classifier.classify(tags, Direction.Forward));
	}


	// =========================================================================
	// 6. PEDESTRIAN_AREA_BICYCLE_YES
	// =========================================================================

	@Test
	void pedestrianAreaBicycleYes_basic() {
		Map<String, String> tags = tags(
			"highway", "pedestrian",
			"bicycle", "yes"
		);
		assertEquals(PEDESTRIAN_AREA_BICYCLE_YES, classifier.classify(tags, Direction.Forward));
	}

	@Test
	void pedestrianAreaBicycleYes_designated() {
		Map<String, String> tags = tags(
			"highway", "pedestrian",
			"bicycle", "designated"
		);
		assertEquals(PEDESTRIAN_AREA_BICYCLE_YES, classifier.classify(tags, Direction.Forward));
	}


	// =========================================================================
	// 7. SHARED_MOTOR_VEHICLE_LANE
	// =========================================================================

	@Test
	void sharedMotorVehicleLane_bySideTagRight() {
		Map<String, String> tags = tags(
			"highway", "secondary",
			"cycleway:right", "shared_lane"
		);
		assertEquals(SHARED_MOTOR_VEHICLE_LANE, classifier.classify(tags, Direction.Forward));
	}


	// =========================================================================
	// 8. CYCLEWAY_ON_HIGHWAY_BETWEEN_LANES (Angstweiche)
	// =========================================================================

	@Test
	void cyclewayBetweenLanes_byCyclewayLanesPipe() {
		Map<String, String> tags = tags(
			"highway", "primary",
			"cycleway:lanes", "|lane|"
		);
		assertEquals(CYCLEWAY_ON_HIGHWAY_BETWEEN_LANES, classifier.classify(tags, Direction.Forward));
	}


	// =========================================================================
	// 9. CYCLEWAY_ON_HIGHWAY_ADVISORY / EXCLUSIVE
	// =========================================================================

	@Test
	void cyclewayOnHighwayAdvisory_bySideRightLane() {
		Map<String, String> tags = tags(
			"highway", "secondary",
			"cycleway:right", "lane",
			"cycleway:right:lane", "advisory"
		);
		assertEquals(CYCLEWAY_ON_HIGHWAY_ADVISORY, classifier.classify(tags, Direction.Forward));
	}

	@Test
	void cyclewayOnHighwayExclusive_bySideRightLane() {
		Map<String, String> tags = tags(
			"highway", "secondary",
			"cycleway:right", "lane",
			"cycleway:right:lane", "exclusive"
		);
		assertEquals(CYCLEWAY_ON_HIGHWAY_EXCLUSIVE, classifier.classify(tags, Direction.Forward));
	}

	@Test
	void cyclewayOnHighwayAdvisoryOrExclusive_unspecifiedLaneType() {
		Map<String, String> tags = tags(
			"highway", "secondary",
			"cycleway:right", "lane"
		);
		assertEquals(CYCLEWAY_ON_HIGHWAY_ADVISORY_OR_EXCLUSIVE, classifier.classify(tags, Direction.Forward));
	}


	// =========================================================================
	// 10. CYCLEWAY_ADJOINING / ISOLATED / ADJOINING_OR_ISOLATED
	// =========================================================================

	@Test
	void cyclewayAdjoining_byTrackAndIsSidepathYes() {
		Map<String, String> tags = tags(
			"highway", "cycleway",
			"cycleway", "track",
			"is_sidepath", "yes"
		);
		assertEquals(CYCLEWAY_ADJOINING, classifier.classify(tags, Direction.Forward));
	}

	@Test
	void cyclewayIsolated_byTrackAndIsSidepathNo() {
		Map<String, String> tags = tags(
			"highway", "cycleway",
			"cycleway", "track",
			"is_sidepath", "no"
		);
		assertEquals(CYCLEWAY_ISOLATED, classifier.classify(tags, Direction.Forward));
	}

	@Test
	void cyclewayAdjoiningOrIsolated_byTrackWithoutIsSidepath() {
		Map<String, String> tags = tags(
			"highway", "cycleway",
			"cycleway", "track"
		);
		assertEquals(CYCLEWAY_ADJOINING_OR_ISOLATED, classifier.classify(tags, Direction.Forward));
	}


	// =========================================================================
	// 11. FOOT_AND_CYCLEWAY_*
	// =========================================================================

	@Test
	void footAndCyclewaySegregated_bySegregatedYes() {
		Map<String, String> tags = tags(
			"highway", "path",
			"foot", "designated",
			"bicycle", "designated",
			"segregated", "yes"
		);
		// no is_sidepath → ADJOINING_OR_ISOLATED suffix
		assertEquals(FOOT_AND_CYCLEWAY_SEGREGATED_ADJOINING_OR_ISOLATED, classifier.classify(tags, Direction.Forward));
	}

	@Test
	void footAndCyclewayShared_bySegregatedNoAndIsSidepathYes() {
		Map<String, String> tags = tags(
			"highway", "path",
			"foot", "designated",
			"bicycle", "designated",
			"segregated", "no",
			"is_sidepath", "yes"
		);
		assertEquals(FOOT_AND_CYCLEWAY_SHARED_ADJOINING, classifier.classify(tags, Direction.Forward));
	}


	// =========================================================================
	// 12. FOOTWAY_BICYCLE_YES*
	// =========================================================================

	@Test
	void footwayBicycleYes_byBicycleYes() {
		Map<String, String> tags = tags(
			"highway", "footway",
			"bicycle", "yes"
		);
		assertEquals(FOOTWAY_BICYCLE_YES_ADJOINING_OR_ISOLATED, classifier.classify(tags, Direction.Forward));
	}

	@Test
	void footwayBicycleYesAdjoining_byIsSidepathYes() {
		Map<String, String> tags = tags(
			"highway", "footway",
			"bicycle", "yes",
			"is_sidepath", "yes"
		);
		assertEquals(FOOTWAY_BICYCLE_YES_ADJOINING, classifier.classify(tags, Direction.Forward));
	}

	@Test
	void footwayBicycleYesAdjoining_bySidewalkRightBicycleYes() {
		Map<String, String> tags = tags(
			"highway", "secondary",
			"sidewalk:right:bicycle", "yes"
		);
		assertEquals(FOOTWAY_BICYCLE_YES_ADJOINING, classifier.classify(tags, Direction.Forward));
	}


	// =========================================================================
	// 13. NEEDS_CLARIFICATION
	// =========================================================================

	@Test
	void needsClarification_highwayCyclewayWithoutDetails() {
		// highway=cycleway alone, no further infra-defining tags
		Map<String, String> tags = tags(
			"highway", "cycleway"
		);
		assertEquals(NEEDS_CLARIFICATION, classifier.classify(tags, Direction.Forward));
	}


	// =========================================================================
	// 14. NONE
	// =========================================================================

	@Test
	void none_plainResidentialStreet() {
		Map<String, String> tags = tags(
			"highway", "residential"
		);
		assertEquals(NONE, classifier.classify(tags, Direction.Forward));
	}

	@Test
	void none_emptyTags() {
		assertEquals(NONE, classifier.classify(tags(), Direction.Forward));
	}


	// =========================================================================
	// Precedence — this is where the rule order matters
	// =========================================================================

	@Test
	void precedence_protectedBeatsCyclewayLink() {
		// Both PROTECTED and LINK conditions met → PROTECTED wins (rule 1 > rule 2)
		Map<String, String> tags = tags(
			"highway", "cycleway",
			"cycleway", "link",
			"is_sidepath", "yes",
			"separation:left", "bollard"
		);
		assertEquals(CYCLEWAY_ON_HIGHWAY_PROTECTED, classifier.classify(tags, Direction.Forward));
	}

	@Test
	void precedence_crossingBeatsBicycleRoad() {
		// path=crossing + bicycle=designated AND bicycle_road=yes → CROSSING wins (rule 3 > rule 4)
		Map<String, String> tags = tags(
			"highway", "path",
			"path", "crossing",
			"bicycle", "designated",
			"bicycle_road", "yes"
		);
		assertEquals(CROSSING, classifier.classify(tags, Direction.Forward));
	}

	@Test
	void precedence_bicycleRoadBeatsBusLane() {
		// bicycle_road=yes AND a bus-lane condition → BICYCLE_ROAD wins (rule 4 > rule 5)
		Map<String, String> tags = tags(
			"highway", "cycleway",
			"bicycle_road", "yes",
			"cycleway", "share_busway"
		);
		BicycleInfraCategory result = classifier.classify(tags, Direction.Forward);
		assertTrue(result == BICYCLE_ROAD || result == BICYCLE_ROAD_VEHICLE_DESTINATION,
			"bicycle road must beat bus lane, got " + result);
	}


	// =========================================================================
	// Direction sensitivity
	// =========================================================================

	@Test
	void direction_sidewalkRightBicycleYesOnlyForward() {
		// sidewalk:right:bicycle=yes hits FOOTWAY_BICYCLE_YES_ADJOINING only on Forward
		Map<String, String> tags = tags(
			"highway", "secondary",
			"sidewalk:right:bicycle", "yes"
		);
		assertEquals(FOOTWAY_BICYCLE_YES_ADJOINING, classifier.classify(tags, Direction.Forward));
		assertEquals(NONE, classifier.classify(tags, Direction.Reverse));
	}

	@Test
	void direction_sidewalkLeftBicycleYesOnlyReverse() {
		Map<String, String> tags = tags(
			"highway", "secondary",
			"sidewalk:left:bicycle", "yes"
		);
		assertEquals(NONE, classifier.classify(tags, Direction.Forward));
		assertEquals(FOOTWAY_BICYCLE_YES_ADJOINING, classifier.classify(tags, Direction.Reverse));
	}

	@Test
	void direction_cyclewayBothDirectionsBidirectional() {
		// cycleway:both=track applies to both directions identically
		Map<String, String> tags = tags(
			"highway", "secondary",
			"cycleway:both", "track"
		);
		BicycleInfraCategory forward = classifier.classify(tags, Direction.Forward);
		BicycleInfraCategory reverse = classifier.classify(tags, Direction.Reverse);
		assertEquals(forward, reverse, "cycleway:both must be direction-symmetric");
	}


	// =========================================================================
	// helpers
	// =========================================================================

	/** Build a small key/value map. Pass an even number of strings: k1, v1, k2, v2, ... */
	private static Map<String, String> tags(String... kv) {
		if (kv.length % 2 != 0) {
			throw new IllegalArgumentException("tags() needs an even number of arguments");
		}
		Map<String, String> m = new HashMap<>();
		for (int i = 0; i < kv.length; i += 2) m.put(kv[i], kv[i + 1]);
		return m;
	}
}
