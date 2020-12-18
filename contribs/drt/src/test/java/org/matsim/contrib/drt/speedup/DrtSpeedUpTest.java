/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2020 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package org.matsim.contrib.drt.speedup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.matsim.contrib.drt.speedup.DrtSpeedUp.computeMovingAverage;
import static org.matsim.contrib.drt.speedup.DrtSpeedUp.isTeleportDrtUsers;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.drt.analysis.DrtRequestAnalyzer;
import org.matsim.contrib.drt.analysis.DrtRequestAnalyzer.PerformedRequestEventSequence;
import org.matsim.contrib.drt.passenger.events.DrtRequestSubmittedEvent;
import org.matsim.contrib.drt.speedup.DrtSpeedUpParams.WaitingTimeUpdateDuringSpeedUp;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.DvrpVehicleSpecification;
import org.matsim.contrib.dvrp.fleet.FleetSpecification;
import org.matsim.contrib.dvrp.fleet.FleetSpecificationImpl;
import org.matsim.contrib.dvrp.fleet.ImmutableDvrpVehicleSpecification;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.passenger.PassengerDroppedOffEvent;
import org.matsim.contrib.dvrp.passenger.PassengerPickedUpEvent;
import org.matsim.contrib.dvrp.passenger.PassengerRequestScheduledEvent;
import org.matsim.contrib.util.distance.DistanceUtils;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.network.NetworkUtils;

import com.google.common.collect.ImmutableMap;

/**
 * @author ikaddoura
 * @author michalm (Michal Maciejewski)
 */
public class DrtSpeedUpTest {
	private final DrtSpeedUpParams drtSpeedUpParams = new DrtSpeedUpParams();
	private final ControlerConfigGroup controlerConfig = new ControlerConfigGroup();

	@Test
	public final void test_computeMovingAverage() {
		List<Double> list = List.of(2., 5., 22.);
		assertThat(computeMovingAverage(2, list)).isEqualTo(27. / 2);
		assertThat(computeMovingAverage(3, list)).isEqualTo(29. / 3);
		assertThat(computeMovingAverage(4, list)).isEqualTo(29. / 3);
	}

	@Test
	public void test_isTeleportDrtUsers() {
		drtSpeedUpParams.setFractionOfIterationsSwitchOn(0.1);
		drtSpeedUpParams.setFractionOfIterationsSwitchOff(0.9);
		drtSpeedUpParams.setIntervalDetailedIteration(10);

		controlerConfig.setLastIteration(100);

		assertThat(isTeleportDrtUsers(drtSpeedUpParams, controlerConfig, 0)).isFalse();

		assertThat(isTeleportDrtUsers(drtSpeedUpParams, controlerConfig, 9)).isFalse();
		assertThat(isTeleportDrtUsers(drtSpeedUpParams, controlerConfig, 10)).isFalse();
		assertThat(isTeleportDrtUsers(drtSpeedUpParams, controlerConfig, 11)).isTrue();

		assertThat(isTeleportDrtUsers(drtSpeedUpParams, controlerConfig, 49)).isTrue();
		assertThat(isTeleportDrtUsers(drtSpeedUpParams, controlerConfig, 50)).isFalse();
		assertThat(isTeleportDrtUsers(drtSpeedUpParams, controlerConfig, 51)).isTrue();

		assertThat(isTeleportDrtUsers(drtSpeedUpParams, controlerConfig, 89)).isTrue();
		assertThat(isTeleportDrtUsers(drtSpeedUpParams, controlerConfig, 90)).isFalse();
		assertThat(isTeleportDrtUsers(drtSpeedUpParams, controlerConfig, 91)).isFalse();

		assertThat(isTeleportDrtUsers(drtSpeedUpParams, controlerConfig, 100)).isFalse();
	}

	//Instead of running actual matsim, each iteration is replaced with:
	// 1. stub DrtRequestAnalyzer.getPerformedRequestSequences() (to fake simulation)
	// 2. call iter end event (to trigger drt-speed-up postprocessing)
	// 3. assert currentAvgWaitingTime and currentAvgInVehicleBeelineSpeed

	private static final String MODE = TransportMode.drt;

	private final Network network = NetworkUtils.createNetwork(ConfigUtils.createConfig());
	private final Node nodeA = NetworkUtils.createAndAddNode(network, Id.createNodeId("A"), new Coord(0, 0));
	private final Node nodeB = NetworkUtils.createAndAddNode(network, Id.createNodeId("B"), new Coord(100, 0));
	private final Node nodeC = NetworkUtils.createAndAddNode(network, Id.createNodeId("C"), new Coord(200, 0));
	private final Link linkAB = NetworkUtils.createAndAddLink(network, Id.createLinkId("AB"), nodeA, nodeB, 100, 15, 20,
			1);
	private final Link linkBC = NetworkUtils.createAndAddLink(network, Id.createLinkId("BA"), nodeB, nodeC, 100, 15, 20,
			1);

