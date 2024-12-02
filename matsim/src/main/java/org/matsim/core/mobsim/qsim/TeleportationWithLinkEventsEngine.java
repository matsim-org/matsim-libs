
/* *********************************************************************** *
 * project: org.matsim.*
 * DefaultTeleportationEngine.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

package org.matsim.core.mobsim.qsim;

import jakarta.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.IterationCounter;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.vis.snapshotwriters.AgentSnapshotInfo;

import java.util.*;

/**
 * Teleportation engine that uses the network route of an agent to teleport it to its destination and generate link events accordingly.
 * TODO: remove, because it was integrated into default teleportation
 */
@Deprecated
public final class TeleportationWithLinkEventsEngine implements TeleportationEngine {
	private static final Logger log = LogManager.getLogger(TeleportationWithLinkEventsEngine.class);

	private final Queue<Tuple<Double, MobsimAgent>> teleportationList = new PriorityQueue<>(
		30, new Comparator<Tuple<Double, MobsimAgent>>() {

		@Override
		public int compare(Tuple<Double, MobsimAgent> o1, Tuple<Double, MobsimAgent> o2) {
			int ret = o1.getFirst().compareTo(o2.getFirst()); // first compare time information
			if (ret == 0) {
				ret = o2.getSecond()
					.getId()
					.compareTo(o1.getSecond()
						.getId()); // if they're equal, compare the Ids: the one with the larger Id should be first
			}
			return ret;
		}
	});

	private final EventsManager eventsManager;

	/**
	 * The modes to handle.
	 */
	private final Set<String> modes;
	private final Map<String, TravelTime> travelTimes;
	private final boolean active;

	private InternalInterface internalInterface;


	@Inject
	public TeleportationWithLinkEventsEngine(Scenario scenario, IterationCounter iterationCounter, EventsManager eventsManager, Map<String, TravelTime> travelTimes) {
		this.eventsManager = eventsManager;
		this.modes = new HashSet<>(scenario.getConfig().routing().getTeleportedRoutedModes());
		this.travelTimes = travelTimes;
		this.active = EventsUtils.shouldWriteEvents(scenario.getConfig().controller(), iterationCounter.getIterationNumber(),
			iterationCounter.getIterationNumber() >= scenario.getConfig().controller().getLastIteration());
	}

	@Override
	public boolean handleDeparture(double now, MobsimAgent agent, Id<Link> linkId) {

		// TODO: how to make sure this is called before the "normal" teleportation engine?

		// This engine is only active when events are being written
		if (!active)
			return false;

		if (!modes.contains(agent.getMode()))
			return false;

		if (!(agent instanceof MobsimDriverAgent driver))
			return false;

		System.out.println(driver.chooseNextLinkId());

		TravelTime tt = travelTimes.get(agent.getMode());

		// TODO: need to get the route of the agent
		System.out.println(agent);

		return false;
	}

	@Override
	public Collection<AgentSnapshotInfo> addAgentSnapshotInfo(Collection<AgentSnapshotInfo> snapshotList) {
		// This functionality is not supported
		return List.of();
	}

	@Override
	public void doSimStep(double time) {

		// TODO: generate the events here

		// see default teleportation engine for reference


	}


	@Override
	public void onPrepareSim() {
	}

	@Override
	public void afterSim() {
		double now = internalInterface.getMobsim().getSimTimer().getTimeOfDay();
		for (Tuple<Double, MobsimAgent> entry : teleportationList) {
			MobsimAgent agent = entry.getSecond();
			eventsManager.processEvent(new PersonStuckEvent(now, agent.getId(), agent.getDestinationLinkId(), agent.getMode()));
		}
		teleportationList.clear();
	}

	@Override
	public void setInternalInterface(InternalInterface internalInterface) {
		this.internalInterface = internalInterface;
	}
}
