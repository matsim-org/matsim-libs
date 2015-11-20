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

package tutorial.programming.example11PluggablePlanStrategyInCode;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.replanning.PlanStrategyModule;
import org.matsim.core.replanning.ReplanningContext;

class MyPlanStrategyModule implements PlanStrategyModule, ActivityEndEventHandler {
	private static final Logger log = Logger.getLogger(MyPlanStrategyModule.class);

	Scenario sc;
	Network net;
	Population pop;

	public MyPlanStrategyModule(Scenario scenario) {
		this.sc = scenario;
		this.net = this.sc.getNetwork();
		this.pop = this.sc.getPopulation();
	}

	@Override
	public void finishReplanning() {
	}

	@Override
	public void handlePlan(Plan plan) {
		log.error("calling handlePlan for person.Id: " + plan.getPerson().getId());
	}

	@Override
	public void prepareReplanning(ReplanningContext replanningContext) {
		log.error("calling prepareReplanning");
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		//log.error("calling handleEvent for an ActivityEndEvent");
	}

	@Override
	public void reset(int iteration) {
		log.error("calling reset");
	}

}
