/* *********************************************************************** *
 * project: org.matsim.*
 * MyAgentFactory.java
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
package playground.christoph.withinday3.unused;

import org.matsim.api.core.v01.population.Person;
import org.matsim.core.mobsim.framework.PersonDriverAgent;
import org.matsim.ptproject.qsim.AgentFactory;
import org.matsim.ptproject.qsim.interfaces.Mobsim;

/**
 * @author nagel
 *
 */
public class MyAgentFactory implements AgentFactory {
	
	private Mobsim mobsim ;
	
	MyAgentFactory( Mobsim mobsim ) {
		this.mobsim = mobsim ;
	}

	@Override
	public PersonDriverAgent createPersonAgent(Person p) {
		return new MyAgent( p, mobsim ) ;
	}

}
