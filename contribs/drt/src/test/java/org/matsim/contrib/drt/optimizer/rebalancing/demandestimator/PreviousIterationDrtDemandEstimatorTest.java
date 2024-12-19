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

package org.matsim.contrib.drt.optimizer.rebalancing.demandestimator;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.common.zones.Zone;
import org.matsim.contrib.common.zones.ZoneImpl;
import org.matsim.contrib.common.zones.ZoneSystem;
import org.matsim.contrib.common.zones.ZoneSystemImpl;
import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingParams;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.core.network.NetworkUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author michalm (Michal Maciejewski)
 */
public class PreviousIterationDrtDemandEstimatorTest {

	private static final int ESTIMATION_PERIOD = 1800;

	private final Network network = createNetwork();

	private final Link link1 = network.getLinks().get(Id.createLinkId("link_1"));
	private final Link link2 = network.getLinks().get(Id.createLinkId("link_2"));

	private final Zone zone1 = ZoneImpl.createDummyZone(Id.create("zone_1", Zone.class), new Coord());
	private final Zone zone2 = ZoneImpl.createDummyZone(Id.create("zone_2", Zone.class), new Coord());
	private final ZoneSystem zonalSystem = new ZoneSystemImpl(List.of(zone1, zone2), coord -> {
        if(coord == link1.getToNode().getCoord()) {
            return Optional.of(zone1);
        } else if(coord == link2.getToNode().getCoord()) {
            return Optional.of(zone2);
        } else {
            throw new RuntimeException();
        }
    }, network);

	@Test
	void noDepartures() {
		PreviousIterationDrtDemandEstimator estimator = createEstimator();

		//no events in previous iterations
		estimator.reset(1);

		assertDemand(estimator, 0, zone1, 0);
		assertDemand(estimator, 2000, zone1, 0);
		assertDemand(estimator, 4000, zone1, 0);
		assertDemand(estimator, 0, zone2, 0);
		assertDemand(estimator, 2000, zone2, 0);
		assertDemand(estimator, 4000, zone2, 0);
	}

	@Test
	void drtDepartures() {
		PreviousIterationDrtDemandEstimator estimator = createEstimator();

		//time bin 0-1800
		estimator.handleEvent(departureEvent(100, link1, TransportMode.drt));
		estimator.handleEvent(departureEvent(200, link1, TransportMode.drt));
		estimator.handleEvent(departureEvent(500, link2, TransportMode.drt));
		estimator.handleEvent(departureEvent(1500, link1, TransportMode.drt));
		//time bin 1800-3600
		estimator.handleEvent(departureEvent(2500, link1, TransportMode.drt));
		//time bin 3600-5400
		estimator.handleEvent(departureEvent(4000, link2, TransportMode.drt));
		//time bin 5400-7200
		estimator.handleEvent(departureEvent(7000, link1, TransportMode.drt));
		estimator.handleEvent(departureEvent(7100, link2, TransportMode.drt));
		estimator.reset(1);

		//time bin 0-1800
		assertDemand(estimator, 0, zone1, 3);
		assertDemand(estimator, 0, zone2, 1);
		//time bin 1800-3600
		assertDemand(estimator, 1800, zone1, 1);
		assertDemand(estimator, 1800, zone2, 0);
		//time bin 3600-5400
		assertDemand(estimator, 3600, zone1, 0);
		assertDemand(estimator, 3600, zone2, 1);
		//time bin 5400-7200
		assertDemand(estimator, 5400, zone1, 1);
		assertDemand(estimator, 5400, zone2, 1);
		//time bin 7200-9000
		assertDemand(estimator, 7200, zone1, 0);
		assertDemand(estimator, 7200, zone2, 0);
	}

	@Test
	void nonDrtDepartures() {
		PreviousIterationDrtDemandEstimator estimator = createEstimator();

		estimator.handleEvent(departureEvent(100, link1, "mode X"));
		estimator.handleEvent(departureEvent(200, link2, TransportMode.car));
		estimator.reset(1);

		assertDemand(estimator, 0, zone1, 0);
		assertDemand(estimator, 0, zone2, 0);
	}

	@Test
	void currentCountsAreCopiedToPreviousAfterReset() {
		PreviousIterationDrtDemandEstimator estimator = createEstimator();

		estimator.handleEvent(departureEvent(100, link1, TransportMode.drt));
		estimator.handleEvent(departureEvent(200, link2, TransportMode.drt));

		assertDemand(estimator, 0, zone1, 0);
		assertDemand(estimator, 0, zone2, 0);

		estimator.reset(1);

		assertDemand(estimator, 0, zone1, 1);
		assertDemand(estimator, 0, zone2, 1);
	}

	@Test
	void timeBinsAreRespected() {
		PreviousIterationDrtDemandEstimator estimator = createEstimator();

		estimator.handleEvent(departureEvent(100, link1, TransportMode.drt));
		estimator.handleEvent(departureEvent(2200, link2, TransportMode.drt));
		estimator.reset(1);

		assertDemand(estimator, 0, zone1, 1);
		assertDemand(estimator, 1799, zone1, 1);
		assertDemand(estimator, 1800, zone1, 0);

		assertDemand(estimator, 1799, zone2, 0);
		assertDemand(estimator, 1800, zone2, 1);
		assertDemand(estimator, 3599, zone2, 1);
		assertDemand(estimator, 3600, zone2, 0);
	}

	@Test
	void noTimeLimitIsImposed() {
		PreviousIterationDrtDemandEstimator estimator = createEstimator();

		estimator.handleEvent(departureEvent(10000000, link1, TransportMode.drt));
		estimator.reset(1);

		assertDemand(estimator, 10000000, zone1, 1);
	}

	private PreviousIterationDrtDemandEstimator createEstimator() {
		RebalancingParams rebalancingParams = new RebalancingParams();
		rebalancingParams.interval = ESTIMATION_PERIOD;

		DrtConfigGroup drtConfigGroup = new DrtConfigGroup();
		drtConfigGroup.addParameterSet(rebalancingParams);

		return new PreviousIterationDrtDemandEstimator(zonalSystem, drtConfigGroup, ESTIMATION_PERIOD);
	}

	private PersonDepartureEvent departureEvent(double time, Link link, String mode) {
		return new PersonDepartureEvent(time, null, link.getId(), mode, mode);
	}

	private void assertDemand(PreviousIterationDrtDemandEstimator estimator, double fromTime, Zone zone,
			double expectedDemand) {
		assertThat(estimator.getExpectedDemand(fromTime, ESTIMATION_PERIOD).applyAsDouble(zone)).isEqualTo(
				expectedDemand);
	}

	static Network createNetwork() {
		Network network = NetworkUtils.createNetwork();
		Node a = network.getFactory().createNode(Id.createNodeId("a"), new Coord());
		Node b = network.getFactory().createNode(Id.createNodeId("b"), new Coord());
		network.addNode(a);
		network.addNode(b);

		Link ab = network.getFactory().createLink(Id.createLinkId("link_1"), a, b);
		Link ba = network.getFactory().createLink(Id.createLinkId("link_2"), b, a);
		network.addLink(ab);
		network.addLink(ba);
		return network;
	}
}
