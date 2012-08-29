/* *********************************************************************** *
 * project: org.matsim.*
 * LeastCostPathCalculatorSelector.java
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

package playground.christoph.evacuation.router;

import java.util.Set;

import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.IntermodalLeastCostPathCalculator;
import org.matsim.vehicles.Vehicle;

import playground.christoph.evacuation.mobsim.decisiondata.DecisionDataProvider;

/**
 *  Uses a Dijkstra based router when a person is calm or a random compass
 *  router if a person is in panic.
 * 
 *  @author cdobler
 */
public class LeastCostPathCalculatorSelector implements IntermodalLeastCostPathCalculator {

	private final DecisionDataProvider decisionDataProvider;
	private final IntermodalLeastCostPathCalculator panicRouter;
	private final IntermodalLeastCostPathCalculator nonPanicRouter;
	
	LeastCostPathCalculatorSelector(IntermodalLeastCostPathCalculator nonPanicRouter,
			 IntermodalLeastCostPathCalculator panicRouter, DecisionDataProvider decisionDataProvider) {
		this.nonPanicRouter = nonPanicRouter;
		this.panicRouter = panicRouter;
		this.decisionDataProvider = decisionDataProvider;
	}
	
	@Override
	public Path calcLeastCostPath(Node fromNode, Node toNode, double starttime,
			Person person, Vehicle vehicle) {
		boolean inPanic = decisionDataProvider.getPersonDecisionData(person.getId()).isInPanic();
		
		if (inPanic) return panicRouter.calcLeastCostPath(fromNode, toNode, starttime, person, vehicle);
		else return nonPanicRouter.calcLeastCostPath(fromNode, toNode, starttime, person, vehicle);
	}

	@Override
	public void setModeRestriction(Set<String> modeRestriction) {
		this.nonPanicRouter.setModeRestriction(modeRestriction);
		this.panicRouter.setModeRestriction(modeRestriction);
	}
}