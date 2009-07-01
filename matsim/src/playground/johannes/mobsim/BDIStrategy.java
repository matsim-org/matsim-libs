/* *********************************************************************** *
 * project: org.matsim.*
 * BDIStrategy.java
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

/**
 * 
 */
package playground.johannes.mobsim;

import java.util.List;

import org.matsim.core.population.PlanImpl;

/**
 * @author illenberger
 *
 */
public class BDIStrategy implements IntradayStrategy {

	private AgentBeliefs beliefs;
	
	private OptionsGenerator generator;
	
	private OptionsFilter filter;

	private PlanAgent agent;
	
	public BDIStrategy(PlanAgent agent, AgentBeliefs beliefs, OptionsGenerator generator, OptionsFilter filter) {
		this.agent = agent;
		this.beliefs = beliefs;
		this.generator = generator;
		this.filter = filter;
	}
	
	public AgentBeliefs getBeliefs() {
		return beliefs;
	}

	public OptionsGenerator getGenerator() {
		return generator;
	}

	public OptionsFilter getFilter() {
		return filter;
	}

	public PlanAgent getAgent() {
		return agent;
	}

	public PlanImpl replan(double time) {
		/*
		 * TODO: What about replanAllowed()? -> no options
		 */
		beliefs.update();
		List<PlanImpl> options = generator.generateOptions();
		return filter.filterOptions(options);
	}

}
