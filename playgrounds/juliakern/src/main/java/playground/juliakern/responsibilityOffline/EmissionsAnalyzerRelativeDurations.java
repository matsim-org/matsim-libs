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
package playground.juliakern.responsibilityOffline;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.emissions.events.EmissionEventsReader;
import org.matsim.contrib.emissions.types.ColdPollutant;
import org.matsim.contrib.emissions.types.WarmPollutant;
import org.matsim.contrib.emissions.utils.EmissionUtils;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.vehicles.Vehicle;

import playground.vsp.analysis.modules.AbstractAnalysisModule;

/**
 * This module requires an emissions events file.
 * 
 * It then provides:
 * - total emissions per emission type
 * - warm emissions per person and emission type
 * - cold emissions per person and emission type
 * - sum of warm and cold emissions per person and emission type
 * 
 * @author ikaddoura, benjamin
 *
 */
public class EmissionsAnalyzerRelativeDurations extends AbstractAnalysisModule{
	private final static Logger log = Logger.getLogger(EmissionsAnalyzerRelativeDurations.class);
//	private ScenarioImpl scenario;
	private final String emissionEventsFile;
	private EmissionUtils emissionUtils;
	private EmissionCostPerPersonWarmEventHandlerRelativeDurations warmHandler;
	private EmissionCostPerPersonColdEventHandlerRelativeDurations coldHandler;
	private Map<Id<Vehicle>, Map<WarmPollutant, Double>> person2warmEmissions;
	private Map<Id<Vehicle>, Map<ColdPollutant, Double>> person2coldEmissions;
	private Map<Id<Vehicle>, SortedMap<String, Double>> person2totalEmissions;
	private SortedMap<String, Double> totalEmissions;
	private HashMap<Double, Double[][]> durations;
	private int noOfXCells = 160;
	private int noOfYCells = 120;
	private HashMap<Double, Double[][]> relativeDurationFactor;
	private Map<Id<Link>, Integer> links2xbins;
	private Map<Id<Link>, Integer> links2ybins;
	
	public EmissionsAnalyzerRelativeDurations(String emissionsEventsFile, HashMap<Double, Double[][]> durations, Map<Id<Link>, Integer> link2xbins, Map<Id<Link>, Integer> link2ybins) {
		super(EmissionsAnalyzerRelativeDurations.class.getSimpleName());
		this.durations = durations;
		this.emissionEventsFile = emissionsEventsFile;
		this.links2xbins = link2xbins;
		this.links2ybins = link2ybins;
		// calc relative factor
		this.relativeDurationFactor = calcRelativeDurations();
		
//		this.scenario = scenario;
		this.emissionUtils = new EmissionUtils();
		this.warmHandler = new EmissionCostPerPersonWarmEventHandlerRelativeDurations(relativeDurationFactor, links2xbins, links2ybins);
		this.coldHandler = new EmissionCostPerPersonColdEventHandlerRelativeDurations(relativeDurationFactor, links2xbins, links2ybins);
	
	}
	
	public void init(MutableScenario scenario) {
//		// calc relative factor
//		this.relativeDurationFactor = calcRelativeDurations();
//		
////		this.scenario = scenario;
//		this.emissionUtils = new EmissionUtils();
//		this.warmHandler = new EmissionCostPerPersonWarmEventHandlerRelativeDurations(relativeDurationFactor, links2xbins, links2ybins);
//		this.coldHandler = new EmissionCostPerPersonColdEventHandlerRelativeDurations(relativeDurationFactor, links2xbins, links2ybins);
	}
	


	@Override
	public List<EventHandler> getEventHandler() {
		List<EventHandler> handler = new LinkedList<EventHandler>();
		// the standard eventsFile is not read in this module
		return handler;
	}

	@Override
	public void preProcessData() {
		EventsManager eventsManager = EventsUtils.createEventsManager();
		EmissionEventsReader emissionReader = new EmissionEventsReader(eventsManager);
		
		eventsManager.addHandler(this.warmHandler);
		eventsManager.addHandler(this.coldHandler);
		
		emissionReader.parse(this.emissionEventsFile);
	}

	@Override
	public void postProcessData() {
		this.person2warmEmissions = this.warmHandler.getWarmEmissionCostsPerPerson();
		this.person2coldEmissions = this.coldHandler.getColdEmissionCostsPerPerson();
		this.person2totalEmissions = this.emissionUtils.sumUpEmissionsPerId(person2warmEmissions, person2coldEmissions);
		this.totalEmissions = this.emissionUtils.getTotalEmissions(this.person2totalEmissions);
	}

	// TODO: should probably also write out person2totalEmissions...
	@Override
	public void writeResults(String outputFolder) {
		String fileName = outputFolder + "emissions.txt";
		File file = new File(fileName);
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));

			for(String pollutant : emissionUtils.getListOfPollutants()){
				bw.write(pollutant + "\t");
			}
			bw.newLine();

			for(String pollutant : this.totalEmissions.keySet()){
				Double pollutantValue = this.totalEmissions.get(pollutant);
				bw.write(pollutantValue.toString() + "\t");
			}
			bw.newLine();
			
			bw.close();
			log.info("Finished writing output to " + fileName);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public SortedMap<String, Double> getTotalEmissions() {
		return totalEmissions;
	}

	public Map<Id<Vehicle>, Map<WarmPollutant, Double>> getPerson2warmEmissionCosts() {
		return person2warmEmissions;
	}

	public Map<Id<Vehicle>, Map<ColdPollutant, Double>> getPerson2coldEmissionCosts() {
		return person2coldEmissions;
	}

	public Map<Id<Vehicle>, SortedMap<String, Double>> getPerson2totalEmissionCosts() {
		return person2totalEmissions;
	}
	
	private HashMap<Double, Double[][]> calcRelativeDurations() {
		HashMap<Double, Double> timeBinsToAvgDensity = new HashMap<Double, Double>();
		
		//calc avg density for each time bin
		for(Double timeBin : durations.keySet()){
			Double avgDen=0.0;
			for(int i=0; i< durations.get(timeBin).length; i++){
				for(int j=0; j< durations.get(timeBin)[i].length; j++){
					avgDen+= durations.get(timeBin)[i][j];
//					System.out.println(durations.get(timeBin)[i][j]);
				}
			}
			timeBinsToAvgDensity.put(timeBin, avgDen/noOfXCells/noOfYCells);
		}
		
		HashMap<Double, Double[][]> timeBinsToRelativeDurations = new HashMap<Double, Double[][]>();
		
		// calc relative density for each cell in each time bin
		for(Double timeBin: durations.keySet()){
			timeBinsToRelativeDurations.put(timeBin, new Double[noOfXCells][noOfYCells]);
			for(int i=0; i< durations.get(timeBin).length; i++){
				for(int j=0; j< durations.get(timeBin)[i].length; j++){
					timeBinsToRelativeDurations.get(timeBin)[i][j]=durations.get(timeBin)[i][j]/timeBinsToAvgDensity.get(timeBin);
				}
			}
		}
		return timeBinsToRelativeDurations;
	}
}
