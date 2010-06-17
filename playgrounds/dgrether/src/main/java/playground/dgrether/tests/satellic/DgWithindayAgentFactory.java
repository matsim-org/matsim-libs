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
package playground.dgrether.tests.satellic;

import java.util.Random;

import org.matsim.api.core.v01.population.Person;
import org.matsim.ptproject.qsim.AgentFactory;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.ptproject.qsim.helpers.QPersonAgent;


/**
 * @author dgrether
 *
 */
public class DgWithindayAgentFactory extends AgentFactory {

	private Random random;

	public DgWithindayAgentFactory(QSim simulation, Random random) {
		super(simulation);
		this.random = random;
	}

	@Override
	public QPersonAgent createPersonAgent(final Person p) {
		QPersonAgent agent = new DgWithindayQPersonAgent(p, this.simulation, this.random);
		return agent;
	}
	
}
