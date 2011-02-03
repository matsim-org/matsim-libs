/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.mzilske.deteval;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.Vehicles;

import playground.mrieser.core.mobsim.api.MobsimVehicle;
import playground.mrieser.core.mobsim.features.MobsimFeature;
import playground.mrieser.core.mobsim.features.NetworkFeature;
import playground.mrieser.core.mobsim.impl.DefaultMobsimVehicle;
import playground.mrieser.core.mobsim.impl.DefaultTimestepSimEngine;
import playground.mrieser.core.mobsim.network.api.MobsimLink;

public class CarDistributor implements MobsimFeature {

	Logger logger = Logger.getLogger(CarDistributor.class);

	private Population population;
	private Vehicles vehicles;
	private NetworkFeature networkFeature;
	private Map<MobsimVehicle, MobsimLink> expectedVehicleLocations = new HashMap<MobsimVehicle, MobsimLink>();

	private boolean punishVehicleMove = true;

	private DefaultTimestepSimEngine engine;

	public CarDistributor(Population population, Vehicles vehicles, NetworkFeature networkFeature, DefaultTimestepSimEngine engine) {
		this.population = population;
		this.vehicles = vehicles;
		this.networkFeature = networkFeature;
		this.engine = engine;
	}

	private void createCarForPersonIfWantsOne(Person person) {
		Plan plan = person.getSelectedPlan();
		for (PlanElement planElement : plan.getPlanElements()) {
			if (planElement instanceof Activity) {

			}
			if (planElement instanceof Leg) {
				Leg leg = (Leg) planElement;
				if (TransportMode.car.equals(leg.getMode())) {
					Id homeLinkId = ((Activity) plan.getPlanElements().get(0)).getLinkId();
					Id linkId = homeLinkId;
					Id vehicleId = person.getId();
					Vehicle vehicle = vehicles.getVehicles().get(vehicleId);
					if (vehicle != null) {
						MobsimLink link2 = this.networkFeature.getSimNetwork().getLinks().get(linkId);
						MobsimVehicle simVehicle = new DefaultMobsimVehicle(vehicle);
						link2.insertVehicle(simVehicle, MobsimLink.POSITION_AT_TO_NODE, MobsimLink.PRIORITY_PARKING);
						expectedVehicleLocations.put(simVehicle, link2);
						logger.info("Parked car for agent " + person.getId() + " at link " + linkId);
						return;
					} else {
						logger.error("Agent needs a car but hasn't got one.");
						return;
					}
				}
			}
		}
	}

	@Override
	public void doSimStep(double time) {
		// TODO Auto-generated method stub

	}

	@Override
	public void afterMobSim() {
		if (punishVehicleMove) {
			for (Map.Entry<MobsimVehicle, MobsimLink> entry : expectedVehicleLocations.entrySet()) {
				MobsimVehicle vehicle = entry.getKey();
				MobsimLink link = entry.getValue();
				if (link.getParkedVehicle(vehicle.getId()) == null) {
					engine.getEventsManager().processEvent(engine.getEventsManager().getFactory().createAgentStuckEvent(99999999, vehicle.getId(), link.getId(), TransportMode.car));
				}
			}
		}
	}

	@Override
	public void beforeMobSim() {
		for (Person person : population.getPersons().values()) {
			createCarForPersonIfWantsOne(person);
		}
	}

	public void setPunishVehicleMove(boolean punishVehicleMove) {
		this.punishVehicleMove = punishVehicleMove;
	}

}

