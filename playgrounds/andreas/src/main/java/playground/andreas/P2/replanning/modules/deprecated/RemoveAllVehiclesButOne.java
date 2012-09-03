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
import org.matsim.core.basic.v01.IdImpl;

import playground.andreas.P2.operator.Cooperative;
import playground.andreas.P2.replanning.PPlan;
import playground.andreas.P2.replanning.PPlanStrategy;
import playground.andreas.P2.replanning.PStrategy;

/**
 * 
 * Clones a given plan, but resets the number of vehicles to one.
 * 
 * @author aneumann
 *
 */
public class RemoveAllVehiclesButOne extends PStrategy implements PPlanStrategy{
	
	private final static Logger log = Logger.getLogger(RemoveAllVehiclesButOne.class);
	public static final String STRATEGY_NAME = "RemoveAllVehiclesButOne";

	public RemoveAllVehiclesButOne(ArrayList<String> parameter) {
		super(parameter);
		if(parameter.size() != 0){
			log.error("Too many parameter. Will ignore: " + parameter);
		}
	}

	@Override
	public PPlan run(Cooperative cooperative) {
		// profitable route, change startTime
		PPlan newPlan = new PPlan(new IdImpl(cooperative.getCurrentIteration()), this.getName(), cooperative.getBestPlan());
		newPlan.setLine(cooperative.getRouteProvider().createTransitLine(cooperative.getId(), newPlan.getStartTime(), newPlan.getEndTime(), 1, newPlan.getStopsToBeServed(), new IdImpl(cooperative.getCurrentIteration())));

		return newPlan;
	}

	@Override
	public String getName() {
		return RemoveAllVehiclesButOne.STRATEGY_NAME;
	}

}