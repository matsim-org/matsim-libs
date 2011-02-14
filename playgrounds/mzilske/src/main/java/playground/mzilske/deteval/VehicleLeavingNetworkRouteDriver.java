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

package playground.mzilske.deteval;

import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsFactoryImpl;
import org.matsim.core.population.routes.NetworkRoute;

import playground.mrieser.core.mobsim.api.DriverAgent;
import playground.mrieser.core.mobsim.api.MobsimVehicle;
import playground.mrieser.core.mobsim.api.NewMobsimEngine;
import playground.mrieser.core.mobsim.api.PlanAgent;
import playground.mrieser.core.mobsim.network.api.MobsimLink;

public class VehicleLeavingNetworkRouteDriver implements DriverAgent {

	private final PlanAgent agent;
	private final MobsimVehicle vehicle;
	private final NewMobsimEngine simEngine;
	private final Id[] linkIds;
	private int nextLinkIndex = 0;
	private Id nextLinkId = null;

	public VehicleLeavingNetworkRouteDriver(final PlanAgent agent, final NewMobsimEngine simEngine, final NetworkRoute route, final MobsimVehicle vehicle) {
		this.agent = agent;
		this.simEngine = simEngine;
		this.vehicle = vehicle;
		List<Id> tmpIds = route.getLinkIds();
		boolean sameEndAsStart = route.getStartLinkId().equals(route.getEndLinkId());
		boolean emptyRoute = tmpIds.size() == 0;

		if (sameEndAsStart && emptyRoute) {
			this.linkIds = new Id[2];
		} else {
			this.linkIds = new Id[3 + tmpIds.size()];
		}
		this.linkIds[0] = route.getStartLinkId();
		int index = 1;
		for (Id id : tmpIds) {
			this.linkIds[index] = id;
			index++;
		}
		if (sameEndAsStart && emptyRoute) {
			this.linkIds[index] = null; // sentinel
		} else {
			this.linkIds[index] = route.getEndLinkId();
			this.linkIds[index+1] = null; // sentinel
		}
		this.nextLinkId = this.linkIds[this.nextLinkIndex];
	}

	@Override
	public Id getNextLinkId() {
		return this.nextLinkId;
	}

	@Override
	public void notifyMoveToNextLink() {
		this.nextLinkIndex++;
		if (this.nextLinkIndex == this.linkIds.length) {
			this.nextLinkIndex--;
		}
		this.nextLinkId = this.linkIds[this.nextLinkIndex];
	}

	@Override
	public double getNextActionOnCurrentLink() {
		if (this.nextLinkId == null) {
			return MobsimLink.POSITION_AT_TO_NODE;
		}
		return -1.0;
	}

	@Override
	public void handleNextAction(final MobsimLink link, final double time) {
		link.parkVehicle(this.vehicle);
		EventsManager eventsManager = this.simEngine.getEventsManager();
		eventsManager.processEvent(((EventsFactoryImpl) eventsManager.getFactory()).createPersonLeavesVehicleEvent(this.simEngine.getCurrentTime(), agent.getPlan().getPerson().getId(), vehicle.getId(), null));
		this.simEngine.handleAgent(this.agent);
	}

}
