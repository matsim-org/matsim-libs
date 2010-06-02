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

package org.matsim.core.mobsim.queuesim;

import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.internal.MatsimFactory;
import org.matsim.core.mobsim.framework.PersonDriverAgent;
import org.matsim.ptproject.qsim.QSimI;

/**
 * @author dgrether
 */
/* package */ class AgentFactory implements MatsimFactory {

	private final QueueSimulation simulation;

	/*package*/ AgentFactory(final QueueSimulation simulation) {
		this.simulation = simulation;
	}

	/*package*/ PersonDriverAgent createPersonAgent(final Person p) {
		PersonDriverAgent agent = StaticFactoriesContainer.createQueuePersonAgent(p, this.simulation);
		return agent;
	}

}
