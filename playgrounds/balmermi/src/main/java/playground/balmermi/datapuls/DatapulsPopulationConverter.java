/* *********************************************************************** *
 * project: org.matsim.*
 * DatapulsPopulationConverter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.balmermi.datapuls;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.core.router.util.AStarLandmarksFactory;
import org.matsim.core.scenario.ScenarioLoaderImpl;

import playground.balmermi.datapuls.modules.FacilitiesWriteTables;
import playground.balmermi.datapuls.modules.LinkTablesFromPopulation;
import playground.balmermi.datapuls.modules.PopulationWriteTable;

public class DatapulsPopulationConverter {
	
	private final static Logger log = Logger.getLogger(DatapulsConverter.class);

	public static void main(String[] args) {
		log.info("gathering time bin size...");
		int timeBinSize = Integer.parseInt(args[1]);
		log.info("=> timeBinSize: "+timeBinSize);
		log.info("done.");
		
		log.info("loading scenario...");
		Scenario scenario = new ScenarioLoaderImpl(args[0]).loadScenario();
		log.info("done.");

		log.info("extracting output directory... ");
		String outdir = scenario.getConfig().facilities().getOutputFile();
		outdir = outdir.substring(0,outdir.lastIndexOf("/"));
		log.info("=> "+outdir);
		log.info("done.");
		
		new FacilitiesWriteTables().run(((ScenarioImpl) scenario).getActivityFacilities(),outdir);
		new PopulationWriteTable(((ScenarioImpl) scenario).getActivityFacilities()).run(scenario.getPopulation(),outdir);
		FreespeedTravelTimeCost timeCostCalculator = new FreespeedTravelTimeCost(scenario.getConfig().charyparNagelScoring());
		AStarLandmarksFactory factory = new AStarLandmarksFactory(scenario.getNetwork(), timeCostCalculator);
		LinkTablesFromPopulation planAlgo = new LinkTablesFromPopulation(timeBinSize, outdir, scenario.getNetwork(), ((ScenarioImpl)scenario).getActivityFacilities(), factory.createPathCalculator(scenario.getNetwork(), timeCostCalculator, timeCostCalculator), scenario.getConfig().plansCalcRoute());
		log.info("processing plans...");
		try {
			planAlgo.run(scenario.getPopulation());
		} catch (IOException e) {
			e.printStackTrace();
		}
		log.info("done.");
	}

}
