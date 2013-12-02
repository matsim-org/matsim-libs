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
package playground.vsp.buildingEnergy.energyCalculation;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;

import playground.vsp.analysis.modules.simpleTripAnalyzer.SimpleTripAnalyzerModule;
import playground.vsp.buildingEnergy.energyCalculation.BuildingEnergyActivityProbabilityCalculator.ActivityProbabilities;
import playground.vsp.buildingEnergy.energyCalculation.BuildingEnergyAggregatedEnergyConsumptionCalculator.EnergyConsumption;
import playground.vsp.buildingEnergy.energyCalculation.BuildingEnergyConsumptionRule.BuildingEnergyConsumptionRuleFactory;
import playground.vsp.buildingEnergy.energyCalculation.BuildingEnergyMATSimDataReader.LinkOccupancyStats;
import playground.vsp.buildingEnergy.energyCalculation.BuildingEnergyMATSimDataReader.PopulationStats;

/**
 * Analyzes the base-run and all other runs for the agents energy consumption.
 * The class assumes they all use the same network!
 * Only agents that perform both, home and work-activities, will be analyzed. 
 * 
 * @author droeder
 *
 */
public class BuildingEnergyAnalyzer {


	private static final Logger log = Logger
			.getLogger(BuildingEnergyAnalyzer.class);
	/*package*/ static final String all = "--complete--";
	private List<String> runIds;
	private String baseRunId;
	private int tMax;
	private int td;
	private String outputPath;
	private String inputPath;
	private String workType;
	private String homeType;
	private Map<String, Map<String, LinkOccupancyStats>> run2type2RawOccupancy;
	private Map<String, PopulationStats> run2PopulationStats;
	private List<Id> links;
	private List<Integer> timeBins;
	private HashSet<String> actTypes;
	private HashMap<String, Map<Id, Integer>> maxPerLink;
	private BuildingEnergyConsumptionRuleFactory rules;
	private Map<String, EnergyConsumption> energyConsumption;

	private ActivityProbabilities probabilities;
	
	/*package*/ static boolean berlin = false;

	public BuildingEnergyAnalyzer(String inputPath, 
									String outputPath, 
									int td, 
									int tmax, 
									String baseRun, 
									List<String> runs, 
									final String homeType, 
									final String workType,
									BuildingEnergyConsumptionRuleFactory consumptionRuleFactory)
									{
		this.inputPath = inputPath;
		this.outputPath = outputPath;
		if(!new File(outputPath).exists()){
			throw new IllegalArgumentException(outputPath + " does not exist.");
		}
		this.td = td;
		this.tMax = tmax;
		this.baseRunId = baseRun;
		this.runIds = runs;
		this.homeType = homeType;
		this.workType = workType;
		this.actTypes = new HashSet<String>(){{
			// TODO[dr] make this configurable
			add(homeType);
			add(workType);
		}};
		this.rules = consumptionRuleFactory;
		log.warn("you may define more activity-types than home and work. Note, the analysis of probabilities and " +
				"populationstatistics will be based only on home- and work-activities.");
	}
	
	/**
	 * @param outputPath 
	 * 
	 */
	public void run() {
		if(berlin){
			log.warn("Plans and events will be modified. All ``not specified'' activities and events will be replaced " +
					"with " + homeType + "-activities. This will work for Berlin-Scenario only! Make sure this is what you want!");
		}
		initTimeBins();
		runRawAnalysis();
		writeRawData();
		createAggregatedAnalysis();
		writeAggregatedData();
	}

	/**
	 * 
	 */
	private void initTimeBins() {
		this.timeBins = new ArrayList<Integer>();
		for(int i = 0; i < tMax ; i += td){
			timeBins.add(i);
		}
	}
	
	/**
	 * 
	 */
	private void runRawAnalysis() {
		log.info("process raw-data from runs.");
		// TODO[dr] make iterations configurable
		log.warn("iteration is currently hard coded...");
		this.run2type2RawOccupancy = new HashMap<String, Map<String, LinkOccupancyStats>>();
		this.run2PopulationStats =  new HashMap<String, BuildingEnergyMATSimDataReader.PopulationStats>();
		analyseSingleRunRaw(baseRunId, 1000);
		for(String id : runIds){
			analyseSingleRunRaw(id, 300);
		}
		log.info("finished (processing raw-data from runs).");
	}

