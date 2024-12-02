/**
 * ********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 * *
 * copyright       : (C) 2023 by the members listed in the COPYING,        *
 * LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 * *
 * *********************************************************************** *
 * *
 * This program is free software; you can redistribute it and/or modify  *
 * it under the terms of the GNU General Public License as published by  *
 * the Free Software Foundation; either version 2 of the License, or     *
 * (at your option) any later version.                                   *
 * See also COPYING, LICENSE and WARRANTY file                           *
 * *
 * ***********************************************************************
 */

package org.matsim.freight.receiver.replanning;

import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.freight.receiver.Receiver;
import org.matsim.freight.receiver.ReceiverPlan;
import org.matsim.core.replanning.GenericPlanStrategy;
import org.matsim.core.replanning.GenericStrategyManager;
import org.matsim.core.replanning.GenericStrategyManagerImpl;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.selectors.PlanSelector;

import java.util.List;

class ReceiverStrategyManagerImpl implements ReceiverStrategyManager{
	final GenericStrategyManager<ReceiverPlan, Receiver> delegate = new GenericStrategyManagerImpl<>();

	@Override
	public void addStrategy(GenericPlanStrategy<ReceiverPlan, Receiver> strategy, String subpopulation, double weight) {
		delegate.addStrategy(strategy, subpopulation, weight);
	}

	@Override
	public void run(Iterable<? extends HasPlansAndId<ReceiverPlan, Receiver>> persons, int iteration, ReplanningContext replanningContext) {
		delegate.run(persons, iteration, replanningContext);
	}

	@Override
	public void setMaxPlansPerAgent(int maxPlansPerAgent) {
		delegate.setMaxPlansPerAgent(maxPlansPerAgent);
	}

	@Override
	public void addChangeRequest(int iteration, GenericPlanStrategy<ReceiverPlan, Receiver> strategy, String subpopulation, double newWeight) {
		delegate.addChangeRequest(iteration, strategy, subpopulation, newWeight);
	}

	@Override
	public void setPlanSelectorForRemoval(PlanSelector<ReceiverPlan, Receiver> planSelector) {
		delegate.setPlanSelectorForRemoval(planSelector);
	}

	@Override
	public List<GenericPlanStrategy<ReceiverPlan, Receiver>> getStrategies(String subpopulation) {
		return delegate.getStrategies(subpopulation);
	}

	@Override
	public List<Double> getWeights(String subpopulation) {
		return delegate.getWeights(subpopulation);
	}
}
