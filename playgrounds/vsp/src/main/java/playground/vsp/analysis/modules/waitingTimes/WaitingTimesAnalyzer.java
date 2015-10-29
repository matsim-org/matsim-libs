/* *********************************************************************** *
 * project: org.matsim.*
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

/**
 * 
 * @author ikaddoura
 * 
 */
package playground.vsp.analysis.modules.waitingTimes;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.scenario.MutableScenario;

import playground.vsp.analysis.modules.AbstractAnalysisModule;
import playground.vsp.analysis.modules.ptDriverPrefix.PtDriverIdAnalyzer;

/**
 * This module analyzes the waiting times for public vehicles.
 * It calculates the sum of all waiting times, the avg. waiting time, 
 * the avg. waiting time for each transit stop, the avg. waiting time per transit stop,
 * the avg. waiting time for each pt user and the avg. waiting time per pt user.
 * 
 * @author ikaddoura
 *
 */
public class WaitingTimesAnalyzer extends AbstractAnalysisModule {
	private final static Logger log = Logger.getLogger(WaitingTimesAnalyzer.class);
	private MutableScenario scenario;
	
	private List<AbstractAnalysisModule> anaModules = new LinkedList<AbstractAnalysisModule>();
	private PtDriverIdAnalyzer ptDriverIdAnalyzer;
	
	private WaitingTimeHandler waitingTimeHandler;
	private double sumOfAllWaitingTimes;
	private double avgWaitingTime;
	private double avgWaitingTimePerPerson;
	private double avgWaitingTimePerStopFacility;
	private Map <Id, Double> personID2avgWaitingTime;
	private Map <Id, Double> stopFacilityID2avgWaitingTime;
	
	public WaitingTimesAnalyzer() {
		super(WaitingTimesAnalyzer.class.getSimpleName());
	}
	
	public void init(MutableScenario scenario) {
		this.scenario = scenario;
		
		// (sub-)module
		this.ptDriverIdAnalyzer = new PtDriverIdAnalyzer();
		this.ptDriverIdAnalyzer.init(scenario);
		this.anaModules.add(ptDriverIdAnalyzer);		
		
		this.waitingTimeHandler = new WaitingTimeHandler(this.ptDriverIdAnalyzer);
		this.personID2avgWaitingTime = new HashMap<Id, Double>();
		this.stopFacilityID2avgWaitingTime = new HashMap<Id, Double>();
	}
	
	@Override
	public List<EventHandler> getEventHandler() {
		List<EventHandler> allEventHandler = new LinkedList<EventHandler>();

		// from (sub-)modules
		for (AbstractAnalysisModule module : this.anaModules) {
			for (EventHandler handler : module.getEventHandler()) {
				allEventHandler.add(handler);
			}
		}
		
		// own handler
		allEventHandler.add(waitingTimeHandler);
		
		return allEventHandler;
	}

	@Override
	public void preProcessData() {
		log.info("Preprocessing all (sub-)modules...");
		for (AbstractAnalysisModule module : this.anaModules) {
			module.preProcessData();
		}
		log.info("Preprocessing all (sub-)modules... done.");
	}

