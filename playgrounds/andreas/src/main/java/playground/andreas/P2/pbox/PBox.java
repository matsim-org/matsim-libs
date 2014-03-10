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

package playground.andreas.P2.pbox;

import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.ScoringEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.ScoringListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.pt.transitSchedule.TransitScheduleWriterV1;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import playground.andreas.P2.helper.PConfigGroup;
import playground.andreas.P2.helper.PConstants.CoopState;
import playground.andreas.P2.operator.Cooperative;
import playground.andreas.P2.replanning.OperatorInitializer;
import playground.andreas.P2.replanning.PStrategyManager;
import playground.andreas.P2.replanning.TimeProvider;
import playground.andreas.P2.schedule.PStopsFactory;
import playground.andreas.P2.schedule.PTransitScheduleImpl;
import playground.andreas.P2.scoring.StageContainer2AgentMoneyEvent;
import playground.andreas.P2.scoring.ScoreContainer;
import playground.andreas.P2.scoring.ScorePlansHandler;
import playground.andreas.P2.scoring.fare.StageContainerCreator;
import playground.andreas.P2.scoring.fare.TicketMachine;
import playground.andreas.P2.scoring.operator.OperatorCostCollectorHandler;

/**
 * Black box for paratransit
 * 
 * @author aneumann
 *
 */
public class PBox implements StartupListener, IterationStartsListener, ScoringListener{
	
	@SuppressWarnings("unused")
	private final static Logger log = Logger.getLogger(PBox.class);
	
	private LinkedList<Cooperative> cooperatives;
	
	private final PConfigGroup pConfig;
	private PFranchise franchise;
	private OperatorInitializer operatorInitializer;

	private TransitSchedule pStopsOnly;
	private TransitSchedule pTransitSchedule;
	
	private final ScorePlansHandler scorePlansHandler;
	private StageContainerCreator stageCollectorHandler;
	private OperatorCostCollectorHandler operatorCostCollectorHandler;
	private PStrategyManager strategyManager;

	private final TicketMachine ticketMachine;
	private TimeProvider timeProvider;

	
	public PBox(PConfigGroup pConfig) {
		this.pConfig = pConfig;
		this.ticketMachine = new TicketMachine(this.pConfig.getEarningsPerBoardingPassenger(), this.pConfig.getEarningsPerKilometerAndPassenger() / 1000.0);
		this.scorePlansHandler = new ScorePlansHandler(this.ticketMachine);
		this.stageCollectorHandler = new StageContainerCreator(this.pConfig.getPIdentifier());
		this.operatorCostCollectorHandler = new OperatorCostCollectorHandler(this.pConfig.getPIdentifier(), this.pConfig.getCostPerVehicleAndDay(), this.pConfig.getCostPerKilometer() / 1000.0, this.pConfig.getCostPerHour() / 3600.0);
		this.franchise = new PFranchise(this.pConfig.getUseFranchise(), pConfig.getGridSize());
		this.strategyManager = new PStrategyManager(this.pConfig);
	}

	@Override
	public void notifyStartup(StartupEvent event) {
		// This is the first iteration
		
		this.timeProvider = new TimeProvider(this.pConfig, event.getControler().getControlerIO().getOutputPath());
		event.getControler().getEvents().addHandler(this.timeProvider);
		
		// initialize strategy manager
		this.strategyManager.init(this.pConfig, event.getControler().getEvents(), this.stageCollectorHandler, this.ticketMachine, this.timeProvider);
		
		// init fare collector
		this.stageCollectorHandler.init(event.getControler().getNetwork());
		event.getControler().getEvents().addHandler(this.stageCollectorHandler);
		event.getControler().addControlerListener(this.stageCollectorHandler);
		this.stageCollectorHandler.addStageContainerHandler(this.scorePlansHandler);
		
		// init operator cost collector
		this.operatorCostCollectorHandler.init(event.getControler().getNetwork());
		event.getControler().getEvents().addHandler(this.operatorCostCollectorHandler);
		event.getControler().addControlerListener(this.operatorCostCollectorHandler);
		this.operatorCostCollectorHandler.addOperatorCostContainerHandler(this.scorePlansHandler);
		
		// init fare2moneyEvent
		StageContainer2AgentMoneyEvent fare2AgentMoney = new StageContainer2AgentMoneyEvent(event.getControler(), this.ticketMachine);
		this.stageCollectorHandler.addStageContainerHandler(fare2AgentMoney);
		
		// init possible paratransit stops
		this.pStopsOnly = PStopsFactory.createPStops(event.getControler().getNetwork(), this.pConfig, event.getControler().getScenario().getTransitSchedule());
		
		this.cooperatives = new LinkedList<Cooperative>();
		this.operatorInitializer = new OperatorInitializer(this.pConfig, this.franchise, this.pStopsOnly, event.getControler(), this.timeProvider);
		
		// init additional cooperatives from a given transit schedule file
		LinkedList<Cooperative> coopsFromSchedule = this.operatorInitializer.createOperatorsFromSchedule(event.getControler().getScenario().getTransitSchedule());
		this.cooperatives.addAll(coopsFromSchedule);
		
		// init initial set of cooperatives - reduced by the number of preset coops
		LinkedList<Cooperative> initialCoops = this.operatorInitializer.createAdditionalCooperatives(this.strategyManager, event.getControler().getConfig().controler().getFirstIteration(), (this.pConfig.getNumberOfCooperatives() - coopsFromSchedule.size()));
		this.cooperatives.addAll(initialCoops);
		
		// collect the transit schedules from all cooperatives
		this.pTransitSchedule = new PTransitScheduleImpl(this.pStopsOnly.getFactory());
		for (TransitStopFacility stop : this.pStopsOnly.getFacilities().values()) {
			this.pTransitSchedule.addStopFacility(stop);
		}
		for (Cooperative cooperative : this.cooperatives) {
			this.pTransitSchedule.addTransitLine(cooperative.getCurrentTransitLine());
		}
		
		// Reset the franchise system - TODO necessary?
		this.franchise.reset(this.cooperatives);
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		// This is NOT called in the first iteration
		this.strategyManager.updateStrategies(event.getIteration());
		
		// Adapt number of cooperatives
		this.handleBankruptCopperatives(event.getIteration());
		
		// Replan all cooperatives
		for (Cooperative cooperative : this.cooperatives) {
			cooperative.replan(this.strategyManager, event.getIteration());
		}
		
		// Collect current lines offered
		this.pTransitSchedule = new PTransitScheduleImpl(this.pStopsOnly.getFactory());
		for (TransitStopFacility stop : this.pStopsOnly.getFacilities().values()) {
			this.pTransitSchedule.addStopFacility(stop);
		}
		for (Cooperative cooperative : this.cooperatives) {
			this.pTransitSchedule.addTransitLine(cooperative.getCurrentTransitLine());
		}
		
		// Reset the franchise system
		this.franchise.reset(this.cooperatives);
	}

