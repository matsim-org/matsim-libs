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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;

public interface InternalInterface {
	public Netsim getMobsim(); 
	public void arrangeNextAgentState(MobsimAgent agent);
	void registerAdditionalAgentOnLink(MobsimAgent agent);
	MobsimAgent unregisterAdditionalAgentOnLink(Id<Person> agentId, Id<Link> linkId);
	@Deprecated // use same method from QSim directly and try to get rid of the handle to internal interface. kai, mar'15
	public void rescheduleActivityEnd(MobsimAgent agent);
}
