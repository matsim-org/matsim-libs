/* *********************************************************************** *
 * project: org.matsim.*
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
package playground.andreas.intersection.dijkstra;

import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.replanning.PlanStrategyModule;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.selectors.RandomPlanSelector;

/**
 * @author dgrether
 * @deprecated this class is no longer needed due to new routing infrastructure
 * where a common LeastCostPathCalculator is used that is to be set in the 
 * Controler
 */
@Deprecated
public class SignalSystemRoutePlanStrategy implements PlanStrategy {
	PlanStrategyImpl psDelegate = null ;

	public SignalSystemRoutePlanStrategy(MatsimServices controler) {
		this.psDelegate = new PlanStrategyImpl( new RandomPlanSelector());
        this.addStrategyModule(new ReRouteDijkstraTurningMoves(controler.getConfig(), controler.getScenario().getNetwork(),
				controler.createTravelDisutilityCalculator(), controler.getLinkTravelTimes(), ((PopulationFactoryImpl) controler.getScenario().getPopulation().getFactory()).getRouteFactory()));
	}

	public void addStrategyModule(PlanStrategyModule module) {
		this.psDelegate.addStrategyModule(module);
	}

	@Override
	public void finish() {
		this.psDelegate.finish();
	}

	public int getNumberOfStrategyModules() {
		return this.psDelegate.getNumberOfStrategyModules();
	}

	@Override
	public void init(ReplanningContext replanningContext) {
		this.psDelegate.init(replanningContext);
	}

	@Override
	public void run(HasPlansAndId<Plan, Person> person) {
		this.psDelegate.run(person);
	}

	@Override
	public String toString() {
		return this.psDelegate.toString();
	}

}
