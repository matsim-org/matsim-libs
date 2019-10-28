/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.vsp.avparking;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.DvrpVehicleImpl;
import org.matsim.contrib.dvrp.fleet.DvrpVehicleSpecification;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.fleet.ImmutableDvrpVehicleSpecification;
import org.matsim.contrib.parking.parkingsearch.manager.ParkingSearchManager;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;

import com.google.common.collect.ImmutableMap;

/**
 * @author  jbischoff
 *
 */


public class PrivateAVFleetGenerator implements Fleet, BeforeMobsimListener {

	private ImmutableMap<Id<DvrpVehicle>, DvrpVehicle> vehiclesForIteration;

	private final Population population;
	
	private String mode;

	private Network network;
	
	@Inject
	ParkingSearchManager manager;
	/**
	 * 
	 */
	public PrivateAVFleetGenerator(Scenario scenario) {
		mode = TaxiConfigGroup.getSingleModeTaxiConfig(scenario.getConfig()).getMode();
		this.population= scenario.getPopulation();
		this.network = scenario.getNetwork();
//		this.manager = manager;
		vehiclesForIteration = ImmutableMap.of();
	}
	
	
	@Override
	public ImmutableMap<Id<DvrpVehicle>, ? extends DvrpVehicle> getVehicles() {
		return vehiclesForIteration;
	}

	/* (non-Javadoc)
	 * @see org.matsim.core.controler.listener.IterationStartsListener#notifyIterationStarts(org.matsim.core.controler.events.IterationStartsEvent)
	 */
	@Override
	public void notifyBeforeMobsim(BeforeMobsimEvent event) {
		Map<Id<DvrpVehicle>, DvrpVehicle> lastIterationVehicles = new HashMap<>();
		lastIterationVehicles.putAll(vehiclesForIteration);
		vehiclesForIteration.clear();
		for (Person p : population.getPersons().values()){
			
			Id<Link> vehicleStartLink = null;
			Plan plan = p.getSelectedPlan();
			for (PlanElement pe : plan.getPlanElements()){
				if (pe instanceof Leg){
					if (((Leg) pe).getMode().equals(mode)){
						vehicleStartLink = ((Leg) pe).getRoute().getStartLinkId();
						break;
					}
				}
			}
			if (vehicleStartLink!=null){
				Id<DvrpVehicle> vehicleId = Id.create(p.getId().toString() + "_av", DvrpVehicle.class);
				if (lastIterationVehicles.containsKey(vehicleId)){
					Id<Link> lastParkingPosition = manager.getVehicleParkingLocation(Id.createVehicleId(vehicleId));
					if (lastParkingPosition!=null){
						vehicleStartLink = lastParkingPosition;
						lastIterationVehicles.remove(vehicleId);
					}
				}
				Link startLink = network.getLinks().get(vehicleStartLink);
				DvrpVehicleSpecification specification = ImmutableDvrpVehicleSpecification.newBuilder()
						.id(vehicleId)
						.startLinkId(startLink.getId())
						.capacity(4)
						.serviceBeginTime((double)0)
						.serviceEndTime((double)(30 * 3600))
						.build();
				DvrpVehicle veh = new DvrpVehicleImpl(specification, startLink);
				vehiclesForIteration.put(veh.getId(), veh);
			}
		}
		for (Id<DvrpVehicle> vid : lastIterationVehicles.keySet()) {
			Id<org.matsim.vehicles.Vehicle> vehicleId = Id.createVehicleId(vid);
			Id<Link> parkLink = manager.getVehicleParkingLocation(vehicleId);
			if (parkLink!=null){
				manager.unParkVehicleHere(vehicleId, parkLink, 30*3600);
				Logger.getLogger(getClass()).info("Removing AV: "+vid);
			}
		}
	}
}
