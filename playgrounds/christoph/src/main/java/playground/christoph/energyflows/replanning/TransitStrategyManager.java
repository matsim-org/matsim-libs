/* *********************************************************************** *
 * project: org.matsim.*
 * TransitStrategyManager.java
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

package playground.christoph.energyflows.replanning;

import org.matsim.api.core.v01.population.Person;
import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.StrategyManager;
import org.matsim.core.replanning.modules.ReRoute;
import org.matsim.core.replanning.selectors.KeepSelected;
import org.matsim.core.replanning.selectors.RandomPlanSelector;

/**
 *	A subset of the switzerland population are transit agents. They can by identified by
 *	their id (> 1000000000). Their departure times and transport mode is already defined
 *	and should not be changed by a replanning module. The only replanning that we want
 *	to be allowed is re-routing. If another module is selected, we replace it with
 *	"KeepSelected".
 *	
 *	I do not see a way to identify the replanning type of a PlanStrategy. Therefore we
 *	create our own rerouting PlanStrategy.
 * 
 *	@author cdobler
 */
public class TransitStrategyManager extends StrategyManager {

	private PlanStrategy reroutingStrategy;
	private double reroutingShare;
	
	public TransitStrategyManager(Controler controler, double replanningShare) {
		reroutingStrategy = new PlanStrategyImpl(new RandomPlanSelector());
		reroutingStrategy.addStrategyModule(new ReRoute(controler));
		
		this.reroutingShare = replanningShare;
	}
	
	@Override
	public PlanStrategy chooseStrategy(final Person person) {	
		
		/*
		 * Is it a transit agent?
		 */
		int id = Integer.valueOf(person.getId().toString());
		if (id > 1000000000) {
			double rnd = MatsimRandom.getRandom().nextDouble();
			if (rnd <= reroutingShare) return reroutingStrategy;
			else return new PlanStrategyImpl(new KeepSelected());
		} else return super.chooseStrategy(person);
	}
}