	private final FleetSpecification fleetSpecification = new FleetSpecificationImpl();
	private final DrtRequestAnalyzer requestAnalyzer = mock(DrtRequestAnalyzer.class);

	@Test
	public void test_useOnlyInitialEstimates_noRegression() {
		//iters 0 & 100 - simulated, iters 1...99 - teleported
		drtSpeedUpParams.setFractionOfIterationsSwitchOn(0.0);
		drtSpeedUpParams.setFractionOfIterationsSwitchOff(1.0);
		drtSpeedUpParams.setIntervalDetailedIteration(100);

		//use always the initial estimates of waiting time (60) and beeline in-vehicle speed (10)
		drtSpeedUpParams.setFirstSimulatedDrtIterationToReplaceInitialDrtPerformanceParams(999);
		drtSpeedUpParams.setInitialWaitingTime(60);
		drtSpeedUpParams.setInitialInVehicleBeelineSpeed(15);

		controlerConfig.setLastIteration(100);

		DrtSpeedUp drtSpeedUp = new DrtSpeedUp(MODE, drtSpeedUpParams, controlerConfig, network, fleetSpecification,
				requestAnalyzer);

		// ===== start iterations:
		assertAverages(drtSpeedUp, 60, 15); //initial values

		// simulated iteration 0
		updateRequestAnalyser(eventSequence("r1", 0, 100, 20));
		iterationEnds(drtSpeedUp, 0);
		assertAverages(drtSpeedUp, 60, 15); // same as initial values

		for (int i = 1; i < 100; i++) {
			// teleported iteration I
			iterationEnds(drtSpeedUp, i);
			assertAverages(drtSpeedUp, 60, 15); // same as initial values
		}

		// simulated iteration 100
		updateRequestAnalyser(eventSequence("r1", 100, 1000, 2));
		iterationEnds(drtSpeedUp, 100);
		assertAverages(drtSpeedUp, 60, 15); // same as initial values
	}

	@Test
	public void test_useAveragesFromLastTwoSimulations_noRegression() {
		//iters 0, 2, 4 - simulated, iters 1, 3 - teleported
		drtSpeedUpParams.setFractionOfIterationsSwitchOn(0.0);
		drtSpeedUpParams.setFractionOfIterationsSwitchOff(1.0);
		drtSpeedUpParams.setIntervalDetailedIteration(2);

		//use the computed estimates of waiting time and beeline in-vehicle speed
		drtSpeedUpParams.setFirstSimulatedDrtIterationToReplaceInitialDrtPerformanceParams(0);
		drtSpeedUpParams.setInitialWaitingTime(60);
		drtSpeedUpParams.setInitialInVehicleBeelineSpeed(15);

		//moving average
		drtSpeedUpParams.setMovingAverageSize(2);

		controlerConfig.setLastIteration(4);

		DrtSpeedUp drtSpeedUp = new DrtSpeedUp(MODE, drtSpeedUpParams, controlerConfig, network, fleetSpecification,
				requestAnalyzer);

		// ===== start iterations:

		assertAverages(drtSpeedUp, 60, 15); //initial values

		// simulated iteration 0
		updateRequestAnalyser(eventSequence("r1", 0, 100, 1));
		iterationEnds(drtSpeedUp, 0);
		assertAverages(drtSpeedUp, 100, 1); // from iter 0

		// teleported iteration 1
		iterationEnds(drtSpeedUp, 1);
		assertAverages(drtSpeedUp, 100, 1); // from iter 0

		// simulated iteration 2
		updateRequestAnalyser(eventSequence("r1", 10, 50, 2));
		iterationEnds(drtSpeedUp, 2);
		assertAverages(drtSpeedUp, 0.5 * (100 + 50), 0.5 * (1 + 2)); // from iter 0 & 2

		// teleported iteration 3
		iterationEnds(drtSpeedUp, 3);
		assertAverages(drtSpeedUp, 0.5 * (100 + 50), 0.5 * (1 + 2)); // from iter 0 & 2

		// simulated iteration 4
		updateRequestAnalyser(eventSequence("r1", 100, 200, 10));
		iterationEnds(drtSpeedUp, 4);
		assertAverages(drtSpeedUp, 0.5 * (50 + 200), 0.5 * (2 + 10)); // from iter 2 & 4
	}

