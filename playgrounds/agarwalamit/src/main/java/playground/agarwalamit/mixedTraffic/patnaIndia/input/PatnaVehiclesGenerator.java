/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.agarwalamit.mixedTraffic.patnaIndia.input;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;

import playground.agarwalamit.mixedTraffic.MixedTrafficVehiclesUtils;
import playground.agarwalamit.utils.LoadMyScenarios;

/**
 * @author amit
 */
public class PatnaVehiclesGenerator {
	
	private Scenario scenario ;
	private Vehicles vehicles;
	
	/**
	 * @param plansFile is required to get vehicles for each agent type in population.
	 */
	public PatnaVehiclesGenerator(final String plansFile) {
		this.scenario = LoadMyScenarios.loadScenarioFromPlans(plansFile);
	}
	
	public void createVehicles (final Collection <String> modes) {

 		vehicles = VehicleUtils.createVehiclesContainer();

		Map<String, VehicleType> modesType = new HashMap<String, VehicleType>();
		
		for (String vehicleType : modes) {
			VehicleType veh = VehicleUtils.getFactory().createVehicleType(Id.create(vehicleType,VehicleType.class));
			veh.setMaximumVelocity( MixedTrafficVehiclesUtils.getSpeed( vehicleType.split("_")[0] ) );// this should not harm other use cases.
			veh.setPcuEquivalents( MixedTrafficVehiclesUtils.getPCU( vehicleType.split("_")[0] ) );
			
			modesType.put(vehicleType, veh);
			vehicles.addVehicleType(veh);
		}
		
		for(Person p : scenario.getPopulation().getPersons().values()){
			for(PlanElement pe:p.getSelectedPlan().getPlanElements()) {
				if(pe instanceof Leg ){
					String travelMode =  ((Leg) pe).getMode();
					if( ! modesType.containsKey(travelMode) ) throw new RuntimeException("Vehicle Type is not defined. Define "+ travelMode+ " vehicle Type.");	

					VehicleType vType = modesType.get(travelMode);
					Vehicle veh =  VehicleUtils.getFactory().createVehicle(Id.create(p.getId(), Vehicle.class), vType);
					vehicles.addVehicle(veh);
					break;//this is necessary only if a person has same travel mode in all trips.
				}
			}
		}
	}
	
	public Vehicles getPatnaVehicles(){
		return vehicles;
	}
}