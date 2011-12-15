/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package org.matsim.ptproject.qsim.qnetsimengine;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.ptproject.qsim.interfaces.MobsimVehicle;

/**
 * 
 * Please read the docu of QBufferItem, QLane, QLinkInternalI (arguably to be renamed
 * into something like AbstractQLink) and QLinkImpl jointly. kai, nov'11
 * 
 * 
 * @author nagel
 *
 */
abstract class AbstractQLink extends AbstractQLane implements NetsimLink {
	
	AbstractQLink(Link link, QNetwork network) {
		this.link = link ;
		this.network = network;
		this.netElementActivator = network.simEngine;
	}
	
	final Link link ;
	
	final QNetwork network;	
	
	abstract boolean moveLink(double now);

	abstract QVehicle removeParkedVehicle(Id vehicleId);
	
	abstract void activateLink();

	abstract void addFromIntersection(final QVehicle veh);
	
	abstract void addDepartingVehicle(MobsimVehicle vehicle);
	
	abstract void registerAdditionalAgentOnLink(MobsimAgent planAgent);
	
	abstract MobsimAgent unregisterAdditionalAgentOnLink(Id mobsimAgentId);

	abstract void addParkedVehicle(MobsimVehicle vehicle);
	
	abstract QNode getToNode() ;

	final void letAgentDepartWithVehicle(MobsimDriverAgent agent, QVehicle vehicle, double now) {
		vehicle.setDriver(agent);
		if ( agent.getDestinationLinkId().equals(link.getId()) && (agent.chooseNextLinkId() == null)) {
			// yyyy this should be handled at person level, not vehicle level.  kai, feb'10

			agent.endLegAndAssumeControl(now);
			this.addParkedVehicle(vehicle);
		} else {
			Id transitRouteId = null;
			EventsManager eventsManager = network.simEngine.getMobsim().getEventsManager();
			eventsManager.processEvent(eventsManager.getFactory().createPersonEntersVehicleEvent(now, agent.getId(), vehicle.getId(), transitRouteId));
			this.addDepartingVehicle(vehicle);
		}
	}
	
	// joint implementation for Customizable
	private Map<String, Object> customAttributes = new HashMap<String, Object>();

	protected NetElementActivator netElementActivator;

	@Override
	public final Map<String, Object> getCustomAttributes() {
		return customAttributes;
	}

	public void setNetElementActivator(NetElementActivator qSimEngineRunner) {
		this.netElementActivator = qSimEngineRunner;
	}
	
}
