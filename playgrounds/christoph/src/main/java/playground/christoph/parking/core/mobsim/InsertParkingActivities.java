/* *********************************************************************** *
 * project: org.matsim.*
 * InsertParkingActivities.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.facilities.ActivityFacility;
import org.matsim.population.algorithms.PlanAlgorithm;

public class InsertParkingActivities implements PlanAlgorithm {

	public static String PARKINGACTIVITY = "parking";
	
	private final Scenario scenario;
	private final ModeRouteFactory modeRouteFactory;
	private final ParkingInfrastructure parkingInfrastructure;
	
	// TODO: instead of selecting closest parking, we could select parking from previous day.
	public InsertParkingActivities(Scenario scenario, ParkingInfrastructure parkingInfrastructure) {
		this.scenario = scenario;
		this.parkingInfrastructure = parkingInfrastructure;
		
		this.modeRouteFactory = ((PopulationFactoryImpl) scenario.getPopulation().getFactory()).getModeRouteFactory();
	}

	@Override
	public void run(Plan plan) {

		Person person = plan.getPerson();
		Id personId = person.getId();
		Id vehicleId = this.parkingInfrastructure.getVehicleId(person);
		
		boolean firstParking = true;
		
		List<PlanElement> planElements = plan.getPlanElements();

		/*
		 * Changes to this plan will be executed but not written to the person
		 */
		List<Leg> carLegs = new ArrayList<Leg>();
		for (PlanElement planElement : planElements) {
			if (planElement instanceof Leg) {
				if (((Leg) planElement).getMode().equals(TransportMode.car)) {
					carLegs.add((Leg) planElement);
				}
			}
		}

		// if no car legs are performed, no adaption of the plan is necessary
		if (carLegs.size() == 0) return;

		for (Leg carLeg : carLegs) {
			int index = planElements.indexOf(carLeg);
			
			Activity previousActivity = (Activity) planElements.get(index - 1);
//			Leg carLeg = (Leg) planElements.get(index);
			Activity nextActivity = (Activity) planElements.get(index + 1);
			
			/*
			 * If the carLeg starts and ends on the same link, we replace it by a walkLeg.
			 * Otherwise we create walk legs and parking activities
			 */
			if (previousActivity.getLinkId().equals(nextActivity.getLinkId())) {
				Leg walkLeg = createWalkLeg(carLeg.getDepartureTime(), previousActivity.getLinkId(), nextActivity.getLinkId());
				carLeg.setMode(TransportMode.walk);
				carLeg.setRoute(walkLeg.getRoute());
			} else {				
				// create walk legs and parking activities
//			Leg walkLegToParking = createWalkLeg(carLeg.getDepartureTime(), previousActivity.getLinkId(), previousActivity.getLinkId());
				Activity firstParkingActivity = createParkingActivity(personId, previousActivity.getFacilityId(), vehicleId, firstParking);
				Leg walkLegToParking = createWalkLeg(carLeg.getDepartureTime(), previousActivity.getLinkId(), firstParkingActivity.getLinkId());
				firstParking = false;
				Activity secondParkingActivity = createParkingActivity(personId, nextActivity.getFacilityId(), vehicleId, firstParking);
//			Leg walkLegFromParking = createWalkLeg(carLeg.getDepartureTime() + carLeg.getTravelTime(), nextActivity.getLinkId(), nextActivity.getLinkId());
				Leg walkLegFromParking = createWalkLeg(carLeg.getDepartureTime() + carLeg.getTravelTime(), firstParkingActivity.getLinkId(), nextActivity.getLinkId());
				
				// add legs and activities to plan
				planElements.add(index + 1, walkLegFromParking);
				planElements.add(index + 1, secondParkingActivity);
				planElements.add(index, firstParkingActivity);
				planElements.add(index, walkLegToParking);
			}		
		}
	}

	private Activity createParkingActivity(Id personId, Id facilityId, Id vehicleId, boolean firstParking) {

		// get the facility where the activity is performed
		ActivityFacility facility = this.scenario.getActivityFacilities().getFacilities().get(facilityId);
		
		/*
		 * If it is the agents first parking, its vehicle will be placed there.
		 * Therefore, we have to reserve the parking spot.
		 * 
		 * Otherwise select the parking facility which is closest to the activity.
		 * The simulation will relocate it, if no parking spot is available when the
		 * agent arrives.
		 */
		ActivityFacility parkingFacility;
		if (firstParking) {
			
			String parkingFacilityIdString = (String) this.scenario.getPopulation().getPersonAttributes().getAttribute(personId.toString(), 
					InitialParkingSelector.INITIALPARKINGFACILITY);
			Id<ActivityFacility> parkingFacilityId = Id.create(parkingFacilityIdString, ActivityFacility.class);
			
			// get the closest free parking facility
			parkingFacility = this.scenario.getActivityFacilities().getFacilities().get(parkingFacilityId);
			
			this.parkingInfrastructure.reserveParking(vehicleId, parkingFacilityId);
		} else {
			// get the closest parking facility (ignore capacity restrictions!!)
//			parkingFacility = this.parkingInfrastructure.getClosestParkingFacility(facility.getCoord());

			/*
			 * Assume that the agent can park at its activity location. The simulation will "fix" this on-the-fly.
			 */
			parkingFacility = facility;
		}

		Id linkId = parkingFacility.getLinkId();
		ActivityImpl activity = (ActivityImpl) this.scenario.getPopulation().getFactory().createActivityFromLinkId(PARKINGACTIVITY, linkId);
		activity.setMaximumDuration(180);
		activity.setCoord(parkingFacility.getCoord());
		activity.setFacilityId(parkingFacility.getId());
		
		return activity;
	}
	
	private Leg createWalkLeg(double departureTime, Id startLinkId, Id endLinkId) {
		Leg walkLeg = this.scenario.getPopulation().getFactory().createLeg(TransportMode.walk);
		walkLeg.setDepartureTime(departureTime);
		walkLeg.setRoute(modeRouteFactory.createRoute(TransportMode.walk, startLinkId, endLinkId));
		return walkLeg;
	}

//	private Coord getActivityCoord(Activity activity) {
//		
//		if (activity.getFacilityId() != null) {
//			return scenario.getActivityFacilities().getFacilities().get(activity.getFacilityId()).getCoord();
//		} else {
//			return scenario.getNetwork().getLinks().get(activity.getLinkId()).getCoord();
//		}
//	}
}