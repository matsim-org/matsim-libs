/* *********************************************************************** *
 * project: org.matsim.*
 * InsertParkingActivities.java
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


package playground.christoph.parking.core.mobsim;

import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.ActivityFacility;
import org.matsim.population.algorithms.PersonAlgorithm;
import org.matsim.population.algorithms.PlanAlgorithm;

public class InitialParkingSelector implements PersonAlgorithm, PlanAlgorithm {

	public final static String INITIALPARKINGFACILITY = "initialParkingFacility";
	
	private final Scenario scenario;
	private final Set<String> initialParkingTypes;
	private final ParkingInfrastructure parkingInfrastructure;
	
	public InitialParkingSelector(Scenario scenario, Set<String> initialParkingTypes, ParkingInfrastructure parkingInfrastructure) {
		this.scenario = scenario;
		this.initialParkingTypes = initialParkingTypes;
		this.parkingInfrastructure = parkingInfrastructure;
	}
	
	@Override
	public void run(Plan plan) {
			
		/*
		 * Check whether there is already an initial parking facility id defined in the
		 * person's object attributes. If not, define one.
		 */
		Person person = plan.getPerson();
		Id personId = person.getId();
		String parkingFacilityIdString = (String) this.scenario.getPopulation().getPersonAttributes().getAttribute(personId.toString(), 
				INITIALPARKINGFACILITY);
		if (parkingFacilityIdString != null) {
			Id<ActivityFacility> parkingFacilityId = Id.create(parkingFacilityIdString, ActivityFacility.class);
			Id vehicleId = this.parkingInfrastructure.getVehicleId(person);
			this.parkingInfrastructure.reserveParking(vehicleId, parkingFacilityId);
		} else {
			// get the first activity
			Activity firstActivity = (Activity) plan.getPlanElements().get(0);
			
			// get the facility where the activity is performed
			ActivityFacility firstFacility = this.scenario.getActivityFacilities().getFacilities().get(firstActivity.getFacilityId());
			Coord firstCoord = firstFacility.getCoord();
			
			// get the closest free parking facility
			ActivityFacility parkingFacility = null;
			double distance = Double.MAX_VALUE;
			for (String parkingType : this.initialParkingTypes) {
				ActivityFacility potentialParkingFacility = this.parkingInfrastructure.getClosestFreeParkingFacility(firstCoord, parkingType);
				double potentialDistance = CoordUtils.calcDistance(potentialParkingFacility.getCoord(), firstCoord);
				if (potentialDistance < distance) {
					parkingFacility = potentialParkingFacility;
					distance = potentialDistance;
				}
			}
			
			if (parkingFacility == null) throw new RuntimeException("No parking facility with free capacity was found!");
			
			Id vehicleId = this.parkingInfrastructure.getVehicleId(person);
			this.parkingInfrastructure.reserveParking(vehicleId, parkingFacility.getId());
			
			this.scenario.getPopulation().getPersonAttributes().putAttribute(personId.toString(), INITIALPARKINGFACILITY, 
					parkingFacility.getId().toString());			
		}
	}

	@Override
	public void run(Person person) {
		this.run(person.getSelectedPlan());
	}

}
