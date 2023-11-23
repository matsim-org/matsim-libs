/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2022 by the members listed in the COPYING,        *
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

package org.matsim.contrib.drt.extension.preplanned.optimizer;

import static java.util.stream.Collectors.toMap;
import static org.matsim.contrib.drt.extension.preplanned.optimizer.PreplannedDrtOptimizer.PreplannedRequest;
import static org.matsim.contrib.drt.extension.preplanned.optimizer.PreplannedDrtOptimizer.PreplannedStop;

import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.drt.extension.preplanned.optimizer.PreplannedDrtOptimizer.PreplannedRequestKey;
import org.matsim.contrib.drt.extension.preplanned.optimizer.PreplannedDrtOptimizer.PreplannedSchedules;
import org.matsim.contrib.drt.extension.preplanned.run.RunPreplannedDrtExample;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;

/**
 * @author Michal Maciejewski (michalm)
 */
public class RunPreplannedDrtExampleIT {
	@Test
	public void testRun() {
		// scenario with 1 shared taxi (max 2 pax) and 10 requests
		URL configUrl = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("dvrp-grid"), "one_shared_taxi_config.xml");

		// create preplanned requests (they will be mapped to drt requests created during simulation)
		var preplannedRequest_0 = new PreplannedRequest(
				new PreplannedRequestKey(Set.of(Id.createPersonId("passenger_0")), Id.createLinkId("114"),
						Id.createLinkId("349")), 0.0, 900.0, 844.4);
		var preplannedRequest_1 = new PreplannedRequest(
				new PreplannedRequestKey(Set.of(Id.createPersonId("passenger_1")), Id.createLinkId("144"),
						Id.createLinkId("437")), 300.0, 1200.0, 1011.8);
		var preplannedRequest_2 = new PreplannedRequest(
				new PreplannedRequestKey(Set.of(Id.createPersonId("passenger_2")), Id.createLinkId("223"),
						Id.createLinkId("347")), 600.0, 1500.0, 1393.7);
		var preplannedRequest_3 = new PreplannedRequest(
				new PreplannedRequestKey(Set.of(Id.createPersonId("passenger_3")), Id.createLinkId("234"),
						Id.createLinkId("119")), 900.0, 1800.0, 1825.0);
		var preplannedRequest_4 = new PreplannedRequest(
				new PreplannedRequestKey(Set.of(Id.createPersonId("passenger_4")), Id.createLinkId("314"),
						Id.createLinkId("260")), 1200.0, 2100.0, 1997.6);
		var preplannedRequest_5 = new PreplannedRequest(
				new PreplannedRequestKey(Set.of(Id.createPersonId("passenger_5")), Id.createLinkId("333"),
						Id.createLinkId("438")), 1500.0, 2400.0, 2349.6);
		var preplannedRequest_6 = new PreplannedRequest(
				new PreplannedRequestKey(Set.of(Id.createPersonId("passenger_6")), Id.createLinkId("325"),
						Id.createLinkId("111")), 1800.0, 2700.0, 2600.2);
		var preplannedRequest_7 = new PreplannedRequest(
				new PreplannedRequestKey(Set.of(Id.createPersonId("passenger_7")), Id.createLinkId("412"),
						Id.createLinkId("318")), 2100.0, 3000.0, 2989.9);
		var preplannedRequest_8 = new PreplannedRequest(
				new PreplannedRequestKey(Set.of(Id.createPersonId("passenger_8")), Id.createLinkId("455"),
						Id.createLinkId("236")), 2400.0, 3300.0, 3110.5);
		var preplannedRequest_9 = new PreplannedRequest(
				new PreplannedRequestKey(Set.of(Id.createPersonId("passenger_9")), Id.createLinkId("139"),
						Id.createLinkId("330")), 2700.0, 3600.0, 3410.5);
		var preplannedRequests = List.of(preplannedRequest_0, preplannedRequest_1, preplannedRequest_2,
				preplannedRequest_3, preplannedRequest_4, preplannedRequest_5, preplannedRequest_6);

		// there is only one shared taxi
		var taxiId = Id.create("shared_taxi_one", DvrpVehicle.class);

		// all requests are assigned to that taxi
		var preplannedRequestsToVehicleId = preplannedRequests.stream()
				.collect(toMap(PreplannedRequest::key, r -> taxiId));

		// the taxi will serve requests one by one (so actually no sharing, but of course we can change the sequence of stops)
		var preplannedStops = preplannedRequests.stream()
				.flatMap(r -> Stream.of(new PreplannedStop(r, true), new PreplannedStop(r, false)))
				.toList();
		var preplannedStopsByVehicleId = Map.of(taxiId, (Queue<PreplannedStop>)new LinkedList<>(preplannedStops));

		var unassignedRequests = Stream.of(preplannedRequest_7, preplannedRequest_8, preplannedRequest_9)
				.collect(toMap(PreplannedRequest::key, r -> r));

		// put all input data together
		var preplannedSchedules = new PreplannedSchedules(preplannedRequestsToVehicleId, preplannedStopsByVehicleId,
				unassignedRequests);

		// run simulation that re-plays the pre-computed vehicle schedules
		RunPreplannedDrtExample.run(configUrl, false, 0, Map.of("drt", preplannedSchedules));
	}
}
