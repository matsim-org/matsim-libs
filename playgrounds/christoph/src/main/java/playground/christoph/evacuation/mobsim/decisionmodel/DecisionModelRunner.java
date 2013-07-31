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

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.gbl.MatsimRandom;

import playground.christoph.evacuation.config.EvacuationConfig;
import playground.christoph.evacuation.mobsim.decisiondata.DecisionDataGrabber;
import playground.christoph.evacuation.mobsim.decisiondata.DecisionDataProvider;

public class DecisionModelRunner implements BeforeMobsimListener, AfterMobsimListener {

	private final Scenario scenario;
	private final DecisionDataGrabber decisionDataGrabber;
	
	private final DecisionDataProvider decisionDataProvider;
	private final PanicModel panicModel;
	private final PickupModel pickupModel;
	private final EvacuationDecisionModel evacuationDecisionModel;
	private final LatestAcceptedLeaveTimeModel latestAcceptedLeaveTimeModel;
	
	public DecisionModelRunner(Scenario scenario, DecisionDataGrabber decisionDataGrabber) {
		this.scenario = scenario;
		this.decisionDataGrabber = decisionDataGrabber;
		
		this.decisionDataProvider = new DecisionDataProvider();
		this.panicModel = new PanicModel(this.decisionDataProvider, EvacuationConfig.panicShare);
		this.pickupModel = new PickupModel(this.decisionDataProvider);
		this.evacuationDecisionModel = new EvacuationDecisionModel(this.scenario, MatsimRandom.getLocalInstance(), this.decisionDataProvider);
		this.latestAcceptedLeaveTimeModel = new LatestAcceptedLeaveTimeModel(this.decisionDataProvider);
	}
	
	public EvacuationDecisionModel getEvacuationDecisionModel() {
		return this.evacuationDecisionModel;
	}
	
	public LatestAcceptedLeaveTimeModel getLatestAcceptedLeaveTimeModel() {
		return this.latestAcceptedLeaveTimeModel;
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
		 */
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
