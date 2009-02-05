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

package org.matsim.mobsim.deqsim;

import java.util.ArrayList;

import org.matsim.events.ActStartEvent;
import org.matsim.events.BasicEvent;
import org.matsim.events.AgentArrivalEvent;
import org.matsim.population.Act;
import org.matsim.population.Leg;
import org.matsim.population.Plan;

public class EndLegMessage extends EventMessage {

	public EndLegMessage(Scheduler scheduler, Vehicle vehicle) {
		super(scheduler, vehicle);
		priority = SimulationParameters.PRIORITY_ARRIVAL_MESSAGE;
	}

	@Override
	public void handleMessage() {
		/*
		 * start next leg. assumption: actions and legs are alternating in plans
		 * file
		 */
		vehicle.setLegIndex(vehicle.getLegIndex() + 2);
		// reset link index
		vehicle.setLinkIndex(-1);

		Plan plan = vehicle.getOwnerPerson().getSelectedPlan();
		ArrayList<Object> actsLegs = plan.getActsLegs();
		if ((actsLegs.size() > vehicle.getLegIndex())) {
			vehicle.setCurrentLeg((Leg) actsLegs.get(vehicle.getLegIndex()));
			// the leg the agent performs
			double departureTime = vehicle.getCurrentLeg().getDepartureTime();

			/*
			 * if the departureTime for the leg is in the past (this means we
			 * arrived late), then set the departure time to the current
			 * simulation time this avoids that messages in the past are put
			 * into the scheduler (which makes no sense anyway)
			 */
			if (departureTime < getMessageArrivalTime()) {
				departureTime = getMessageArrivalTime();
			}

			// update current link (we arrived at a new activity)
			vehicle.setCurrentLink(((Act) actsLegs.get(vehicle.getLegIndex() - 1)).getLink());

			Road road = Road.getRoad(vehicle.getCurrentLink().getId().toString());
			// schedule a departure from the current link in future
			vehicle.scheduleStartingLegMessage(departureTime, road);
		}

	}

	public void processEvent() {
		BasicEvent event = null;

		// schedule AgentArrivalEvent
		event = new AgentArrivalEvent(this.getMessageArrivalTime(), vehicle.getOwnerPerson().getId()
				.toString(), vehicle.getCurrentLink().getId().toString(), vehicle.getLegIndex() - 1);

		SimulationParameters.processEventThread.processEvent(event);

		// schedule ActStartEvent
		Act nextAct = vehicle.getNextActivity();
		double actStartEventTime = nextAct.getStartTime();

		if (this.getMessageArrivalTime() > actStartEventTime) {
			actStartEventTime = this.getMessageArrivalTime();
		}

		event = new ActStartEvent(actStartEventTime, vehicle.getOwnerPerson().getId().toString(), vehicle
				.getCurrentLink().getId().toString(), nextAct.getType());
		SimulationParameters.processEventThread.processEvent(event);

	}

}
