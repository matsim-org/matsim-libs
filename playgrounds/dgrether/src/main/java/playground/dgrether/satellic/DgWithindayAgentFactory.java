/* *********************************************************************** *
 * project: org.matsim.*
 * DgWithindayAgentFactory
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
package playground.dgrether.satellic;

import java.util.Random;

import org.matsim.api.core.v01.population.Person;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.PersonDriverAgentImpl;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;


/**
 * @author dgrether
 *
 */
public class DgWithindayAgentFactory implements AgentFactory {

	private Random random;
	private Netsim simulation ;

	public DgWithindayAgentFactory(QSim simulation, Random random) {
		this.simulation = simulation ;
		this.random = random;
	}

	@Override
	public PersonDriverAgentImpl createMobsimAgentFromPerson(final Person p) {
//		PersonDriverAgentImpl agent = DgWithindayQPersonAgent.createDgWithindayQPersonAgent(p,
//				this.simulation, this.random);
//		return agent;
		throw new RuntimeException("ExperimentalWithindayAgent, on which DgWithindayAgent depended, is no longer ...") ;
	}
	
}
