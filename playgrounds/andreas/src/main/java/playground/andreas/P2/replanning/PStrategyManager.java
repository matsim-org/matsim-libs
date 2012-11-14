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
import playground.andreas.P2.replanning.modules.AggressiveIncreaseNumberOfVehicles;
import playground.andreas.P2.replanning.modules.ConvexHullRouteExtension;
import playground.andreas.P2.replanning.modules.MaxRandomEndTimeAllocator;
import playground.andreas.P2.replanning.modules.MaxRandomStartTimeAllocator;
import playground.andreas.P2.replanning.modules.RandomEndTimeAllocator;
import playground.andreas.P2.replanning.modules.RandomRouteEndExtension;
import playground.andreas.P2.replanning.modules.RandomRouteStartExtension;
import playground.andreas.P2.replanning.modules.RandomStartTimeAllocator;
import playground.andreas.P2.replanning.modules.RectangleHullRouteExtension;
import playground.andreas.P2.replanning.modules.ReduceStopsToBeServed;
import playground.andreas.P2.replanning.modules.ReduceStopsToBeServedRFare;
import playground.andreas.P2.replanning.modules.ReduceTimeServed;
import playground.andreas.P2.replanning.modules.ReduceTimeServedRFare;
import playground.andreas.P2.replanning.modules.RouteEnvelopeExtension;
import playground.andreas.P2.replanning.modules.TimeReduceDemand;
import playground.andreas.P2.replanning.modules.deprecated.AddRandomStop;
import playground.andreas.P2.replanning.modules.deprecated.IncreaseNumberOfVehicles;
import playground.andreas.P2.replanning.modules.deprecated.ReduceStopsToBeServedR;
import playground.andreas.P2.replanning.modules.deprecated.ReduceTimeServedR;
import playground.andreas.P2.replanning.modules.deprecated.RemoveAllVehiclesButOne;
import playground.andreas.P2.replanning.modules.deprecated.StopReduceDemand;
import playground.andreas.P2.scoring.fare.StageContainerCreator;
import playground.andreas.P2.scoring.fare.TicketMachine;

/**
 * Loads strategies from config and chooses strategies according to their weights.
 * 
 * @author aneumann
 *
 */
public class PStrategyManager {
	
	private final static Logger log = Logger.getLogger(PStrategyManager.class);
	
	private EventsManager eventsManager;
	
	private final ArrayList<PStrategy> strategies = new ArrayList<PStrategy>();
	private final ArrayList<Double> weights = new ArrayList<Double>();
	private final ArrayList<Integer> disableInIteration = new ArrayList<Integer>();
	private double totalWeights = 0.0;
	
	private String pIdentifier;
	private ReduceTimeServed reduceTimeServed = null;
	private ReduceStopsToBeServed reduceStopsToBeServed = null;

	public PStrategyManager(PConfigGroup pConfig){
		this.pIdentifier = pConfig.getPIdentifier();
	}
	
	// TODO[an] always initialize TimeReduceDemand
	public void init(PConfigGroup pConfig, EventsManager eventsManager, StageContainerCreator stageContainerCreator, TicketMachine ticketMachine) {
		for (PStrategySettings settings : pConfig.getStrategySettings()) {
			String classname = settings.getModuleName();
			double rate = settings.getProbability();
			if (rate == 0.0) {
				log.info("The following strategy has a weight set to zero. Will drop it. " + classname);
				continue;
			}
			PStrategy strategy = loadStrategy(classname, settings, eventsManager, stageContainerCreator, ticketMachine);
			this.addStrategy(strategy, rate, settings.getDisableInIteration());
		}
		
		log.info("enabled with " + this.strategies.size()  + " strategies");
	}

