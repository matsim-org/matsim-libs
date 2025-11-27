/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package org.matsim.core.mobsim.qsim.agents;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Message;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.PersonInitializedEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.NetworkPartition;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup.PersonInitializedEventsSetting;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.dsim.DistributedAgentSource;
import org.matsim.core.mobsim.dsim.DistributedMobsimAgent;
import org.matsim.core.mobsim.dsim.DistributedMobsimVehicle;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.interfaces.InsertableMobsim;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicleFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicleImpl;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleUtils;

import com.google.inject.Inject;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

public final class PopulationAgentSource implements AgentSource, DistributedAgentSource {
	private static final Logger log = LogManager.getLogger(PopulationAgentSource.class);
	private static int cnt = 5;
	private final Population population;
	private final AgentFactory agentFactory;
	private final QVehicleFactory qVehicleFactory;
	private final Netsim qsim;
	private final Collection<String> mainModes;
	private final Map<Id<Vehicle>, Id<Link>> seenVehicleIds = new HashMap<>();
	private final PersonInitializedEventsSetting personHelloEventsSetting;
	private int warnCnt = 0;

	@Inject
	PopulationAgentSource(Population population, AgentFactory agentFactory, QVehicleFactory qVehicleFactory, Netsim qsim) {
		this.population = population;
		this.agentFactory = agentFactory;
		this.qVehicleFactory = qVehicleFactory;
		this.qsim = qsim;
		this.mainModes = qsim.getScenario().getConfig().qsim().getMainModes();
		this.personHelloEventsSetting = qsim.getScenario().getConfig().qsim().getPersonInitializedEventsSetting();
	}

	public static Id<Link> getStartLink(Scenario scenario, Person person) {
		Plan plan = person.getSelectedPlan();
		if (plan == null)
			plan = person.getPlans().getFirst();

		return switch (plan.getPlanElements().getFirst()) {
			case Activity a -> PopulationUtils.decideOnLinkIdForActivity(a, scenario);
			case Leg l -> l.getRoute().getStartLinkId();
			default -> throw new IllegalStateException("Unexpected class: " + plan.getPlanElements().getFirst().getClass());
		};
	}

	@Override
	public void insertAgentsIntoMobsim() {
		// insert vehicles first, as this modifies plans by setting vehicle ids. When MobsimAgents are created from Persons, they might make
		// a copy of the person's plan so that changes to the plan after that point are not reflected in the MobsimAgent's plan. Janek nov' 24
		for (Person p : population.getPersons().values()) {
			insertVehicles(p, NetworkPartition.SINGLE_INSTANCE, qsim::addParkedVehicle);
		}
		for (Person p : population.getPersons().values()) {
			MobsimAgent agent = this.agentFactory.createMobsimAgentFromPerson(p);
			qsim.insertAgentIntoMobsim(agent);
			if (this.personHelloEventsSetting == PersonInitializedEventsSetting.all
					|| (this.personHelloEventsSetting == PersonInitializedEventsSetting.singleActAgentsOnly && p.getSelectedPlan().getPlanElements().size() == 1)) {
				Coord firstActCoord = ((Activity)p.getSelectedPlan().getPlanElements().get(0)).getCoord();
				this.qsim.getEventsManager().processEvent(new PersonInitializedEvent(0, p.getId(), firstActCoord));
			}
		}
	}

