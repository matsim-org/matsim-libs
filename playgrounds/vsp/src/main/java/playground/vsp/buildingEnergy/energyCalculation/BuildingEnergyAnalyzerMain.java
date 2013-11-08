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

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;

import playground.vsp.buildingEnergy.energyCalculation.EnergyConsumptionCalculator.OfficeEnergyConsumptionCalculatorImpl;
import playground.vsp.buildingEnergy.linkOccupancy.LinkActivityOccupancyCounter;

/**
 * analyzes the base-run and all other runs. Compares ``other'' runs against base run. 
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
		"2kW.s1"
	};

	private static final Logger log = Logger
			.getLogger(BuildingEnergyAnalyzerMain.class);
	private static final String all = "--all--";
	private List<String> runIds;
	private String baseRunId;
	private int tMax;
	private int td;
	private String outputPath;
	private String inputPath;
	private String workType;
	private String homeType;
	private RawRunAnalysisData baseRunRawAnalyzed;
	private Map<String, RawRunAnalysisData> runsRawAnalyzed;
	private Set<Id> links;
	private List<Integer> timeBins;
	private HashMap<String, Map<Integer, Double>> aggregated;
	private OfficeEnergyConsumptionCalculatorImpl ec;

	BuildingEnergyAnalyzerMain(String inputPath, 
									String outputPath, 
									int td, 
									int tmax, 
									String baseRun, 
									List<String> runs, 
									String homeType, 
									String workType,
									EnergyConsumptionCalculator calculator) {
		this.inputPath = inputPath;
		this.outputPath = outputPath;
		this.td = td;
		this.tMax = tmax;
		this.baseRunId = baseRun;
		this.runIds = runs;
		this.homeType = homeType;
		this.workType = workType;
		// TODO[dr] implement
		log.warn("check energy calculator settings. Currently it's hard coded...");
		this.ec = new OfficeEnergyConsumptionCalculatorImpl(td, 0.35625, 0.83125, 0.5);
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
		dumpAggregatedData();
	}


	/**
	 * @return
	 */
	private Map<Id, Integer> findOfficeSize() {
		Map<Id, Integer> map = new HashMap<Id, Integer>();
		for(Id link: links){
			int temp = baseRunRawAnalyzed.getWorkActs().get(all).getMaximumOccupancy(link);
			for(RawRunAnalysisData rra: runsRawAnalyzed.values()){
				temp = Math.max(temp, rra.getWorkActs().get(all).getMaximumOccupancy(link));
			}
			map.put(link, temp);
		}
		return map;
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
		// TODO[dr] implement
		log.warn("iteration is currently hard coded...");
		this.baseRunRawAnalyzed = analyseSingleRunRaw(baseRunId, 1000);
		this.runsRawAnalyzed = new HashMap<String, BuildingEnergyAnalyzerMain.RawRunAnalysisData>();
		Gbl.printMemoryUsage();
		for(String id : runIds){
			this.runsRawAnalyzed.put(id, analyseSingleRunRaw(id, 300));
			Gbl.printMemoryUsage();
		}
		log.info("finished (processing raw-data from runs).");
	}

	/**
	 * @param runId
	 * @return
	 */
	private RawRunAnalysisData analyseSingleRunRaw(String runId, int iter) {
		log.info("running raw analysis for run " + runId + ", iteration " + iter + ".");
		String plansFile = getPlansFileName(runId, iter);
		String networkFile = getNetworkFileName(runId);
		String eventsFile = getEventsFileName(runId, iter);
		//load and analyse scenario
		BuildingEnergyPlansAnalyzer plansAna = new BuildingEnergyPlansAnalyzer(homeType, workType);
		Scenario sc = prepareScenario(plansFile, networkFile, plansAna);
		if(links ==  null){
			links = sc.getNetwork().getLinks().keySet(); 
		}
		// load and analyse events
		Map<String, LinkActivityOccupancyCounter> home = initOccupancyCounter(homeType, sc.getPopulation());
		Map<String, LinkActivityOccupancyCounter> work = initOccupancyCounter(workType, sc.getPopulation());
		EventsManager manager = EventsUtils.createEventsManager();
		for(LinkActivityOccupancyCounter v: home.values()){
			manager.addHandler(v);
		}
		for(LinkActivityOccupancyCounter v: work.values()){
			manager.addHandler(v);
		}
		new MatsimEventsReader(manager).readFile(eventsFile);
		for(LinkActivityOccupancyCounter v: home.values()){
			v.finish();
		}
		for(LinkActivityOccupancyCounter v: work.values()){
			v.finish();
		}
		RawRunAnalysisData temp = new RawRunAnalysisData(runId, 
											plansAna.getHomeCnt(), 
											plansAna.getWorkCnt(), 
											plansAna.getHomeAndWorkCnt(),
											home, work, sc.getNetwork());
		sc = null;
		log.info("finished (running raw analysis for run " + runId + ", iteration " + iter + ").");
		return temp;
	}

	/**
	 * @param plansFile
	 * @param plansAna 
	 * @return
	 */
	private Scenario prepareScenario(String plansFile, String networkFile, BuildingEnergyPlansAnalyzer plansAna) {
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(sc).readFile(networkFile);
		new MatsimPopulationReader(sc).readFile(plansFile);
		((PopulationImpl) sc.getPopulation()).addAlgorithm(plansAna);
		((PopulationImpl) sc.getPopulation()).runAlgorithms();
		return sc;
	}
	
	/**
	 * @param string
	 * @param td
	 * @param tmax
	 * @return
	 */
	private Map<String, LinkActivityOccupancyCounter> initOccupancyCounter(
			String string, Population p) {
		Map<String, LinkActivityOccupancyCounter> map = new HashMap<String, LinkActivityOccupancyCounter>();
		for(int i : timeBins){
			map.put(String.valueOf(i), new LinkActivityOccupancyCounter(p, i, i + td , string));
		}
		map.put(all, new LinkActivityOccupancyCounter(p, 0, tMax , string));
		return map;
	}
	
	/**
	 */
	private void dumpRawData() {
		log.info("writing raw-data to + " + outputPath + ".");
		StringBuffer b = new StringBuffer();
		b.append(";personWithWorkActivity;personsWithHomeActivity;personWithHomeAndWorkActivity;\n");
		dumpSingleRunRawData(baseRunId, baseRunRawAnalyzed);
		b.append(baseRunId + ";" + baseRunRawAnalyzed.getWorkCnt() + ";" + baseRunRawAnalyzed.getHomeCnt() + ";" + baseRunRawAnalyzed.getHomeWorkCnt() + ";\n");
		for(String id : runIds){
			dumpSingleRunRawData(id, runsRawAnalyzed.get(id));
			b.append(id + ";" + runsRawAnalyzed.get(id).getWorkCnt() + ";" + runsRawAnalyzed.get(id).getHomeCnt() + ";" +  runsRawAnalyzed.get(id).getHomeWorkCnt() + ";\n");
		}
		BufferedWriter w =  IOUtils.getBufferedWriter(outputPath + "populationActivityStats.csv.gz");
		try {
			w.write(b.toString());
			w.flush();
			w.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		log.info("finished (writing raw-data to + " + outputPath + ").");
	}

	/**
	 * @param id
	 * @param rawRunAnalysis 
	 */
	private void dumpSingleRunRawData(String id, RawRunAnalysisData rawRunAnalysis) {
		writeOccupancy(id + ".home", rawRunAnalysis.getHomeActs());
		writeOccupancy(id + ".work", rawRunAnalysis.getWorkActs());
	}

	/**
	 * @param id
	 * @param map
	 */
	private void writeOccupancy(String id, Map<String, LinkActivityOccupancyCounter> map) {
		BufferedWriter writer =  IOUtils.getBufferedWriter(outputPath + id + ".activityCounts.csv.gz");
		try {
			// write header
			writer.write(";");
			for(int t: timeBins){
				writer.write(String.valueOf(t) + ";");
			}
			writer.write(all + ";");
			//write content
			writer.write("\n");
			for(Id l: this.links){
				writer.write(l.toString() + ";");
				for(int t: timeBins){
					writer.write(map.get(String.valueOf(t)).getMaximumOccupancy(l)+ ";");
				}
				writer.write(map.get(all).getMaximumOccupancy(l)+ ";");
				writer.write("\n");
			}
			writer.flush();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	/**
	 * 
	 */
	private void createAggregatedAnalysis() {
		Map<Id, Integer> officeSizePerLink = findOfficeSize();
		this.aggregated = new HashMap<String, Map<Integer,Double>>();
		this.aggregated.put(baseRunId, calcSingleRunEnergyConsumptionWork(baseRunRawAnalyzed, officeSizePerLink));
		for(Entry<String,RawRunAnalysisData> rra: runsRawAnalyzed.entrySet()){
			this.aggregated.put(rra.getKey(), calcSingleRunEnergyConsumptionWork(rra.getValue(), officeSizePerLink));
		}
	}

	/**
	 * @param baseRunRawAnalyzed2
	 * @param officeSizePerLink 
	 * @return
	 */
	private Map<Integer, Double> calcSingleRunEnergyConsumptionWork(
			RawRunAnalysisData baseRunRawAnalyzed2, Map<Id, Integer> officeSizePerLink) {
		Map<Integer, Double> map = new LinkedHashMap<Integer, Double>();
		for(Id l: links){
			Integer officeSize = officeSizePerLink.get(l);
			// no activities.
			if(officeSize == 0) continue;
			for(int i : timeBins){
				LinkActivityOccupancyCounter oc = baseRunRawAnalyzed.getWorkActs().get(String.valueOf(i));
				Integer maxOccupancy = oc.getMaximumOccupancy(l);
				Double energyConsumption = ec.getEnergyConsumption_kWh(officeSize, maxOccupancy);
				Double d = map.put(i, energyConsumption);
				if(d != null) map.put(i, d + energyConsumption);
			}
		}
		return map;
	}
	

	/**
	 * 
	 */
	private void dumpAggregatedData() {
		BufferedWriter w = IOUtils.getBufferedWriter(outputPath + "energyConsumption.csv.gz");
		try {
			//header
			w.write(";");
			for(Integer i : timeBins){
				w.write(String.valueOf(i) + ";");
			}
			w.write("\n");
			//content
			for(Entry<String, Map<Integer, Double>> e: this.aggregated.entrySet()){
				w.write(e.getKey() + ";");
				for(Integer i : timeBins){
					w.write(String.valueOf(e.getValue().get(i)) + ";");
				}
				w.write("\n");
			}
			w.flush();
			w.close();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
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
	
	// ##################### private Static classes ##############################
	
	private static class RawRunAnalysisData{
		
		private int personHomeCnt;
		private int personsWorkCnt;
		private int personsHomeAndWorkCnt;
		private Map<String, LinkActivityOccupancyCounter> workActs;
		private Map<String, LinkActivityOccupancyCounter> homeActs;

		RawRunAnalysisData(String runId, 
				int personsHomeCnt, 
				int personsWorkingCnt,
				int personsHomeAndWorkCnt,
				Map<String, 
				LinkActivityOccupancyCounter> homeActs, 
				Map<String, LinkActivityOccupancyCounter> workActs, Network network){
			this.personHomeCnt = personsHomeCnt;
			this.personsWorkCnt = personsWorkingCnt;
			this.homeActs = homeActs;
			this.workActs = workActs;
			this.personsHomeAndWorkCnt = personsHomeAndWorkCnt;
		}
		
		public int getHomeCnt(){
			return personHomeCnt;
		}
		
		public int getWorkCnt(){
			return personsWorkCnt;
		}
		
		public int getHomeWorkCnt(){
			return personsHomeAndWorkCnt;
		}


		/**
		 * @return the workActs
		 */
		public Map<String, LinkActivityOccupancyCounter> getWorkActs() {
			return workActs;
		}

		/**
		 * @return the homeActs
		 */
		public Map<String, LinkActivityOccupancyCounter> getHomeActs() {
			return homeActs;
		}
	}

	/**
	 * 
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		Gbl.startMeasurement();
		boolean time = Gbl.enableThreadCpuTimeMeasurement();
		args = ARGS;
		if(args.length < 7){
			throw new IllegalArgumentException("expecting min 8 arguments {inputpath, outputPath, timeSliceSize, tmax, baseRunId, homeActivityType, workActivityType, runIds...");
		}
		String inputPath = new File(args[0]).getAbsolutePath() + System.getProperty("file.separator");
		String outputPath = new File(args[1]).getAbsolutePath() + System.getProperty("file.separator");
		int td = Integer.parseInt(args[2]);
		int tmax = Integer.parseInt(args[3]);
		String baseRun = args[4];
		String homeType = args[5];
		String workType = args[6];
		List<String> runs = new ArrayList<String>();
		for(int i = 7; i < args.length; i++){
			runs.add(args[i]);
		}
		//catch logEntries
		OutputDirectoryLogging.initLogging(new OutputDirectoryHierarchy(outputPath, BuildingEnergyAnalyzerMain.class.getSimpleName(), true, false));
		OutputDirectoryLogging.catchLogEntries();
		// dump input-parameters to log
		log.info("running class: " + System.getProperty("sun.java.command"));
		log.info("inputPath: " + inputPath);
		log.info("outputPath: " + outputPath);
		log.info("timeSliceDuration: " + String.valueOf(td));
		log.info("tMax: " + tmax);
		log.info("baseRun: " + baseRun);
		log.info("homeType: " + homeType);
		log.info("workType: " + workType); 
		for(int i = 0; i < runs.size(); i++){
			log.info("caseStudy " + (i + 1) + ": " + runs.get(i));
		}
		// run
		new BuildingEnergyAnalyzerMain(inputPath, outputPath, td, tmax, baseRun, runs, homeType, workType, null).run();
		if(time){
			Gbl.printCurrentThreadCpuTime();
		}
		Gbl.printElapsedTime();
		log.info("finished.");
	}


}

