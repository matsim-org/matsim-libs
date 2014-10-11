/* *********************************************************************** *
 * project: org.matsim.*
 * DepartureDelayModel.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.households.Household;
import org.matsim.households.Households;

import playground.christoph.evacuation.mobsim.decisiondata.DecisionDataProvider;
import playground.christoph.evacuation.mobsim.decisiondata.HouseholdDecisionData;
import playground.christoph.evacuation.utils.DeterministicRNG;

/**
 * TODO: use a function to estimate the departure time based on household characteristics and current time
 * 
 * So far use a Rayleigh Distribution with a sigma of 600. After 706s ~ 50% of all households have reached
 * their departure time.
 */
public class DepartureDelayModel implements HouseholdDecisionModel {
	
	private static final Logger log = Logger.getLogger(DepartureDelayModel.class);
	
	public static final String departureDelayModelFile = "departureDelayModel.txt.gz";
		
	private final DeterministicRNG rng;
	private final DecisionDataProvider decisionDataProvider;
	
	private int count = 0;
	private double sumDelays = 0.0;
	
	private double sigma;	// use as default 600
	private final double upperLimit = 0.999999;
	
	public DepartureDelayModel(DecisionDataProvider decisionDataProvider, double sigma, long rngInitialValue) {
		this.rng = new DeterministicRNG(rngInitialValue);
		this.decisionDataProvider = decisionDataProvider;
		this.sigma = sigma;
	}
	
	@Override
	public void runModel(Household household) {
				
		HouseholdDecisionData hdd = this.decisionDataProvider.getHouseholdDecisionData(household.getId()); 
		
		double departureTimeDelay = this.calculateDepartureDelay(household.getId());
		hdd.setDepartureTimeDelay(departureTimeDelay);
		
		this.count++;
		this.sumDelays += departureTimeDelay;
	}
	
	private double calculateDepartureDelay(Id<Household> householdId) {
		
		double rand = this.rng.idToRandomDouble(householdId);
		
		if (rand == 0.0) return 0.0;
		else if (rand > upperLimit) rand = upperLimit;
		
		return Math.floor(Math.sqrt(-2 * sigma*sigma * Math.log(1 - rand)));	
	}
	
	@Override
	public void runModel(Households households) {
		for (Household household : households.getHouseholds().values()) runModel(household);
	}
		
	@Override
	public void printStatistics() {
		log.info("departure time delays:");
		log.info("average\t" + this.sumDelays / this.count);
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
		modelWriter.write("departure time delay");
		modelWriter.write(newLine);
	}
	
	private void writeRows(BufferedWriter modelWriter) throws IOException {
		for (HouseholdDecisionData hdd : this.decisionDataProvider.getHouseholdDecisionData()) {
			modelWriter.write(hdd.getHouseholdId().toString());
			modelWriter.write(delimiter);
			modelWriter.write(String.valueOf(hdd.getDepartureTimeDelay()));
			modelWriter.write(newLine);
		}
	}

	@Override
	public void readDecisionsFromFile(String file) {
		try {
			BufferedReader modelReader = IOUtils.getBufferedReader(file);
			
			readHeader(modelReader);
			readRows(modelReader);
			
			modelReader.close();
			
		} catch (IOException e) {
			throw new RuntimeException(e);
		}	
	}
	
	private void readHeader(BufferedReader modelReader) throws IOException {
		// just skip header
		modelReader.readLine();
	}
	
	private void readRows(BufferedReader modelReader) throws IOException {
		
		String line = null;
		while ((line = modelReader.readLine()) != null) {
			String[] columns = line.split(delimiter);
			Id<Household> householdId = Id.create(columns[0], Household.class);
			HouseholdDecisionData hdd = this.decisionDataProvider.getHouseholdDecisionData(householdId);
			hdd.setDepartureTimeDelay(Double.valueOf(columns[1]));
		}
	}
}