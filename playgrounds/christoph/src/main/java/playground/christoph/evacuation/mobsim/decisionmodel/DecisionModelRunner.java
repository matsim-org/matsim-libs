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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;
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
public class DecisionModelRunner implements MobsimInitializedListener, MobsimBeforeSimStepListener, AfterMobsimListener {

	private final Scenario scenario;
	private final DecisionDataGrabber decisionDataGrabber;
	private final InformedHouseholdsTracker informedHouseholdsTracker;
	
	private final DecisionDataProvider decisionDataProvider;
	private final PanicModel panicModel;
	private final PickupModel pickupModel;
	private final EvacuationDecisionModel evacuationDecisionModel;
	private final LatestAcceptedLeaveTimeModel latestAcceptedLeaveTimeModel;
	
	public DecisionModelRunner(Scenario scenario, DecisionDataGrabber decisionDataGrabber,
			InformedHouseholdsTracker informedHouseholdsTracker) {
		this.scenario = scenario;
		this.decisionDataGrabber = decisionDataGrabber;
		this.informedHouseholdsTracker = informedHouseholdsTracker;
		
		this.decisionDataProvider = new DecisionDataProvider();
		this.panicModel = new PanicModel(this.decisionDataProvider, EvacuationConfig.panicShare);
		this.pickupModel = new PickupModel(this.decisionDataProvider);
		this.evacuationDecisionModel = new EvacuationDecisionModel(this.scenario, MatsimRandom.getLocalInstance(), this.decisionDataProvider);
		this.latestAcceptedLeaveTimeModel = new LatestAcceptedLeaveTimeModel(this.decisionDataProvider);
	}
	
	public DecisionDataProvider getDecisionDataProvider() {
		return this.decisionDataProvider;
	}
	
	/*
	 * This cannot be implemented as BeforeMobsimListener since the
	 * DecisionDataGrabber need information from a HouseholdsTracker
	 * which cannot be collected before the Mobsim has been initialized.
	 */
	@Override
	public void notifyMobsimInitialized(MobsimInitializedEvent e) {
		
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
		 */
	}
	
	public void notifyMobsimBeforeSimStep(MobsimBeforeSimStepEvent e) {
		
		for (Id householdId : this.informedHouseholdsTracker.getHouseholdsInformedInLastTimeStep()) {
			
			Household household = ((ScenarioImpl) scenario).getHouseholds().getHouseholds().get(householdId);
			
			this.evacuationDecisionModel.runModel(household);
			this.latestAcceptedLeaveTimeModel.runModel(household);
		}

	}
	
	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		
		panicModel.printStatistics();
		pickupModel.printStatistics();
		evacuationDecisionModel.printStatistics();
		latestAcceptedLeaveTimeModel.printStatistics();
		
		String panicModelFile = event.getControler().getControlerIO().getIterationFilename(event.getIteration(), PanicModel.panicModelFile);
		panicModel.writeDecisionsToFile(panicModelFile);
		
		String pickupModelFile = event.getControler().getControlerIO().getIterationFilename(event.getIteration(), PickupModel.pickupModelFile);
		pickupModel.writeDecisionsToFile(pickupModelFile);
		
		String evacuationDecisionModelFile = event.getControler().getControlerIO().getIterationFilename(event.getIteration(), 
				EvacuationDecisionModel.evacuationDecisionModelFile);
		evacuationDecisionModel.writeDecisionsToFile(evacuationDecisionModelFile);
		
		String latestAcceptedLeaveTimeModelFile = event.getControler().getControlerIO().getIterationFilename(event.getIteration(), 
				LatestAcceptedLeaveTimeModel.latestAcceptedLeaveTimeFile);
		latestAcceptedLeaveTimeModel.writeDecisionsToFile(latestAcceptedLeaveTimeModelFile);
	}
	
}
