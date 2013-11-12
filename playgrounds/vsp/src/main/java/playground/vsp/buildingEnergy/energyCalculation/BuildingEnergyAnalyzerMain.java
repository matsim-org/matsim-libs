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
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.gbl.Gbl;

import playground.vsp.buildingEnergy.energyCalculation.BuildingEnergyConsumptionCalculator.EnergyConsumption;
import playground.vsp.buildingEnergy.energyCalculation.BuildingEnergyConsumptionRule.BuildingEnergyConsumptionRuleFactory;
import playground.vsp.buildingEnergy.energyCalculation.BuildingEnergyConsumptionRule.HomeEnergyConsumptionRuleImpl;
import playground.vsp.buildingEnergy.energyCalculation.BuildingEnergyConsumptionRule.OfficeEnergyConsumptionRuleImpl;
import playground.vsp.buildingEnergy.energyCalculation.BuildingEnergyDataReader.LinkOccupancyStats;
import playground.vsp.buildingEnergy.energyCalculation.BuildingEnergyDataReader.PopulationStats;

/**
 * Analyzes the base-run and all other runs for the agents energy consumption.
 * The class assumes they all use the same network!
 * 
 * @author droeder
 *
 */
class BuildingEnergyAnalyzerMain {
	private static final String[] ARGS = new String[]{
		"E:\\VSP\\svn\\shared-svn\\studies\\droeder\\buildingEnergy\\runs\\",
		"E:\\VSP\\svn\\shared-svn\\studies\\droeder\\buildingEnergy\\runs\\outputCaseStudies\\",
		"900",
		"86400",
		"2kW.15",
		"home",
		"work",
		"0.35625",
		"0.83125",
		"5.0",
		"0.366",
		"0.854",
		"2kW.s1"
	};

	private static final Logger log = Logger
			.getLogger(BuildingEnergyAnalyzerMain.class);
	static final String all = "--complete--";
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

	BuildingEnergyAnalyzerMain(String inputPath, 
									String outputPath, 
									int td, 
									int tmax, 
									String baseRun, 
									List<String> runs, 
									final String homeType, 
									final String workType,
									BuildingEnergyConsumptionRuleFactory consumptionRuleFactory)
//									final BuildingEnergyConsumptionRule calculatorWork,
//									final BuildingEnergyConsumptionRule calculatorHome) 
									{
		this.inputPath = inputPath;
		this.outputPath = outputPath;
		this.td = td;
		this.tMax = tmax;
		this.baseRunId = baseRun;
		this.runIds = runs;
		this.homeType = homeType;
		this.workType = workType;
		log.warn("check the analyzed activitytypes.");
		this.actTypes = new HashSet<String>(){{
			// TODO[dr] make this configurable
			add(homeType);
			add(workType);
			add(new String("not specified"));
		}};
		this.rules = consumptionRuleFactory;
	}
	
