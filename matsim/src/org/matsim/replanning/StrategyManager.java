/* *********************************************************************** *
 * project: org.matsim.*
 * StrategyManager.java
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

package org.matsim.replanning;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.gbl.Gbl;
import org.matsim.gbl.MatsimRandom;
import org.matsim.interfaces.core.v01.Person;
import org.matsim.interfaces.core.v01.Population;

/**
 * Manages and applies strategies to agents for re-planning.
 *
 * @author mrieser
 */
public class StrategyManager {

	protected final ArrayList<PlanStrategy> strategies = new ArrayList<PlanStrategy>();
	private final ArrayList<Double> weights = new ArrayList<Double>();
	private double totalWeights = 0.0;
	private int maxPlansPerAgent = 0;

	private final TreeMap<Integer, Map<PlanStrategy, Double>> changeRequests =
			new TreeMap<Integer, Map<PlanStrategy, Double>>();

	/**
	 * Adds a strategy to this manager with the specified weight. This weight
	 * compared to the sum of weights of all strategies in this manager defines
	 * the probability this strategy will be used for an agent.
	 *
	 * @param strategy
	 * @param weight
	 */
	public void addStrategy(final PlanStrategy strategy, final double weight) {
		this.strategies.add(strategy);
		this.weights.add(Double.valueOf(weight));
		this.totalWeights += weight;
	}

	/**
	 * removes the specified strategy from this manager
	 *
	 * @param strategy the strategy to be removed
	 * @return true if the strategy was successfully removed from this manager,
	 * 		false if the strategy was not part of this manager and could thus not be removed.
	 */
	public boolean removeStrategy(final PlanStrategy strategy) {
		int idx = this.strategies.indexOf(strategy);
		if (idx != -1) {
			this.strategies.remove(idx);
			double weight = this.weights.remove(idx).doubleValue();
			this.totalWeights -= weight;
			return true;
		}
		return false;
	}

	/**
	 * changes the weight of the specified strategy
	 *
	 * @param strategy
	 * @param newWeight
	 * @return true if the strategy is part of this manager and the weight could
	 * 		be changed successfully, false otherwise.
	 */
	public boolean changeStrategy(final PlanStrategy strategy, final double newWeight) {
		int idx = this.strategies.indexOf(strategy);
		if (idx != -1) {
			double oldWeight = this.weights.set(idx, Double.valueOf(newWeight)).doubleValue();
			this.totalWeights += (newWeight - oldWeight);
			return true;
		}
		return false;
	}

	/**
	 * Randomly chooses for each person of the population a strategy and uses that
	 * strategy on the person, after adapting the strategies to any pending change
	 * requests for the specified iteration.
	 *
	 * @param population
	 * @param iteration the current iteration we're handling
	 */
	public void run(final Population population, final int iteration) {
		handleChangeRequests(iteration);
		run(population);
	}

	/**
	 * Randomly chooses for each person of the population a strategy and uses that
	 * strategy on the person.
	 *
	 * @param population
	 */
	public void run(final Population population) {
		// initialize all strategies
		for (PlanStrategy strategy : this.strategies) {
			strategy.init();
		}
		// then go through the population and assign each person to a strategy
		for (Person person : population.getPersons().values()) {
			if (this.maxPlansPerAgent > 0) {
				person.removeWorstPlans(this.maxPlansPerAgent);
			}
			PlanStrategy strategy = this.chooseStrategy();
			if (strategy != null) {
				strategy.run(person);
			} else {
				Gbl.errorMsg("No strategy found!");
			}
		}
		// finally make sure all strategies have finished there work
		for (PlanStrategy strategy : this.strategies) {
			strategy.finish();
		}
	}

	/**
	 * modifies the loaded strategies according to pending change requests for this iteration.
	 *
	 * @param iteration
	 */
	private void handleChangeRequests(final int iteration) {
		Map<PlanStrategy, Double> changes = this.changeRequests.remove(Integer.valueOf(iteration));
		if (changes != null) {
			for (java.util.Map.Entry<PlanStrategy, Double> entry : changes.entrySet()) {
				changeStrategy(entry.getKey(), entry.getValue().doubleValue());
			}
		}
	}

	/**
	 * chooses a (weight-influenced) random strategy
	 *
	 * @return the chosen strategy
	 */
	protected PlanStrategy chooseStrategy() {
		double rnd = MatsimRandom.random.nextDouble() * this.totalWeights;

		double sum = 0.0;
		for (int i = 0, max = this.weights.size(); i < max; i++) {
			sum += this.weights.get(i).doubleValue();
			if (rnd <= sum) {
				return this.strategies.get(i);
			}
		}
		return null;
	}

	/**
	 * Sets the maximal number of plans an agent can memorize. Setting
	 * maxPlansPerAgent to zero means unlimited memory (only limited by RAM).
	 * Agents can have up to maxPlansPerAgent plans plus one additional one with the
	 * currently modified plan they're trying out.
	 *
	 * @param maxPlansPerAgent
	 */
	public void setMaxPlansPerAgent(final int maxPlansPerAgent) {
		this.maxPlansPerAgent = maxPlansPerAgent;
	}
	
	public int getMaxPlansPerAgent() {
		return this.maxPlansPerAgent;
	}

	/**
	 * Schedules a {@link #changeStrategy changeStrategy(Strategy, double)} command for a later iteration. The
	 * change will take place before the strategies are applied.
	 *
	 * @param iteration
	 * @param strategy
	 * @param newWeight
	 */
	public void addChangeRequest(final int iteration, final PlanStrategy strategy, final double newWeight) {
		Integer iter = Integer.valueOf(iteration);
		Map<PlanStrategy, Double> iterationRequests = this.changeRequests.get(iter);
		if (iterationRequests == null) {
			iterationRequests = new HashMap<PlanStrategy, Double>(3);
			this.changeRequests.put(iter, iterationRequests);
		}
		iterationRequests.put(strategy, Double.valueOf(newWeight));
	}

}
