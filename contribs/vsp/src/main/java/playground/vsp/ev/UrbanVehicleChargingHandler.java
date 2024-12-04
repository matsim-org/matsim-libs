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

package playground.vsp.ev;

import com.google.common.collect.ImmutableListMultimap;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.ev.charging.*;
import org.matsim.contrib.ev.fleet.ElectricFleet;
import org.matsim.contrib.ev.fleet.ElectricVehicle;
import org.matsim.contrib.ev.infrastructure.Charger;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructure;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructureUtils;
import org.matsim.core.events.MobsimScopeEventHandler;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.vehicles.Vehicle;

import jakarta.inject.Inject;
import java.util.*;


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
class UrbanVehicleChargingHandler
		implements ActivityStartEventHandler, ActivityEndEventHandler, PersonLeavesVehicleEventHandler,
		ChargingEndEventHandler, ChargingStartEventHandler, QueuedAtChargerEventHandler, MobsimScopeEventHandler {

	private final Map<Id<Person>, Id<Vehicle>> lastVehicleUsed = new HashMap<>();
	private final Map<Id<Vehicle>, Id<Charger>> vehiclesAtChargers = new HashMap<>();

	private final ChargingInfrastructure chargingInfrastructure;
	private final ElectricFleet electricFleet;
	private final ImmutableListMultimap<Id<Link>, Charger> chargersAtLinks;

	private final ChargingStrategy.Factory chargingStrategyFactory;

	private Map<Id<Link>, Map<Id<Person>, Tuple<Id<Vehicle>, Id<Charger>>>> chargingProcedures = new HashMap<>();

	@Inject
	UrbanVehicleChargingHandler(ChargingInfrastructure chargingInfrastructure, ElectricFleet electricFleet, ChargingStrategy.Factory chargingStrategyFactory) {
		this.chargingInfrastructure = chargingInfrastructure;
		this.electricFleet = electricFleet;
		this.chargersAtLinks = ChargingInfrastructureUtils.getChargersAtLinks(chargingInfrastructure );
		this.chargingStrategyFactory = chargingStrategyFactory;
	}

	/**
	 * This assumes no liability which charger is used, as long as the type matches
	 *
	 * @param event
	 */
	@Override
	public void handleEvent(ActivityStartEvent event) {
		if (event.getActType().endsWith( UrbanEVModule.PLUGIN_INTERACTION )) {
			Id<Vehicle> vehicleId = lastVehicleUsed.get(event.getPersonId());
			if (vehicleId != null) {
				Id<Vehicle> evId = Id.create(vehicleId, Vehicle.class);
				if (electricFleet.getElectricVehicles().containsKey(evId)) {
					ElectricVehicle ev = electricFleet.getElectricVehicles().get(evId);
					List<Charger> chargers = chargersAtLinks.get(event.getLinkId());
					Charger charger = chargers.stream()
							.filter(ch -> ev.getChargerTypes().contains(ch.getChargerType()))

							.findAny()
							.get();
					ChargingStrategy strategy = chargingStrategyFactory.createStrategy(charger.getSpecification(), ev);
					charger.getLogic().addVehicle(ev, strategy, event.getTime());
					Map<Id<Person>, Tuple<Id<Vehicle>, Id<Charger>>> proceduresOnLink = this.chargingProcedures.get(event.getLinkId());
					if(proceduresOnLink != null && proceduresOnLink.containsKey(event.getPersonId())){
						throw new RuntimeException("person " + event.getPersonId() + " tries to charge 2 vehicles at the same time on link " + event.getLinkId() +
								". this is not supported.");
					} else if(proceduresOnLink == null) {
						proceduresOnLink = new HashMap<>();
					}
					proceduresOnLink.put(event.getPersonId(), new Tuple<>(vehicleId, charger.getId()));
					this.chargingProcedures.put(event.getLinkId(), proceduresOnLink);
				} else {
					throw new IllegalStateException("can not plug in non-registered ev " + evId + " of person " + event.getPersonId());
				}
			} else {
				throw new IllegalStateException("last used vehicle  of person " + event.getPersonId() + "is null. should not happen");
			}
		}
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		if (event.getActType().endsWith( UrbanEVModule.PLUGOUT_INTERACTION )) {
			Tuple<Id<Vehicle>, Id<Charger>> tuple = chargingProcedures.get(event.getLinkId()).remove(event.getPersonId());
			if (tuple != null) {
				Id<Vehicle> evId = Id.create(tuple.getFirst(), Vehicle.class);
				if(vehiclesAtChargers.remove(evId) != null){ //if null, vehicle is fully charged and de-plugged already (see handleEvent(ChargingEndedEvent) )
					Id<Charger> chargerId = tuple.getSecond();
					Charger c = chargingInfrastructure.getChargers().get(chargerId);
					c.getLogic().removeVehicle(electricFleet.getElectricVehicles().get(evId), event.getTime());
				}
			} else {
				throw new RuntimeException("there is something wrong with the charging procedure of person=" + event.getPersonId() + " on link= " + event.getLinkId());
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

	@Override
	public void handleEvent(ChargingStartEvent event) {
		vehiclesAtChargers.put(event.getVehicleId(), event.getChargerId());
		//Charging has started
	}

	@Override
	public void handleEvent(QueuedAtChargerEvent event) {
		vehiclesAtChargers.put(event.getVehicleId(), event.getChargerId());
		//Charging may start soon
	}
}
