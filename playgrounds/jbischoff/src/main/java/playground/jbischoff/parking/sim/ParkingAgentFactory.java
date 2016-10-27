/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.jbischoff.parking.sim;

import javax.inject.Inject;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dynagent.DynAgent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;

import playground.jbischoff.parking.DynAgent.agentLogic.ParkingAgentLogic;
import playground.jbischoff.parking.choice.ParkingChoiceLogic;
import playground.jbischoff.parking.choice.RandomParkingChoiceLogic;
import playground.jbischoff.parking.manager.ParkingSearchManager;
import playground.jbischoff.parking.manager.WalkLegFactory;
import playground.jbischoff.parking.manager.vehicleteleportationlogic.VehicleTeleportationLogic;
import playground.jbischoff.parking.routing.ParkingRouter;

/**
 * @author jbischoff
 *
 */

public class ParkingAgentFactory implements AgentFactory {

	/**
	 * 
	 */
	@Inject
	WalkLegFactory walkLegFactory;
	@Inject
	ParkingSearchManager parkingManager;

	@Inject
	EventsManager events;
	@Inject
	ParkingRouter parkingRouter;
	@Inject
	Network network;
	@Inject
	VehicleTeleportationLogic teleportationLogic;

	private final QSim qsim;

	/**
	 * 
	 */
	@Inject
	public ParkingAgentFactory(QSim qsim) {
		this.qsim = qsim;
	}

	@Override
	public MobsimAgent createMobsimAgentFromPerson(Person p) {
		ParkingChoiceLogic parkingLogic  = new RandomParkingChoiceLogic(network);
		ParkingAgentLogic agentLogic = new ParkingAgentLogic(p.getSelectedPlan(), parkingManager, walkLegFactory,
				parkingRouter, events, parkingLogic,  ((QSim) qsim).getSimTimer(),teleportationLogic );
		Id<Link> startLinkId = ((Activity) p.getSelectedPlan().getPlanElements().get(0)).getLinkId();
		if (startLinkId == null) {
			throw new NullPointerException(" No start link found. Should not happen.");
		}
		DynAgent agent = new DynAgent(p.getId(), startLinkId, events, agentLogic);
		return agent;
	}

}