	@Override
	public void postProcessData() {
		log.info("Postprocessing all (sub-)modules...");
		for (AbstractAnalysisModule module : this.anaModules) {
			module.postProcessData();
		}
		log.info("Postprocessing all (sub-)modules... done.");
		
		// own postProcessing
	
		List <Double> waitingTimes = this.waitingTimeHandler.getWaitingTimes();
		
		int waitCounter = 0;
		double waitTimeSum = 0.;
		for (Double waitTime : waitingTimes){
			waitCounter++;
			waitTimeSum = waitTimeSum + waitTime;
		}
		this.sumOfAllWaitingTimes = waitTimeSum;
		this.avgWaitingTime = (waitTimeSum / waitCounter);

		// --------------------------------------------------------------
		
		Map <Id, List<Double>> personId2waitingTimes = this.waitingTimeHandler.getPersonId2waitingTimes();
		for (Id personId : personId2waitingTimes.keySet()) {
			List<Double> waitingTimesThisPerson = personId2waitingTimes.get(personId);
			int counter = 0;
			double sum = 0.;
			for (Double waitTime : waitingTimesThisPerson){
				counter++;
				sum = sum + waitTime;
			}
			double avgWaitingTimeThisPerson = (sum / counter);
			this.personID2avgWaitingTime.put(personId, avgWaitingTimeThisPerson);
		}
		
		Map <Id, List<Double>> facilityId2waitingTimes = this.waitingTimeHandler.getFacilityId2waitingTimes();
		for (Id facilityId : facilityId2waitingTimes.keySet()) {
			List<Double> waitingTimesThisFacility = facilityId2waitingTimes.get(facilityId);
			int counter = 0;
			double sum = 0.;
			for (Double waitTime : waitingTimesThisFacility){
				counter++;
				sum = sum + waitTime;
			}
			double avgWaitingTimeThisFacility = (sum / counter);
			this.stopFacilityID2avgWaitingTime.put(facilityId, avgWaitingTimeThisFacility);
		}
		
		// ------------------------------------------------------------------
		
		int personCounter = 0;
		double sum1 = 0.;
		for (Id id : this.personID2avgWaitingTime.keySet()){
			personCounter++;
			sum1 = sum1 + this.personID2avgWaitingTime.get(id);
		}
		this.avgWaitingTimePerPerson = (sum1 / personCounter);
		
		int facilityCounter = 0;
		double sum2 = 0.;
		for (Id id : this.stopFacilityID2avgWaitingTime.keySet()){
			facilityCounter++;
			sum2 = sum2 + this.stopFacilityID2avgWaitingTime.get(id);
		}
		this.avgWaitingTimePerStopFacility = (sum2 / facilityCounter);
	}

	@Override
	public void writeResults(String outputFolder) {
		String fileName1 = outputFolder + "avgWaitingTimes.txt";
		File file1 = new File(fileName1);
				
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file1));
			bw.write("avg. waiting time [sec]: " + this.avgWaitingTime);
			bw.newLine();
			bw.write("avg. waiting time per pt user [sec]: " + this.avgWaitingTimePerPerson);
			bw.newLine();
			bw.write("avg. waiting time per stop facility [sec]: " + this.avgWaitingTimePerStopFacility);
			bw.newLine();
			bw.close();

			log.info("Output written to " + fileName1);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
		String fileName2 = outputFolder + "avgWaitingTimePerPerson.txt";
		File file2 = new File(fileName2);
				
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file2));
			bw.write("personId \t avg. waiting time per person [sec]");
			bw.newLine();
			for (Id id : this.personID2avgWaitingTime.keySet()) {
				bw.write(id + "\t" + this.personID2avgWaitingTime.get(id));
				bw.newLine();
			}
			
			bw.close();

			log.info("Output written to " + fileName2);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		String fileName3 = outputFolder + "avgWaitingTimePerStopFacility.txt";
		File file3 = new File(fileName3);
				
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file3));
			bw.write("stopFacilityId \t avg. waiting time per stop facility [sec]");
			bw.newLine();
			for (Id id : this.stopFacilityID2avgWaitingTime.keySet()) {
				bw.write(id + "\t" + this.stopFacilityID2avgWaitingTime.get(id));
				bw.newLine();
			}
			
			bw.close();

			log.info("Output written to " + fileName3);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public double getSumOfAllWaitingTimes() {
		return sumOfAllWaitingTimes;
	}

	public double getAvgWaitingTimePerPerson() {
		return avgWaitingTimePerPerson;
	}

	public double getAvgWaitingTimePerStopFacility() {
		return avgWaitingTimePerStopFacility;
	}

	public double getAvgWaitingTime() {
		return avgWaitingTime;
	}

	public Map<Id, Double> getPersonID2avgWaitingTime() {
		return personID2avgWaitingTime;
	}

	public Map<Id, Double> getStopFacilityID2avgWaitingTime() {
		return stopFacilityID2avgWaitingTime;
	}
	
	
	
}
