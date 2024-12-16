/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package org.matsim.contrib.ev.charging;
/*
 * created by jbischoff, 09.10.2018
 *  This is an events based approach to trigger vehicle charging. Vehicles will be charged as soon as a person begins a charging activity.
 */

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableListMultimap;
import jakarta.inject.Inject;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.ev.EvConfigGroup;
import org.matsim.contrib.ev.fleet.ElectricFleet;
import org.matsim.contrib.ev.fleet.ElectricVehicle;
import org.matsim.contrib.ev.infrastructure.Charger;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructure;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructureUtils;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.events.MobsimScopeEventHandler;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.vehicles.Vehicle;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This is an events based approach to trigger vehicle charging. Vehicles will be charged as soon as a person begins a charging activity.
 * <p>
 * Do not use this class for charging DVRP vehicles (DynAgents). In that case, vehicle charging is simulated with ChargingActivity (DynActivity).
 * (It may work together, but that would need to be tested. kai based on michal, dec'22)
 */
public class VehicleChargingHandler
		implements ActivityStartEventHandler, ActivityEndEventHandler, PersonLeavesVehicleEventHandler, QueuedAtChargerEventHandler, ChargingStartEventHandler,
		ChargingEndEventHandler, QuitQueueAtChargerEventHandler,
	MobsimBeforeSimStepListener, MobsimScopeEventHandler {

	public static final String CHARGING_IDENTIFIER = " charging";
	public static final String CHARGING_INTERACTION = ScoringConfigGroup.createStageActivityType(
			CHARGING_IDENTIFIER);
	/*
	 * actually this set is not needed as long as driver id's equal the vehicle id's. Because the internal id handling would sort that out
	 * (it seems id types are irrelevant, in the end. Meaning that agentsInChargerQueue.remove(vehicleId) with Id<Vehicle> vehicleId works out, actually.
	 */
	private final Map<Id<Vehicle>, Id<Person>> lastDriver = new HashMap<>();
	private final Map<Id<Person>, Id<Vehicle>> lastVehicleUsed = new HashMap<>();
	private final Map<Id<Vehicle>, Id<Charger>> vehiclesAtChargers = new HashMap<>();
	private final Set<Id<Person>> agentsInChargerQueue = ConcurrentHashMap.newKeySet();

	private final ChargingInfrastructure chargingInfrastructure;
	private final ElectricFleet electricFleet;
	private final ImmutableListMultimap<Id<Link>, Charger> chargersAtLinks;
	private final EvConfigGroup evCfg;

	private final ChargingStrategy.Factory strategyFactory;

	@Inject
	VehicleChargingHandler(ChargingInfrastructure chargingInfrastructure, ElectricFleet electricFleet, EvConfigGroup evConfigGroup, ChargingStrategy.Factory strategyFactory) {
		this.chargingInfrastructure = chargingInfrastructure;
		this.electricFleet = electricFleet;
		this.evCfg = evConfigGroup;
		this.strategyFactory = strategyFactory;
		chargersAtLinks = ChargingInfrastructureUtils.getChargersAtLinks(chargingInfrastructure );
	}

	/**
	 * This assumes no liability which charger is used, as long as the type matches
	 *
	 * @param event the corresponding ActivityStartEvent to handle
	 */
	@Override
	public void handleEvent(ActivityStartEvent event) {
		if (event.getActType().endsWith(CHARGING_INTERACTION)) {
			Id<Vehicle> vehicleId = lastVehicleUsed.get(event.getPersonId());
			if (vehicleId != null) {
				Id<Vehicle> evId = Id.create(vehicleId, Vehicle.class);
				if (electricFleet.getElectricVehicles().containsKey(evId)) {
					ElectricVehicle ev = electricFleet.getElectricVehicles().get(evId);
					List<Charger> chargers = chargersAtLinks.get(event.getLinkId());
					Charger c = chargers.stream()
							.filter(ch -> ev.getChargerTypes().contains(ch.getChargerType()))
							.findAny()
							.get();
					c.getLogic().addVehicle(ev, strategyFactory.createStrategy(c.getSpecification(), ev), event.getTime());
					vehiclesAtChargers.put(evId, c.getId());
				}
			}
		}
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		if (event.getActType().endsWith(CHARGING_INTERACTION)) {
			Id<Vehicle> vehicleId = lastVehicleUsed.get(event.getPersonId());
			if (vehicleId != null) {
				Id<Vehicle> evId = Id.create(vehicleId, Vehicle.class);
				Id<Charger> chargerId = vehiclesAtChargers.remove(evId);
				if (chargerId != null) {
					Charger c = chargingInfrastructure.getChargers().get(chargerId);
					c.getLogic().removeVehicle(electricFleet.getElectricVehicles().get(evId), event.getTime());
				}
			}
		}
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		lastVehicleUsed.put(event.getPersonId(), event.getVehicleId());
		lastDriver.put(event.getVehicleId(), event.getPersonId());
	}

	@Override
	public void handleEvent(ChargingEndEvent event) {
		//Charging has ended before activity ends
		vehiclesAtChargers.remove(event.getVehicleId());
		//the following should not be necessary anymore, because ChargingStartEvent happened already. But assuring this nevertheless
		removeLastDriver(event.getVehicleId());
	}

	@Override
	public void handleEvent(ChargingStartEvent event) {
		removeLastDriver(event.getVehicleId());
	}

	@Override
	public void handleEvent(QueuedAtChargerEvent event) {
		//vehiclesAtChargers should normally already contain the vehicle, but assure this nevertheless
		vehiclesAtChargers.put(event.getVehicleId(), event.getChargerId());
		Id<Person> driver = lastDriver.get(event.getVehicleId());
		if (driver != null){
			//agents this set extend their activity if evCfg.enforceChargingInteractionDuration
			agentsInChargerQueue.add(driver);
		} // else this vehicle is driven by a DynAgent (who did not leave the vehicle for charging)
	}

	/**
	 * This method tries to extend the charging activity as long as the agent is still in the waiting queue. This aims to model
	 * charging for a fixed predefined time (that does not include waiting).
	 * It is inspired by org.matsim.freight.carriers.controler.WithinDayActivityReScheduling
	 * @param e
	 */
	@Override
	public void notifyMobsimBeforeSimStep(MobsimBeforeSimStepEvent e) {
		//TODO only do this every <evConfig.chargeTimeStep> seconds ??

		//not sure how we should best get the MobsimAgent in some other way
		//as PopulationAgentSource does not provide a collection of MobsimAgents and injecting the qsim into this class did not seem like a better solution to me
		//tschlenther, nov' 23
		QSim qsim = (QSim) e.getQueueSimulation();
		for (Id<Person> agentId : agentsInChargerQueue) {
			MobsimAgent mobsimAgent = qsim.getAgents().get(agentId);

			//ideally, we would have an instance of EditPlans and then call rescheduleCurrentActivityEndtime
			//but I don't see an easy way to instantiate EditPlans right now,because it needs EditTrips, which needs a lot of heavy-weight infrastructure..
			PlanElement currentPlanElement = WithinDayAgentUtils.getCurrentPlanElement(mobsimAgent);
			if (currentPlanElement instanceof Activity act) {
				Preconditions.checkState(act.getType().endsWith(CHARGING_INTERACTION),
					"agent " + agentId + " is registered as waiting in a charger queue but the currentPlanElement is not an activity of type " + CHARGING_INTERACTION + "!");
				//EvNetworkRoutingModule models the charging activity with a maximum duration and does not set an end time
				//This means, we just have to call  WithinDayAgentUtils.resetCaches, because this triggers recalculation of the activity end time
				//based on the duration and the _current_ simulation time. This means, an adjustment of act.maximumDuration is not needed but rather obsolete and would need to too long extension!
				//I am not sure, whether this causes some problems later, because the actual activity duration might then be longer than the act.maximumDuration...
				//tschlenther, nov' 23
//				act.setMaximumDuration(act.getMaximumDuration().orElseThrow(IllegalStateException::new) + 1d);
				WithinDayAgentUtils.resetCaches(mobsimAgent);
				WithinDayAgentUtils.rescheduleActivityEnd(mobsimAgent, qsim);
			} else {
				throw new IllegalStateException("agent " + agentId + " is registered as waiting in a charger queue but the currentPlanElement is not an activity!");
			}
		}
	}


	@Override
	public void handleEvent(QuitQueueAtChargerEvent event) {
		if (evCfg.enforceChargingInteractionDuration){
			//this could actually happen when combining with edrt/etaxi/evrp
			throw new RuntimeException("should currently not happen, as this event is only triggered in case the agent quits the charger queue without charging afterwards, " +
				" and this should not happen with fixed charging activity duration.\n" +
				"If you run evrp together with conventional (preplanned) EV, please refer to VSP.");
		} else {
			//Charging has ended before activity ends
			vehiclesAtChargers.remove(event.getVehicleId());
			removeLastDriver(event.getVehicleId());
		}
	}

	private void removeLastDriver(Id<Vehicle> vehicleId) {
		if (lastDriver.get(vehicleId) != null) {
			agentsInChargerQueue.remove(lastDriver.get(vehicleId));
		} // else this vehicle is driven by a DynAgent (who did not leave the vehicle for charging)
	}
}
