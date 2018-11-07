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

package org.matsim.contrib.minibus.hook;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

//import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.minibus.PConfigGroup;
import org.matsim.contrib.minibus.PConstants.OperatorState;
import org.matsim.contrib.minibus.fare.StageContainerCreator;
import org.matsim.contrib.minibus.fare.TicketMachineI;
import org.matsim.contrib.minibus.operator.Operator;
import org.matsim.contrib.minibus.operator.OperatorInitializer;
import org.matsim.contrib.minibus.operator.PFranchise;
import org.matsim.contrib.minibus.operator.POperators;
import org.matsim.contrib.minibus.operator.SubsidyI;
import org.matsim.contrib.minibus.operator.TimeProvider;
import org.matsim.contrib.minibus.replanning.PStrategyManager;
import org.matsim.contrib.minibus.schedule.PStopsFactory;
import org.matsim.contrib.minibus.scoring.OperatorCostCollectorHandler;
import org.matsim.contrib.minibus.scoring.PScoreContainer;
import org.matsim.contrib.minibus.scoring.PScorePlansHandler;
import org.matsim.contrib.minibus.scoring.StageContainer2AgentMoneyEvent;
import org.matsim.contrib.minibus.scoring.routeDesignScoring.RouteDesignScoringManager;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.ScoringEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;

import com.google.inject.Inject;

/**
 * Black box for paratransit
 * 
 * @author aneumann
 *
 */
public final class PBox implements POperators {

	@SuppressWarnings("unused")
	private final static Logger log = Logger.getLogger(PBox.class);

	private LinkedList<Operator> operators;

	private final PConfigGroup pConfig;
	private final PFranchise franchise;
	private OperatorInitializer operatorInitializer;

	private TransitSchedule pStopsOnly;
	private TransitSchedule pTransitSchedule;

	private final PScorePlansHandler scorePlansHandler;
	private final StageContainerCreator stageCollectorHandler;
	private final OperatorCostCollectorHandler operatorCostCollectorHandler;
	private final PStrategyManager strategyManager = new PStrategyManager();
	private final RouteDesignScoringManager routeDesignScoreManager = new RouteDesignScoringManager();

	private final TicketMachineI ticketMachine;
	
	@Inject(optional=true) private SubsidyI subsidy;
	// yy my intuition would be to pass an empty subsidy rather than making it optional. 

	/**
	 * Constructor that allows to set the ticketMachine.  Deliberately in constructor and not as setter to keep the variable final.  Might be
	 * replaced by a builder and/or guice at some later point in time.  But stay with "direct" injection for the time being.  kai, jan'17
	 */
	@Inject PBox(PConfigGroup pConfig, TicketMachineI ticketMachine) {
		this.pConfig = pConfig;
		this.ticketMachine = ticketMachine ;
		this.scorePlansHandler = new PScorePlansHandler(this.ticketMachine);
		this.stageCollectorHandler = new StageContainerCreator(this.pConfig.getPIdentifier());
		this.operatorCostCollectorHandler = new OperatorCostCollectorHandler(this.pConfig.getPIdentifier(), this.pConfig.getCostPerVehicleAndDay(), this.pConfig.getCostPerKilometer() / 1000.0, this.pConfig.getCostPerHour() / 3600.0);
		this.franchise = new PFranchise(this.pConfig.getUseFranchise(), pConfig.getGridSize());	
	}

	void notifyStartup(StartupEvent event) {
		// This is the first iteration

		TimeProvider timeProvider = new TimeProvider(this.pConfig, event.getServices().getControlerIO().getOutputPath());
		event.getServices().getEvents().addHandler(timeProvider);

		// initialize strategy manager
		this.strategyManager.init(this.pConfig, this.stageCollectorHandler, this.ticketMachine, timeProvider);
		
		// initialize route design scoring manager
		this.routeDesignScoreManager.init(this.pConfig);

		// init fare collector
		this.stageCollectorHandler.init(event.getServices().getScenario().getNetwork());
		event.getServices().getEvents().addHandler(this.stageCollectorHandler);
		event.getServices().addControlerListener(this.stageCollectorHandler);
		this.stageCollectorHandler.addStageContainerHandler(this.scorePlansHandler);

		// init operator cost collector
		this.operatorCostCollectorHandler.init(event.getServices().getScenario().getNetwork());
		event.getServices().getEvents().addHandler(this.operatorCostCollectorHandler);
		event.getServices().addControlerListener(this.operatorCostCollectorHandler);
		this.operatorCostCollectorHandler.addOperatorCostContainerHandler(this.scorePlansHandler);

		// init fare2moneyEvent
		StageContainer2AgentMoneyEvent fare2AgentMoney = new StageContainer2AgentMoneyEvent(event.getServices(), this.ticketMachine);
		this.stageCollectorHandler.addStageContainerHandler(fare2AgentMoney);

		// init possible paratransit stops
		this.pStopsOnly = PStopsFactory.createPStops(event.getServices().getScenario().getNetwork(), this.pConfig, event.getServices().getScenario().getTransitSchedule());

		this.operators = new LinkedList<>();
		this.operatorInitializer = new OperatorInitializer(this.pConfig, this.franchise, this.pStopsOnly, event.getServices(), timeProvider);

		// init additional operators from a given transit schedule file
		LinkedList<Operator> operatorsFromSchedule = this.operatorInitializer.createOperatorsFromSchedule(event.getServices().getScenario().getTransitSchedule());
		this.operators.addAll(operatorsFromSchedule);

		// init initial set of operators - reduced by the number of preset operators
		LinkedList<Operator> initialOperators = this.operatorInitializer.createAdditionalOperators(this.strategyManager, event.getServices().getConfig().controler().getFirstIteration(), (this.pConfig.getNumberOfOperators() - operatorsFromSchedule.size()));
		this.operators.addAll(initialOperators);

		// collect the transit schedules from all operators
		this.pTransitSchedule = new TransitScheduleFactoryImpl().createTransitSchedule();
		for (TransitStopFacility stop : this.pStopsOnly.getFacilities().values()) {
			this.pTransitSchedule.addStopFacility(stop);
		}
		for (Operator operator : this.operators) {
			this.pTransitSchedule.addTransitLine(operator.getCurrentTransitLine());
		}

		// Reset the franchise system - TODO necessary?
		this.franchise.reset(this.operators);
	}

