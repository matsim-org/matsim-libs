/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2026 by the members listed in the COPYING,        *
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

package org.matsim.contrib.drt.optimizer.rebalancing.mincostflow;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.common.zones.Zone;
import org.matsim.contrib.common.zones.ZoneImpl;
import org.matsim.contrib.drt.optimizer.rebalancing.mincostflow.AggregatedMinCostRelocationCalculator.DrtZoneVehicleSurplus;
import org.matsim.contrib.drt.optimizer.rebalancing.mincostflow.TransportProblem.Flow;

/**
 * @author nkuehnel
 */
public class TransportProblemTest {

	@Test
	void withoutLimitEvenDistantDeficitsAreServed() {
		List<Flow<Zone, Zone>> flows = TransportProblem.solveForVehicleSurplus(
				List.of(surplus("near", 0, 1), surplus("far", 100_000, -1)));

		assertThat(flows).hasSize(1);
		assertThat(flows.get(0).amount()).isEqualTo(1);
		assertThat(flows.get(0).destination().getId()).isEqualTo(Id.create("far", Zone.class));
	}

	@Test
	void aLimitLeavesDeficitsWithoutSurplusInReachUnserved() {
		List<Flow<Zone, Zone>> flows = TransportProblem.solveForVehicleSurplus(
				List.of(surplus("near", 0, 1), surplus("far", 100_000, -1)), 50_000);

		assertThat(flows).isEmpty();
	}

	/**
	 * The limit prunes arcs, it does not reassign flow to a different destination: the vehicle that cannot reach the far
	 * zone stays where it is, it is not sent to a zone that has no deficit.
	 */
	@Test
	void aLimitServesTheReachableDeficitsOnly() {
		List<Flow<Zone, Zone>> flows = TransportProblem.solveForVehicleSurplus(
				List.of(surplus("origin", 0, 2), surplus("reachable", 10_000, -1), surplus("distant", 100_000, -1)),
				50_000);

		assertThat(flows).hasSize(1);
		assertThat(flows.get(0).origin().getId()).isEqualTo(Id.create("origin", Zone.class));
		assertThat(flows.get(0).destination().getId()).isEqualTo(Id.create("reachable", Zone.class));
		assertThat(flows.get(0).amount()).isEqualTo(1);
	}

	/** A limit at exactly the distance of an arc must keep that arc. */
	@Test
	void theLimitIsInclusive() {
		List<DrtZoneVehicleSurplus> surpluses = List.of(surplus("origin", 0, 1), surplus("destination", 50_000, -1));

		assertThat(TransportProblem.solveForVehicleSurplus(surpluses, 50_000)).hasSize(1);
		assertThat(TransportProblem.solveForVehicleSurplus(surpluses, 49_999)).isEmpty();
	}

	private static DrtZoneVehicleSurplus surplus(String zoneId, double x, int surplus) {
		Zone zone = ZoneImpl.createDummyZone(Id.create(zoneId, Zone.class), new Coord(x, 0));
		return new DrtZoneVehicleSurplus(zone, surplus);
	}
}
