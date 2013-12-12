/* *********************************************************************** *
 * project: org.matsim.*
 * AgentSourceFactory.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.christoph.mobsim.modularqsimfactory;

import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.internal.MatsimFactory;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;

public interface AgentSourceFactory extends MatsimFactory {

	public AgentSource createAgentSource(Population population, AgentFactory agentFactory, Netsim sim);
}
