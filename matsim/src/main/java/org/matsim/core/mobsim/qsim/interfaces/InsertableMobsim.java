package org.matsim.core.mobsim.qsim.interfaces;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.framework.MobsimAgent;

/**
 * Interface providing methods to insert agents and vehicles into the simulation.
 */
public interface InsertableMobsim {

	void addParkedVehicle(MobsimVehicle veh, Id<Link> startLinkId);

	void insertAgentIntoMobsim(MobsimAgent agent);

}
