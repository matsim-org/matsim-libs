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
package playground.wrashid.parkingSearch.ppSim.jdepSim;

import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.Wait2LinkEvent;
import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.utils.misc.Time;

public class EndActivityMessage extends Message{

	

	private int planElementIndex;
	
	// use for first activity only
	EndActivityMessage(){
		this.planElementIndex = 0;
		ActivityImpl ai= (ActivityImpl) person.getSelectedPlan().getPlanElements().get(planElementIndex);
		setMessageArrivalTime(ai.getEndTime());
	}
	
	EndActivityMessage(int planElementIndex, double arrivalTime){
		this.planElementIndex = planElementIndex;
		ActivityImpl ai= (ActivityImpl) person.getSelectedPlan().getPlanElements().get(planElementIndex);
		double endActivityTime = simulateActivity(ai, arrivalTime, person.getId());
		setMessageArrivalTime(endActivityTime);
	}
	
	@Override
	public void processEvent() {
			Event event = null;
			
			
			ActivityImpl ai= (ActivityImpl) person.getSelectedPlan().getPlanElements().get(this.planElementIndex);
			
			// process first activity
			Id personId = person.getId();
			if (this.planElementIndex==0){
				event = new ActivityEndEvent(getMessageArrivalTime(), personId, ai.getLinkId(), ai.getFacilityId(), ai.getType());
				eventsManager.processEvent(event);
			}
			
			int planElemSize = person.getSelectedPlan().getPlanElements().size();
			
			
			if (planElementIndex<planElemSize-1){
				int nextLegIndex = this.planElementIndex+1;
				Leg leg= (LegImpl) person.getSelectedPlan().getPlanElements().get(nextLegIndex);
				
				
			
			
				if (leg.getMode().equalsIgnoreCase(TransportMode.car)){
						event = new AgentDepartureEvent(getMessageArrivalTime(), personId, leg.getRoute().getStartLinkId() , leg.getMode());
						eventsManager.processEvent(event);
						
						List<Id> linkIds = ((LinkNetworkRouteImpl)leg.getRoute()).getLinkIds();
				
						if (linkIds.size()>2){
							event=new Wait2LinkEvent(getMessageArrivalTime(), personId,leg.getRoute().getStartLinkId(),personId);
							eventsManager.processEvent(event);
							int linkIndex = 1;
							Id linkId = linkIds.get(linkIndex);
							
							event=new LinkEnterEvent(getMessageArrivalTime(),personId,linkId,personId);
							eventsManager.processEvent(event);
							
							LeaveLinkMessage leaveLinkMessage = new LeaveLinkMessage(getMessageArrivalTime()+ttMatrix.getTravelTime(getMessageArrivalTime(), linkId),nextLegIndex, linkIndex);
							messageQueue.schedule(leaveLinkMessage);
						} else {
							// we know when arrival + next end energy event
							// departure and arrival on same link
						}
				
				} else {
					// we know when arrival + next end energy event
					// non-car mode
				}
			}
			
			
	}
	
	
	public static double simulateActivity(Activity act, double arrivalTime, Id personId){
		double time=arrivalTime;
		
		Event event = new ActivityStartEvent(time, personId, act.getLinkId(), act.getFacilityId(), act.getType());
		eventsManager.processEvent(event);
		
		double actDurBasedDepartureTime = Double.MAX_VALUE;
		double actEndTimeBasedDepartureTime = Double.MAX_VALUE;

		if (act.getMaximumDuration() != Time.UNDEFINED_TIME) {
			actDurBasedDepartureTime = time + act.getMaximumDuration();
		}

		if (act.getEndTime() != Time.UNDEFINED_TIME) {
			actEndTimeBasedDepartureTime = act.getEndTime();
		}

		double departureTime = actDurBasedDepartureTime < actEndTimeBasedDepartureTime ? actDurBasedDepartureTime
				: actEndTimeBasedDepartureTime;
		
		
		if (departureTime < time) {
			departureTime = time;
		}
		
		time=departureTime;
		
		event = new ActivityEndEvent(time, personId, act.getLinkId(), act.getFacilityId(), act.getType());
		eventsManager.processEvent(event);

		return time;
	}


}

