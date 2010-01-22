/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.core.mobsim.jdeqsim;

import java.util.List;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.events.ActivityStartEventImpl;
import org.matsim.core.events.AgentArrivalEventImpl;
import org.matsim.core.events.EventImpl;
import org.matsim.core.events.LinkEnterEventImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.utils.misc.Time;

/**
 * The micro-simulation internal handler for ending a leg.
 *
 * @author rashid_waraich
 */
public class EndLegMessage extends EventMessage {

	public EndLegMessage(final Scheduler scheduler, final Vehicle vehicle) {
		super(scheduler, vehicle);
		this.priority = SimulationParameters.PRIORITY_ARRIVAL_MESSAGE;
	}

	@Override
	public void handleMessage() {
		/*
		 * start next leg. assumption: actions and legs are alternating in plans
		 * file
		 */
		this.vehicle.setLegIndex(this.vehicle.getLegIndex() + 2);
		// reset link index
		this.vehicle.setLinkIndex(-1);

		Plan plan = this.vehicle.getOwnerPerson().getSelectedPlan();
		List<? extends PlanElement> actsLegs = plan.getPlanElements();
		if ((actsLegs.size() > this.vehicle.getLegIndex())) {
			this.vehicle.setCurrentLeg((LegImpl) actsLegs.get(this.vehicle.getLegIndex()));
			// current act
			ActivityImpl currentAct = (ActivityImpl) actsLegs.get(this.vehicle.getLegIndex() - 1);
			// the leg the agent performs

			// if only duration or end time of act is defined, take that
			// if both are defined: take the one, which is earlier in time
			double actDurBasedDepartureTime = Double.MAX_VALUE;
			double actEndTimeBasedDepartureTime = Double.MAX_VALUE;

			if (currentAct.getDuration() != Time.UNDEFINED_TIME) {
				actDurBasedDepartureTime = getMessageArrivalTime() + currentAct.getDuration();
			}

			if (currentAct.getEndTime() != Time.UNDEFINED_TIME) {
				actEndTimeBasedDepartureTime = currentAct.getEndTime();
			}

			double departureTime = actDurBasedDepartureTime < actEndTimeBasedDepartureTime ? actDurBasedDepartureTime
					: actEndTimeBasedDepartureTime;

			/*
			 * if the departureTime from the act is in the past (this means we
			 * arrived late), then set the departure time to the current
			 * simulation time this avoids that messages in the past are put
			 * into the scheduler (which makes no sense anyway)
			 */
			if (departureTime < getMessageArrivalTime()) {
				departureTime = getMessageArrivalTime();
			}

			// update current link (we arrived at a new activity)
			this.vehicle.setCurrentLinkId(currentAct.getLinkId());

			Road road = Road.getRoad(this.vehicle.getCurrentLinkId());
			// schedule a departure from the current link in future
			this.vehicle.scheduleStartingLegMessage(departureTime, road);
		}

	}

	@Override
	public void processEvent() {
		EventImpl event = null;

		// schedule enter link event
		// only, if car leg and is not empty
		if (vehicle.getCurrentLeg().getMode().equals(TransportMode.car) && (vehicle.getCurrentLinkRoute()!=null && vehicle.getCurrentLinkRoute().length!=0)){
			event = new LinkEnterEventImpl(this.getMessageArrivalTime(), vehicle.getOwnerPerson().getId(), vehicle.getCurrentLinkId());

			SimulationParameters.getProcessEventThread().processEvent(event);
		}
		
		// schedule AgentArrivalEvent
		event = new AgentArrivalEventImpl(this.getMessageArrivalTime(), this.vehicle.getOwnerPerson().getId(), this.vehicle.getCurrentLinkId(), this.vehicle.getCurrentLeg().getMode());

		SimulationParameters.getProcessEventThread().processEvent(event);

		// schedule ActStartEvent
		ActivityImpl nextAct = this.vehicle.getNextActivity();
		double actStartEventTime = nextAct.getStartTime();

		if (this.getMessageArrivalTime() > actStartEventTime) {
			actStartEventTime = this.getMessageArrivalTime();
		}

		event = new ActivityStartEventImpl(actStartEventTime, this.vehicle.getOwnerPerson().getId(), this.vehicle.getCurrentLinkId(), nextAct.getFacilityId(), nextAct);
		SimulationParameters.getProcessEventThread().processEvent(event);

	}

}