	@Test
	public void test_linearRegression() {
		//iters 0, 2, 4 - simulated, iters 1, 3 - teleported
		drtSpeedUpParams.setFractionOfIterationsSwitchOn(0.0);
		drtSpeedUpParams.setFractionOfIterationsSwitchOff(1.0);
		drtSpeedUpParams.setIntervalDetailedIteration(2);

		//use the computed estimates of waiting time and beeline in-vehicle speed
		drtSpeedUpParams.setFirstSimulatedDrtIterationToReplaceInitialDrtPerformanceParams(0);
		drtSpeedUpParams.setInitialWaitingTime(60);
		drtSpeedUpParams.setInitialInVehicleBeelineSpeed(15);

		//linear regression
		drtSpeedUpParams.setWaitingTimeUpdateDuringSpeedUp(WaitingTimeUpdateDuringSpeedUp.LinearRegression);

		controlerConfig.setLastIteration(5);

		DrtSpeedUp drtSpeedUp = new DrtSpeedUp(MODE, drtSpeedUpParams, controlerConfig, network, fleetSpecification,
				requestAnalyzer);

		// ===== start iterations:

		assertAverages(drtSpeedUp, 60, 15); //initial values

		// simulated iteration 0 (1 rides/vehicle => wait time 100 s)
		fleetSpecification.addVehicleSpecification(vehicleSpecification("vehicle_1"));
		updateRequestAnalyser(eventSequence("r1", 0, 100, 1));
		iterationEnds(drtSpeedUp, 0);
		assertAverages(drtSpeedUp, 100, 1); // from iter 0

		// teleported iteration 1
		iterationEnds(drtSpeedUp, 1);
		assertAverages(drtSpeedUp, 100, 1); // from iter 0

		// simulated iteration 2 (0.5 rides/vehicle => wait time 50 s)
		fleetSpecification.addVehicleSpecification(vehicleSpecification("vehicle_2"));
		updateRequestAnalyser(eventSequence("r1", 10, 50, 2));
		iterationEnds(drtSpeedUp, 2);
		assertAverages(drtSpeedUp, 50, 2); // from iter 2

		// teleported iteration 3 (0.25 rides/vehicle => estimated wait time as 25 s - estimation is run after iteration)
		fleetSpecification.addVehicleSpecification(vehicleSpecification("vehicle_3"));
		fleetSpecification.addVehicleSpecification(vehicleSpecification("vehicle_4"));
		iterationEnds(drtSpeedUp, 3);
		assertAverages(drtSpeedUp, 25, 2); // speed from iter 2; waitTime - regression

		// simulated iteration 4
		updateRequestAnalyser(eventSequence("r1", 100, 200, 10));
		iterationEnds(drtSpeedUp, 4);
		assertAverages(drtSpeedUp, 200, 10); // from iter 4
	}

	private void iterationEnds(DrtSpeedUp drtSpeedUp, int iteration) {
		drtSpeedUp.notifyIterationEnds(new IterationEndsEvent(null, iteration, false));
	}

	//mock request analyser instead of running simulations
	private void updateRequestAnalyser(PerformedRequestEventSequence... eventSequences) {
		var sequences = Arrays.stream(eventSequences)
				.collect(ImmutableMap.toImmutableMap(seq -> seq.getSubmitted().getRequestId(), seq -> seq));
		when(requestAnalyzer.getPerformedRequestSequences()).thenReturn(sequences);
	}

	private PerformedRequestEventSequence eventSequence(String id, double submittedTime, double waitTime,
			double inVehicleSpeed) {
		var requestId = Id.create(id, Request.class);
		var submittedEvent = new DrtRequestSubmittedEvent(submittedTime, MODE, requestId, null, linkAB.getId(),
				linkBC.getId(), Double.NaN, Double.NaN);
		var pickupEvent = new PassengerPickedUpEvent(submittedTime + waitTime, MODE, requestId, null, null);
		double rideTime = DistanceUtils.calculateDistance(linkBC, linkAB) / inVehicleSpeed;
		var dropoffEvent = new PassengerDroppedOffEvent(submittedTime + waitTime + rideTime, MODE, requestId, null,
				null);
		return new PerformedRequestEventSequence(submittedEvent, mock(PassengerRequestScheduledEvent.class),
				pickupEvent, dropoffEvent);
	}

	DvrpVehicleSpecification vehicleSpecification(String id) {
		return ImmutableDvrpVehicleSpecification.newBuilder()
				.id(Id.create(id, DvrpVehicle.class))
				.capacity(1)
				.startLinkId(linkAB.getId())
				.serviceBeginTime(0)
				.serviceEndTime(3600)
				.build();
	}

	private void assertAverages(DrtSpeedUp drtSpeedUp, double waitTime, double inVehicleBeelineSpeed) {
		assertThat(drtSpeedUp.getCurrentAvgWaitingTime()).isEqualTo(waitTime);
		assertThat(drtSpeedUp.getCurrentAvgInVehicleBeelineSpeed()).isEqualTo(inVehicleBeelineSpeed);
	}
}
