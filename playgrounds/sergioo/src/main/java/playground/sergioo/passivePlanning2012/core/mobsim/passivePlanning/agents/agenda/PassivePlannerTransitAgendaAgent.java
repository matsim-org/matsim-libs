/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.sergioo.passivePlanning2012.core.mobsim.passivePlanning.agents.agenda;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.utils.misc.Time;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.Facility;
import org.matsim.pt.PtConstants;

import playground.sergioo.passivePlanning2012.api.population.BasePerson;
import playground.sergioo.passivePlanning2012.core.mobsim.passivePlanning.agents.PassivePlannerTransitAgent;
import playground.sergioo.passivePlanning2012.core.population.PlacesSharer;
import playground.sergioo.passivePlanning2012.population.parallelPassivePlanning.PassivePlannerManager;

public class PassivePlannerTransitAgendaAgent extends PassivePlannerTransitAgent  {

	private double legBeginning = Time.UNDEFINED_TIME;
	private Id<ActivityFacility> prevFacilityId = null;
	
	//Constructors
	public PassivePlannerTransitAgendaAgent(final BasePerson basePerson, final Netsim simulation, final PassivePlannerManager passivePlannerManager) {
		super(basePerson, simulation, passivePlannerManager);
		planner = new SinglePlannerAgendaAgent(this);
	}
	
	@Override
	public void endActivityAndComputeNextState(double now) {
		Activity prevAct = (Activity)getCurrentPlanElement();
		if(!prevAct.getType().equals(PtConstants.TRANSIT_ACTIVITY_TYPE)) {
			double time = 0;
			for(PlanElement planElement:getBasePerson().getSelectedPlan().getPlanElements()) {
				if(planElement == prevAct)
					break;
				if(planElement instanceof Activity)
					if(((Activity)planElement).getEndTime()==Time.UNDEFINED_TIME)
						time += ((Activity)planElement).getMaximumDuration();
					else
						time = ((Activity)planElement).getEndTime();
				else
					time += ((Leg)planElement).getTravelTime();
			}
			if(prevAct.getFacilityId()!=null)
				((SinglePlannerAgendaAgent)planner).shareKnownPlace(prevAct.getFacilityId(), time, prevAct.getType());
			legBeginning = now;
			prevFacilityId = prevAct.getFacilityId();
		}
		super.endActivityAndComputeNextState(now);
	}
	@Override
	public void endLegAndComputeNextState(double now) {
		Activity nextAct = ((Activity)getNextPlanElement());
		if(!nextAct.getType().equals(PtConstants.TRANSIT_ACTIVITY_TYPE))
			if(prevFacilityId!=null)
				if(legBeginning==Time.UNDEFINED_TIME)
					throw new RuntimeException("Leg finished with a previous valid activity time");
				else {
					String mode = ((Leg)getCurrentPlanElement()).getMode();
					((SinglePlannerAgendaAgent)planner).getPlaceSharer().addKnownTravelTime(prevFacilityId,
							nextAct.getFacilityId(),mode.equals(TransportMode.transit_walk)?"pt":mode, legBeginning,
							now-legBeginning);
					((SinglePlannerAgendaAgent)planner).shareKnownTravelTime(prevFacilityId, nextAct.getFacilityId(),
							mode.equals(TransportMode.transit_walk)?"pt":mode, legBeginning, now-legBeginning);
					legBeginning = Time.UNDEFINED_TIME;
					prevFacilityId = null;
				}
			else
				throw new RuntimeException("Leg finished with a previous valid activity facility");
		super.endLegAndComputeNextState(now);
	}
	public PlacesSharer getPlaceSharer() {
		return ((SinglePlannerAgendaAgent)planner).getPlaceSharer();
	}
	public void addKnownPerson(PlacesSharer placeSharer) {
		((SinglePlannerAgendaAgent)planner).addKnownPerson(placeSharer);
	}

}
