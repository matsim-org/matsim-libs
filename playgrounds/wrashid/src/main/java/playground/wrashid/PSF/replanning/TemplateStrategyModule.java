/* *********************************************************************** *
 * project: org.matsim.*
 * TemplateStrategyModule.java
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

package playground.wrashid.PSF.replanning;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.replanning.PlanStrategyModule;

public class TemplateStrategyModule implements PlanStrategyModule {

	private final static Logger log = Logger.getLogger(TemplateStrategyModule.class);
	
	private final TemplatePlanAlgorithm planAlgo;
	private int counter = 0;
	
	public TemplateStrategyModule() {
		this.planAlgo = new TemplatePlanAlgorithm();
	}
	
	public void prepareReplanning() {
		this.counter = 0;
	}

	public void handlePlan(final Plan plan) {
		this.counter++;
		this.planAlgo.run(plan);
	}

	public void finishReplanning() {
		log.info("number of handled plans: " + this.counter);
	}


}
