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

package playground.andreas.P2.replanning.modules.deprecated;

import java.util.ArrayList;

import org.apache.log4j.Logger;
import playground.andreas.P2.operator.Cooperative;
import playground.andreas.P2.replanning.PPlan;
import playground.andreas.P2.replanning.AbstractPStrategyModule;

/**
 * 
 * Increases the number of vehicles for the given cooperative's best plan, if the budget allows for that.
 * 
 * @author aneumann
 *
 */
@Deprecated
public class IncreaseNumberOfVehicles extends AbstractPStrategyModule {
	
	private final static Logger log = Logger.getLogger(IncreaseNumberOfVehicles.class);
	
	public static final String STRATEGY_NAME = "IncreaseNumberOfVehicles";
	
	public IncreaseNumberOfVehicles(ArrayList<String> parameter) {
		super(parameter);
		if(parameter.size() != 0){
			log.error("There are no parameters allowed for that module");
		}
	}
	
	@Override
	public PPlan run(Cooperative cooperative) {
		// sufficient founds, so buy one
		PPlan oldPlan = cooperative.getBestPlan();
		PPlan plan = new PPlan(cooperative.getNewRouteId(), this.getName());
		plan.setStopsToBeServed(oldPlan.getStopsToBeServed());
		plan.setStartTime(oldPlan.getStartTime());
		plan.setEndTime(oldPlan.getEndTime());
		plan.setScore(oldPlan.getScore());
		plan.setNVehicles(oldPlan.getNVehicles() + 1);
		plan.setLine(cooperative.getRouteProvider().createTransitLine(cooperative.getId(), plan));
		return plan;			
	}

	@Override
	public String getName() {
		return IncreaseNumberOfVehicles.STRATEGY_NAME;
	}

}
