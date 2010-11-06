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

package org.matsim.ptproject.qsim.agents;

import org.matsim.api.core.v01.population.Person;
import org.matsim.core.mobsim.framework.PersonDriverAgent;
import org.matsim.ptproject.qsim.interfaces.Mobsim;

/**
 * @author dgrether
 */
@Deprecated // should be final, i.e. inheritance from this class is deprecated.  Please use the interface directly.  kai, oct'10
public class DefaultAgentFactory implements AgentFactory {

	protected final Mobsim simulation;

	public DefaultAgentFactory(final Mobsim simulation) {
		this.simulation = simulation;
	}

	public PersonDriverAgent createPersonAgent(final Person p) {
		PersonDriverAgent agent = new PersonDriverAgentImpl(p, this.simulation);
		return agent;
	}

}
