/* *********************************************************************** *
 * project: org.matsim.*
 * WithinDayInitialReplannerFactory.java
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

package playground.christoph.withinday.replanning.replanners.interfaces;

import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.ptproject.qsim.interfaces.AgentCounterI;

public abstract class WithinDayInitialReplannerFactory extends WithinDayReplannerFactory {
	
	public WithinDayInitialReplannerFactory(AgentCounterI agentCounter, AbstractMultithreadedModule abstractMultithreadedModule, double replanningProbability) {
		super(agentCounter, abstractMultithreadedModule, replanningProbability);
	}
	
	public abstract WithinDayInitialReplanner createReplanner();
}
