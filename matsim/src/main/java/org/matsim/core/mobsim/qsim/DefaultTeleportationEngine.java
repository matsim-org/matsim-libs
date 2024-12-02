
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

import java.util.*;

import jakarta.inject.Inject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.TeleportationArrivalEvent;
import org.matsim.core.controler.IterationCounter;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.facilities.Facility;
import org.matsim.vehicles.Vehicle;
import org.matsim.vis.snapshotwriters.AgentSnapshotInfo;
import org.matsim.vis.snapshotwriters.TeleportationVisData;

/**
 * Includes all agents that have transportation modes unknown to the
 * NetsimEngine (often all != "car") or have two activities on the same link
 */
public final class DefaultTeleportationEngine implements TeleportationEngine {
	private static final Logger log = LogManager.getLogger(DefaultTeleportationEngine.class);

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

	/**
	 * Queue with agents teleported over links
	 */
	private final Queue<LinkTeleport> linkEventList = new PriorityQueue<>(
		30, (o1, o2) -> {
		int ret = Double.compare(o1.arrivalTime, o2.arrivalTime);
		if (ret == 0) {
			ret = Objects.compare(o2.agent.getId(), o1.agent.getId(), Comparator.naturalOrder());
		}
		return ret;
	});

	private final LinkedHashMap<Id<Person>, TeleportationVisData> teleportationData = new LinkedHashMap<>();
	private final Scenario scenario;
	private final EventsManager eventsManager;
	private final Set<String> linkEventModes;
	private final boolean generateLinkEvents;
	private final Map<String, TravelTime> travelTimes;
	private final boolean withTravelTimeCheck;

	private InternalInterface internalInterface;

	@Inject
	public DefaultTeleportationEngine(Scenario scenario, IterationCounter iterationCounter, EventsManager eventsManager, Map<String, TravelTime> travelTimes) {
		this(
			scenario,
			eventsManager,
			travelTimes,
			scenario.getConfig().qsim().isUsingTravelTimeCheckInTeleportation(),
			EventsUtils.shouldWriteEvents(scenario.getConfig().controller(), iterationCounter.getIterationNumber(),
				iterationCounter.getIterationNumber() >= scenario.getConfig().controller().getLastIteration())
		);
	}

	/**
	 * Old injected constructor.
	 *
	 * @see #DefaultTeleportationEngine(Scenario, IterationCounter, EventsManager, Map)
	 */
	@Deprecated
	public DefaultTeleportationEngine(Scenario scenario, EventsManager eventsManager) {
		this(scenario, eventsManager, scenario.getConfig().qsim().isUsingTravelTimeCheckInTeleportation());
	}

	/**
	 * Constructor for teleportation without link events.
	 */
	public DefaultTeleportationEngine(Scenario scenario, EventsManager eventsManager, boolean withTravelTimeCheck) {
		this(scenario, eventsManager, Map.of(), withTravelTimeCheck, false);
	}

	/**
	 * Constructor allowing to set all parameters.
	 */
	public DefaultTeleportationEngine(Scenario scenario, EventsManager eventsManager, Map<String, TravelTime> travelTimes,
									  boolean withTravelTimeCheck, boolean generateLinkEvents) {
		this.scenario = scenario;
		this.eventsManager = eventsManager;
		this.linkEventModes = new HashSet<>(scenario.getConfig().routing().getTeleportedRoutedModes());
		this.travelTimes = travelTimes;
		this.withTravelTimeCheck = withTravelTimeCheck;
		this.generateLinkEvents = generateLinkEvents;
	}

	private static Double travelTimeCheck(Double travelTime, Double speed, Facility dpfac, Facility arfac) {
		if (speed == null) {
			// if we don't have a bushwhacking speed, the only thing we can do is trust the router
			return travelTime;
		}

		if (dpfac == null || arfac == null) {
			log.warn("dpfac = " + dpfac);
			log.warn("arfac = " + arfac);
			throw new RuntimeException("have bushwhacking mode but nothing that leads to coordinates; don't know what to do ...");
			// (means that the agent is not correctly implemented)
		}

		if (dpfac.getCoord() == null || arfac.getCoord() == null) {
			// yy this is for example the case if activities are initialized at links, without coordinates.  Could use the link coordinate
			// instead, but somehow this does not feel any better. kai, feb'16

			return travelTime;
		}

		final Coord dpCoord = dpfac.getCoord();
		final Coord arCoord = arfac.getCoord();

		double dist = NetworkUtils.getEuclideanDistance(dpCoord, arCoord);
		double travelTimeTmp = dist / speed;

		if (travelTimeTmp < travelTime) {
			return travelTime;
		}

		return travelTimeTmp;
	}

