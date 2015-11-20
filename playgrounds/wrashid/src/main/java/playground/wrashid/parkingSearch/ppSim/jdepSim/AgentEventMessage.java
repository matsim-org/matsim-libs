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
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.parking.lib.DebugLib;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.utils.misc.Time;
import org.matsim.vehicles.Vehicle;

public class AgentEventMessage extends Message {

	protected int planElementIndex;
	protected int currentLinkIndex;

	public AgentEventMessage(Person person) {
		this.setPerson(person);
		this.setPlanElementIndex(0);
		ActivityImpl ai = (ActivityImpl) person.getSelectedPlan().getPlanElements().get(getPlanElementIndex());
		setMessageArrivalTime(ai.getEndTime());
		messageQueue.schedule(this);
	}

	protected AgentEventMessage() {

	}

	@Override
	public void processEvent() {
		if (getPerson().getSelectedPlan().getPlanElements().get(getPlanElementIndex()) instanceof ActivityImpl) {
			handleActivityEndEvent();
		} else {
			handleLeg();
		}
	}

	protected void handleLeg() {
		Event event = null;

		Leg leg = (LegImpl) getPerson().getSelectedPlan().getPlanElements().get(getPlanElementIndex());
		ActivityImpl prevAct = (ActivityImpl) getPerson().getSelectedPlan().getPlanElements().get(getPlanElementIndex() - 1);
		ActivityImpl nextAct = (ActivityImpl) getPerson().getSelectedPlan().getPlanElements().get(getPlanElementIndex() + 1);

		if (leg.getMode().equalsIgnoreCase(TransportMode.car)) {

			List<Id<Link>> linkIds = ((LinkNetworkRouteImpl) leg.getRoute()).getLinkIds();

			boolean endOfLegReached = getCurrentLinkIndex() == linkIds.size() - 1;

			if (endOfLegReached) {
				processEndOfLegCarMode(leg, nextAct);

			} else {
				Id<Link> currentLinkId = null;
				if (getCurrentLinkIndex() == -1) {
					currentLinkId = prevAct.getLinkId();
				} else {
					currentLinkId = linkIds.get(getCurrentLinkIndex());
				}

				event = new LinkLeaveEvent(getMessageArrivalTime(), getPerson().getId(), currentLinkId, 
						Id.create(getPerson().getId().toString(), Vehicle.class));
				eventsManager.processEvent(event);

				setCurrentLinkIndex(getCurrentLinkIndex() + 1);
				currentLinkId = linkIds.get(getCurrentLinkIndex());

				event = new LinkEnterEvent(getMessageArrivalTime(), getPerson().getId(), currentLinkId, 
						Id.create(getPerson().getId().toString(), Vehicle.class));
				eventsManager.processEvent(event);

				setMessageArrivalTime(getMessageArrivalTime() + ttMatrix.getTravelTime(getMessageArrivalTime(), currentLinkId));
				messageQueue.schedule(this);
			}
		} else {
			processEndOfLegNonCarMode(leg, nextAct);
		}
	}

	public void processEndOfLegCarMode(Leg leg, ActivityImpl nextAct) {
		processEndOfLegCarMode_processEvents(leg, nextAct);

		processEndOfLegCarMode_scheduleNextActivityEndEventIfNeeded(nextAct);
	}

	public void processEndOfLegCarMode_scheduleNextActivityEndEventIfNeeded(ActivityImpl nextAct) {
		// TODO: probably this function is not needed: if we have car leg, it is
		// always followed
		// by car parking activity...

		boolean isLastActivity = duringLeg_isNextActivityLastActivityOfDay();
		setPlanElementIndex(getPlanElementIndex() + 1);

		if (!isLastActivity) {
			double endTimeOfActivity = getEndTimeOfActivity(nextAct, getMessageArrivalTime());

			setMessageArrivalTime(endTimeOfActivity);
			messageQueue.schedule(this);
		}
	}

	public boolean duringLeg_isNextActivityLastActivityOfDay() {
		boolean isLastActivity = getPlanElementIndex() + 1 == getPerson().getSelectedPlan().getPlanElements().size() - 1;
		return isLastActivity;
	}

