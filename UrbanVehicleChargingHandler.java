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

package org.matsim.urbanEV;

import com.google.common.collect.ImmutableListMultimap;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.ev.charging.ChargingEndEvent;
import org.matsim.contrib.ev.charging.ChargingEndEventHandler;
import org.matsim.contrib.ev.charging.VehicleChargingHandler;
import org.matsim.contrib.ev.fleet.ElectricFleet;
import org.matsim.contrib.ev.fleet.ElectricVehicle;
import org.matsim.contrib.ev.infrastructure.Charger;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructure;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructures;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.events.MobsimScopeEventHandler;
import org.matsim.vehicles.Vehicle;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * This is an events based approach to trigger vehicle charging. Vehicles will be charged as soon as a person begins a PLUGIN_INTERACTION activity.
 * Charging will end as soon as the person performs a PLUGOUT_INTERACTION activity.
 * <p>
 * Do not use this class for charging DVRP vehicles (DynAgents). In that case, vehicle charging is simulated with ChargingActivity (DynActivity)
 * <p>
 *
 * This class is a modified version of {@link VehicleChargingHandler}
 *
 * @author tschlenther
 */
public class UrbanVehicleChargingHandler
		implements ActivityStartEventHandler, ActivityEndEventHandler, PersonLeavesVehicleEventHandler,
		ChargingEndEventHandler, MobsimScopeEventHandler {

	static final String PLUGIN_IDENTIFIER = " plugin";
	public static final String PLUGIN_INTERACTION = PlanCalcScoreConfigGroup.createStageActivityType(
			PLUGIN_IDENTIFIER);
	static final String PLUGOUT_IDENTIFIER = " plugout";
	public static final String PLUGOUT_INTERACTION = PlanCalcScoreConfigGroup.createStageActivityType(
			PLUGOUT_IDENTIFIER);
	private final Map<Id<Person>, Id<Vehicle>> lastVehicleUsed = new HashMap<>();
	private final Map<Id<ElectricVehicle>, Id<Charger>> vehiclesAtChargers = new HashMap<>();

	private final ChargingInfrastructure chargingInfrastructure;
	private final ElectricFleet electricFleet;
	private final ImmutableListMultimap<Id<Link>, Charger> chargersAtLinks;

	@Inject
	UrbanVehicleChargingHandler(ChargingInfrastructure chargingInfrastructure, ElectricFleet electricFleet) {
		this.chargingInfrastructure = chargingInfrastructure;
		this.electricFleet = electricFleet;
		chargersAtLinks = ChargingInfrastructures.getChargersAtLinks(chargingInfrastructure);
	}

	/**
	 * This assumes no liability which charger is used, as long as the type matches
	 *
	 * @param event
	 */
	@Override
	public void handleEvent(ActivityStartEvent event) {
		if (event.getActType().endsWith(PLUGIN_INTERACTION)) {
			Id<Vehicle> vehicleId = lastVehicleUsed.get(event.getPersonId());
			if (vehicleId != null) {
				Id<ElectricVehicle> evId = Id.create(vehicleId, ElectricVehicle.class);
				if (electricFleet.getElectricVehicles().containsKey(evId)) {
					ElectricVehicle ev = electricFleet.getElectricVehicles().get(evId);
					List<Charger> chargers = chargersAtLinks.get(event.getLinkId());
					Charger c = chargers.stream()
							.filter(ch -> ev.getChargerTypes().contains(ch.getChargerType()))
							.findAny()
							.get();
					c.getLogic().addVehicle(ev, event.getTime());
					vehiclesAtChargers.put(evId, c.getId());
				}
			}
		}
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		if (event.getActType().endsWith(PLUGOUT_INTERACTION)) {
			Id<Vehicle> vehicleId = lastVehicleUsed.get(event.getPersonId());
			if (vehicleId != null) {
				Id<ElectricVehicle> evId = Id.create(vehicleId, ElectricVehicle.class);
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
	}

	@Override
	public void handleEvent(ChargingEndEvent event) {
		vehiclesAtChargers.remove(event.getVehicleId());
		//Charging has ended before activity ends
	}
}
