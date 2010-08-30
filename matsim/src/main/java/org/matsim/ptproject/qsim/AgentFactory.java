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

package org.matsim.ptproject.qsim;

import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.internal.MatsimFactory;
import org.matsim.ptproject.qsim.helpers.DefaultPersonDriverAgent;
import org.matsim.ptproject.qsim.interfaces.QSimI;

/**
 * @author dgrether
 */
public class AgentFactory implements MatsimFactory {

	protected final QSimI simulation;

	public AgentFactory(final QSimI simulation) {
		this.simulation = simulation;
	}

	public DefaultPersonDriverAgent createPersonAgent(final Person p) {
		DefaultPersonDriverAgent agent = new DefaultPersonDriverAgent(p, this.simulation);
		return agent;
	}

}
