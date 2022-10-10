package org.matsim.contrib.freight.controler;

import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierPlan;
import org.matsim.core.replanning.GenericPlanStrategy;
import org.matsim.core.replanning.GenericStrategyManager;
import org.matsim.core.replanning.GenericStrategyManagerImpl;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.selectors.PlanSelector;

import java.util.List;

public class CarrierStrategyManagerImpl implements CarrierStrategyManager{
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
