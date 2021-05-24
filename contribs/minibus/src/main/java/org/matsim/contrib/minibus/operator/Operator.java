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

package org.matsim.contrib.minibus.operator;

import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.minibus.PConstants.OperatorState;
import org.matsim.contrib.minibus.replanning.PStrategy;
import org.matsim.contrib.minibus.replanning.PStrategyManager;
import org.matsim.contrib.minibus.routeProvider.PRouteProvider;
import org.matsim.contrib.minibus.scoring.PScoreContainer;
import org.matsim.contrib.minibus.scoring.routeDesignScoring.RouteDesignScoringManager;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.vehicles.Vehicle;

/**
 * 
 * @author aneumann
 *
 */
public interface Operator {
	
	public boolean init(PRouteProvider pRouteProvider, PStrategy initialStrategy, int iteration, double initialBudget);
	
	public void score(Map<Id<Vehicle>, PScoreContainer> driverId2ScoreMap, SubsidyI subsidy, RouteDesignScoringManager routeDesignScoringManager);
	
	public void replan(PStrategyManager pStrategyManager, int iteration);

	public TransitLine getCurrentTransitLine();

	public double getBudget();

	public int getNumberOfVehiclesOwned();
	
	public List<PPlan> getAllPlans();

	public int getCurrentIteration();

	public PRouteProvider getRouteProvider();

	public PFranchise getFranchise();

	public Id<Operator> getId();
	
	public Id<PPlan> getNewPlanId();

	public PPlan getBestPlan();
	
	public double getMinOperationTime();
	
	public OperatorState getOperatorState();

	public void setBudget(double budget);

}
