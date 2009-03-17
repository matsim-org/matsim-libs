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

package org.matsim.mobsim.jdeqsim;

import java.util.List;

import org.matsim.events.ActStartEvent;
import org.matsim.events.AgentArrivalEvent;
import org.matsim.events.BasicEventImpl;
import org.matsim.interfaces.basic.v01.population.BasicPlanElement;
import org.matsim.interfaces.core.v01.Activity;
import org.matsim.interfaces.core.v01.Leg;
import org.matsim.interfaces.core.v01.Plan;
import org.matsim.utils.misc.Time;

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
		List<? extends BasicPlanElement> actsLegs = plan.getPlanElements();
		if ((actsLegs.size() > this.vehicle.getLegIndex())) {
			this.vehicle.setCurrentLeg((Leg) actsLegs.get(this.vehicle.getLegIndex()));
			// current act
			Activity currentAct = (Activity) actsLegs.get(this.vehicle.getLegIndex() - 1);
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
			this.vehicle.setCurrentLink(currentAct.getLink());

			Road road = Road.getRoad(this.vehicle.getCurrentLink().getId().toString());
			// schedule a departure from the current link in future
			this.vehicle.scheduleStartingLegMessage(departureTime, road);
		}

	}

	@Override
	public void processEvent() {
		BasicEventImpl event = null;

		// schedule AgentArrivalEvent
		event = new AgentArrivalEvent(this.getMessageArrivalTime(), this.vehicle.getOwnerPerson(), this.vehicle.getCurrentLink(), this.vehicle.getCurrentLeg());

		SimulationParameters.getProcessEventThread().processEvent(event);

		// schedule ActStartEvent
		Activity nextAct = this.vehicle.getNextActivity();
		double actStartEventTime = nextAct.getStartTime();

		if (this.getMessageArrivalTime() > actStartEventTime) {
			actStartEventTime = this.getMessageArrivalTime();
		}

		event = new ActStartEvent(actStartEventTime, this.vehicle.getOwnerPerson(), this.vehicle.getCurrentLink(), nextAct);
		SimulationParameters.getProcessEventThread().processEvent(event);

	}

}
