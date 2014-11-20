/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.juliakern.analysis;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.emissions.events.EmissionEventsReader;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.scenario.ScenarioUtils;

import playground.benjamin.scenarios.munich.analysis.filter.UserGroup;
import playground.benjamin.scenarios.munich.analysis.spatialAvg.Cell;
import playground.benjamin.scenarios.munich.analysis.spatialAvg.CellWeightUtil;
import playground.benjamin.scenarios.munich.analysis.spatialAvg.LinkWeightUtil;
import playground.benjamin.scenarios.munich.analysis.spatialAvg.SpatialAveragingInputData;
import playground.benjamin.scenarios.munich.analysis.spatialAvg.SpatialAveragingWriter;
import playground.benjamin.scenarios.munich.analysis.spatialAvg.SpatialGrid;

public class EmissionCostsBySubgroupAnalysis {
	private static final Logger logger = Logger.getLogger(EmissionCostsBySubgroupAnalysis.class);

	final static int noOfXbins = 160;
	final static int noOfYbins = 120; 
	
	final static int numberOfTimeBins = 1;
	final double smoothingRadius_m = 500.;
	
	private String scenarioName = "exposureInternalization"; // exposureInternalization, latsis, 981
	private String analysisCase = "base"; // base, zone30, pricing, exposurePricing, 983
	private double timeBinSize = 30.0*60.0*60.0;
	private LinkWeightUtil linkweightUtil;
	private EmissionCostsByGroupsAndTimeBinsHandler emissionCostsByGroupsAndTimeBinsHandler;
	private SpatialAveragingInputData inputData;
	private Map<Id<Link>, Cell> linkIds2cells;
	private Map<Link, Cell> links2cells;
	private Scenario scenario;
	private Map<Integer, Map<UserGroup, SpatialGrid>> groupDurations;
	private Map<Integer, SpatialGrid> totalDurations;
	private HashMap<Integer, GroupLinkFlatEmissions> timeBin2causingUserGroup2links2flatEmissionCosts;
	private SpatialGrid sGrid;
	
	public static void main(String[] args) {
		EmissionCostsBySubgroupAnalysis ecbsa = new EmissionCostsBySubgroupAnalysis();
		ecbsa.initialize();
		ecbsa.calculateDurations();
		ecbsa.calculateFlatEmissionCosts();
		ecbsa.calculateExposureCostsByGroup();
		logger.info("done.");
	}
	
	private void initialize(){
		// calculate durations per cell for each usergroup (and in total)
		
		inputData = new SpatialAveragingInputData(scenarioName, analysisCase);
		
		logger.info(inputData.getScenarioInformation());
		
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(inputData.getNetworkFile());
		config.plans().setInputFile(inputData.getPlansFileCompareCase());
		scenario = ScenarioUtils.loadScenario(config);
		
		sGrid = new SpatialGrid(inputData, noOfXbins, noOfYbins);
		// map links to cells
		mapLinksToGridCells(scenario.getNetwork().getLinks().values(), sGrid );
		logger.info("Mapped " + linkIds2cells.size() + " links to cells. ");
	}
	