	/**
	 * @param runId
	 * @return
	 */
	private void analyseSingleRunRaw(String runId, int iter) {
		log.info("running raw analysis for run " + runId + ", iteration " + iter + ".");
		String plansFile = getPlansFileName(runId, iter);
		String networkFile = getNetworkFileName(runId);
		String eventsFile = getEventsFileName(runId, iter);
		BuildingEnergyMATSimDataReader reader = new BuildingEnergyMATSimDataReader(timeBins, iter, tMax, actTypes);
		reader.run(networkFile, plansFile, eventsFile, homeType, workType, runId);
		
		run2type2RawOccupancy.put(runId, reader.getLinkActivityStats());
		run2PopulationStats.put(runId, reader.getPStats());
		if(this.links == null){
			this.links = reader.getLinkIds();
		}
		log.info("writing trip-Data for agents of interest.");
		reader.getTripsAnalysis().postProcessData();
		reader.getTripsAnalysis().writeResults(outputPath);
		log.info("finished (writing trip-Data for agents of interest).");
		log.info("finished (running raw analysis for run " + runId + ", iteration " + iter + ").");
	}

	
	/**
	 */
	private void writeRawData() {
		new BuildingEnergyRawDataWriter(baseRunId, run2type2RawOccupancy, 
				run2PopulationStats, timeBins, links).write(outputPath);
	}


//
	/**
	 * 
	 */
	private void createAggregatedAnalysis() {
		this.maxPerLink = new HashMap<String, Map<Id, Integer>>();
		for(String s: actTypes){
			maxPerLink.put(s, findGlobalMaxPerLink(s));
		}
		BuildingEnergyAggregatedEnergyConsumptionCalculator calculator =  new BuildingEnergyAggregatedEnergyConsumptionCalculator(rules, links, timeBins, maxPerLink);
		this.energyConsumption = calculator.run(run2type2RawOccupancy);
		this.probabilities = new BuildingEnergyActivityProbabilityCalculator(links).run(run2type2RawOccupancy, run2PopulationStats);
	}


	/**
	 * @return
	 */
	private Map<Id, Integer> findGlobalMaxPerLink(String type) {
		Map<Id, Integer> map = new HashMap<Id, Integer>();
		for(Id link: links){
			Integer temp = 0;
			for(Map<String, LinkOccupancyStats> rra: run2type2RawOccupancy.values()){
				temp = Math.max(temp, rra.get(type).getStats().get(all).getMaximumOccupancy(link));
			}
			map.put(link, temp);
		}
		return map;
	}
	
	private void writeAggregatedData(){
		new BuildingEnergyAggregatedEnergyDataWriter().write(outputPath, energyConsumption, timeBins);
		new BuildingEnergyActivityProbabilityDataWriter().write(outputPath, probabilities, timeBins);
	}
	
	
	// ##################### helper methods ##############################
	/**
	 * @param runId
	 * @return
	 */
	private String getEventsFileName(String runId, int iter) {
		return new String(inputPath + runId + System.getProperty("file.separator") + "ITERS" + System.getProperty("file.separator") +
				"it." + String.valueOf(iter) + System.getProperty("file.separator") +  runId + "." + String.valueOf(iter) + ".events.xml.gz");
	}

	/**
	 * @param runId
	 * @return
	 */
	private String getNetworkFileName(String runId) {
		return new String(inputPath + runId + System.getProperty("file.separator") + runId + ".output_network.xml.gz");
	}

	/**
	 * @param runId
	 * @return
	 */
	private String getPlansFileName(String runId, int iter) {
		return new String(inputPath + runId + System.getProperty("file.separator") + "ITERS" + System.getProperty("file.separator") +
				"it." + String.valueOf(iter) + System.getProperty("file.separator") +  runId + "." + String.valueOf(iter) + ".plans.xml.gz");
	}

	/**
	 * @param isBerlin
	 */
	public void setBerlin(Boolean isBerlin) {
		berlin = isBerlin;
	}
}

