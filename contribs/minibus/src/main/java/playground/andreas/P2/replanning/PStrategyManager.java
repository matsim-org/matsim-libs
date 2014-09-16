/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.andreas.P2.replanning;

import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.gbl.MatsimRandom;

import playground.andreas.P2.helper.PConfigGroup;
import playground.andreas.P2.helper.PConfigGroup.PStrategySettings;
import playground.andreas.P2.replanning.modules.EndRouteExtension;
import playground.andreas.P2.replanning.modules.MaxRandomEndTimeAllocator;
import playground.andreas.P2.replanning.modules.MaxRandomStartTimeAllocator;
import playground.andreas.P2.replanning.modules.ReduceStopsToBeServedRFare;
import playground.andreas.P2.replanning.modules.ReduceTimeServedRFare;
import playground.andreas.P2.replanning.modules.SidewaysRouteExtension;
import playground.andreas.P2.replanning.modules.WeightedEndTimeExtension;
import playground.andreas.P2.replanning.modules.WeightedStartTimeExtension;
import playground.andreas.P2.scoring.fare.StageContainerCreator;
import playground.andreas.P2.scoring.fare.TicketMachine;

/**
 * Loads strategies from config and chooses strategies according to their weights.
 * 
 * @author aneumann
 *
 */
public final class PStrategyManager {
	
	private final static Logger log = Logger.getLogger(PStrategyManager.class);
	
	private final ArrayList<PStrategy> strategies = new ArrayList<PStrategy>();
	private final ArrayList<Double> weights = new ArrayList<Double>();
	private final ArrayList<Integer> disableInIteration = new ArrayList<Integer>();
	private double totalWeights = 0.0;

	public void init(PConfigGroup pConfig, EventsManager eventsManager, StageContainerCreator stageContainerCreator, TicketMachine ticketMachine, TimeProvider timeProvider) {
		for (PStrategySettings settings : pConfig.getStrategySettings()) {
			String classname = settings.getModuleName();
			double rate = settings.getProbability();
			if (rate == 0.0) {
				log.info("The following strategy has a weight set to zero. Will drop it. " + classname);
				continue;
			}
			PStrategy strategy = loadStrategy(classname, settings, eventsManager, stageContainerCreator, ticketMachine, timeProvider);
			this.addStrategy(strategy, rate, settings.getDisableInIteration());
		}
		
		log.info("enabled with " + this.strategies.size()  + " strategies");
	}

	private PStrategy loadStrategy(final String name, final PStrategySettings settings, EventsManager eventsManager, StageContainerCreator stageContainerCreator, TicketMachine ticketMachine, TimeProvider timeProvider) {
		PStrategy strategy = null;
		
		if (name.equals(MaxRandomStartTimeAllocator.STRATEGY_NAME)) {
			strategy = new MaxRandomStartTimeAllocator(settings.getParametersAsArrayList());
		} else if (name.equals(MaxRandomEndTimeAllocator.STRATEGY_NAME)) {
			strategy = new MaxRandomEndTimeAllocator(settings.getParametersAsArrayList());
		} else if(name.equals(SidewaysRouteExtension.STRATEGY_NAME)){
			strategy = new SidewaysRouteExtension(settings.getParametersAsArrayList());
		} else if(name.equals(EndRouteExtension.STRATEGY_NAME)){
			strategy = new EndRouteExtension(settings.getParametersAsArrayList());
		} else if (name.equals(ReduceTimeServedRFare.STRATEGY_NAME)) {
			ReduceTimeServedRFare strat = new ReduceTimeServedRFare(settings.getParametersAsArrayList());
			strat.setTicketMachine(ticketMachine);
			stageContainerCreator.addStageContainerHandler(strat);
			strategy = strat;
		} else if (name.equals(ReduceStopsToBeServedRFare.STRATEGY_NAME)) {
			ReduceStopsToBeServedRFare strat = new ReduceStopsToBeServedRFare(settings.getParametersAsArrayList());
			strat.setTicketMachine(ticketMachine);
			stageContainerCreator.addStageContainerHandler(strat);
			strategy = strat;
		} else if (name.equals(WeightedStartTimeExtension.STRATEGY_NAME)) {
			WeightedStartTimeExtension strat = new WeightedStartTimeExtension(settings.getParametersAsArrayList());
			strat.setTimeProvider(timeProvider);
			strategy = strat;
		} else if (name.equals(WeightedEndTimeExtension.STRATEGY_NAME)) {
			WeightedEndTimeExtension strat = new WeightedEndTimeExtension(settings.getParametersAsArrayList());
			strat.setTimeProvider(timeProvider);
			strategy = strat;
		}
		
		if (strategy == null) {
			log.error("Could not initialize strategy named " + name);
		}
		
		return strategy;
	}

	private void addStrategy(final PStrategy strategy, final double weight, int disableInIteration) {
		this.strategies.add(strategy);
		this.weights.add(Double.valueOf(weight));
		this.disableInIteration.add(Integer.valueOf(disableInIteration));
		this.totalWeights += weight;
	}

	/**
	 * Changes the weights of each strategy to zero and removes it from the choice set if it needs to be disabled
	 * 
	 * @param iteration
	 */
	public void updateStrategies(int iteration) {
		for (int i = 0; i < this.disableInIteration.size(); i++) {
			if (this.disableInIteration.get(i) == iteration) {
				double weight = this.weights.get(i);
				this.weights.set(i, new Double(0.0));
				this.strategies.set(i, null);
				this.totalWeights -= weight;
			}
		}
	}

	/**
	 * Picks randomly one strategy from the set of available strategies. Strategies with a higher weight have a higher probability of being picked.
	 *  
	 * @return The strategy picked.
	 */
	public PStrategy chooseStrategy() {
		double rnd = MatsimRandom.getRandom().nextDouble() * this.totalWeights;

		double sum = 0.0;
		for (int i = 0, max = this.weights.size(); i < max; i++) {
			sum += this.weights.get(i).doubleValue();
			if (rnd <= sum) {
				return this.strategies.get(i);
			}
		}
		log.error("This line should not be reachable. Check it.");
		return null;
	}

	@Override
	public String toString() {
		StringBuffer strBuffer = new StringBuffer();
		strBuffer.append("Strategies: ");
		strBuffer.append(this.strategies.get(0).getStrategyName()); strBuffer.append(" ("); strBuffer.append(this.weights.get(0)); strBuffer.append(")");
		
		for (int i = 1; i < this.strategies.size(); i++) {
			strBuffer.append(", "); strBuffer.append(this.strategies.get(i).getStrategyName()); strBuffer.append(" ("); strBuffer.append(this.weights.get(i)); strBuffer.append(")");
		}
		return strBuffer.toString();
	}
}