	private void calculateDurations(){
		logger.info("Starting to calculate durations...");
		EventsManager eventsManager = EventsUtils.createEventsManager();
		EventsReaderXMLv1 eventsReader = new EventsReaderXMLv1(eventsManager);
		IntervalHandlerGroups intervalHandlerGroups = new IntervalHandlerGroups(numberOfTimeBins, inputData, linkIds2cells);
		eventsManager.addHandler(intervalHandlerGroups);
		eventsReader.parse(inputData.getEventsFileCompareCase());
		
		totalDurations = intervalHandlerGroups.getTotalDurations();
		groupDurations = intervalHandlerGroups.getGroupDurations();
		
		SpatialAveragingWriter saw = new SpatialAveragingWriter(inputData.getMinX(), inputData.getMaxX(), inputData.getMinY(), inputData.getMaxY(),
				noOfXbins, noOfYbins, smoothingRadius_m, inputData.getMunichShapeFile(), inputData.getTargetCRS(), false);

		for(int timeBin =0; timeBin<numberOfTimeBins; timeBin++){
			logger.info(inputData.getScenarioInformation());
			logger.info("Writing duration output for time interval " + timeBin + " of " + numberOfTimeBins + " time intervals.");
			String timeIntervalEnd = Double.toString(((timeBin+1.0)*timeBinSize));
			saw.writeRoutput(totalDurations.get(timeBin).getWeightedValuesOfGrid(), inputData.getAnalysisOutPathForCompareCase()+".totalDurations.timeIntervalEnd."+ timeIntervalEnd +".txt");
			for(UserGroup ug: UserGroup.values()){
				saw.writeRoutput(groupDurations.get(timeBin).get(ug).getWeightedValuesOfGrid(), inputData.getAnalysisOutPathForCompareCase()+"."+ug.toString()+"durations.timeIntervalEnd."+ timeIntervalEnd +".txt");
			}
		}
		logger.info("Done calculating and writing durations.");
		
	}
	private void calculateFlatEmissionCosts(){
		logger.info("Starting to calculate flat emission costs...");
		EventsManager eventsManager = EventsUtils.createEventsManager();
		EmissionEventsReader emissionReader = new EmissionEventsReader(eventsManager);
		// calculate flat emission costs per link for each causing usergroup	
		emissionCostsByGroupsAndTimeBinsHandler = new EmissionCostsByGroupsAndTimeBinsHandler(timeBinSize , numberOfTimeBins);
		
		eventsManager.addHandler(emissionCostsByGroupsAndTimeBinsHandler);
		emissionReader.parse(inputData.getEmissionFileForCompareCase());
		timeBin2causingUserGroup2links2flatEmissionCosts = emissionCostsByGroupsAndTimeBinsHandler.getAllFlatCosts();

		// write output? TODO
		logger.info("Done calculating flat emission costs.");
	}
	
	private void calculateExposureCostsByGroup(){
		logger.info("Starting to calculate group specific emission costs. This may take a while....");
		// calculate scaled (relative duration density, scenario scaling factor)
		// emission costs -> timebin x subgroup x subgroup matrix
		
//		linkweightUtil = new LinkLineWeightUtil(500., inputData.getBoundingboxSizeSquareMeter()/noOfXbins/noOfYbins);
		linkweightUtil = new CellWeightUtil(links2cells, sGrid);
		HashMap<Integer, GroupXGroupExposureCosts> timeBin2GroupEmissionCostMatrix = new HashMap<Integer, GroupXGroupExposureCosts>();
		for(int i=0; i<numberOfTimeBins; i++){
			logger.info("Calculating group specific emission costs for time interval " + (i+1) + ".");
			Double averageDurationPerCell = getAverageDurationPerCell(totalDurations.get(i));
			timeBin2GroupEmissionCostMatrix.put(i, new GroupXGroupExposureCosts(inputData.getScalingFactor()));
			timeBin2GroupEmissionCostMatrix.get(i).calculateGroupCosts(timeBin2causingUserGroup2links2flatEmissionCosts.get(i),
					groupDurations.get(i), linkweightUtil, averageDurationPerCell,
					scenario.getNetwork().getLinks());
			logger.info(inputData.getScenarioInformation());
			timeBin2GroupEmissionCostMatrix.get(i).print();
		}
	}

	//TODO verschieben
	private static Double getAverageDurationPerCell(SpatialGrid spatialGrid) {
		Double sum = 0.0;
		for(Double[] db: spatialGrid.getWeightedValuesOfGrid()){
			for(Double dd: db){
				sum +=dd;
			}
		}
		return (sum/noOfXbins/noOfYbins);
	}

	private void mapLinksToGridCells(Collection<? extends Link> links, SpatialGrid grid) {
		linkIds2cells = new HashMap<Id<Link>, Cell>();
		links2cells = new HashMap<Link, Cell>();
		
		for(Link link: links){
			Cell cCell = grid.getCellForCoordinate(link.getCoord());
			if(cCell!=null){
				links2cells.put(link, cCell);
				linkIds2cells.put(link.getId(), cCell);
			}
		}		
	}
}
