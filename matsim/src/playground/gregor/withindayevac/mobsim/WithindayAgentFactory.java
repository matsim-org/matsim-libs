/* *********************************************************************** *
 * project: org.matsim.*
 * KmlNetworkWriter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package playground.gregor.withindayevac.mobsim;

import org.matsim.mobsim.queuesim.AgentFactory;
import org.matsim.mobsim.queuesim.PersonAgent;
import org.matsim.network.NetworkLayer;
import org.matsim.population.Person;

import playground.gregor.withindayevac.BDIAgent;
import playground.gregor.withindayevac.communication.InformationExchanger;


/**
 * @author dgrether
 *
 */
public class WithindayAgentFactory extends AgentFactory {

	private final InformationExchanger informationExchanger;
	private final NetworkLayer network;
	private final int iteration;

	public WithindayAgentFactory(final InformationExchanger informationExchanger,
			final NetworkLayer networkLayer, final int iteration) {
		this.informationExchanger = informationExchanger;
		this.network = networkLayer;
		this.iteration = iteration;
	}

	@Override
	public PersonAgent createPersonAgent(final Person p) {
		return new BDIAgent(p, this.informationExchanger, this.network, this.iteration);
	}
	
	
	

}
