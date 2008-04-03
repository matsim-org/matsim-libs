/* *********************************************************************** *
 * project: org.matsim.*
 * DeliberateAgentFactory.java
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

import java.util.ArrayList;
import java.util.List;

import org.matsim.plans.Plans;

/**
 * @author illenberger
 *
 */
public abstract class DeliberateAgentBuilder implements MobsimAgentBuilder {

	private PlanAgentBuilder planAgentBuilder;
	
	private IntradayStrategyBuilder strategyBuilder;
	
	public DeliberateAgentBuilder(Plans population, IntradayStrategyBuilder sBuilder) {
		this.strategyBuilder = sBuilder;
		planAgentBuilder = new PlanAgentBuilder(population);
	}
	
	public List<DeliberateAgent> buildAgents() {
		List<PlanAgent> planAgents = planAgentBuilder.buildAgents();
		List<DeliberateAgent> deliberateAgents = new ArrayList<DeliberateAgent>(planAgents.size());
		
		for(PlanAgent a : planAgents)
			deliberateAgents.add(newDeliberateAgent(a));
		
		return deliberateAgents;
	}

	protected DeliberateAgent newDeliberateAgent(PlanAgent pAgent) {
		IntradayStrategy strategy = strategyBuilder.newIntradayStrategy(pAgent);
		DeliberateAgent dAgent = new DeliberateAgent(pAgent, strategy);
		return dAgent;
	}
}
