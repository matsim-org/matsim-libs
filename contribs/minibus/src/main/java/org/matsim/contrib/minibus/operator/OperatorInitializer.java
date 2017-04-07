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

package org.matsim.contrib.minibus.operator;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.minibus.PConfigGroup;
import org.matsim.contrib.minibus.replanning.CreateNew24hPlan;
import org.matsim.contrib.minibus.replanning.CreateNewPlan;
import org.matsim.contrib.minibus.replanning.PStrategy;
import org.matsim.contrib.minibus.replanning.PStrategyManager;
import org.matsim.contrib.minibus.routeProvider.PRouteProvider;
import org.matsim.contrib.minibus.routeProvider.PRouteProviderFactory;
import org.matsim.core.controler.MatsimServices;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import java.util.ArrayList;
import java.util.LinkedList;

/**
 * 
 * @author aneumann
 *
 */
public final class OperatorInitializer {

	private final static Logger log = Logger.getLogger(OperatorInitializer.class);
	private final PConfigGroup pConfig;
	private final OperatorFactory operatorFactory;
	private final PRouteProvider routeProvider;
	private final PStrategy initialStrategy;
	private int counter;


	public OperatorInitializer(PConfigGroup pConfig, PFranchise franchise, TransitSchedule pStopsOnly, MatsimServices controler, 
			TimeProvider timeProvider) {
		this.pConfig = pConfig;
		this.operatorFactory = new OperatorFactory(this.pConfig, franchise);
		this.routeProvider = PRouteProviderFactory.createRouteProvider(controler.getScenario().getNetwork(), controler.getScenario().getPopulation(), this.pConfig, pStopsOnly, controler.getControlerIO().getOutputPath(), controler.getEvents());

		if (this.pConfig.getStartWith24Hours()) {
			this.initialStrategy = new CreateNew24hPlan(new ArrayList<String>());
		} else {
			ArrayList<String> parameter = new ArrayList<>();
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
		LinkedList<Operator> emptyOperators = new LinkedList<>();
		for (int i = 0; i < numberOfNewOperators; i++) {
			Operator operator = this.operatorFactory.createNewOperator(this.createNewIdForOperator(iteration));
			emptyOperators.add(operator);
		}

		LinkedList<Operator> initializedOperator = new LinkedList<>();
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
