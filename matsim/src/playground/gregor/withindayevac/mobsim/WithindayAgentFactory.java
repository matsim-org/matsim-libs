/* *********************************************************************** *
 * project: org.matsim.*
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

import org.matsim.core.api.network.Network;
import org.matsim.core.api.population.Person;
import org.matsim.core.mobsim.queuesim.AgentFactory;
import org.matsim.core.mobsim.queuesim.PersonAgent;
import org.matsim.core.mobsim.queuesim.QueueSimulation;
import org.matsim.core.network.NetworkLayer;

import playground.gregor.withindayevac.BDIAgent;
import playground.gregor.withindayevac.communication.InformationExchanger;


/**
 * @author dgrether
 *
 */
public class WithindayAgentFactory extends AgentFactory {

	private final InformationExchanger informationExchanger;
	private final Network network;
	private final int iteration;

	public WithindayAgentFactory(final QueueSimulation simulation, final InformationExchanger informationExchanger,
			final Network networkLayer, final int iteration) {
		super(simulation);
		this.informationExchanger = informationExchanger;
		this.network = networkLayer;
		this.iteration = iteration;
	}

	@Override
	public PersonAgent createPersonAgent(final Person p) {
		return new BDIAgent(p, this.simulation, this.informationExchanger, (NetworkLayer)this.network, this.iteration);
	}




}
