/* *********************************************************************** *
 * project: org.matsim.*
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
package playground.wrashid.parkingSearch.withinDay_v_STRC.scoring;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.mobsim.qsim.agents.PlanBasedWithinDayAgent;
import org.matsim.core.population.LegImpl;

import playground.christoph.parking.core.mobsim.ParkingInfrastructure;
import playground.wrashid.parkingSearch.withinDay_v_STRC.WithinDayParkingController;
import playground.wrashid.parkingSearch.withinDay_v_STRC.util.ParkingAgentsTracker_v2;
import playground.wrashid.parkingSearch.withindayFW.parkingTracker.CaptureParkingWalkTimesDuringDay;

public class ParkingScoreManager extends LayerForAddingDataCollectionEventHandlers {

	public ParkingScoreManager(Scenario scenario, ParkingInfrastructure parkingInfrastructure, double distance,
			WithinDayParkingController controler) {
		super(scenario, parkingInfrastructure, distance, controler);
		
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		super.handleEvent(event);
		Id personId = event.getPersonId();

		if (event.getActType().equalsIgnoreCase("parking")){
			PlanBasedWithinDayAgent planBasedWithinDayAgent = this.agents.get(personId);
			
			if (isAgentNextDrivingAwayFromParking(planBasedWithinDayAgent)){
				if (isNextLegFirstCarDepartureOfDay(planBasedWithinDayAgent)){
					//TODO: probably log departure time for scoring of last activity
				} else {
					updateParkingActivityScoreDuringDay(planBasedWithinDayAgent);
				}
			}
			
		}
	}

	private void updateParkingActivityScoreDuringDay(PlanBasedWithinDayAgent planBasedWithinDayAgent) {
		// TODO Auto-generated method stub
		
	}

	private boolean isNextLegFirstCarDepartureOfDay(PlanBasedWithinDayAgent planBasedWithinDayAgent) {
		
		return false;
	}

	private boolean isAgentNextDrivingAwayFromParking(PlanBasedWithinDayAgent planBasedWithinDayAgent) {
		LegImpl leg = (LegImpl) planBasedWithinDayAgent.getNextPlanElement();
		return leg.getMode().equals(TransportMode.car);
	}
	
	

}

