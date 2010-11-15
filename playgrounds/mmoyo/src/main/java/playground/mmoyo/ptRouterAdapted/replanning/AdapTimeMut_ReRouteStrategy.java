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

package playground.mmoyo.ptRouterAdapted.replanning;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.replanning.PlanStrategyModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.modules.TimeAllocationMutator;
import org.matsim.core.replanning.selectors.PlanSelector;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.pt.replanning.TransitActsRemoverStrategy;

public class AdapTimeMut_ReRouteStrategy implements PlanStrategy {
	private static final Logger log = Logger.getLogger(AdapTimeMut_ReRouteStrategy.class);
	PlanStrategy planStrategyDelegate = null ;
	
	public AdapTimeMut_ReRouteStrategy(Controler controler) {
		
		planStrategyDelegate = new PlanStrategyImpl( new RandomPlanSelector( ) );
		log.info("Using Experimental AdapTimeMut_ReRouteStrategy");

		//remove transit acts
		addStrategyModule(new TransitActsRemoverStrategy(controler.getConfig()));
	
		//add time allocation mutator
		int mutationRange= Integer.parseInt(controler.getConfig().getParam(TimeAllocationMutator.CONFIG_GROUP, TimeAllocationMutator.CONFIG_MUTATION_RANGE));
		TimeAllocationMutator timeAllocationMutator = new TimeAllocationMutator(controler.getConfig(), mutationRange);
		this.addStrategyModule(timeAllocationMutator);

		//reroute with adapted router
		addStrategyModule(new AdapPlanStrategyModule(controler));
		
		// these modules may, at the same time, be events listeners (so that they can collect information):
		//controler.getEvents().addHandler( adapPlanStrategyModule ) ;

		// to collect events:
		//controler.getEvents().addHandler( (EventHandler) this.getPlanSelector() ) ;
	}

	@Override
	public void addStrategyModule(PlanStrategyModule module) {
		planStrategyDelegate.addStrategyModule(module);
	}


	@Override
	public void finish() {
		planStrategyDelegate.finish();
	}

	@Override
	public int getNumberOfStrategyModules() {
		return planStrategyDelegate.getNumberOfStrategyModules();
	}
	
	@Override
	public PlanSelector getPlanSelector() {
		return planStrategyDelegate.getPlanSelector();
	}

	@Override
	public void init() {
		planStrategyDelegate.init();
	}

	@Override
	public void run(Person person) {
		planStrategyDelegate.run(person);
	}

	public String toString() {
		return planStrategyDelegate.toString();
	}
}
