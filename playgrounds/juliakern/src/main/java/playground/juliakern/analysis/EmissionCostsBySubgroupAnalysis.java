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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.scenario.ScenarioUtils;

import playground.benjamin.scenarios.munich.analysis.filter.UserGroup;
import playground.benjamin.scenarios.munich.analysis.filter.UserGroupUtils;
import playground.benjamin.scenarios.munich.analysis.spatialAvg.Cell;
import playground.benjamin.scenarios.munich.analysis.spatialAvg.LinkLineWeightUtil;
import playground.benjamin.scenarios.munich.analysis.spatialAvg.LinkWeightUtil;
import playground.benjamin.scenarios.munich.analysis.spatialAvg.SpatialAveragingInputData;
import playground.benjamin.scenarios.munich.analysis.spatialAvg.SpatialAveragingWriter;
import playground.benjamin.scenarios.munich.analysis.spatialAvg.SpatialGrid;
import playground.juliakern.newInternalization.IntervalHandler;

public class EmissionCostsBySubgroupAnalysis {

	final static int noOfXbins = 160;
	final static int noOfYbins = 120; 
	
	final static int numberOfTimeBins = 1;
	final double smoothingRadius_m = 500.;
	
	private static String baseCase = "exposureInternalization"; // exposureInternalization, latsis, 981
	private static String compareCase = "exposurePricing"; // zone30, pricing, exposurePricing, 983
	private static double timeBinSize = 30.0*60.0*60.0;
	private static LinkWeightUtil linkweightUtil;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
				// calculate durations per cell for each usergroup (and in total)
		UserGroupUtils ugu = new UserGroupUtils();
		SpatialAveragingInputData inputData = new SpatialAveragingInputData(baseCase, compareCase);
		System.out.println(inputData.getEndTime());
		
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(inputData.getNetworkFile());
		config.plans().setInputFile(inputData.getPlansFile());
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Population pop = scenario.getPopulation();
		
		SpatialGrid sGrid = new SpatialGrid(inputData, noOfXbins, noOfYbins);
		// map links to cells
		Map<Id<Link>, Cell> links2cells = mapLinksToGridCells(scenario.getNetwork().getLinks().values(), sGrid );
		EventsManager eventsManager = EventsUtils.createEventsManager();
		EventsReaderXMLv1 eventsReader = new EventsReaderXMLv1(eventsManager);
		
		IntervalHandlerGroups intervalHandlerGroups = new IntervalHandlerGroups(numberOfTimeBins, inputData, links2cells);
		
		eventsManager.addHandler(intervalHandlerGroups);
		eventsReader.parse(inputData.getEventsFile());
		Map<Integer, SpatialGrid> totalDurations = intervalHandlerGroups.getTotalDurations();
		Map<Integer, Map<UserGroup, SpatialGrid>> groupDurations = intervalHandlerGroups.getGroupDurations();
		
		//eventsManager.removeHandler(intervalHandlerGroups);
		
		SpatialAveragingWriter saw = new SpatialAveragingWriter(inputData.getMinX(), inputData.getMaxX(), inputData.getMinY(), inputData.getMaxY(),
									noOfXbins, noOfYbins, 500., inputData.getMunichShapeFile(), inputData.getTargetCRS(), false);
		saw.writeRoutput(totalDurations.get(0).getWeightedValuesOfGrid(), inputData.getAnalysisOutPathForBaseCase()+".totalDurations.txt");
		for(UserGroup ug: UserGroup.values()){
			saw.writeRoutput(groupDurations.get(0).get(ug).getWeightedValuesOfGrid(), inputData.getAnalysisOutPathForBaseCase()+"."+ug.toString()+"durations.txt");
		}
		
		System.out.println("done calculating durations, starting to calc flat emission costs");
		eventsManager = EventsUtils.createEventsManager();
		// calculate flat emission costs per link for each causing usergroup	
		EmissionCostsByGroupsAndTimeBinsHandler emissionCostsByGroupsAndTimeBinsHandler = new EmissionCostsByGroupsAndTimeBinsHandler(timeBinSize , numberOfTimeBins);
		eventsManager.addHandler(emissionCostsByGroupsAndTimeBinsHandler);
		eventsReader = new EventsReaderXMLv1(eventsManager);
		eventsReader.parse(inputData.getEmissionFileForBaseCase());
		HashMap<Integer, GroupLinkFlatEmissions> timeBin2causingUserGroup2links2flatEmissionCosts = emissionCostsByGroupsAndTimeBinsHandler.getAllFlatCosts();
		System.out.println(timeBin2causingUserGroup2links2flatEmissionCosts.get(0).getUserGroupCosts(UserGroup.URBAN));
		System.out.println(timeBin2causingUserGroup2links2flatEmissionCosts.size());
		System.out.println(timeBin2causingUserGroup2links2flatEmissionCosts.get(0).toString());
		
		System.out.println("done calculating flat emission costs, starting to calc group specific emission costs");
		// calculate scaled (relative duration density, scenario scaling factor)
		// emission costs -> timebin x subgroup x subgroup matrix
		
		linkweightUtil = new LinkLineWeightUtil(500., inputData.getBoundingboxSizeSquareMeter()/noOfXbins/noOfYbins);
		HashMap<Integer, GroupXGroupEmissionCosts> timeBin2GroupEmissionCostMatrix = new HashMap<Integer, GroupXGroupEmissionCosts>();
		for(int i=0; i<numberOfTimeBins; i++){
			Double averageDurationPerCell = getAverageDurationPerCell(totalDurations.get(i));
			timeBin2GroupEmissionCostMatrix.put(i, new GroupXGroupEmissionCosts());
			timeBin2GroupEmissionCostMatrix.get(i).calculateGroupCosts(timeBin2causingUserGroup2links2flatEmissionCosts.get(i),
					groupDurations.get(i), linkweightUtil, averageDurationPerCell,
					scenario.getNetwork().getLinks());
			timeBin2GroupEmissionCostMatrix.get(i).print();
		}
		
		

	}

	private static Double getAverageDurationPerCell(SpatialGrid spatialGrid) {
		Double sum = 0.0;
		for(Double[] db: spatialGrid.getWeightedValuesOfGrid()){
			for(Double dd: db){
				sum +=dd;
			}
		}
		return (sum/noOfXbins/noOfYbins);
	}

	private static Map<Id<Link>, Cell> mapLinksToGridCells(Collection<? extends Link> links, SpatialGrid grid) {
		Map<Id<Link>, Cell> links2Cells = new HashMap<Id<Link>, Cell>();
		
		for(Link link: links){
			Cell cCell = grid.getCellForCoordinate(link.getCoord());
			if(cCell!=null)links2Cells.put(link.getId(), cCell);
		}
		System.out.println("Mapped " + links2Cells.size() + " links to grid");
		return links2Cells;
	}
}
