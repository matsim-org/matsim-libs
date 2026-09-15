/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package org.matsim.core.mobsim.qsim;

import org.apache.commons.lang3.NotImplementedException;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.mobsim.dsim.DistributedMobsimAgent;
import org.matsim.core.mobsim.dsim.DistributedMobsimVehicle;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.interfaces.DepartureHandler;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;

import java.util.Collection;

/**
 * Design thoughts:<ul>
 * <li> The main functionality of this interface is arrangeNextAgentState.
 * <li> getMobsim is provided as a convenience.
 * </ul>
 *
 * @author nagel
 *
 */
public interface InternalInterface {

	void arrangeNextAgentState(MobsimAgent agent);

	void registerAdditionalAgentOnLink(MobsimAgent agent);

	MobsimAgent unregisterAdditionalAgentOnLink(Id<Person> agentId, Id<Link> linkId);

	// so far we only have agents or vehicles moving as physical objects in the simulation. Therefore, we hardcode those two
	// options here. If, at some point in time, more flexibility is needed, a more generic approach would be necessary.

	default void notifyAgentEntersPartition(DistributedMobsimAgent agent) {
		throw new NotImplementedException(getClass().getName() + " does not implement notifyAgentEnteringPartition");
	}

	/**
	 * Callback to notify the mobsim, that an agent is leaving the partition.
	 */
	default void notifyAgentLeavesPartition(DistributedMobsimAgent agent, int toPartition) {
		throw new NotImplementedException(getClass().getName() + " does not implement notifyAgentLeavesPartition");
	}

	/**
	 * Callback to notify the mobsim, that a vehicle is entering the partition. It is expected that {@link #notifyAgentEntersPartition(DistributedMobsimAgent)}
	 * is called for all agents inside that vehicle as well. This is necessary, as the mobsim does not know about the vehicles and its occupants.
	 */
	default void notifyVehicleEntersPartition(DistributedMobsimVehicle vehicle) {
		throw new NotImplementedException(getClass().getName() + " does not implement notifyVehicleEnteringPartition");
	}

	/**
	 * Callback to notify the mobsim, that a vehicle is leaving the partition. It is expected that {@link #notifyAgentLeavesPartition(DistributedMobsimAgent, int)}
	 * is called for all agents inside that vehicle as well. This is necessary, as the mobsim does not know about the vehicles and its occupants.
	 */
	default void notifyVehicleLeavesPartition(DistributedMobsimVehicle vehicle, int toPartition) {
		throw new NotImplementedException(getClass().getName() + " does not implement notifyAgentLeavesPartition");
	}

	// the methods below allow access to the state of the simulation. I don't think those belong into this interface
	// janek mar' 26
	Netsim getMobsim();

	Collection<? extends DepartureHandler> getDepartureHandlers();
}