	@Override
	public void notifyScoring(ScoringEvent event) {
		TreeMap<Id, ScoreContainer> driverId2ScoreMap = this.scorePlansHandler.getDriverId2ScoreMap();
		for (Cooperative cooperative : this.cooperatives) {
			cooperative.score(driverId2ScoreMap);
		}
		
		this.pTransitSchedule = new PTransitScheduleImpl(this.pStopsOnly.getFactory());
		for (TransitStopFacility stop : this.pStopsOnly.getFacilities().values()) {
			this.pTransitSchedule.addStopFacility(stop);
		}
		
		for (Cooperative cooperative : this.cooperatives) {
			this.pTransitSchedule.addTransitLine(cooperative.getCurrentTransitLine());
		}
		
		writeScheduleToFile(this.pTransitSchedule, event.getControler().getControlerIO().getIterationFilename(event.getIteration(), "transitScheduleScored.xml.gz"));		
	}

	private void handleBankruptCopperatives(int iteration) {
		
		LinkedList<Cooperative> cooperativesToKeep = new LinkedList<Cooperative>();
		int coopsProspecting = 0;
		int coopsInBusiness = 0;
		int coopsBankrupt = 0;
		
		// Get cooperatives with positive budget
		for (Cooperative cooperative : this.cooperatives) {
			if(cooperative.getCoopState().equals(CoopState.PROSPECTING)){
				cooperativesToKeep.add(cooperative);
				coopsProspecting++;
			}
			
			if(cooperative.getCoopState().equals(CoopState.INBUSINESS)){
				cooperativesToKeep.add(cooperative);
				coopsInBusiness++;
			}
			
			if(cooperative.getCoopState().equals(CoopState.BANKRUPT)){
				coopsBankrupt++;
			}
		}
		
		// get the number of new coops
		int numberOfNewCoopertives = coopsBankrupt;
		
		if(this.pConfig.getUseAdaptiveNumberOfCooperatives()){
			// adapt the number of cooperatives
//			if((double) nonBankruptCooperatives.size() / (double) this.cooperatives.size() < this.pConfig.getShareOfCooperativesWithProfit()){
//				// too few with profit, decrease number of new cooperatives by one
//				numberOfNewCoopertives--;
//			} else {
//				// too many with profit, there should be some market niche left, increase number of new cooperatives by one
//				numberOfNewCoopertives++;
//			}
			
			// calculate the exact number necessary
			numberOfNewCoopertives = (int) (coopsInBusiness * (1.0/this.pConfig.getShareOfCooperativesWithProfit() - 1.0) + 0.0000000000001) - coopsProspecting;
		}
		
		// delete bankrupt ones
		this.cooperatives = cooperativesToKeep;
		
		if (this.pConfig.getDisableCreationOfNewCooperativesInIteration() > iteration) {
			// recreate all other
			LinkedList<Cooperative> newCoops1 = this.operatorInitializer.createAdditionalCooperatives(this.strategyManager, iteration, numberOfNewCoopertives);
			this.cooperatives.addAll(newCoops1);
			
			// too few cooperatives in play, increase to the minimum specified in the config
			LinkedList<Cooperative> newCoops2 = this.operatorInitializer.createAdditionalCooperatives(this.strategyManager, iteration, (this.pConfig.getNumberOfCooperatives() - this.cooperatives.size()));
			this.cooperatives.addAll(newCoops2);
			
			// all coops are in business, increase by one to ensure minimal mutation
			if (this.cooperatives.size() == coopsInBusiness) {
				LinkedList<Cooperative> newCoops3 = this.operatorInitializer.createAdditionalCooperatives(this.strategyManager, iteration, 1);
				this.cooperatives.addAll(newCoops3);
			}
		}
	}

	public TransitSchedule getpTransitSchedule() {
		return this.pTransitSchedule;
	}

	public List<Cooperative> getCooperatives() {
		return cooperatives;
	}

	private void writeScheduleToFile(TransitSchedule schedule, String iterationFilename) {
		TransitScheduleWriterV1 writer = new TransitScheduleWriterV1(schedule);
		writer.write(iterationFilename);		
	}
}