	public void processEndOfLegCarMode_processEvents(Leg leg, ActivityImpl nextAct) {
		Event event;

		List<Id<Link>> linkIds = ((LinkNetworkRouteImpl) leg.getRoute()).getLinkIds();
		Id<Link> currentLinkId = null;
		if (getCurrentLinkIndex() == -1) {
			currentLinkId = ((LinkNetworkRouteImpl) leg.getRoute()).getStartLinkId();
		} else {
			currentLinkId = linkIds.get(getCurrentLinkIndex());
		}

		event = new LinkLeaveEvent(getMessageArrivalTime(), getPerson().getId(), currentLinkId, 
				Id.create(getPerson().getId().toString(), Vehicle.class));
		eventsManager.processEvent(event);

		Id<Link> endLinkId = leg.getRoute().getEndLinkId();
		event = new LinkEnterEvent(getMessageArrivalTime(), getPerson().getId(), endLinkId, 
				Id.create(getPerson().getId().toString(), Vehicle.class));
		eventsManager.processEvent(event);

		event = new PersonArrivalEvent(getMessageArrivalTime(), getPerson().getId(), endLinkId, leg.getMode());
		eventsManager.processEvent(event);

		event = new ActivityStartEvent(getMessageArrivalTime(), getPerson().getId(), endLinkId, nextAct.getFacilityId(),
				nextAct.getType());
		eventsManager.processEvent(event);
	}

	protected void processEndOfLegNonCarMode(Leg leg, ActivityImpl nextAct) {
		Event event;
		event = new PersonArrivalEvent(getMessageArrivalTime(), getPerson().getId(), nextAct.getLinkId(), leg.getMode());
		eventsManager.processEvent(event);

		boolean isLastActivity = duringLeg_isNextActivityLastActivityOfDay();
		setPlanElementIndex(getPlanElementIndex() + 1);

		event = new ActivityStartEvent(getMessageArrivalTime(), getPerson().getId(), nextAct.getLinkId(), nextAct.getFacilityId(),
				nextAct.getType());
		eventsManager.processEvent(event);

		if (!isLastActivity) {
			double endTimeOfActivity = getEndTimeOfActivity(nextAct, getMessageArrivalTime());

			setMessageArrivalTime(endTimeOfActivity);
			messageQueue.schedule(this);
		}
	}

	protected void handleActivityEndEvent() {
		Event event = null;
		Id personId = getPerson().getId();
		ActivityImpl curAct = (ActivityImpl) getPerson().getSelectedPlan().getPlanElements().get(this.getPlanElementIndex());

		// process first activity
		event = new ActivityEndEvent(getMessageArrivalTime(), personId, curAct.getLinkId(), curAct.getFacilityId(),
				curAct.getType());
		eventsManager.processEvent(event);

		int nextLegIndex = this.getPlanElementIndex() + 1;
		Leg leg = (LegImpl) getPerson().getSelectedPlan().getPlanElements().get(nextLegIndex);

		if (leg.getMode().equalsIgnoreCase(TransportMode.car)) {
		//	AgentWithParking.parkingManager.unParkAgentVehicle(getPerson().getId());
			
			event = new PersonDepartureEvent(getMessageArrivalTime(), personId, leg.getRoute().getStartLinkId(), leg.getMode());
			eventsManager.processEvent(event);

			ActivityImpl nextAct = (ActivityImpl) getPerson().getSelectedPlan().getPlanElements().get(getPlanElementIndex() + 2);
			boolean departureAndArrivalOnSameLink = curAct.getLinkId().toString().equalsIgnoreCase(nextAct.getLinkId().toString());
			if (departureAndArrivalOnSameLink) {
				
				DebugLib.stopSystemAndReportInconsistency("this should not happen due to current assumptions");
				// => increase distance cutoff car leg or properly implement here => future
				
				setPlanElementIndex(getPlanElementIndex() + 1);
				setPlanElementIndex(getPlanElementIndex() + 1);
				ActivityImpl act = (ActivityImpl) getPerson().getSelectedPlan().getPlanElements().get(getPlanElementIndex());

				event = new PersonArrivalEvent(getMessageArrivalTime(), getPerson().getId(), act.getLinkId(), leg.getMode());
				eventsManager.processEvent(event);

				boolean isCurrentActivityLastActivityOfDay = getPlanElementIndex() == getPerson().getSelectedPlan()
						.getPlanElements().size() - 1;

				// process last activity
				event = new ActivityStartEvent(getMessageArrivalTime(), getPerson().getId(), act.getLinkId(), act.getFacilityId(),
						act.getType());
				eventsManager.processEvent(event);

				if (!isCurrentActivityLastActivityOfDay) {
					double endTimeOfActivity = getEndTimeOfActivity(act, getMessageArrivalTime());

					setMessageArrivalTime(endTimeOfActivity);
					messageQueue.schedule(this);
				}
				

			} else {
				event = new VehicleEntersTrafficEvent(getMessageArrivalTime(), personId, leg.getRoute().getStartLinkId(), personId, leg.getMode(), 1.0);
				eventsManager.processEvent(event);
				setCurrentLinkIndex(-1);
				Id linkId = curAct.getLinkId();

				setPlanElementIndex(getPlanElementIndex() + 1);

				setMessageArrivalTime(getMessageArrivalTime() + ttMatrix.getTravelTime(getMessageArrivalTime(), linkId));
				messageQueue.schedule(this);
			}
		} else {

			event = new PersonDepartureEvent(getMessageArrivalTime(), personId, curAct.getLinkId(), leg.getMode());
			eventsManager.processEvent(event);

			setPlanElementIndex(getPlanElementIndex() + 1);
			setMessageArrivalTime(getMessageArrivalTime() + leg.getTravelTime());
			messageQueue.schedule(this);

		}

	}

