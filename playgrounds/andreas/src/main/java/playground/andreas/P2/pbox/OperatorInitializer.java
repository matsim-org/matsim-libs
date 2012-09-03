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

package playground.andreas.P2.pbox;

import java.util.ArrayList;
import java.util.LinkedList;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import playground.andreas.P2.helper.PConfigGroup;
import playground.andreas.P2.operator.Cooperative;
import playground.andreas.P2.operator.CooperativeFactory;
import playground.andreas.P2.replanning.CreateCooperativeFromSchedule;
import playground.andreas.P2.replanning.CreateNew24hPlan;
import playground.andreas.P2.replanning.CreateNewPlan;
import playground.andreas.P2.replanning.PStrategy;
import playground.andreas.P2.replanning.PStrategyManager;
import playground.andreas.P2.routeProvider.PRouteProvider;

/**
 * 
 * @author aneumann
 *
 */
public class OperatorInitializer {
	
	private final static Logger log = Logger.getLogger(OperatorInitializer.class);
	private PConfigGroup pConfig;
	private CooperativeFactory cooperativeFactory;
	private PRouteProvider routeProvider;
	private PStrategy initialStrategy;
	private int counter;
	
	
	public OperatorInitializer(PConfigGroup pConfig, PFranchise franchise, PRouteProvider routeProvider) {
		this.pConfig = pConfig;
		this.cooperativeFactory = new CooperativeFactory(this.pConfig, franchise);
		this.routeProvider = routeProvider;
		
		if (this.pConfig.getStartWith24Hours()) {
			this.initialStrategy = new CreateNew24hPlan(new ArrayList<String>());
		} else {
			this.initialStrategy = new CreateNewPlan(new ArrayList<String>());
		}
	}
	
	/**
	 * Creates operators from a given transit schedule
	 *  
	 * @param originalSchedule The transit schedule given
	 * @return A list containing the operators created
	 */
	public LinkedList<Cooperative> createOperatorsFromSchedule(TransitSchedule originalSchedule){
		return new CreateCooperativeFromSchedule(this.cooperativeFactory, this.routeProvider, this.pConfig, originalSchedule).run();
	}
	
	/**
	 * Create the number of additional operators
	 * 
	 * @param pStrategyManager
	 * @param iteration
	 * @param numberOfNewCooperatives
	 * @return
	 */
	public LinkedList<Cooperative> createAdditionalCooperatives(PStrategyManager pStrategyManager, int iteration, int numberOfNewCooperatives) {
		LinkedList<Cooperative> initialCoops = new LinkedList<Cooperative>();
		for (int i = 0; i < numberOfNewCooperatives; i++) {
			Cooperative cooperative = this.cooperativeFactory.createNewCooperative(this.createNewIdForCooperative(iteration));
			initialCoops.add(cooperative);
		}
		
		for (Cooperative cooperative : initialCoops) {
			cooperative.init(this.routeProvider, this.initialStrategy, iteration, this.pConfig.getInitialBudget());
			cooperative.replan(pStrategyManager, iteration);
		}
		
		return initialCoops;
	}
	
	private Id createNewIdForCooperative(int iteration){
		this.counter++;
		return new IdImpl(this.pConfig.getPIdentifier() + iteration + "_" + this.counter);
	}
}
