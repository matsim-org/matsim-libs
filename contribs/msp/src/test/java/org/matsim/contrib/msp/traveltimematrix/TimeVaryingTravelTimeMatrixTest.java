package org.matsim.contrib.msp.traveltimematrix;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.common.zones.ZoneSystem;
import org.matsim.contrib.msp.traveltimematrix.congestion.CongestionIndex;
import org.matsim.contrib.msp.traveltimematrix.congestion.CongestionIndexProfile;
import org.matsim.contrib.zone.skims.TravelTimeMatrix;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the matrix itself: that the free-speed travel time is scaled by the index of the right time bin,
 * of the right road classes, by the right factor.
 * <p>
 * The free-speed base is a stub returning a fixed travel time, so that every number asserted here is the
 * doing of the decorator and not of the DVRP matrix underneath it.
 */
class TimeVaryingTravelTimeMatrixTest {

	private static final int FREE_SPEED_TRAVEL_TIME = 600;

	@Test
	void freeFlowLeavesTheFreeSpeedTravelTimeAlone() {

		TimeVaryingTravelTimeMatrix matrix = TimeVaryingTravelTimeMatrix.global(freeSpeedMatrix(),
			CongestionIndex.freeFlow(), config());

		assertEquals(FREE_SPEED_TRAVEL_TIME, matrix.getTravelTime(null, null, 8 * 3600));
	}

	@Test
	void theTravelTimeIsTheFreeSpeedOneTimesOnePlusTheIndex() {

		// 0.2 in the bin the trip is in: 600 s of free speed take 720 s
		TimeVaryingTravelTimeMatrix matrix = TimeVaryingTravelTimeMatrix.global(freeSpeedMatrix(),
			CongestionIndex.constant(0.2), config());

		assertEquals(720, matrix.getTravelTime(null, null, 8 * 3600));
	}

	@Test
	void theIndexIsReadAtTheMiddleOfTheTrip() {

		// one bin per hour, congested only in the hour from 08:00
		double[] hourly = new double[24];
		hourly[8] = 1.0;
		CongestionIndexProfile profile = new CongestionIndexProfile(3600,
			Map.of(RoadClassification.ALL, hourly));

		TravelTimeMatrixConfigGroup midpoint = config();
		midpoint.setEvaluateIndexAt(TravelTimeMatrixConfigGroup.EvaluateIndexAt.MIDPOINT);
		TimeVaryingTravelTimeMatrix matrix = TimeVaryingTravelTimeMatrix.global(freeSpeedMatrix(), profile, midpoint);

		// departing at 07:55, so the trip is mostly inside the congested hour: 07:55 + 5 min is 08:00
		assertEquals(2 * FREE_SPEED_TRAVEL_TIME, matrix.getTravelTime(null, null, 7 * 3600 + 55 * 60));
		// departing at 07:40 the middle of the trip is 07:50, still in the free-flowing hour
		assertEquals(FREE_SPEED_TRAVEL_TIME, matrix.getTravelTime(null, null, 7 * 3600 + 40 * 60));
		// departing at 08:55 the middle is 09:00, out of the peak again
		assertEquals(FREE_SPEED_TRAVEL_TIME, matrix.getTravelTime(null, null, 8 * 3600 + 55 * 60));
	}

	@Test
	void theDepartureTimeVariantReadsTheIndexAtTheStart() {

		double[] hourly = new double[24];
		hourly[8] = 1.0;
		CongestionIndexProfile profile = new CongestionIndexProfile(3600, Map.of(RoadClassification.ALL, hourly));

		TravelTimeMatrixConfigGroup atDeparture = config();
		atDeparture.setEvaluateIndexAt(TravelTimeMatrixConfigGroup.EvaluateIndexAt.DEPARTURE);
		TimeVaryingTravelTimeMatrix matrix = TimeVaryingTravelTimeMatrix.global(freeSpeedMatrix(), profile,
			atDeparture);

		assertEquals(FREE_SPEED_TRAVEL_TIME, matrix.getTravelTime(null, null, 7 * 3600 + 55 * 60));
		assertEquals(2 * FREE_SPEED_TRAVEL_TIME, matrix.getTravelTime(null, null, 8 * 3600 + 55 * 60));
	}

	@Test
	void theCalibrationFactorScalesTheIndexAndNotTheTravelTime() {

		TravelTimeMatrixConfigGroup halved = config();
		halved.setCalibrationFactor(0.5);

		TimeVaryingTravelTimeMatrix matrix = TimeVaryingTravelTimeMatrix.global(freeSpeedMatrix(),
			CongestionIndex.constant(0.2), halved);

		// 600 * (1 + 0.5 * 0.2), not 0.5 * 600 * 1.2
		assertEquals(660, matrix.getTravelTime(null, null, 0));
	}

