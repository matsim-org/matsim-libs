/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
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

package org.matsim.core.mobsim.qsim.qnetsimengine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimBuilder;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkChangeEvent.ChangeType;
import org.matsim.core.network.NetworkChangeEvent.ChangeValue;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

/**
 * Characterises what a {@link NetworkChangeEvent} carrying a lanes change actually does to the QSim.
 * <p>
 * The existing coverage in {@code org.matsim.integration.timevariantnetworks.QSimIntegrationTest} exercises
 * freespeed and flow capacity but never lanes, so the behaviour asserted here was previously untested.
 * <p>
 * The mechanism is not obvious from reading {@link QueueWithBuffer#calculateStorageCapacity()} alone, because that
 * method reads a cached {@code effectiveNumberOfLanesUsedInQsim} field and the time-dependent lookup next to it is
 * commented out. The cache is refreshed by {@link QLinkImpl#recalcTimeVariantAttributes()}, which pushes
 * {@code getNumberOfLanes(now)} into the lane before recalculating -- "flowCap &amp; nLanes are 'push', freeSpeed is
 * 'pull'", as the source comment there puts it.
 *
 * @author pieterfourie
 */
class NetworkChangeEventLanesTest {

	private static final double LENGTH = 1000.0;
	private static final double FREESPEED = 10.0;
	private static final double CAPACITY = 1800.0;
	private static final double INITIAL_LANES = 2.0;
	private static final double REDUCED_LANES = 1.0;

	/** {@code NetworkImpl.DEFAULT_EFFECTIVE_CELL_SIZE}, the length in metres that one queued vehicle occupies. */
	private static final double EFFECTIVE_CELL_SIZE = 7.5;

	private static final double CHANGE_TIME = 3600.0;
	private static final Id<Link> LINK_ID = Id.create("1", Link.class);

	/**
	 * The gate: a lanes change must reach the QSim's storage capacity, otherwise nothing can be built on top of it.
	 * <p>
	 * Storage capacity is {@code length * lanes / effectiveCellSize * storageCapFactor}. The link is deliberately long
	 * and fast enough that neither the buffer floor nor the slow-link floor in
	 * {@link QueueWithBuffer#calculateStorageCapacity()} clamps the result: the slow-link floor here is
	 * {@code (1000/10) * 0.5 = 50} vehicles, well below both expected values.
	 */
	@Test
	void lanesChangeAltersQsimStorageCapacity() {
		Fixture f = new Fixture();

		assertEquals(LENGTH * INITIAL_LANES / EFFECTIVE_CELL_SIZE, f.qlink.getSpaceCap(), MatsimTestUtils.EPSILON,
			"before the change event is due, storage capacity should reflect the link's two lanes");

		f.qsim.getSimTimer().setTime(CHANGE_TIME);
		f.qlink.recalcTimeVariantAttributes();

		assertEquals(LENGTH * REDUCED_LANES / EFFECTIVE_CELL_SIZE, f.qlink.getSpaceCap(), MatsimTestUtils.EPSILON,
			"after the lanes change is due, storage capacity should have halved");
	}

	/**
	 * The lanes change must not disturb flow capacity. Storage and flow are independent in the network file, and a
	 * kerb-parking model that reduces lanes needs to know it is not silently throttling throughput as well.
	 */
	@Test
	void lanesChangeLeavesFlowCapacityAlone() {
		Fixture f = new Fixture();
		double flowCapBefore = f.qlink.getSimulatedFlowCapacityPerTimeStep();

		f.qsim.getSimTimer().setTime(CHANGE_TIME);
		f.qlink.recalcTimeVariantAttributes();

		assertEquals(flowCapBefore, f.qlink.getSimulatedFlowCapacityPerTimeStep(), MatsimTestUtils.EPSILON,
			"a lanes-only change event must not alter flow capacity");
	}

	/**
	 * The time-variant link itself must report the changed value; this is the "pull" half of the mechanism, and it
	 * failing would point at the network layer rather than at the QSim.
	 */
	@Test
	void timeVariantLinkReportsTheChangedLaneCount() {
		Fixture f = new Fixture();

		assertEquals(INITIAL_LANES, f.link.getNumberOfLanes(CHANGE_TIME - 1), MatsimTestUtils.EPSILON,
			"before the change event is due the link should still report two lanes");
		assertEquals(REDUCED_LANES, f.link.getNumberOfLanes(CHANGE_TIME), MatsimTestUtils.EPSILON,
			"from the change event onwards the link should report one lane");
	}

	/**
	 * The within-day path that a kerb-parking model would use: the change event does not exist when the mobsim starts,
	 * and is injected once a vehicle parks. {@code NetworkChangeEventsEngine#addNetworkChangeEvent} registers the event
	 * on the network and, when its start time has already passed, applies it straight away; this reproduces that pair
	 * of steps without needing to stub {@code InternalInterface}.
	 */
	@Test
	void eventInjectedDuringTheSimTakesEffectImmediately() {
		Fixture f = new Fixture(false);

		f.qsim.getSimTimer().setTime(7200.0);
		assertEquals(LENGTH * INITIAL_LANES / EFFECTIVE_CELL_SIZE, f.qlink.getSpaceCap(), MatsimTestUtils.EPSILON,
			"with no change event registered, storage capacity should still reflect two lanes");

		NetworkChangeEvent injected = new NetworkChangeEvent(7200.0);
		injected.addLink(f.link);
		injected.setLanesChange(new ChangeValue(ChangeType.ABSOLUTE_IN_SI_UNITS, REDUCED_LANES));
		NetworkUtils.addNetworkChangeEvent(f.network, injected);
		f.qlink.recalcTimeVariantAttributes();

		assertEquals(LENGTH * REDUCED_LANES / EFFECTIVE_CELL_SIZE, f.qlink.getSpaceCap(), MatsimTestUtils.EPSILON,
			"an event injected mid-simulation for the current time should apply at once");
	}

