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

package soc.ai.matsim.dbsim;

import org.matsim.api.core.v01.population.Person;

/**
 * @author dgrether
 */
public class AgentFactory {

	protected final DBSimulation simulation;

	public AgentFactory(final DBSimulation simulation) {
		this.simulation = simulation;
	}

	public PersonAgent createPersonAgent(final Person p) {
		PersonAgent agent = new PersonAgent(p, this.simulation);
		return agent;
	}

}