	@Test
	void aPairOfZeroTravelTimeStaysZero() {

		TravelTimeMatrix zero = (from, to, departureTime) -> 0;
		TimeVaryingTravelTimeMatrix matrix = TimeVaryingTravelTimeMatrix.global(zero,
			CongestionIndex.constant(10), config());

		assertEquals(0, matrix.getTravelTime(null, null, 8 * 3600));
	}

	@Test
	void levelOneWeighsTheIndexByTheRoadClassesOfThePair() {

		Network network = TestNetworks.corridor();
		ZoneSystem zoneSystem = TestNetworks.oneZonePerNode(network);
		RoadClassification roadClasses = RoadClassification.byHighwayType(network);
		RoadClassShares shares = RoadClassSharesCalculator.calculateFreeSpeedShares(network, zoneSystem, roadClasses,
			1, 1);

		// a free-flowing motorway and a badly congested residential network
		Map<String, double[]> byClass = new LinkedHashMap<>();
		byClass.put(RoadClassification.ALL, new double[]{0.5});
		byClass.put("motorway", new double[]{0.0});
		byClass.put("residential", new double[]{1.0});
		byClass.put(RoadClassification.OTHER, new double[]{0.0});
		CongestionIndexProfile profile = new CongestionIndexProfile(86400, byClass);

		TimeVaryingTravelTimeMatrix matrix = TimeVaryingTravelTimeMatrix.byRoadClass(freeSpeedMatrix(), profile,
			shares, config());

		Node n1 = network.getNodes().get(Id.createNodeId("n1"));
		Node n2 = network.getNodes().get(Id.createNodeId("n2"));
		Node n3 = network.getNodes().get(Id.createNodeId("n3"));
		Node n4 = network.getNodes().get(Id.createNodeId("n4"));

		// n2 to n3 is all motorway, which is free flowing: no slowdown at all
		assertEquals(FREE_SPEED_TRAVEL_TIME, matrix.getTravelTime(n2, n3, 0));
		// n1 to n2 is all residential, which is twice as slow
		assertEquals(2 * FREE_SPEED_TRAVEL_TIME, matrix.getTravelTime(n1, n2, 0));
		// n1 to n4 is 202 of 223 s residential, so the factor is 1 + 202/223
		double expected = FREE_SPEED_TRAVEL_TIME * (1 + 202.0 / 223);
		assertEquals(expected, matrix.getTravelTime(n1, n4, 0), FREE_SPEED_TRAVEL_TIME / 255.0 + 1);
		// and that is strictly between the two pure cases, which is the whole point of level 1
		assertTrue(matrix.getTravelTime(n2, n3, 0) < matrix.getTravelTime(n1, n4, 0));
		assertTrue(matrix.getTravelTime(n1, n4, 0) < matrix.getTravelTime(n1, n2, 0));
	}

	@Test
	void levelOneRefusesAnIndexThatDoesNotCoverTheRoadClasses() {

		Network network = TestNetworks.corridor();
		ZoneSystem zoneSystem = TestNetworks.oneZonePerNode(network);
		RoadClassShares shares = RoadClassSharesCalculator.calculateFreeSpeedShares(network, zoneSystem,
			RoadClassification.byHighwayType(network), 1, 1);

		// an index measured without a road class split: level 1 has nothing to weigh with
		CongestionIndexProfile global = new CongestionIndexProfile(86400,
			Map.of(RoadClassification.ALL, new double[]{0.5}));

		IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
			() -> TimeVaryingTravelTimeMatrix.byRoadClass(freeSpeedMatrix(), global, shares, config()));
		assertTrue(e.getMessage().contains("motorway"), e.getMessage());
	}

	@Test
	void theFreeSpeedTravelTimeStaysAvailable() {

		TimeVaryingTravelTimeMatrix matrix = TimeVaryingTravelTimeMatrix.global(freeSpeedMatrix(),
			CongestionIndex.constant(0.5), config());

		assertEquals(FREE_SPEED_TRAVEL_TIME, matrix.getFreeSpeedTravelTime(null, null));
		assertEquals(1.5, matrix.congestionFactor(null, null, 0, FREE_SPEED_TRAVEL_TIME), 1e-12);
	}

	@Test
	void theIndexItReadsIsTheOneItWasGiven() {
		CongestionIndex index = CongestionIndex.constant(0.3);
		assertSame(index, TimeVaryingTravelTimeMatrix.global(freeSpeedMatrix(), index, config()).getCongestionIndex());
	}

	/**
	 * A base matrix of one fixed free-speed travel time, so that the decorator is the only thing under
	 * test. It ignores the nodes, which is why most tests here can pass null for them.
	 */
	private static TravelTimeMatrix freeSpeedMatrix() {
		return (from, to, departureTime) -> FREE_SPEED_TRAVEL_TIME;
	}

	private static TravelTimeMatrixConfigGroup config() {
		TravelTimeMatrixConfigGroup config = new TravelTimeMatrixConfigGroup();
		config.setEvaluateIndexAt(TravelTimeMatrixConfigGroup.EvaluateIndexAt.MIDPOINT);
		return config;
	}
}