	/**
	 * Several change events on one link at one second all apply, and each attribute keeps its own value.
	 * <p>
	 * Change events are grouped by start time, so an event no longer evicts an earlier one at the same second. The
	 * events at a given time are applied in registration order and collapse into a single value per attribute, which
	 * keeps the times strictly increasing for the binary search in {@code getValue}.
	 */
	@Test
	void twoEventsAtTheSameSecondOnOneLinkBothApply() {
		Fixture f = new Fixture(false);

		NetworkChangeEvent lanes = new NetworkChangeEvent(1800.0);
		lanes.addLink(f.link);
		lanes.setLanesChange(new ChangeValue(ChangeType.ABSOLUTE_IN_SI_UNITS, REDUCED_LANES));
		NetworkUtils.addNetworkChangeEvent(f.network, lanes);

		NetworkChangeEvent freespeed = new NetworkChangeEvent(1800.0);
		freespeed.addLink(f.link);
		freespeed.setFreespeedChange(new ChangeValue(ChangeType.ABSOLUTE_IN_SI_UNITS, FREESPEED / 2.0));
		NetworkUtils.addNetworkChangeEvent(f.network, freespeed);

		assertEquals(REDUCED_LANES, f.link.getNumberOfLanes(1800.0), MatsimTestUtils.EPSILON,
			"the lanes event must survive a same-second event on another attribute");
		assertEquals(FREESPEED / 2.0, f.link.getFreespeed(1800.0), MatsimTestUtils.EPSILON,
			"the freespeed event must apply too");

		assertEquals(INITIAL_LANES, f.link.getNumberOfLanes(0.0), MatsimTestUtils.EPSILON,
			"before the change time the link keeps its base lane count");
		assertEquals(FREESPEED, f.link.getFreespeed(0.0), MatsimTestUtils.EPSILON,
			"before the change time the link keeps its base freespeed");
	}

	/**
	 * Two events at one second changing the <em>same</em> attribute are applied in registration order, so the later
	 * one wins for an absolute change. They still collapse into a single value for that time.
	 */
	@Test
	void twoEventsAtTheSameSecondOnOneAttributeApplyInOrder() {
		Fixture f = new Fixture(false);

		NetworkChangeEvent first = new NetworkChangeEvent(1800.0);
		first.addLink(f.link);
		first.setLanesChange(new ChangeValue(ChangeType.ABSOLUTE_IN_SI_UNITS, INITIAL_LANES));
		NetworkUtils.addNetworkChangeEvent(f.network, first);

		NetworkChangeEvent second = new NetworkChangeEvent(1800.0);
		second.addLink(f.link);
		second.setLanesChange(new ChangeValue(ChangeType.OFFSET_IN_SI_UNITS, -1.0));
		NetworkUtils.addNetworkChangeEvent(f.network, second);

		assertEquals(INITIAL_LANES - 1.0, f.link.getNumberOfLanes(1800.0), MatsimTestUtils.EPSILON,
			"the offset must be applied on top of the absolute change registered before it");
		assertEquals(INITIAL_LANES, f.link.getNumberOfLanes(0.0), MatsimTestUtils.EPSILON,
			"before the change time the link keeps its base lane count");
	}

	private static final class Fixture {
		private final Link link;
		private final Network network;
		private final QSim qsim;
		private final QLinkImpl qlink;

		/**
		 * @param withPreloadedLaneReduction register a lanes change at {@link #CHANGE_TIME} before the mobsim is built
		 */
		private Fixture(boolean withPreloadedLaneReduction) {
			Config config = ConfigUtils.createConfig();
			config.network().setTimeVariantNetwork(true);

			MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(config);
			this.network = scenario.getNetwork();
			this.network.setCapacityPeriod(3600.0);

			Node node1 = NetworkUtils.createAndAddNode(this.network, Id.create("1", Node.class), new Coord(0, 0));
			Node node2 = NetworkUtils.createAndAddNode(this.network, Id.create("2", Node.class), new Coord(LENGTH, 0));
			this.link = NetworkUtils.createAndAddLink(this.network, LINK_ID, node1, node2, LENGTH, FREESPEED, CAPACITY,
				INITIAL_LANES);

			if (withPreloadedLaneReduction) {
				NetworkChangeEvent laneReduction = new NetworkChangeEvent(CHANGE_TIME);
				laneReduction.addLink(this.link);
				laneReduction.setLanesChange(new ChangeValue(ChangeType.ABSOLUTE_IN_SI_UNITS, REDUCED_LANES));
				NetworkUtils.addNetworkChangeEvent(this.network, laneReduction);
			}

			EventsManager eventsManager = EventsUtils.createEventsManager();
			this.qsim = new QSimBuilder(config).useDefaults().build(scenario, eventsManager);
			this.qlink = (QLinkImpl) this.qsim.getNetsimNetwork().getNetsimLink(LINK_ID);
		}

		private Fixture() {
			this(true);
		}
	}
}
