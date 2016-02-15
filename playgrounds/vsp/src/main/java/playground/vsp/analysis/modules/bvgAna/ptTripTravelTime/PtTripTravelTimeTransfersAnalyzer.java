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
package playground.vsp.analysis.modules.bvgAna.ptTripTravelTime;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.scenario.MutableScenario;

import playground.vsp.analysis.modules.AbstractAnalysisModule;
import playground.vsp.analysis.modules.ptDriverPrefix.PtDriverIdAnalyzer;

/**
 * This module analyzes pt trip travel times.
 * 
 * @author ikaddoura
 *
 */
public class PtTripTravelTimeTransfersAnalyzer extends AbstractAnalysisModule{
	private final static Logger log = Logger.getLogger(PtTripTravelTimeTransfersAnalyzer.class);
	private MutableScenario scenario;
	
	private List<AbstractAnalysisModule> anaModules = new LinkedList<AbstractAnalysisModule>();
	private PtDriverIdAnalyzer ptDriverIdAnalyzer;
	
	private PtTripTravelTimeEventHandler ptTtHandler;
	private Map<Id, List<Double>> personId2ptTripTravelTimes;
	private Map<Id, List<Integer>> personId2ptTripTransfers;
			
	public PtTripTravelTimeTransfersAnalyzer() {
		super(PtTripTravelTimeTransfersAnalyzer.class.getSimpleName());
	}
	
	public void init(MutableScenario scenario) {
		this.scenario = scenario;
		
		// (sub-)module
		this.ptDriverIdAnalyzer = new PtDriverIdAnalyzer();
		this.ptDriverIdAnalyzer.init(scenario);
		this.anaModules.add(ptDriverIdAnalyzer);
		
		this.ptTtHandler = new PtTripTravelTimeEventHandler(this.ptDriverIdAnalyzer);
		this.personId2ptTripTravelTimes = new HashMap<Id, List<Double>>();
		this.personId2ptTripTransfers = new HashMap<Id, List<Integer>>();
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
		allEventHandler.add(this.ptTtHandler);
		
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
		
		for(Id personId : this.ptTtHandler.getAgentId2PtTripTravelTimeData().keySet()) {
			List<PtTripTravelTimeData> ptTripDataThisPerson = this.ptTtHandler.getAgentId2PtTripTravelTimeData().get(personId);
			
			for(PtTripTravelTimeData ptTrip : ptTripDataThisPerson) {
				if(this.personId2ptTripTravelTimes.get(personId) == null){
					List<Double> travelTimes = new ArrayList<Double>();
					travelTimes.add(ptTrip.getTotalTripTravelTime());
					this.personId2ptTripTravelTimes.put(personId, travelTimes);
				} else {
					List<Double> travelTimes = this.personId2ptTripTravelTimes.get(personId);
					travelTimes.add(ptTrip.getTotalTripTravelTime());
				}
			}
						
			for(PtTripTravelTimeData ptTrip : ptTripDataThisPerson) {
				if(this.personId2ptTripTransfers.get(personId) == null){
					List<Integer> transfers = new ArrayList<Integer>();
					transfers.add(ptTrip.getNumberOfTransfers());
					this.personId2ptTripTransfers.put(personId, transfers);
				} else {
					List<Integer> travelTimes = this.personId2ptTripTransfers.get(personId);
					travelTimes.add(ptTrip.getNumberOfTransfers());
				}
			}
			
		}
		
	}

	@Override
	public void writeResults(String outputFolder) {
		String fileName1 = outputFolder + "ptTripTravelTimes.txt";
		File file1 = new File(fileName1);
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file1));
			bw.write("personId \t travel time per trip");
			bw.newLine();
			for (Id id : this.personId2ptTripTravelTimes.keySet()){
				bw.write(id.toString());
				for (Double tt : this.personId2ptTripTravelTimes.get(id)){
					bw.write("\t" +  tt.toString());
				}
				bw.newLine();
			}

			log.info("Output written to " + fileName1);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// -----
		
		String fileName2 = outputFolder + "ptTripTransfers.txt";
		File file2 = new File(fileName2);
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file2));
			bw.write("personId \t transfers per trip (-1 means there was no transfer because the whole trip was a transit_walk trip, right? ik");
			bw.newLine();
			for (Id id : this.personId2ptTripTransfers.keySet()){
				bw.write(id.toString());
				for (Integer tr : this.personId2ptTripTransfers.get(id)){
					bw.write("\t" +  tr.toString());
				}
				bw.newLine();
			}

			log.info("Output written to " + fileName2);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	
	}

	public Map<Id, List<Double>> getPersonId2ptTripTravelTimes() {
		return personId2ptTripTravelTimes;
	}

	public Map<Id, List<Integer>> getPersonId2ptTripTransfers() {
		return personId2ptTripTransfers;
	}
	
	

}
