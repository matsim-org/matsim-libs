/*
 *   *********************************************************************** *
 *   project: org.matsim.*
 *   *********************************************************************** *
 *                                                                           *
 *   copyright       : (C)  by the members listed in the COPYING,        *
 *                     LICENSE and WARRANTY file.                            *
 *   email           : info at matsim dot org                                *
 *                                                                           *
 *   *********************************************************************** *
 *                                                                           *
 *     This program is free software; you can redistribute it and/or modify  *
 *     it under the terms of the GNU General Public License as published by  *
 *     the Free Software Foundation; either version 2 of the License, or     *
 *     (at your option) any later version.                                   *
 *     See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                           *
 *   ***********************************************************************
 *
 */

package org.matsim.freight.carriers.controller;

import java.util.List;
import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.core.replanning.GenericPlanStrategy;
import org.matsim.core.replanning.GenericStrategyManager;
import org.matsim.core.replanning.GenericStrategyManagerImpl;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.selectors.PlanSelector;
import org.matsim.freight.carriers.Carrier;
import org.matsim.freight.carriers.CarrierPlan;

class CarrierStrategyManagerImpl implements CarrierStrategyManager{
	final GenericStrategyManager<CarrierPlan, Carrier> delegate = new GenericStrategyManagerImpl<>();
	@Override public void addStrategy( GenericPlanStrategy<CarrierPlan, Carrier> strategy, String subpopulation, double weight ){
		delegate.addStrategy( strategy, subpopulation, weight );
	}
	@Override public void run( Iterable<? extends HasPlansAndId<CarrierPlan, Carrier>> persons, int iteration, ReplanningContext replanningContext ){
		delegate.run( persons, iteration, replanningContext );
	}
	@Override public void setMaxPlansPerAgent( int maxPlansPerAgent ){
		delegate.setMaxPlansPerAgent( maxPlansPerAgent );
	}
	@Override public void addChangeRequest( int iteration, GenericPlanStrategy<CarrierPlan, Carrier> strategy, String subpopulation, double newWeight ){
		delegate.addChangeRequest( iteration, strategy, subpopulation, newWeight );
	}
	@Override public void setPlanSelectorForRemoval( PlanSelector<CarrierPlan, Carrier> planSelector ){
		delegate.setPlanSelectorForRemoval( planSelector );
	}
	@Override public List<GenericPlanStrategy<CarrierPlan, Carrier>> getStrategies( String subpopulation ){
		return delegate.getStrategies( subpopulation );
	}
	@Override public List<Double> getWeights( String subpopulation ){
		return delegate.getWeights( subpopulation );
	}
}
