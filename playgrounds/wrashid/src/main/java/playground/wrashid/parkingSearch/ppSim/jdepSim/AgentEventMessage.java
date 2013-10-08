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
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.Wait2LinkEvent;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.utils.misc.Time;

public class AgentEventMessage extends Message {

	private int planElementIndex;
	private int currentLinkIndex;

	public AgentEventMessage(Person person) {
		this.person = person;
		this.planElementIndex = 0;
		ActivityImpl ai = (ActivityImpl) person.getSelectedPlan().getPlanElements().get(planElementIndex);
		setMessageArrivalTime(ai.getEndTime());
		messageQueue.schedule(this);
	}

	@Override
	public void processEvent() {
		if (person.getSelectedPlan().getPlanElements().get(planElementIndex) instanceof ActivityImpl){
			handleActivityEndEvent();
		} else {
			handleLeg();
		}
	}
	
	
	private void handleLeg() {
		Event event = null;
	
		Leg leg = (LegImpl) person.getSelectedPlan().getPlanElements().get(planElementIndex);
		ActivityImpl nextAct = (ActivityImpl) person.getSelectedPlan().getPlanElements().get(planElementIndex+1);
	
		if (leg.getMode().equalsIgnoreCase(TransportMode.car)){
			List<Id> linkIds = ((LinkNetworkRouteImpl)leg.getRoute()).getLinkIds();
			Id linkId = linkIds.get(currentLinkIndex);
			
			boolean endOfLegReached = currentLinkIndex==linkIds.size()-1;
			
			if (endOfLegReached){
				processEndOfLegReached(leg, nextAct);
				
			} else {
				currentLinkIndex++;
				event=new LinkLeaveEvent(getMessageArrivalTime(),person.getId(),linkId,person.getId());
				eventsManager.processEvent(event);
				
				event=new LinkEnterEvent(getMessageArrivalTime(),person.getId(),linkIds.get(currentLinkIndex),person.getId());
				eventsManager.processEvent(event);
				
				setMessageArrivalTime(getMessageArrivalTime()+ttMatrix.getTravelTime(getMessageArrivalTime(), linkId));
				messageQueue.schedule(this);
			}
		} else {
			processEndOfLegReached(leg, nextAct);
		}
		
	}

	private void processEndOfLegReached(Leg leg, ActivityImpl nextAct) {
		Event event;
		event = new PersonArrivalEvent(getMessageArrivalTime(),person.getId(),nextAct.getLinkId() , leg.getMode());
		eventsManager.processEvent(event);
		
		planElementIndex++;
		boolean isLastActivity = planElementIndex==person.getSelectedPlan().getPlanElements().size()-1;
		
		event = new ActivityStartEvent(getMessageArrivalTime(),person.getId(), nextAct.getLinkId(), nextAct.getFacilityId(), nextAct.getType());
		eventsManager.processEvent(event);
		
		
		if (!isLastActivity){
			double endTimeOfActivity = getEndTimeOfActivity(nextAct,getMessageArrivalTime());

			setMessageArrivalTime(endTimeOfActivity);
			messageQueue.schedule(this);
		}
	}

	private void handleActivityEndEvent() {
		
			Event event = null;
			Id personId = person.getId();
			ActivityImpl ai = (ActivityImpl) person.getSelectedPlan().getPlanElements().get(this.planElementIndex);
			// process first activity
			event = new ActivityEndEvent(getMessageArrivalTime(), personId, ai.getLinkId(), ai.getFacilityId(), ai.getType());
			eventsManager.processEvent(event);

			
			

			int nextLegIndex = this.planElementIndex + 1;
			Leg leg = (LegImpl) person.getSelectedPlan().getPlanElements().get(nextLegIndex);
			

			if (leg.getMode().equalsIgnoreCase(TransportMode.car)) {
				event = new PersonDepartureEvent(getMessageArrivalTime(), personId, leg.getRoute().getStartLinkId(), leg.getMode());
				eventsManager.processEvent(event);

				List<Id> linkIds = ((LinkNetworkRouteImpl) leg.getRoute()).getLinkIds();

				boolean departureAndArrivalNotOnSameLink = linkIds.size() > 0;
				if (departureAndArrivalNotOnSameLink) {
					event = new Wait2LinkEvent(getMessageArrivalTime(), personId, leg.getRoute().getStartLinkId(), personId);
					eventsManager.processEvent(event);
					currentLinkIndex = 0;
					Id linkId = linkIds.get(currentLinkIndex);

					event = new LinkEnterEvent(getMessageArrivalTime(), personId, linkId, personId);
					eventsManager.processEvent(event);
					
					planElementIndex++;
					
					setMessageArrivalTime(getMessageArrivalTime() + ttMatrix.getTravelTime(getMessageArrivalTime(), linkId));
					messageQueue.schedule(this);
				} else {
					planElementIndex++;
					planElementIndex++;
					ActivityImpl act= (ActivityImpl) person.getSelectedPlan().getPlanElements().get(planElementIndex);

					event = new PersonArrivalEvent(getMessageArrivalTime(), person.getId(), act.getLinkId(), leg.getMode());
					eventsManager.processEvent(event);

					
					boolean isLastActivity = planElementIndex==person.getSelectedPlan().getPlanElements().size()-1;
					
						// process last activity
						event = new ActivityStartEvent(getMessageArrivalTime(),person.getId(), act.getLinkId(), act.getFacilityId(), act.getType());
						eventsManager.processEvent(event);
						
					if (!isLastActivity){
						double endTimeOfActivity = getEndTimeOfActivity(act,getMessageArrivalTime());

						setMessageArrivalTime(endTimeOfActivity);
						messageQueue.schedule(this);
					}
				}
			} else {
				// TODO: use proper mode travel time here
				
				event = new PersonDepartureEvent(getMessageArrivalTime(), personId, leg.getRoute().getStartLinkId(), leg.getMode());
				eventsManager.processEvent(event);
				
				planElementIndex++;
				double modeTravelTime=0;
				setMessageArrivalTime(getMessageArrivalTime()+modeTravelTime);
				messageQueue.schedule(this);
				
			}
		
		
	}

	public double getEndTimeOfActivity(Activity act, double arrivalTime) {
		double time = arrivalTime;

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

		time = departureTime;

		return time;
	}

	

}
