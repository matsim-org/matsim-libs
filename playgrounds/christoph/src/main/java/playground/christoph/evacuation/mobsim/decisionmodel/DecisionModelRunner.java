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
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;

import playground.christoph.evacuation.config.EvacuationConfig;
import playground.christoph.evacuation.mobsim.decisiondata.DecisionDataProvider;

public class DecisionModelRunner implements MobsimInitializedListener, AfterMobsimListener {

	private final Scenario scenario;
	private final DecisionDataProvider decisionDataProvider;
	
	private PanicModel panicModel;
	private PickupModel pickupModel;
	private EvacuationDecisionModel evacuationDecisionModel;
	
	public DecisionModelRunner(Scenario scenario, DecisionDataProvider decisionDataProvider) {
		this.scenario = scenario;
		this.decisionDataProvider = decisionDataProvider;
		
		panicModel = new PanicModel(this.decisionDataProvider, EvacuationConfig.panicShare);
		pickupModel = new PickupModel(this.decisionDataProvider);
		evacuationDecisionModel = new EvacuationDecisionModel(this.scenario, MatsimRandom.getLocalInstance(), this.decisionDataProvider);
	}
	
	public EvacuationDecisionModel getEvacuationDecisionModel() {
		return this.evacuationDecisionModel;
	}
	
	@Override
	public void notifyMobsimInitialized(MobsimInitializedEvent e) {
		
		/*
		 * Run panic model which decides which agents act rational and which panic.
		 */
		panicModel.runModel(this.scenario.getPopulation());
		
		/*
		 * Run pickup model which decides which agents would pick-up agents and which would not. 
		 */
		pickupModel.runModel(this.scenario.getPopulation());
		
		/*
		 * EvacuationDecisionModel has to be run during the simulation when a household
		 * is informed.
		 */
	}

	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		
		panicModel.printStatistics();
		pickupModel.printStatistics();
		evacuationDecisionModel.printStatistics();
		
		String panicModelFile = event.getControler().getControlerIO().getIterationFilename(event.getIteration(), PanicModel.panicModelFile);
		panicModel.writeDecisionsToFile(panicModelFile);
		
		String pickupModelFile = event.getControler().getControlerIO().getIterationFilename(event.getIteration(), PickupModel.pickupModelFile);
		pickupModel.writeDecisionsToFile(pickupModelFile);
		
		String evacuationDecisionModelFile = event.getControler().getControlerIO().getIterationFilename(event.getIteration(), 
				EvacuationDecisionModel.evacuationDecisionModelFile);
		evacuationDecisionModel.writeDecisionsToFile(evacuationDecisionModelFile);
	}
	
}