	private PStrategy loadStrategy(final String name, final PStrategySettings settings, EventsManager eventsManager, StageContainerCreator stageContainerCreator, TicketMachine ticketMachine) {
		this.eventsManager = eventsManager;
		PStrategy strategy = null;
		
		if (name.equals(RemoveAllVehiclesButOne.STRATEGY_NAME)) {
			strategy = new RemoveAllVehiclesButOne(settings.getParametersAsArrayList());
		} else if (name.equals(RandomStartTimeAllocator.STRATEGY_NAME)) {
			strategy = new RandomStartTimeAllocator(settings.getParametersAsArrayList());
		} else if (name.equals(RandomEndTimeAllocator.STRATEGY_NAME)) {
			strategy = new RandomEndTimeAllocator(settings.getParametersAsArrayList());
		} else if (name.equals(MaxRandomStartTimeAllocator.STRATEGY_NAME)) {
			strategy = new MaxRandomStartTimeAllocator(settings.getParametersAsArrayList());
		} else if (name.equals(MaxRandomEndTimeAllocator.STRATEGY_NAME)) {
			strategy = new MaxRandomEndTimeAllocator(settings.getParametersAsArrayList());
		} else if (name.equals(IncreaseNumberOfVehicles.STRATEGY_NAME)) {
			strategy = new IncreaseNumberOfVehicles(settings.getParametersAsArrayList());
		} else if (name.equals(AddRandomStop.STRATEGY_NAME)) {
			strategy = new AddRandomStop(settings.getParametersAsArrayList());
		} else if (name.equals(AggressiveIncreaseNumberOfVehicles.STRATEGY_NAME)) {
			strategy = new AggressiveIncreaseNumberOfVehicles(settings.getParametersAsArrayList());
		} else if(name.equals(ConvexHullRouteExtension.STRATEGY_NAME)){
			strategy = new ConvexHullRouteExtension(settings.getParametersAsArrayList());
		} else if(name.equals(RectangleHullRouteExtension.STRATEGY_NAME)){
			strategy = new RectangleHullRouteExtension(settings.getParametersAsArrayList());
		} else if(name.equals(RandomRouteEndExtension.STRATEGY_NAME)){
			strategy = new RandomRouteEndExtension(settings.getParametersAsArrayList());
		} else if(name.equals(RandomRouteStartExtension.STRATEGY_NAME)){
			strategy = new RandomRouteStartExtension(settings.getParametersAsArrayList());
		} else if(name.equals(RouteEnvelopeExtension.STRATEGY_NAME)){
			strategy = new RouteEnvelopeExtension(settings.getParametersAsArrayList());
		} else if (name.equals(TimeReduceDemand.STRATEGY_NAME)) {
			TimeReduceDemand strat = new TimeReduceDemand(settings.getParametersAsArrayList());
			strat.setPIdentifier(this.pIdentifier);
			eventsManager.addHandler(strat);
			strategy = strat;
		} else if (name.equals(ReduceTimeServed.STRATEGY_NAME)) {
			ReduceTimeServed strat = new ReduceTimeServed(settings.getParametersAsArrayList());
			strat.setPIdentifier(this.pIdentifier);
			eventsManager.addHandler(strat);
			strategy = strat;
			this.reduceTimeServed = strat;
		} else if (name.equals(ReduceTimeServedR.STRATEGY_NAME)) {
			ReduceTimeServedR strat = new ReduceTimeServedR(settings.getParametersAsArrayList());
			strat.setPIdentifier(this.pIdentifier);
			eventsManager.addHandler(strat);
			strategy = strat;
		} else if (name.equals(ReduceTimeServedRFare.STRATEGY_NAME)) {
			ReduceTimeServedRFare strat = new ReduceTimeServedRFare(settings.getParametersAsArrayList());
			strat.setTicketMachine(ticketMachine);
			stageContainerCreator.addStageContainerHandler(strat);
			strategy = strat;
		} else if (name.equals(StopReduceDemand.STRATEGY_NAME)) {
			StopReduceDemand strat = new StopReduceDemand(settings.getParametersAsArrayList());
			strat.setPIdentifier(this.pIdentifier);
			eventsManager.addHandler(strat);
			strategy = strat;
		} else if (name.equals(ReduceStopsToBeServed.STRATEGY_NAME)) {
			ReduceStopsToBeServed strat = new ReduceStopsToBeServed(settings.getParametersAsArrayList());
			strat.setPIdentifier(this.pIdentifier);
			eventsManager.addHandler(strat);
			strategy = strat;
			this.reduceStopsToBeServed = strat;
		} else if (name.equals(ReduceStopsToBeServedR.STRATEGY_NAME)) {
			ReduceStopsToBeServedR strat = new ReduceStopsToBeServedR(settings.getParametersAsArrayList());
			strat.setPIdentifier(this.pIdentifier);
			eventsManager.addHandler(strat);
			strategy = strat;
		} else if (name.equals(ReduceStopsToBeServedRFare.STRATEGY_NAME)) {
			ReduceStopsToBeServedRFare strat = new ReduceStopsToBeServedRFare(settings.getParametersAsArrayList());
			strat.setTicketMachine(ticketMachine);
			stageContainerCreator.addStageContainerHandler(strat);
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

	public PStrategy chooseStrategy() {
		double rnd = MatsimRandom.getRandom().nextDouble() * this.totalWeights;

		double sum = 0.0;
		for (int i = 0, max = this.weights.size(); i < max; i++) {
			sum += this.weights.get(i).doubleValue();
			if (rnd <= sum) {
				return this.strategies.get(i);
			}
		}
		// This line should not be reachable
		return null;
	}

	@Override
	public String toString() {
		StringBuffer strBuffer = new StringBuffer();
		strBuffer.append("Strategies: ");
		strBuffer.append(this.strategies.get(0).getName()); strBuffer.append(" ("); strBuffer.append(this.weights.get(0)); strBuffer.append(")");
		
		for (int i = 1; i < this.strategies.size(); i++) {
			strBuffer.append(", "); strBuffer.append(this.strategies.get(i).getName()); strBuffer.append(" ("); strBuffer.append(this.weights.get(i)); strBuffer.append(")");
		}
		return strBuffer.toString();
	}

	public ReduceTimeServed getReduceTimeServed() {
		if (this.reduceTimeServed == null) {
			log.warn(ReduceTimeServed.STRATEGY_NAME + " not configuried in config file. Adding my own version of it with parameters: 1.0, 700");
			ArrayList<String> param = new ArrayList<String>();
			param.add("1.0");
			param.add("700");
			ReduceTimeServed strat = new ReduceTimeServed(param);
			strat.setPIdentifier(this.pIdentifier);
			this.eventsManager.addHandler(strat);
			this.reduceTimeServed = strat;
		}
		return this.reduceTimeServed;
	}
	
	public ReduceStopsToBeServed getReduceStopsToBeServed() {
		if (this.reduceStopsToBeServed == null) {
			log.warn(ReduceStopsToBeServed.STRATEGY_NAME + " not configuried in config file. Adding my own version of it with parameters: 1.0");
			ArrayList<String> param = new ArrayList<String>();
			param.add("1.0");
			ReduceStopsToBeServed strat = new ReduceStopsToBeServed(param);
			strat.setPIdentifier(this.pIdentifier);
			this.eventsManager.addHandler(strat);
			this.reduceStopsToBeServed = strat;
		}
		return this.reduceStopsToBeServed;
	}	
}