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

import org.matsim.core.population.PopulationImpl;

/**
 * A builder class that builds {@link DeliberateAgent} instances by decorating
 * {@link PlanAgent} instances built with an internal {@link PlanAgentBuilder}
 * with {@link DeliberateAgent} instances. The {@link DeliberateAgent} instances
 * are equipped with {@link IntradayStrategy} instances supplied by a
 * {@link IntradayStrategyBuilder}.
 * 
 * @author illenberger
 * 
 */
public class DeliberateAgentBuilder implements MobsimAgentBuilder {

	// =======================================================
	// private fields
	// =======================================================

	private PlanAgentBuilder planAgentBuilder;
	
	private IntradayStrategyBuilder strategyBuilder;
	
	private int replanCoolDownTime = DeliberateAgent.DEFAULT_REPLAN_COOL_DOWN_TIME;

	// =======================================================
	// constructor
	// =======================================================

	/**
	 * Creates a new DeliberateAgenBuilder that builds DeliberateAgents out of
	 * the persons in <tt>population</tt> equipped with
	 * {@link IntradayStrategy} instances supplied by <tt>sBuilder</tt>.
	 * 
	 * @param population the population of persons.
	 * @param sBuilder the strategy builder.
	 */
	public DeliberateAgentBuilder(PopulationImpl population, IntradayStrategyBuilder sBuilder) {
		this.strategyBuilder = sBuilder;
		planAgentBuilder = new PlanAgentBuilder(population);
	}
	
	// =======================================================
	// accessor methods
	// =======================================================
	
	/**
	 * @see {@link DeliberateAgent#setReplanCoolDownTime(int)};
	 */
	public void setReplanCoolDownTime(int time) {
		this.replanCoolDownTime = time;
	}
	
	/**
	 * @see {@link DeliberateAgent#getReplanCoolDownTime()}.
	 * @return
	 */
	public int getReplanCoolDownTime() {
		return replanCoolDownTime;
	}
	
	// =======================================================
	// interface implementation
	// =======================================================

	/**
	 * Returns a list of newly built DeliberateAgents. First a {@link PlanAgent}
	 * is built out of each person, which is then decorated by a DeliberateAgent
	 * equipped with the {@link IntradayStrategy} supplied by the
	 * {@link IntradayStrategyBuilder}.
	 * 
	 * @return a list of newly built DeliberateAgents.
	 */
	public List<DeliberateAgent> buildAgents() {
		List<PlanAgent> planAgents = planAgentBuilder.buildAgents();
		List<DeliberateAgent> deliberateAgents = new ArrayList<DeliberateAgent>(planAgents.size());
		
		for(PlanAgent a : planAgents)
			deliberateAgents.add(newDeliberateAgent(a));
		
		return deliberateAgents;
	}

	// =======================================================
	// protected factories
	// =======================================================

	/**
	 * Builds a DeliberateAgent by decorating <tt>pAgent</tt>.
	 * 
	 * @param pAgent
	 *            the {@link PlanAgent} that will be decorated.
	 * @return a new DeliberateAgent.
	 */
	protected DeliberateAgent newDeliberateAgent(PlanAgent pAgent) {
		IntradayStrategy strategy = strategyBuilder.newIntradayStrategy(pAgent);
		DeliberateAgent dAgent = new DeliberateAgent(pAgent, strategy);
		dAgent.setReplanCoolDownTime(replanCoolDownTime);
		return dAgent;
	}
}