	/**
	 * Inserts vehicles into the simulation. As a side effect, this method adds vehicle ids to legs in the
	 * selected plan of the supplied person. This method is aware of network partitions. It will set vehicle
	 * ids on selected plans of all supplied persons, but it will only insert vehicles which have their first
	 * starting link on the supplied network partition.
	 */
	private void insertVehicles(Person person, NetworkPartition partition, BiConsumer<MobsimVehicle, Id<Link>> vehicleInsertion) {
		// this is called in every iteration.  So if a route without a vehicle id is found (e.g. after mode choice),
		// then the id is generated here.  kai/amit, may'18

		Map<String, Id<Vehicle>> seenModes = new HashMap<>();
		for (Leg leg : TripStructureUtils.getLegs(person.getSelectedPlan())) {

			// only simulated modes get vehicles:
			if (!this.mainModes.contains(leg.getMode())) {
				continue;
			}

			// determine the vehicle ID
			NetworkRoute route = (NetworkRoute) leg.getRoute();
			Id<Vehicle> vehicleId = null;
			if (route != null) {
				vehicleId = route.getVehicleId();
			}
			if (vehicleId == null) {
				vehicleId = VehicleUtils.getVehicleId(person, leg.getMode());
				if (route != null) {
					route.setVehicleId(vehicleId);
				}
			}

			// determine the vehicle (not the same as just its ID):

			final QSimConfigGroup.VehiclesSource vehiclesSource = qsim.getScenario().getConfig().qsim().getVehiclesSource();

			if (vehiclesSource != QSimConfigGroup.VehiclesSource.fromVehiclesData) {
				// without this condition, it is surely wrong when the vehicles come from the vehicles data ... because a second vehicle for
				// the same mode is not placed.  kai, nov'19

				// yyyy I am not sure if at this point vehicleId actually can be null ... because the above VehicleUtils.getVehicleId(...,
				// mode) already should contain a map of all the vehicles per person.   ???  kai, nov'19

				// we have seen the mode before in the plan:
				if (seenModes.containsKey(leg.getMode())) {
					if (vehicleId == null && route != null) {
						vehicleId = seenModes.get(leg.getMode());
						route.setVehicleId(vehicleId);
						// yyyy what is the meaning of route==null? kai, jun'18

						if (cnt > 0) {
							log.warn("encountered vehicleId=null where I am not sure how and why this should happen ...");
							cnt--;
							if (cnt == 0) {
								log.warn(Gbl.FUTURE_SUPPRESSED);
							}
						}
					}
					continue;
				}
			}

			// if we are here, we haven't seen the mode before in this plan

			// now memorizing mode and its vehicle ID:
			seenModes.put(leg.getMode(), vehicleId);

			// find the vehicle from the vehicles container.  It should be there, see automatic vehicle creation in PrepareForSim.
			Vehicle vehicle = qsim.getScenario().getVehicles().getVehicles().get(vehicleId);
			if (vehicle == null) {
				String msg = "Could not get the requested vehicle with ID=" + vehicleId + " from the vehicles container. ";
				switch (vehiclesSource) {
					case defaultVehicle:
					case modeVehicleTypesFromVehiclesData:
						msg += "You are using the config switch qsim.vehiclesSource=" + vehiclesSource.name() + "; this should have worked so " +
							"please report under matsim.org/faq .";
						break;
					case fromVehiclesData:
						msg += "You are using the config switch qsim.vehiclesSource=" + vehiclesSource.name() + "; with that setting, you have to" +
							" provide all needed vehicles yourself.";
						break;
					default:
						throw new RuntimeException(Gbl.NOT_IMPLEMENTED);
				}
				throw new RuntimeException(msg);
			}

			if (!(VehicleUtils.hasVehicleId(person, leg.getMode()) && VehicleUtils.getVehicleId(person, leg.getMode()).equals(vehicleId))) {
				// This is a routing-only vehicle, which is not actually picked up by any agent
				// and is required for WithinDay-functionality and (possibly shared) vehicles,
				// that are not assigned exclusively to persons. It is replaced by a real
				// vehicle when the agent actually starts its trip e.g,
				// SharingEngine>SharingLogic in shared_mobility. hrewald, oct'25
				continue; // do not place it in the simulation
			}

			// find the link ID of where to place the vehicle:
			Id<Link> vehicleLinkId = findVehicleLink(person, vehicle);

			// If vehicle behavior is 'teleport' every partition should have all vehicles, so that they are available everywhere.
			// Otherwise, only the partition the vehicle starts on, gets the vehicle, and another partition should generate the vehicle.
			if (qsim.getScenario().getConfig().qsim().getVehicleBehavior() != QSimConfigGroup.VehicleBehavior.teleport &&
				!partition.containsLink(vehicleLinkId)) {
				return;
			}

			// Checking if the vehicle has been seen before:
			Id<Link> result = this.seenVehicleIds.get(vehicleId);
			if (result != null) {
				if (warnCnt <= 5) {
					log.info("have seen vehicle with id {} before; not placing it again.", vehicleId);
				}
				if (warnCnt == 5) {
					log.warn(Gbl.FUTURE_SUPPRESSED);
				}
				warnCnt++;
				if (result != vehicleLinkId) {
					throw new RuntimeException("vehicle placement error: vehicleId=" + vehicleId +
						"; previous placement link=" + vehicleLinkId + "; current placement link=" + result);
				}
				// yyyyyy The above condition is too strict; it should be possible that one person drives
				// a car to some place, and some other person picks it up there.  However, this
				// method here is sorted by persons, not departure times, and so we don't know
				// which plan needs the vehicle first. (Looks to me that it should actually be possible
				// to resolve this.)

			} else {
				this.seenVehicleIds.put(vehicleId, vehicleLinkId);
				vehicleInsertion.accept(this.qVehicleFactory.createQVehicle(vehicle), vehicleLinkId);
			}
		}
	}