	@Override
	public boolean handleDeparture(double now, MobsimAgent agent, Id<Link> linkId) {
		if (agent.getExpectedTravelTime().isUndefined()) {
			LogManager.getLogger(this.getClass()).info("mode: " + agent.getMode());
			throw new RuntimeException("teleportation does not work when travel time is undefined.  There is also really no magic fix for this,"
				+ " since we cannot guess travel times for arbitrary modes and arbitrary landscapes.  kai/mz, apr'15 & feb'16");
		}

		double travelTime = agent.getExpectedTravelTime().seconds();
		if (withTravelTimeCheck) {
			Double speed = scenario.getConfig().routing().getTeleportedModeSpeeds().get(agent.getMode());
			Facility dpfac = agent.getCurrentFacility();
			Facility arfac = agent.getDestinationFacility();
			travelTime = DefaultTeleportationEngine.travelTimeCheck(travelTime, speed, dpfac, arfac);
		}

		double arrivalTime = now + travelTime;
		this.teleportationList.add(new Tuple<>(arrivalTime, agent));

		// === below here is only visualization, no dynamics ===
		Id<Person> agentId = agent.getId();
		Link currLink = this.scenario.getNetwork().getLinks().get(linkId);
		Link destLink = this.scenario.getNetwork().getLinks().get(agent.getDestinationLinkId());
		Coord fromCoord = currLink.getToNode().getCoord();
		Coord toCoord = destLink.getToNode().getCoord();
		TeleportationVisData agentInfo = new TeleportationVisData(now, agentId, fromCoord, toCoord, travelTime);
		this.teleportationData.put(agentId, agentInfo);

		if (generateLinkEvents && linkEventModes.contains(agent.getMode())) {

			if (!(agent instanceof MobsimDriverAgent driver))
				throw new RuntimeException("Only driver agents are supported for teleported link events");

			TravelTime tt = travelTimes.get(agent.getMode());

			Person person = scenario.getPopulation().getPersons().get(agentId);
			Vehicle vehicle = scenario.getVehicles().getVehicles().get(driver.getPlannedVehicleId());

			double linkTT = tt.getLinkTravelTime(currLink, now, person, vehicle);
			LinkTeleport entry = new LinkTeleport(driver, person, vehicle, currLink, now + linkTT);

			linkEventList.add(entry);
		}

		return true;
	}

	@Override
	public Collection<AgentSnapshotInfo> addAgentSnapshotInfo(Collection<AgentSnapshotInfo> snapshotList) {
		double time = internalInterface.getMobsim().getSimTimer().getTimeOfDay();
		for (TeleportationVisData teleportationVisData : teleportationData.values()) {
			teleportationVisData.updatePosition(time);
			snapshotList.add(teleportationVisData);
		}
		return snapshotList;
	}

	@Override
	public void doSimStep(double time) {

		if (generateLinkEvents) {
			handleTeleportationLinkEvents(time);
		}

		handleTeleportationArrivals(time);
	}

	private void handleTeleportationArrivals(double now) {
		while (!teleportationList.isEmpty()) {
			Tuple<Double, MobsimAgent> entry = teleportationList.peek();
			if (entry.getFirst() <= now) {
				teleportationList.poll();
				MobsimAgent personAgent = entry.getSecond();
				personAgent.notifyArrivalOnLinkByNonNetworkMode(personAgent.getDestinationLinkId());
				double distance = personAgent.getExpectedTravelDistance();
				this.eventsManager.processEvent(
					new TeleportationArrivalEvent(now, personAgent.getId(), distance, personAgent.getMode()));
				personAgent.endLegAndComputeNextState(now);
				this.teleportationData.remove(personAgent.getId());
				internalInterface.arrangeNextAgentState(personAgent);
			} else {
				break;
			}
		}
	}

	private void handleTeleportationLinkEvents(double now) {

		while (!linkEventList.isEmpty() && linkEventList.peek().arrivalTime <= now) {
			LinkTeleport entry = linkEventList.poll();

			// TODO: check if this logic is correct
			// TODO: ensure agents don't arrive before link sequence is finished
			if (entry.agent.isWantingToArriveOnCurrentLink()) {
				continue;
			}

			Id<Link> nextLinkId = entry.agent.chooseNextLinkId();
			Link nextLink = scenario.getNetwork().getLinks().get(nextLinkId);

			TravelTime tt = travelTimes.get(entry.agent.getMode());
			double linkTT = tt.getLinkTravelTime(nextLink, now, entry.person, entry.vehicle);
			LinkTeleport e = new LinkTeleport(entry.agent, entry.person, entry.vehicle, nextLink, entry.arrivalTime + linkTT);


			linkEventList.add(e);
		}
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
		linkEventList.clear();
	}

	@Override
	public void setInternalInterface(InternalInterface internalInterface) {
		this.internalInterface = internalInterface;
	}

	private record LinkTeleport(MobsimDriverAgent agent, Person person, Vehicle vehicle, Link link, double arrivalTime) {
	}

}
