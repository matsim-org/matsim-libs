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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.minibus.PConfigGroup;
import org.matsim.contrib.minibus.PConstants.OperatorState;
import org.matsim.contrib.minibus.fare.StageContainerCreator;
import org.matsim.contrib.minibus.fare.TicketMachine;
import org.matsim.contrib.minibus.operator.*;
import org.matsim.contrib.minibus.replanning.PStrategyManager;
import org.matsim.contrib.minibus.schedule.PStopsFactory;
import org.matsim.contrib.minibus.scoring.OperatorCostCollectorHandler;
import org.matsim.contrib.minibus.scoring.ScoreContainer;
import org.matsim.contrib.minibus.scoring.ScorePlansHandler;
import org.matsim.contrib.minibus.scoring.StageContainer2AgentMoneyEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.ScoringEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.TransitScheduleWriterV1;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Black box for paratransit
 * 
 * @author aneumann
 *
 */
final class PBox implements Operators {
	
	@SuppressWarnings("unused")
	private final static Logger log = Logger.getLogger(PBox.class);
	
	private LinkedList<Operator> operators;
	
	private final PConfigGroup pConfig;
	private final PFranchise franchise;
	private OperatorInitializer operatorInitializer;

	private TransitSchedule pStopsOnly;
	private TransitSchedule pTransitSchedule;
	
	private final ScorePlansHandler scorePlansHandler;
	private final StageContainerCreator stageCollectorHandler;
	private final OperatorCostCollectorHandler operatorCostCollectorHandler;
	private final PStrategyManager strategyManager = new PStrategyManager();

	private final TicketMachine ticketMachine;

    PBox(PConfigGroup pConfig) {
		this.pConfig = pConfig;
		this.ticketMachine = new TicketMachine(this.pConfig.getEarningsPerBoardingPassenger(), this.pConfig.getEarningsPerKilometerAndPassenger() / 1000.0);
		this.scorePlansHandler = new ScorePlansHandler(this.ticketMachine);
		this.stageCollectorHandler = new StageContainerCreator(this.pConfig.getPIdentifier());
		this.operatorCostCollectorHandler = new OperatorCostCollectorHandler(this.pConfig.getPIdentifier(), this.pConfig.getCostPerVehicleAndDay(), this.pConfig.getCostPerKilometer() / 1000.0, this.pConfig.getCostPerHour() / 3600.0);
		this.franchise = new PFranchise(this.pConfig.getUseFranchise(), pConfig.getGridSize());
	}

	void notifyStartup(StartupEvent event) {
		// This is the first iteration

        TimeProvider timeProvider = new TimeProvider(this.pConfig, event.getControler().getControlerIO().getOutputPath());
		event.getControler().getEvents().addHandler(timeProvider);
		
		// initialize strategy manager
		this.strategyManager.init(this.pConfig, this.stageCollectorHandler, this.ticketMachine, timeProvider);
		
		// init fare collector
        this.stageCollectorHandler.init(event.getControler().getScenario().getNetwork());
		event.getControler().getEvents().addHandler(this.stageCollectorHandler);
		event.getControler().addControlerListener(this.stageCollectorHandler);
		this.stageCollectorHandler.addStageContainerHandler(this.scorePlansHandler);
		
		// init operator cost collector
        this.operatorCostCollectorHandler.init(event.getControler().getScenario().getNetwork());
		event.getControler().getEvents().addHandler(this.operatorCostCollectorHandler);
		event.getControler().addControlerListener(this.operatorCostCollectorHandler);
		this.operatorCostCollectorHandler.addOperatorCostContainerHandler(this.scorePlansHandler);
		
		// init fare2moneyEvent
		StageContainer2AgentMoneyEvent fare2AgentMoney = new StageContainer2AgentMoneyEvent(event.getControler(), this.ticketMachine);
		this.stageCollectorHandler.addStageContainerHandler(fare2AgentMoney);
		
		// init possible paratransit stops
        this.pStopsOnly = PStopsFactory.createPStops(event.getControler().getScenario().getNetwork(), this.pConfig, event.getControler().getScenario().getTransitSchedule());
		
		this.operators = new LinkedList<>();
		this.operatorInitializer = new OperatorInitializer(this.pConfig, this.franchise, this.pStopsOnly, event.getControler(), timeProvider);
		
		// init additional operators from a given transit schedule file
		LinkedList<Operator> operatorsFromSchedule = this.operatorInitializer.createOperatorsFromSchedule(event.getControler().getScenario().getTransitSchedule());
		this.operators.addAll(operatorsFromSchedule);
		
		// init initial set of operators - reduced by the number of preset operators
		LinkedList<Operator> initialOperators = this.operatorInitializer.createAdditionalOperators(this.strategyManager, event.getControler().getConfig().controler().getFirstIteration(), (this.pConfig.getNumberOfOperators() - operatorsFromSchedule.size()));
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
		Map<Id<Vehicle>, ScoreContainer> driverId2ScoreMap = this.scorePlansHandler.getDriverId2ScoreMap();
		for (Operator operator : this.operators) {
			operator.score(driverId2ScoreMap);
		}
		
		// why is the following done twice (see notifyIterationstarts)?
		this.pTransitSchedule = new TransitScheduleFactoryImpl().createTransitSchedule();
		for (TransitStopFacility stop : this.pStopsOnly.getFacilities().values()) {
			this.pTransitSchedule.addStopFacility(stop);
		}
		for (Operator operator : this.operators) {
			this.pTransitSchedule.addTransitLine(operator.getCurrentTransitLine());
		}
		
		writeScheduleToFile(this.pTransitSchedule, event.getControler().getControlerIO().getIterationFilename(event.getIteration(), "transitScheduleScored.xml.gz"));		
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
		TransitScheduleWriterV1 writer = new TransitScheduleWriterV1(schedule);
		writer.write(iterationFilename);		
	}
}