	/**
	 * A more careful way to decide where this agent should have its vehicles created
	 * than to ask agent.getCurrentLinkId() after creation.
	 *
	 * @param person TODO
	 */
	private Id<Link> findVehicleLink(Person person, Vehicle vehicle) {
		/*
		 * Manual case: the initial link id of the vehicle is saved as an attribute
		 */
		Id<Link> initialLinkId = VehicleUtils.getInitialLinkId(vehicle);
		if (initialLinkId != null) {
			return initialLinkId;
		}

		/* Cases that come to mind:
		 * (1) multiple persons share car located at home, but possibly brought to different place by someone else.
		 *      This is treated by the following algo.
		 * (2) person starts day with non-car leg and has car parked somewhere else.  This is NOT treated by the following algo.
		 *      It could be treated by placing the vehicle at the beginning of the first link where it is needed, but this would not
		 *      be compatible with variant (1).
		 *  Also see comment in insertVehicles.
		 */
		for (PlanElement planElement : person.getSelectedPlan().getPlanElements()) {
			if (planElement instanceof Activity activity) {
				final Id<Link> activityLinkId = PopulationUtils.decideOnLinkIdForActivity(activity, qsim.getScenario());
				if (activityLinkId != null) {
					return activityLinkId;
				}
			} else if (planElement instanceof Leg leg) {
				if (leg.getRoute().getStartLinkId() != null) {
					return leg.getRoute().getStartLinkId();
				}
			}
		}
		throw new RuntimeException("Don't know where to put a vehicle for this agent.");
	}

	@Override
	public void createAgentsAndVehicles(NetworkPartition partition, InsertableMobsim mobsim) {
		for (Person p : population.getPersons().values()) {

			// insert vehicles checks whether vehicles start on this partition. We have to execute this method for each person regardless of its starting
			// partition, for the case that the starting link of a vehicle is not the same as the starting partition of an agent.
			insertVehicles(p, partition, mobsim::addParkedVehicle);

			Id<Link> startLink = getStartLink(qsim.getScenario(), p);
			if (partition.containsLink(startLink)) {
				// first create vehicles, then create persons, as the 'insertVehicles' method sets vehicle ids on the plan
				// create mobsim agent might copy the plan so that changes to the original plan must be set before the mobsim
				// agent is created.
				var agent = this.agentFactory.createMobsimAgentFromPerson(p);
				mobsim.insertAgentIntoMobsim(agent);
			}
		}
	}

	@Override
	public Set<Class<? extends DistributedMobsimAgent>> getAgentClasses() {
		return Set.of(BasicPlanAgentImpl.class, PersonDriverAgentImpl.class);
	}

	@Override
	public DistributedMobsimAgent agentFromMessage(Class<? extends DistributedMobsimAgent> type, Message message) {
		return this.agentFactory.createMobsimAgentFromMessage(message);
	}

	@Override
	public Set<Class<? extends DistributedMobsimVehicle>> getVehicleClasses() {
		return Set.of(QVehicleImpl.class);
	}

	@Override
	public DistributedMobsimVehicle vehicleFromMessage(Class<? extends DistributedMobsimVehicle> type, Message message) {
		return new QVehicleImpl((QVehicleImpl.Msg) message);
	}
}
