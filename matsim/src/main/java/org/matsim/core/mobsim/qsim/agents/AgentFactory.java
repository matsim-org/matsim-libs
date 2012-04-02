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

package org.matsim.core.mobsim.qsim.agents;

import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.internal.MatsimFactory;
import org.matsim.core.mobsim.framework.MobsimAgent;

public interface AgentFactory extends MatsimFactory {

	/**
     * Seems that in current implementations, this does not only create the agent,
     * but also inserts it into the simulation. This is against our current design
     * principles ("no side effects", "no object registration behind the scenes"), 
     * but this is really old design :-(.  It also means that this is not 
     * a plain factory.  kai, nov'11
	 */
	public MobsimAgent createMobsimAgentFromPerson(final Person p);

}