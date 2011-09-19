/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.andreas.P2.pbox;

import java.util.List;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.pt.transitSchedule.api.TransitLine;

import playground.andreas.P2.plan.PPlan;
import playground.andreas.P2.plan.PRouteProvider;
import playground.andreas.P2.replanning.PStrategyManager;
import playground.andreas.P2.scoring.ScoreContainer;

/**
 * 
 * @author aneumann
 *
 */
public interface Cooperative {
	
	public void init(PRouteProvider pRouteProvider, int iteration);
	
	public void score(TreeMap<Id, ScoreContainer> driverId2ScoreMap);
	
	public void replan(PStrategyManager pStrategyManager, int iteration);

	public TransitLine getCurrentTransitLine();

	public double getBudget();

	public List<PPlan> getAllPlans();

	public int getCurrentIteration();

	public PRouteProvider getRouteProvider();

	public PFranchise getFranchise();

	public Id getId();

	public PPlan getBestPlan();
	
	public double getMinOperationTime();

}
