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

import java.io.BufferedWriter;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.households.Household;
import org.matsim.households.Households;

import playground.christoph.evacuation.config.EvacuationConfig;
import playground.christoph.evacuation.mobsim.decisiondata.DecisionDataProvider;
import playground.christoph.evacuation.mobsim.decisiondata.HouseholdDecisionData;
import playground.christoph.evacuation.mobsim.decisionmodel.EvacuationDecisionModel.EvacuationDecision;
import playground.christoph.evacuation.utils.DeterministicRNG;

/**
 * Calculates the latest time that a household accepts to leave the affected area.
 * The value is influenced by the household's evacuation decision (immediately, 
 * later, never). Therefore, the model cannot be run before a household has decided
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
	private final DeterministicRNG rng;
	
	public LatestAcceptedLeaveTimeModel(DecisionDataProvider decisionDataProvider, long rngInitialValue) {
		this.decisionDataProvider = decisionDataProvider;
		this.rng = new DeterministicRNG(rngInitialValue);
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
//			double rand = 0.75 + (this.random.nextDouble() / 4);
//			double dt = Math.round(EvacuationConfig.evacuationDelayTime * rand);
			
			/*
			 * Calculate time value between 0.5 and 1.0 * EvacuationConfig.evacuationDelayTime.
			 * The returned values are based on a Rayleigh distribution.
			 */
			double randomTime = calculateDepartureDelay(household.getId(), EvacuationConfig.evacuationDelayTime / 2.0);
			double dt = Math.floor(EvacuationConfig.evacuationDelayTime / 2.0 + randomTime); 
			
			hdd.setLatestAcceptedLeaveTime(EvacuationConfig.evacuationTime + dt);			
		} else if (evacuationDecision == EvacuationDecision.NEVER) {
			hdd.setLatestAcceptedLeaveTime(EvacuationConfig.evacuationTime);
		} else {
			throw new RuntimeException("Household's EvacuationDecision is undefined " + evacuationDecision);
		}
		
		// old behavioral model
//		hdd.setLatestAcceptedLeaveTime(EvacuationConfig.evacuationTime + 2*3600);
	}

	
	/*
	 * By Using a sigma of 950, f(3600s) returns ~ 0,9992, which we round to 1.0
	 * We scale the time accordingly to the time window available, e.g.
	 * departure time window = 4h; 
	 * -> f returns 45 min for 1 hour reference;
	 * -> departure delay = 4 * 45 min = 3h
	 */
	private final double sigma = 950;

	private double calculateDepartureDelay(Id householdId, double tMax) {
		
		double rand = this.rng.idToRandomDouble(householdId);
		
		if (rand == 0.0) return 0.0;
		
		double value = Math.floor(Math.sqrt(-2 * sigma*sigma * Math.log(1 - rand)));
		if (value > 3600.0) value = 3600.0;
		
		double scaleFactor = tMax / 3600.0;
		return value * scaleFactor;
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
		
		try {
			BufferedWriter modelWriter = IOUtils.getBufferedWriter(file);
			
			writeHeader(modelWriter);
			writeRows(modelWriter);
			
			modelWriter.flush();
			modelWriter.close();			
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void writeHeader(BufferedWriter modelWriter) throws IOException {
		modelWriter.write("householdId");
		modelWriter.write(delimiter);
		modelWriter.write("latest accepted leave time");
		modelWriter.write(newLine);
	}
	
	private void writeRows(BufferedWriter modelWriter) throws IOException {
		for (HouseholdDecisionData hdd : this.decisionDataProvider.getHouseholdDecisionData()) {
			modelWriter.write(hdd.getHouseholdId().toString());
			modelWriter.write(delimiter);
			modelWriter.write(String.valueOf(hdd.getLatestAcceptedLeaveTime()));
			modelWriter.write(newLine);
		}
	}

	@Override
	public void readDecisionsFromFile(String file) {
		// So far, we do not read this model's results from a file.
	}

}
