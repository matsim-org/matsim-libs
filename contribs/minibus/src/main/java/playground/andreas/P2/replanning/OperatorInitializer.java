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
import org.matsim.core.controler.Controler;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import playground.andreas.P2.helper.PConfigGroup;
import playground.andreas.P2.operator.Operator;
import playground.andreas.P2.operator.OperatorFactory;
import playground.andreas.P2.pbox.PFranchise;
import playground.andreas.P2.replanning.modules.CreateNew24hPlan;
import playground.andreas.P2.replanning.modules.CreateNewPlan;
import playground.andreas.P2.routeProvider.PRouteProvider;
import playground.andreas.P2.routeProvider.PRouteProviderFactory;

/**
 * 
 * @author aneumann
 *
 */
public final class OperatorInitializer {
	
	private final static Logger log = Logger.getLogger(OperatorInitializer.class);
	private PConfigGroup pConfig;
	private OperatorFactory operatorFactory;
	private PRouteProvider routeProvider;
	private PStrategy initialStrategy;
	private int counter;
	
	
	public OperatorInitializer(PConfigGroup pConfig, PFranchise franchise, TransitSchedule pStopsOnly, Controler controler, TimeProvider timeProvider) {
		this.pConfig = pConfig;
		this.operatorFactory = new OperatorFactory(this.pConfig, franchise);
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
	public LinkedList<Operator> createOperatorsFromSchedule(TransitSchedule originalSchedule){
		return new CreateOperatorFromTransitSchedule(this.operatorFactory, this.routeProvider, this.pConfig, originalSchedule).run();
	}
	
	/**
	 * Create the number of additional operators
	 * 
	 * @param pStrategyManager
	 * @param iteration
	 * @param numberOfNewOperators
	 * @return
	 */
	public LinkedList<Operator> createAdditionalOperators(PStrategyManager pStrategyManager, int iteration, int numberOfNewOperators) {
		LinkedList<Operator> emptyOperators = new LinkedList<Operator>();
		for (int i = 0; i < numberOfNewOperators; i++) {
			Operator operator = this.operatorFactory.createNewOperator(this.createNewIdForOperator(iteration));
			emptyOperators.add(operator);
		}
		
		LinkedList<Operator> initializedOperator = new LinkedList<Operator>();
		int numberOfOperatorsFailedToBeInitialized = 0;
		for (Operator operator : emptyOperators) {
			boolean initComplete = operator.init(this.routeProvider, this.initialStrategy, iteration, this.pConfig.getInitialBudget());
			if (initComplete) {
				operator.replan(pStrategyManager, iteration);
				initializedOperator.add(operator);
			} else {
				numberOfOperatorsFailedToBeInitialized++;
			}
		}
		
		if (numberOfOperatorsFailedToBeInitialized > 0) {
			log.warn(numberOfOperatorsFailedToBeInitialized + " out of " + numberOfNewOperators + " operators could no be initialized. Proceeding with " + initializedOperator.size() + " new operators.");
		}
		
		return initializedOperator;
	}
	
	private Id<Operator> createNewIdForOperator(int iteration){
		this.counter++;
		return Id.create(this.pConfig.getPIdentifier() + iteration + "_" + this.counter, Operator.class);
	}
}
