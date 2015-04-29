/* *********************************************************************** *
 * project: org.matsim.*
 * ConvertOsmToMatsim.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.jjoubert.projects.wasteCollection;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyFactory;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.selectors.PlanSelector;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

@Singleton
public class MclarpifPlanStrategyFactory implements Provider<PlanStrategy> {
	private final Logger log = Logger.getLogger(MclarpifPlanStrategyFactory.class);
	private Scenario sc;
	
	@Inject
	MclarpifPlanStrategyFactory(Scenario sc) {
		this.sc = sc;
	}
	
	@Override
	public PlanStrategy get() {
		log.warn("Instantiating the MCLARPIF plan strategy factory.");
		
		/* Plans selector. */
		PlanSelector planSelector = new MclarpifPlanSelector();
		PlanStrategyImpl.Builder builder = new PlanStrategyImpl.Builder(planSelector);
		
		/* Doing something with that selected plan. */
		MclarpifPlanStrategyModule mod = new MclarpifPlanStrategyModule(this.sc);
		builder.addStrategyModule(mod);
				
		return builder.build();
	}

}
