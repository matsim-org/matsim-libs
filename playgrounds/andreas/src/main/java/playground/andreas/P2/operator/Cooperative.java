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

package playground.andreas.P2.operator;

import java.util.List;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.pt.transitSchedule.api.TransitLine;

import playground.andreas.P2.helper.PConstants.CoopState;
import playground.andreas.P2.pbox.PFranchise;
import playground.andreas.P2.replanning.PPlan;
import playground.andreas.P2.replanning.PStrategy;
import playground.andreas.P2.replanning.PStrategyManager;
import playground.andreas.P2.routeProvider.PRouteProvider;
import playground.andreas.P2.scoring.ScoreContainer;

/**
 * 
 * @author aneumann
 *
 */
public interface Cooperative {
	
	public boolean init(PRouteProvider pRouteProvider, PStrategy initialStrategy, int iteration, double initialBudget);
	
	public void score(TreeMap<Id, ScoreContainer> driverId2ScoreMap);
	
	public void replan(PStrategyManager pStrategyManager, int iteration);

	public TransitLine getCurrentTransitLine();

	public double getBudget();

	public int getNumberOfVehiclesOwned();
	
	public List<PPlan> getAllPlans();

	public int getCurrentIteration();

	public PRouteProvider getRouteProvider();

	public PFranchise getFranchise();

	public Id getId();
	
	public Id getNewRouteId();

	public PPlan getBestPlan();
	
	public double getMinOperationTime();
	
	public double getCostPerVehicleBuy();

	public double getCostPerVehicleSell();
	
	public CoopState getCoopState();

	public void setBudget(double budget);

}
