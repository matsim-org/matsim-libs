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
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;

public class LeaveLinkMessage extends Message{

	private int currentLinkIndex;
	private int currentLegIndex; 
	private double leaveLinkTime;

	public LeaveLinkMessage(double leaveLinkTime, int currentLegIndex, int currentLinkIndex) {
		this.leaveLinkTime = leaveLinkTime;
		this.currentLegIndex = currentLegIndex;
		this.currentLinkIndex = currentLinkIndex;
		setMessageArrivalTime(leaveLinkTime);
	}

	@Override
	public void processEvent() {
		Event event = null;
		Leg leg= (LegImpl) person.getSelectedPlan().getPlanElements().get(currentLegIndex);
		List<Id> linkIds = ((LinkNetworkRouteImpl)leg.getRoute()).getLinkIds();
		Id linkId = linkIds.get(currentLinkIndex);
		
		boolean endOfLegReached = currentLinkIndex==linkIds.size()-1;
		
		if (endOfLegReached){
			event = new AgentArrivalEvent(getMessageArrivalTime(),person.getId(),linkId , leg.getMode());
			eventsManager.processEvent(event);
			
			ActivityImpl act= (ActivityImpl) person.getSelectedPlan().getPlanElements().get(currentLegIndex++);
			
			boolean isLastActivity = currentLegIndex==person.getSelectedPlan().getPlanElements().size()-1;
			if (isLastActivity){
				// process last activity
				event = new ActivityStartEvent(getMessageArrivalTime(),person.getId(), act.getLinkId(), act.getFacilityId(), act.getType());
				eventsManager.processEvent(event);
			} else {
				double activityEndTime = EndActivityMessage.simulateActivity(act, getMessageArrivalTime(), person.getId());
				
				// handle next leg, analog zu code in end activity message.
			}
			
		} else {
			currentLinkIndex++;
			event=new LinkLeaveEvent(getMessageArrivalTime(),person.getId(),linkId,person.getId());
			eventsManager.processEvent(event);
			
			event=new LinkEnterEvent(getMessageArrivalTime(),person.getId(),linkIds.get(currentLinkIndex),person.getId());
			eventsManager.processEvent(event);
			
			setMessageArrivalTime(getMessageArrivalTime()+ttMatrix.getTravelTime(getMessageArrivalTime(), linkId));
			messageQueue.schedule(this);
		}
	}

	

}

