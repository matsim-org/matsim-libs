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
import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.modules.TimeAllocationMutator;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.pt.replanning.TransitActsRemoverStrategy;


public class AdapTimeMut_ReRouteStrategy extends PlanStrategyImpl {
	private static final Logger log = Logger.getLogger(AdapTimeMut_ReRouteStrategy.class);
	
	public AdapTimeMut_ReRouteStrategy(Controler controler) {
		super( new RandomPlanSelector() );
		log.info("Using Experimental AdapTimeMut_ReRouteStrategy");

		//remove transit acts
		addStrategyModule(new TransitActsRemoverStrategy(controler.getConfig()));
		//reroute with adapted router
		addStrategyModule(new AdapPlanStrategyModule(controler));
		//add time allocation mutator
		int mutationRange= Integer.parseInt(controler.getConfig().getParam(TimeAllocationMutator.CONFIG_GROUP, TimeAllocationMutator.CONFIG_MUTATION_RANGE));
		TimeAllocationMutator timeAllocationMutator = new TimeAllocationMutator(controler.getConfig(), mutationRange);
		this.addStrategyModule(timeAllocationMutator);
		
		
		// these modules may, at the same time, be events listeners (so that they can collect information):
		//controler.getEvents().addHandler( adapPlanStrategyModule ) ;

		// to collect events:
		//controler.getEvents().addHandler( (EventHandler) this.getPlanSelector() ) ;
	}
}
