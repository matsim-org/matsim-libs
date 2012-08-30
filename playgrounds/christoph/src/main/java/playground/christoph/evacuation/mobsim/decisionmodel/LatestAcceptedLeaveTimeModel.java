/* *********************************************************************** *
 * project: org.matsim.*
 * LatestAcceptedLeaveTimeModel.java
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

import org.apache.log4j.Logger;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.households.Household;
import org.matsim.households.Households;

import playground.christoph.evacuation.config.EvacuationConfig;
import playground.christoph.evacuation.mobsim.decisiondata.DecisionDataProvider;
import playground.christoph.evacuation.mobsim.decisiondata.HouseholdDecisionData;
import playground.christoph.evacuation.mobsim.decisionmodel.EvacuationDecisionModel.EvacuationDecision;

/**
 * Calculates the latest time that a household accepts to leave the affected area.
 * The value is influenced by the household's evacuation decision (immediately, 
 * later, never). Therefore, the model cannot be run before a household has deciced
 * whether to evacuate or not.
 * 
 * Note: this is NOT the time when the household wants to start its evacuation trip
 * from its home facility!
 * 
 * @author cdobler
 */
public class LatestAcceptedLeaveTimeModel implements HouseholdDecisionModel {

	static final Logger log = Logger.getLogger(LatestAcceptedLeaveTimeModel.class);
	
	public static final String latestAcceptedLeaveTimeFile = "latestAcceptedLeaveTimeModel.txt.gz";
	
	private final DecisionDataProvider decisionDataProvider;
	private final Random random;
	
	public LatestAcceptedLeaveTimeModel(DecisionDataProvider decisionDataProvider) {
		this.decisionDataProvider = decisionDataProvider;
		this.random = MatsimRandom.getLocalInstance();
	}
	
	/*
	 * The household defines its latest accepted evacuation time based on its evacuation decision:
	 * 	- evacuate immediately: latest accepted time = incident time
	 * 	- evacuate later: latest accepted time = incident time + random(0.75 .. 1.0) * delay time
	 * 	- evacuate never: latest accepted time = incident time (households meets at home)
	 */
	@Override
	public void runModel(Household household) {
		HouseholdDecisionData hdd = this.decisionDataProvider.getHouseholdDecisionData(household.getId());
		EvacuationDecision evacuationDecision = hdd.getEvacuationDecision();

		if (evacuationDecision == EvacuationDecision.IMMEDIATELY) {
			hdd.setLatestAcceptedLeaveTime(EvacuationConfig.evacuationTime);
		}
		else if (evacuationDecision == EvacuationDecision.LATER) {
			
			// calculate random double value between 0.75 and 1.0
			double rand = 0.75 + (this.random.nextDouble() / 4);
			
			double dt = Math.round(EvacuationConfig.evacuationDelayTime * rand);
			hdd.setLatestAcceptedLeaveTime(EvacuationConfig.evacuationTime + dt);			
		} else if (evacuationDecision == EvacuationDecision.NEVER) {
			hdd.setLatestAcceptedLeaveTime(EvacuationConfig.evacuationTime);
		} else {
			throw new RuntimeException("Household's EvacuationDecision is undefined " + evacuationDecision);
		}
		
		// old behavioral model
//		hdd.setLatestAcceptedLeaveTime(EvacuationConfig.evacuationTime + 2*3600);
	}

	@Override
	public void runModel(Households households) {
		for (Household household : households.getHouseholds().values()) runModel(household);
	}
	
	@Override
	public void printStatistics() {
		// So far, we do not calculate statistics for this model.
	}

	@Override
	public void writeDecisionsToFile(String file) {
		// So far, we do not write this model's results to a file.
	}

	@Override
	public void readDecisionsFromFile(String file) {
		// So far, we do not read this model's results from a file.
	}

}
