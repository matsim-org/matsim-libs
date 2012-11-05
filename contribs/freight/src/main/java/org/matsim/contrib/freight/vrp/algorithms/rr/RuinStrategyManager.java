/*******************************************************************************
 * Copyright (c) 2011 Stefan Schroeder.
 * eMail: stefan.schroeder@kit.edu
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Stefan Schroeder - initial API and implementation
 ******************************************************************************/
package org.matsim.contrib.freight.vrp.algorithms.rr;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.matsim.contrib.freight.vrp.algorithms.rr.ruin.RuinStrategy;
import org.matsim.contrib.freight.vrp.utils.RandomNumberGeneration;
import org.matsim.core.utils.collections.Tuple;

/**
 * Manages ruin algorithms.
 * 
 * @author stefan schroeder
 * 
 */

public class RuinStrategyManager {

	private List<RuinStrategy> strategies = new ArrayList<RuinStrategy>();

	private List<Double> weights = new ArrayList<Double>();

	private Random random = RandomNumberGeneration.getRandom();

	public void setRandom(Random random) {
		this.random = random;
	}

	public RuinStrategyManager() {
		super();
	}

	/**
	 * Weight is the probability of the ruin-strategy to be chosen.
	 * 
	 * @param strat
	 * @param weight
	 */
	public void addStrategy(RuinStrategy strat, Double weight) {
		strategies.add(strat);
		weights.add(weight);
	}

	public RuinStrategy getRandomStrategy() {
		double randomFig = random.nextDouble();
		double sumWeight = 0.0;
		for (int i = 0; i < weights.size(); i++) {
			sumWeight += weights.get(i);
			if (randomFig < sumWeight) {
				return strategies.get(i);
			}
		}
		throw new IllegalStateException("no ruin-strategy found");
	}

	public List<Tuple<RuinStrategy, Double>> getStrategies() {
		List<Tuple<RuinStrategy, Double>> l = new ArrayList<Tuple<RuinStrategy, Double>>();
		for (int i = 0; i < strategies.size(); i++) {
			l.add(new Tuple<RuinStrategy, Double>(strategies.get(i), weights
					.get(i)));
		}
		return l;
	}

}