	void notifyIterationStarts(IterationStartsEvent event) {

		this.strategyManager.updateStrategies(event.getIteration());

		// Adapt number of operators
		this.handleBankruptOperators(event.getIteration());

		// Replan all operators
		for (Operator operator : this.operators) {
			operator.replan(this.strategyManager, event.getIteration());
		}

		// Collect current lines offered
		// why is the following done twice (see notifyScoring)?
		this.pTransitSchedule = new TransitScheduleFactoryImpl().createTransitSchedule();
		for (TransitStopFacility stop : this.pStopsOnly.getFacilities().values()) {
			this.pTransitSchedule.addStopFacility(stop);
		}
		for (Operator operator : this.operators) {
			this.pTransitSchedule.addTransitLine(operator.getCurrentTransitLine());
		}

		// Reset the franchise system
		this.franchise.reset(this.operators);
	}

	void notifyScoring(ScoringEvent event) {

		if (this.subsidy != null) {
			subsidy.computeSubsidy();
		}

		Map<Id<Vehicle>, PScoreContainer> driverId2ScoreMap = this.scorePlansHandler.getDriverId2ScoreMap();
		for (Operator operator : this.operators) {
			operator.score(driverId2ScoreMap, subsidy, routeDesignScoreManager);
		}

		// why is the following done twice (see notifyIterationstarts)?
		this.pTransitSchedule = new TransitScheduleFactoryImpl().createTransitSchedule();
		for (TransitStopFacility stop : this.pStopsOnly.getFacilities().values()) {
			this.pTransitSchedule.addStopFacility(stop);
		}
		for (Operator operator : this.operators) {
			this.pTransitSchedule.addTransitLine(operator.getCurrentTransitLine());
		}

		writeScheduleToFile(this.pTransitSchedule, event.getServices().getControlerIO().getIterationFilename(event.getIteration(), "transitScheduleScored.xml.gz"));
	}

	private void handleBankruptOperators(int iteration) {

		LinkedList<Operator> operatorsToKeep = new LinkedList<>();
		int operatorsProspecting = 0;
		int operatorsInBusiness = 0;
		int operatorsBankrupt = 0;

		// Get operators with positive budget
		for (Operator operator : this.operators) {
			if(operator.getOperatorState().equals(OperatorState.PROSPECTING)){
				operatorsToKeep.add(operator);
				operatorsProspecting++;
			}

			if(operator.getOperatorState().equals(OperatorState.INBUSINESS)){
				operatorsToKeep.add(operator);
				operatorsInBusiness++;
			}

			if(operator.getOperatorState().equals(OperatorState.BANKRUPT)){
				operatorsBankrupt++;
			}
		}

		// get the number of new operators
		int numberOfNewOperators = operatorsBankrupt;

		if(this.pConfig.getUseAdaptiveNumberOfOperators()){
			// adapt the number of operators by calculating the exact number necessary
			numberOfNewOperators = (int) (operatorsInBusiness * (1.0/this.pConfig.getShareOfOperatorsWithProfit() - 1.0) + 0.0000000000001) - operatorsProspecting;
		}

		// delete bankrupt ones
		this.operators = operatorsToKeep;

		if (this.pConfig.getDisableCreationOfNewOperatorsInIteration() > iteration) {
			// recreate all other
			LinkedList<Operator> newOperators1 = this.operatorInitializer.createAdditionalOperators(this.strategyManager, iteration, numberOfNewOperators);
			this.operators.addAll(newOperators1);

			// too few operators in play, increase to the minimum specified in the config
			LinkedList<Operator> newOperators2 = this.operatorInitializer.createAdditionalOperators(this.strategyManager, iteration, (this.pConfig.getNumberOfOperators() - this.operators.size()));
			this.operators.addAll(newOperators2);

			// all operators are in business, increase by one to ensure minimal mutation
			if (this.operators.size() == operatorsInBusiness) {
				LinkedList<Operator> newOperators3 = this.operatorInitializer.createAdditionalOperators(this.strategyManager, iteration, 1);
				this.operators.addAll(newOperators3);
			}
		}
	}

	TransitSchedule getpTransitSchedule() {
		return this.pTransitSchedule;
	}

	public List<Operator> getOperators() {
		return operators;
	}

	private void writeScheduleToFile(TransitSchedule schedule, String iterationFilename) {
		TransitScheduleWriter writer = new TransitScheduleWriter(schedule);
		writer.writeFile(iterationFilename);
	}
}
