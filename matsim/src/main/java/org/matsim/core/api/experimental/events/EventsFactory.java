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
import org.matsim.core.events.GenericEventImpl;
import org.matsim.core.events.LinkChangeFlowCapacityEventImpl;
import org.matsim.core.events.LinkChangeFreespeedEventImpl;
import org.matsim.core.events.LinkChangeLanesEventImpl;
import org.matsim.core.events.PersonEntersVehicleEvent;
import org.matsim.core.events.PersonLeavesVehicleEvent;
import org.matsim.core.events.SignalGroupStateChangedEvent;
import org.matsim.core.events.SignalGroupStateChangedEventImpl;
import org.matsim.core.events.TransitDriverStartsEvent;
import org.matsim.core.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.events.VehicleArrivesAtFacilityEventImpl;
import org.matsim.core.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.events.VehicleDepartsAtFacilityEventImpl;
import org.matsim.core.network.NetworkChangeEvent.ChangeValue;
import org.matsim.signalsystems.model.SignalGroupState;

/**
 * @author dgrether
 *
 */
public class EventsFactory {

	public ActivityEndEvent createActivityEndEvent(double time, Id agentId, Id linkId, Id facilityId, String acttype) {
		return new ActivityEndEvent(time, agentId, linkId, facilityId, acttype);
	}

	public ActivityStartEvent createActivityStartEvent(double time, Id agentId, Id linkId, Id facilityId, String acttype) {
		return new ActivityStartEvent(time, agentId, linkId, facilityId, acttype);
	}

	public AgentArrivalEvent createAgentArrivalEvent(double time, Id agentId, Id linkId, final String legMode) {
		return new AgentArrivalEvent(time, agentId, linkId, legMode);
	}

	public AgentDepartureEvent createAgentDepartureEvent(double time, Id agentId, Id linkId, final String legMode) {
		return new AgentDepartureEvent(time, agentId, linkId, legMode);
	}

	public AgentMoneyEvent createAgentMoneyEvent(double time, Id agentId, double amountMoney) {
		return new AgentMoneyEvent(time, agentId, amountMoney);
	}

	public AgentStuckEvent createAgentStuckEvent(double time, Id agentId, Id linkId, final String legMode) {
		return new AgentStuckEvent(time, agentId, linkId, legMode);
	}

	public AgentWait2LinkEvent createAgentWait2LinkEvent(double time, Id agentId, Id linkId, Id vehicleId) {
		return new AgentWait2LinkEvent(time, agentId, linkId, vehicleId);
	}

	public LinkEnterEvent createLinkEnterEvent(double time, Id agentId, Id linkId, Id vehicleId) {
		return new LinkEnterEvent(time, agentId, linkId, vehicleId);
	}

	public LinkLeaveEvent createLinkLeaveEvent(double time, Id agentId, Id linkId, Id vehicleId) {
		return new LinkLeaveEvent(time, agentId, linkId, vehicleId);
	}

	public PersonEntersVehicleEvent createPersonEntersVehicleEvent(final double time, final Id personId, final Id vehicleId) {
		PersonEntersVehicleEvent e = new PersonEntersVehicleEvent(time, personId, vehicleId);
		return e;
	}

	public PersonLeavesVehicleEvent createPersonLeavesVehicleEvent(final double time, final Id personId, final Id vehicleId) {
		PersonLeavesVehicleEvent e = new PersonLeavesVehicleEvent(time, personId, vehicleId);
		return e;
	}

	public VehicleArrivesAtFacilityEvent createVehicleArrivesAtFacilityEvent(final double time, final Id vehicleId, final Id facilityId, final double delay) {
		return new VehicleArrivesAtFacilityEventImpl(time, vehicleId, facilityId, delay);
	}

	public VehicleDepartsAtFacilityEvent createVehicleDepartsAtFacilityEvent(final double time, final Id vehicleId, final Id facilityId, final double delay) {
		return new VehicleDepartsAtFacilityEventImpl(time, vehicleId, facilityId, delay);
	}

	public TransitDriverStartsEvent createTransitDriverStartsEvent(final double time, final Id driverId, final Id vehicleId, final Id transitLineId, final Id transitRouteId, final Id departureId) {
		return new TransitDriverStartsEvent(time, driverId, vehicleId, transitLineId, transitRouteId, departureId);
	}
	
	public SignalGroupStateChangedEvent createSignalGroupStateChangedEvent(double time, final Id systemId, final Id groupId, SignalGroupState newState){
		return new SignalGroupStateChangedEventImpl(time, systemId, groupId, newState);
	}

	public LinkChangeEvent createLinkChangeFlowCapacityEvent(double time, Id linkId, ChangeValue changeValue) {
		return new LinkChangeFlowCapacityEventImpl(time, linkId, changeValue);
	}

	public LinkChangeEvent createLinkChangeFreespeedEvent(double time, Id linkId, ChangeValue changeValue) {
		return new LinkChangeFreespeedEventImpl(time, linkId, changeValue);
	}

	public LinkChangeEvent createLinkChangeLanesEvent(double time, Id linkId, ChangeValue changeValue) {
		return new LinkChangeLanesEventImpl(time, linkId, changeValue);
	}

	public GenericEvent createGenericEvent(String type, double time) {
		return new GenericEventImpl( type, time ) ;
	}

}