	protected double getEndTimeOfActivity(Activity act, double arrivalTime) {
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

	public int getPlanElementIndex() {
		return planElementIndex;
	}

	public void setPlanElementIndex(int planElementIndex) {
		this.planElementIndex = planElementIndex;
	}

	public int getCurrentLinkIndex() {
		return currentLinkIndex;
	}

	public void setCurrentLinkIndex(int currentLinkIndex) {
		this.currentLinkIndex = currentLinkIndex;
	}

	public int duringCarLeg_getPlanElementIndexOfNextCarLeg() {
		Plan selectedPlan = getPerson().getSelectedPlan();
		List<PlanElement> planElements = selectedPlan.getPlanElements();

		int i = planElementIndex + 1;
		while (i < planElements.size()) {
			if (planElements.get(i) instanceof LegImpl) {
				Leg leg = (Leg) planElements.get(i);
				if (leg.getMode().equalsIgnoreCase(TransportMode.car)) {
					return i;
				}
			}
			i++;
		}

		return -1;

	}
	
	public int duringAct_getPlanElementIndexOfPreviousCarLeg() {
		Plan selectedPlan = getPerson().getSelectedPlan();
		List<PlanElement> planElements = selectedPlan.getPlanElements();

		int i = planElementIndex;
		while (i > 0) {
			if (planElements.get(i) instanceof LegImpl) {
				Leg leg = (Leg) planElements.get(i);
				if (leg.getMode().equalsIgnoreCase(TransportMode.car)) {
					return i;
				}
			}
			i--;
		}

		return -1;
	}
	
	public int getPlanElementIndexOfLastCarLeg() {
		Plan selectedPlan = getPerson().getSelectedPlan();
		List<PlanElement> planElements = selectedPlan.getPlanElements();

		int i =  planElements.size()-1;
		while (i > 0) {
			if (planElements.get(i) instanceof LegImpl) {
				Leg leg = (Leg) planElements.get(i);
				if (leg.getMode().equalsIgnoreCase(TransportMode.car)) {
					return i;
				}
			}
			i--;
		}

		return -1;
	}
	public int getIndexOfFirstCarLegOfDay() {
		Plan selectedPlan = getPerson().getSelectedPlan();
		List<PlanElement> planElements = selectedPlan.getPlanElements();

		int i = 0;
		while (i < planElements.size()) {
			if (planElements.get(i) instanceof LegImpl) {
				Leg leg = (Leg) planElements.get(i);
				if (leg.getMode().equalsIgnoreCase(TransportMode.car)) {
					return i;
				}
			}
			i++;
		}

		return -1;
	}

}
