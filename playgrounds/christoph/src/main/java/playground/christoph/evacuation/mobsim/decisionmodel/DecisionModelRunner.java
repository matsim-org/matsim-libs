/* *********************************************************************** *
 * project: org.matsim.*
 * DecisionModelRunner.java
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

package playground.christoph.evacuation.mobsim.decisionmodel;

import java.util.Random;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.households.Household;

import playground.christoph.evacuation.config.EvacuationConfig;
import playground.christoph.evacuation.mobsim.InformedHouseholdsTracker;
import playground.christoph.evacuation.mobsim.decisiondata.DecisionDataGrabber;
import playground.christoph.evacuation.mobsim.decisiondata.DecisionDataProvider;

/**
 * 
 * Is responsible for running the decision models. Depending on its type, a model can
 * be run when the mobsim has been initialized or when a household has been informed. 
 * 
 * @author cdobler
 */
public class DecisionModelRunner implements BeforeMobsimListener, MobsimBeforeSimStepListener, AfterMobsimListener {

	private final Scenario scenario;
	private final DecisionDataGrabber decisionDataGrabber;
	private final InformedHouseholdsTracker informedHouseholdsTracker;
	
	private final DecisionDataProvider decisionDataProvider;
	private final PanicModel panicModel;
	private final PickupModel pickupModel;
	private final EvacuationDecisionModel evacuationDecisionModel;
	private final LatestAcceptedLeaveTimeModel latestAcceptedLeaveTimeModel;
	private final DepartureDelayModel departureDelayModel;
	
	public DecisionModelRunner(Scenario scenario, DecisionDataGrabber decisionDataGrabber,
			InformedHouseholdsTracker informedHouseholdsTracker) {
		this.scenario = scenario;
		this.decisionDataGrabber = decisionDataGrabber;
		this.informedHouseholdsTracker = informedHouseholdsTracker;
		
		this.decisionDataProvider = new DecisionDataProvider();
		this.panicModel = new PanicModel(this.decisionDataProvider, EvacuationConfig.panicShare);
		this.pickupModel = new PickupModel(this.decisionDataProvider);
		this.evacuationDecisionModel = new EvacuationDecisionModel(this.scenario, 
				new Random(EvacuationConfig.evacuationDecisionRandomSeed + EvacuationConfig.deterministicRNGOffset), this.decisionDataProvider);
		this.latestAcceptedLeaveTimeModel = new LatestAcceptedLeaveTimeModel(this.decisionDataProvider,
				EvacuationConfig.latestAcceptedLeaveTimeRandomSeed + EvacuationConfig.deterministicRNGOffset);
		this.departureDelayModel = new DepartureDelayModel(decisionDataProvider, 600.0, 
				EvacuationConfig.departureDelayRandomSeed + EvacuationConfig.deterministicRNGOffset);
	}
	
	public DecisionDataProvider getDecisionDataProvider() {
		return this.decisionDataProvider;
	}
	

	@Override
	public void notifyBeforeMobsim(BeforeMobsimEvent event) {
		
		/*
		 * Grab decision data.
		 */
		this.decisionDataGrabber.grabDecisionData(this.decisionDataProvider);
		
		/*
		 * Run panic model which decides which agents act rational and which panic.
		 */
		panicModel.runModel(this.scenario.getPopulation());
		
		/*
		 * Run pickup model which decides which agents would pick-up agents and which would not. 
		 */
		pickupModel.runModel(this.scenario.getPopulation());
		
		/*
		 * EvacuationDecisionModel and LatestAcceptedLeaveTimeModel have to be run during the 
		 * simulation when a household is informed.
		 * 
		 * DepartureDelayModel could be run here, but might be extended and then could only be
		 * run during the simulation.
		 */
	}
	
	public void notifyMobsimBeforeSimStep(MobsimBeforeSimStepEvent e) {
		
		for (Id householdId : this.informedHouseholdsTracker.getHouseholdsInformedInLastTimeStep()) {
			
			Household household = ((ScenarioImpl) scenario).getHouseholds().getHouseholds().get(householdId);
			
			this.evacuationDecisionModel.runModel(household);
			this.latestAcceptedLeaveTimeModel.runModel(household);
			
			// this might be run even later - e.g. when the household has met
			this.departureDelayModel.runModel(household);
		}
	}
	
	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		
		this.panicModel.printStatistics();
		this.pickupModel.printStatistics();
		this.evacuationDecisionModel.printStatistics();
		this.latestAcceptedLeaveTimeModel.printStatistics();
		this.departureDelayModel.printStatistics();
		
		String panicModelFile = event.getServices().getControlerIO().getIterationFilename(event.getIteration(), PanicModel.panicModelFile);
		this.panicModel.writeDecisionsToFile(panicModelFile);
		
		String pickupModelFile = event.getServices().getControlerIO().getIterationFilename(event.getIteration(), PickupModel.pickupModelFile);
		this.pickupModel.writeDecisionsToFile(pickupModelFile);
		
		String evacuationDecisionModelFile = event.getServices().getControlerIO().getIterationFilename(event.getIteration(),
				EvacuationDecisionModel.evacuationDecisionModelFile);
		this.evacuationDecisionModel.writeDecisionsToFile(evacuationDecisionModelFile);
		
		String latestAcceptedLeaveTimeModelFile = event.getServices().getControlerIO().getIterationFilename(event.getIteration(),
				LatestAcceptedLeaveTimeModel.latestAcceptedLeaveTimeFile);
		this.latestAcceptedLeaveTimeModel.writeDecisionsToFile(latestAcceptedLeaveTimeModelFile);
		
		String departureDelayModelFile = event.getServices().getControlerIO().getIterationFilename(event.getIteration(),
				DepartureDelayModel.departureDelayModelFile);
		this.departureDelayModel.writeDecisionsToFile(departureDelayModelFile);
	}
	
}
