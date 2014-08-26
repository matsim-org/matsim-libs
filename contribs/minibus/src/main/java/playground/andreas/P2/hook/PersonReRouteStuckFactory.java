/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.andreas.P2.hook;

import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.population.algorithms.PlanAlgorithm;


/**
 * @author droeder
 *
 */
public interface PersonReRouteStuckFactory {
	

	/**
	 * @param router
	 * @param scenario
	 * @param agentsStuck
	 * @return
	 */
	public AbstractPersonReRouteStuck getReRouteStuck(PlanAlgorithm router, ScenarioImpl scenario, Set<Id> agentsStuck) ;

}