	/**
	 * @param outputPath 
	 * 
	 */
	private void run() {
		initTimeBins();
		runRawAnalysis();
		dumpRawData();
		createAggregatedAnalysis();
		writeEnergyConsumption();
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
		this.run2PopulationStats =  new HashMap<String, BuildingEnergyDataReader.PopulationStats>();
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
		BuildingEnergyDataReader reader = new BuildingEnergyDataReader(timeBins, iter, tMax, actTypes);
		reader.run(networkFile, plansFile, eventsFile, homeType, workType);
		run2type2RawOccupancy.put(runId, reader.getLinkActivityStats());
		run2PopulationStats.put(runId, reader.getPStats());
		if(this.links == null){
			this.links = reader.getLinkIds();
		}
		log.info("finished (running raw analysis for run " + runId + ", iteration " + iter + ").");
	}

	
	/**
	 */
	private void dumpRawData() {
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
		BuildingEnergyConsumptionCalculator calculator =  new BuildingEnergyConsumptionCalculator(rules, links, timeBins, maxPerLink);
		calculator.process(run2type2RawOccupancy);
		this.energyConsumption = calculator.getEnergyConsumptionPerRun();
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
	
	private void writeEnergyConsumption(){
		new BuidlingEnergyAggregatedDataWriter().write(outputPath, energyConsumption, timeBins);
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
	 * 
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		boolean time = Gbl.enableThreadCpuTimeMeasurement();
		Gbl.startMeasurement();
		if(args.length == 0){
			log.warn("using hard coded static variables. Make sure this what you want.");
			args = ARGS;
		}
		if(args.length < 12){
			throw new IllegalArgumentException("expecting min 12 arguments {inputpath, outputPath, timeSliceSize, tmax, " +
					"baseRunId, homeActivityType, workActivityType, P_bo, P_so, beta, P_bh, P_ah, runIds...}");
		}
		String inputPath = new File(args[0]).getAbsolutePath() + System.getProperty("file.separator");
		String outputPath = new File(args[1]).getAbsolutePath() + System.getProperty("file.separator");
		int td = Integer.parseInt(args[2]);
		int tmax = Integer.parseInt(args[3]);
		String baseRun = args[4];
		String homeType = args[5];
		String workType = args[6];
		Double pbo = Double.parseDouble(args[7]);
		Double pso = Double.parseDouble(args[8]);
		Double beta = Double.parseDouble(args[9]);
		Double pbh = Double.parseDouble(args[10]);
		Double pah = Double.parseDouble(args[11]);
		List<String> runs = new ArrayList<String>();
		for(int i = 12; i < args.length; i++){
			runs.add(args[i]);
		}
		//catch logEntries
		OutputDirectoryLogging.initLogging(new OutputDirectoryHierarchy(outputPath, BuildingEnergyAnalyzerMain.class.getSimpleName(), true, false));
		OutputDirectoryLogging.catchLogEntries();
		// dump input-parameters to log
		log.info("running class: " + System.getProperty("sun.java.command"));
		log.info("inputPath: " + inputPath);
		log.info("outputPath: " + outputPath);
		log.info("timeSliceDuration [s]\t: " + String.valueOf(td));
		log.info("tMax [s]\t\t\t: " + tmax);
		log.info("homeType\t\t\t: " + homeType);
		log.info("workType\t\t\t: " + workType);
		log.info("P_bo [kW]\t\t\t: " + pbo);
		log.info("P_so [kW]\t\t\t: " + pso);
		log.info("beta []\t\t\t: " + beta);
		log.info("P_bh [kW]\t\t\t: " + pbh);
		log.info("P_ah [kW]\t\t\t: " + pah);
		log.info("baseRun\t\t\t: " + baseRun);
		for(int i = 0; i < runs.size(); i++){
			log.info("caseStudy " + (i + 1) + "\t\t: " + runs.get(i));
		}
		BuildingEnergyConsumptionRule ecWork = new OfficeEnergyConsumptionRuleImpl(td, pbo, pso, beta);
		BuildingEnergyConsumptionRule ecHome = new HomeEnergyConsumptionRuleImpl(td, pbh, pah);
		BuildingEnergyConsumptionRule ecNotSpecified = new HomeEnergyConsumptionRuleImpl(td, 0, pah);
		BuildingEnergyConsumptionRuleFactory factory =  new BuildingEnergyConsumptionRuleFactory();
		factory.setRule(homeType, ecHome);
		factory.setRule(workType, ecWork);
		// seems ``not specified'' is the morning home-activity (for the berlin-scenario)
		factory.setRule(new String("not specified"), ecNotSpecified);
		// run
		new BuildingEnergyAnalyzerMain(inputPath, outputPath, td, tmax, baseRun, runs, homeType, workType, factory).run();
		if(time){
			Gbl.printCurrentThreadCpuTime();
		}
		Gbl.printElapsedTime();
		log.info("finished.");
	}


}

