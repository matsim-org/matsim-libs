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

/**
 * 
 */
package playground.jjoubert.projects.wasteCollection;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.replanning.PlanStrategyModule;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.ReplanningEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.ReplanningListener;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.replanning.ReplanningContext;

/**
 * @author jwjoubert
 *
 */
public class MclarpifPlanStrategyModule implements PlanStrategyModule, ReplanningListener{
	final private Logger log = Logger.getLogger(MclarpifPlanStrategyModule.class);
	Scenario sc;
	
	public MclarpifPlanStrategyModule(Scenario sc) {
		// TODO Auto-generated constructor stub
		this.sc = sc;
	}
	@Override
	public void prepareReplanning(ReplanningContext replanningContext) {
		// TODO Auto-generated method stub
		log.warn(" ==> Preparing for MCLARPIF replanning");
	}

	@Override
	public void handlePlan(Plan plan) {
		// TODO Auto-generated method stub
		log.warn(" ==> Handle MCLARPIF plan during replanning");
	}

	@Override
	public void finishReplanning() {
		// TODO Auto-generated method stub
		log.warn(" ==> Finishing MCLARPIF replanning");
	}
	
	@Override
	public void notifyReplanning(ReplanningEvent event) {
		// TODO Auto-generated method stub
		log.warn(" ==> Replanning event caught for MCLARPIF replanning");
	}
	
}
