/* *********************************************************************** *
 * project: org.matsim.*
 * EventsBuilderImpl
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package org.matsim.core.api.experimental.events;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.GenericEvent;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.Wait2LinkEvent;
import org.matsim.signalsystems.model.SignalGroupState;

/**
 * Collection of static helper methods to create events.
 * Can be used in place of direct constructor calls.
 * 
 * @author dgrether
 */
public class EventsFactory {

	public static ActivityEndEvent createActivityEndEvent(double time, Id agentId, Id linkId, Id facilityId, String acttype) {
		return new ActivityEndEvent(time, agentId, linkId, facilityId, acttype);
	}

	public static ActivityStartEvent createActivityStartEvent(double time, Id agentId, Id linkId, Id facilityId, String acttype) {
		return new ActivityStartEvent(time, agentId, linkId, facilityId, acttype);
	}

	public static PersonArrivalEvent createAgentArrivalEvent(double time, Id agentId, Id linkId, final String legMode) {
		return new PersonArrivalEvent(time, agentId, linkId, legMode);
	}

	public static PersonDepartureEvent createAgentDepartureEvent(double time, Id agentId, Id linkId, final String legMode) {
		return new PersonDepartureEvent(time, agentId, linkId, legMode);
	}

	public static PersonMoneyEvent createAgentMoneyEvent(double time, Id agentId, double amountMoney) {
		return new PersonMoneyEvent(time, agentId, amountMoney);
	}

	public static PersonStuckEvent createAgentStuckEvent(double time, Id agentId, Id linkId, final String legMode) {
		return new PersonStuckEvent(time, agentId, linkId, legMode);
	}

	public static Wait2LinkEvent createAgentWait2LinkEvent(double time, Id agentId, Id linkId, Id vehicleId) {
		return new Wait2LinkEvent(time, agentId, linkId, vehicleId);
	}

	public static LinkEnterEvent createLinkEnterEvent(double time, Id agentId, Id linkId, Id vehicleId) {
		return new LinkEnterEvent(time, agentId, linkId, vehicleId);
	}

	public static LinkLeaveEvent createLinkLeaveEvent(double time, Id agentId, Id linkId, Id vehicleId) {
		return new LinkLeaveEvent(time, agentId, linkId, vehicleId);
	}

	public static PersonEntersVehicleEvent createPersonEntersVehicleEvent(final double time, final Id personId, final Id vehicleId) {
		PersonEntersVehicleEvent e = new PersonEntersVehicleEvent(time, personId, vehicleId);
		return e;
	}

	public static PersonLeavesVehicleEvent createPersonLeavesVehicleEvent(final double time, final Id personId, final Id vehicleId) {
		PersonLeavesVehicleEvent e = new PersonLeavesVehicleEvent(time, personId, vehicleId);
		return e;
	}

	public static VehicleArrivesAtFacilityEvent createVehicleArrivesAtFacilityEvent(final double time, final Id vehicleId, final Id facilityId, final double delay) {
		return new VehicleArrivesAtFacilityEvent(time, vehicleId, facilityId, delay);
	}

	public static VehicleDepartsAtFacilityEvent createVehicleDepartsAtFacilityEvent(final double time, final Id vehicleId, final Id facilityId, final double delay) {
		return new VehicleDepartsAtFacilityEvent(time, vehicleId, facilityId, delay);
	}

	public static TransitDriverStartsEvent createTransitDriverStartsEvent(final double time, final Id driverId, final Id vehicleId, final Id transitLineId, final Id transitRouteId, final Id departureId) {
		return new TransitDriverStartsEvent(time, driverId, vehicleId, transitLineId, transitRouteId, departureId);
	}
	
	public static SignalGroupStateChangedEvent createSignalGroupStateChangedEvent(double time, final Id systemId, final Id groupId, SignalGroupState newState){
		return new SignalGroupStateChangedEvent(time, systemId, groupId, newState);
	}

	public static GenericEvent createGenericEvent(String type, double time) {
		return new GenericEvent( type, time ) ;
	}

}
