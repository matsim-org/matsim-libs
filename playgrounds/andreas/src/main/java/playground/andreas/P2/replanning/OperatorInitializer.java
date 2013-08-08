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

package playground.andreas.P2.replanning;

import java.util.ArrayList;
import java.util.LinkedList;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.controler.Controler;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import playground.andreas.P2.helper.PConfigGroup;
import playground.andreas.P2.operator.Cooperative;
import playground.andreas.P2.operator.CooperativeFactory;
import playground.andreas.P2.pbox.PFranchise;
import playground.andreas.P2.routeProvider.PRouteProvider;
import playground.andreas.P2.routeProvider.PRouteProviderFactory;

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
	
	
	public OperatorInitializer(PConfigGroup pConfig, PFranchise franchise, TransitSchedule pStopsOnly, Controler controler, TimeProvider timeProvider) {
		this.pConfig = pConfig;
		this.cooperativeFactory = new CooperativeFactory(this.pConfig, franchise);
		this.routeProvider = PRouteProviderFactory.createRouteProvider(controler.getNetwork(), controler.getPopulation(), this.pConfig, pStopsOnly, controler.getControlerIO().getOutputPath(), controler.getEvents());
		
		if (this.pConfig.getStartWith24Hours()) {
			this.initialStrategy = new CreateNew24hPlan(new ArrayList<String>());
		} else {
			ArrayList<String> parameter = new ArrayList<String>();
			parameter.add(Double.toString(pConfig.getTimeSlotSize()));
			parameter.add(Double.toString(pConfig.getMinInitialStopDistance()));
			this.initialStrategy = new CreateNewPlan(parameter);
			((CreateNewPlan) this.initialStrategy).setTimeProvider(timeProvider);
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
		LinkedList<Cooperative> emptyCoops = new LinkedList<Cooperative>();
		for (int i = 0; i < numberOfNewCooperatives; i++) {
			Cooperative cooperative = this.cooperativeFactory.createNewCooperative(this.createNewIdForCooperative(iteration));
			emptyCoops.add(cooperative);
		}
		
		LinkedList<Cooperative> initializedCoops = new LinkedList<Cooperative>();
		int numberOfCoopsFailedToBeInitialized = 0;
		for (Cooperative cooperative : emptyCoops) {
			boolean initComplete = cooperative.init(this.routeProvider, this.initialStrategy, iteration, this.pConfig.getInitialBudget());
			if (initComplete) {
				cooperative.replan(pStrategyManager, iteration);
				initializedCoops.add(cooperative);
			} else {
				numberOfCoopsFailedToBeInitialized++;
			}
		}
		
		if (numberOfCoopsFailedToBeInitialized > 0) {
			log.warn(numberOfCoopsFailedToBeInitialized + " out of " + numberOfNewCooperatives + " operators could no be initialized. Proceeding with " + initializedCoops.size() + " new operators.");
		}
		
		return initializedCoops;
	}
	
	private Id createNewIdForCooperative(int iteration){
		this.counter++;
		return new IdImpl(this.pConfig.getPIdentifier() + iteration + "_" + this.counter);
	}
}
