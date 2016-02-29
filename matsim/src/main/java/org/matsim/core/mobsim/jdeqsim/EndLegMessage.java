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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.mobsim.qsim.agents.ActivityDurationUtils;
import org.matsim.core.population.ActivityImpl;

/**
 * The micro-simulation internal handler for ending a leg.
 *
 * @author rashid_waraich
 */
public class EndLegMessage extends EventMessage {
	private final PlansConfigGroup.ActivityDurationInterpretation activityDurationInterpretation ;
	public EndLegMessage(final Scheduler scheduler, final Vehicle vehicle) {
		// need the time interpretation info here.  Attaching it to the message feels weird.  The scheduler seems a pure simulation object.
		// Consequence: attach it to Vehicle
		super(scheduler, vehicle);
		this.priority = JDEQSimConfigGroup.PRIORITY_ARRIVAL_MESSAGE;
		if ( vehicle == null ) {
			this.activityDurationInterpretation = PlansConfigGroup.ActivityDurationInterpretation.minOfDurationAndEndTime ;
			// need this for some test cases. kai, nov'13
		} else {
			this.activityDurationInterpretation = vehicle.getActivityEndTimeInterpretation() ;
		}
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
			this.vehicle.setCurrentLeg((Leg) actsLegs.get(this.vehicle.getLegIndex()));
			// current act
			ActivityImpl currentAct = (ActivityImpl) actsLegs.get(this.vehicle.getLegIndex() - 1);
			// the leg the agent performs

			double departureTime = ActivityDurationUtils.calculateDepartureTime(currentAct, getMessageArrivalTime(), activityDurationInterpretation) ;

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
		Event event = null;

		// schedule enter link event
		// only, if car leg and is not empty
		if (vehicle.getCurrentLeg().getMode().equals(TransportMode.car) && (vehicle.getCurrentLinkRoute()!=null && vehicle.getCurrentLinkRoute().length!=0)){
			event = new LinkEnterEvent(this.getMessageArrivalTime(), Id.create(vehicle.getOwnerPerson().getId().toString(), org.matsim.vehicles.Vehicle.class), 
					vehicle.getCurrentLinkId());

			eventsManager.processEvent(event);
		}

		// schedule VehicleLeavesTrafficEvent
		Id<org.matsim.vehicles.Vehicle> vehicleId = Id.create( this.vehicle.getOwnerPerson().getId() , org.matsim.vehicles.Vehicle.class ) ;
		event = new VehicleLeavesTrafficEvent(this.getMessageArrivalTime(), this.vehicle.getOwnerPerson().getId(), this.vehicle.getCurrentLinkId(), 
				vehicleId, this.vehicle.getCurrentLeg().getMode(), 1.0 );
		eventsManager.processEvent(event);

		// schedule AgentArrivalEvent
		event = new PersonArrivalEvent(this.getMessageArrivalTime(), this.vehicle.getOwnerPerson().getId(), this.vehicle.getCurrentLinkId(), this.vehicle.getCurrentLeg().getMode());
		eventsManager.processEvent(event);

		// schedule ActStartEvent
		Activity nextAct = this.vehicle.getNextActivity();
		double actStartEventTime = nextAct.getStartTime();

		if (this.getMessageArrivalTime() > actStartEventTime) {
			actStartEventTime = this.getMessageArrivalTime();
		}

		event = new ActivityStartEvent(actStartEventTime, this.vehicle.getOwnerPerson().getId(), this.vehicle.getCurrentLinkId(), nextAct.getFacilityId(), nextAct.getType());
		eventsManager.processEvent(event);

	}

}
