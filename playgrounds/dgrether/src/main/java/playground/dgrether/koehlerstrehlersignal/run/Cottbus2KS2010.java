/* *********************************************************************** *
 * project: org.matsim.*
 * DgCb2Ks2010
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
package playground.dgrether.koehlerstrehlersignal.run;

import java.io.File;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.misc.Time;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.dgrether.DgPaths;
import playground.dgrether.koehlerstrehlersignal.conversion.M2KS2010Converter;
import playground.dgrether.koehlerstrehlersignal.demand.PopulationToOd;
import playground.dgrether.koehlerstrehlersignal.demand.ZoneBuilder;
import playground.dgrether.koehlerstrehlersignal.network.NetLanesSignalsShrinker;
import playground.dgrether.signalsystems.cottbus.DgCottbusScenarioPaths;
import playground.dgrether.utils.zones.DgZones;


/**
 * @author dgrether
 *
 */
public class Cottbus2KS2010 {

	private static final Logger log = Logger.getLogger(Cottbus2KS2010.class);
	
	private static String shapeFileDirectoryName = "shapes/";
	
	public static void main(String[] args) throws Exception {
		// parameters
		int cellsX = 5;
		int cellsY = 5;
		double boundingBoxOffset = 50.0;
		double startTime = 5.5 * 3600.0;
		double endTime = 9.5 * 3600.0;
	//private static double startTime = 13.5 * 3600.0;
	//private static double endTime = 18.5 * 3600.0;
		double matsimPopSampleSize = 1.0;
		double ksModelCommoditySampleSize = 1.0;
		double minCommodityFlow = 10.0;
		String networkFilename = "/media/data/work/repos/shared-svn/studies/dgrether/cottbus/cottbus_feb_fix/network_wgs84_utm33n.xml.gz";
		String lanesFilename = DgCottbusScenarioPaths.LANES_FILENAME;
		String signalSystemsFilename = DgPaths.REPOS +  "shared-svn/studies/dgrether/cottbus/cottbus_feb_fix/signal_systems.xml";
		String signalGroupsFilename = DgCottbusScenarioPaths.SIGNAL_GROUPS_FILENAME;
		String signalControlFilename = DgCottbusScenarioPaths.SIGNAL_CONTROL_FIXEDTIME_FILENAME;
		//TODO change to run1712 when finished
//		String populationFilename = DgPaths.REPOS + "runs-svn/run1292/1292.output_plans_sample.xml";
		String populationFilename = DgPaths.REPOS + "runs-svn/run1712/1712.output_plans.xml.gz";
		String name = "run run1712 output plans between 05:30 and 09:30";
		CoordinateReferenceSystem crs = MGC.getCRS(TransformationFactory.WGS84_UTM33N);
		final String outputDirectory = DgPaths.REPOS + "shared-svn/projects/cottbus/cb2ks2010/2013-07-19_test_10/";
		String ksModelOutputFilename = "ks2010_model_";
		ksModelOutputFilename += Double.toString(minCommodityFlow) + "_" + Double.toString(startTime) + ".xml";
				
		// run
		OutputDirectoryLogging.initLoggingWithOutputDirectory(outputDirectory);
		String shapeFileDirectory = createShapeFileDirectory(outputDirectory);
		Scenario fullScenario = Cottbus2KS2010.loadScenario(networkFilename, populationFilename, lanesFilename, signalSystemsFilename, signalGroupsFilename, signalControlFilename);
		
		// reduce the size of the scenario
		NetLanesSignalsShrinker scenarioShrinker = new NetLanesSignalsShrinker(fullScenario,  crs);
		scenarioShrinker.shrinkScenario(outputDirectory, shapeFileDirectory, boundingBoxOffset);
		
		//create the zones (that are currently not used) but serve as container for the OD pairs
		ZoneBuilder zoneBuilder = new ZoneBuilder(crs);
		DgZones zones = zoneBuilder.createAndWriteZones(scenarioShrinker.getShrinkedNetwork(), scenarioShrinker.getSignalsBoundingBox(),
				cellsX, cellsY, shapeFileDirectory);
		
		// match population to the small network and convert to od 
		PopulationToOd pop2od = new PopulationToOd();
		pop2od.setMatsimPopSampleSize(matsimPopSampleSize);
		pop2od.setOriginalToSimplifiedLinkMapping(scenarioShrinker.getOriginalToSimplifiedLinkIdMatching());
		pop2od.convertPopulation2OdPairs(zones, fullScenario.getNetwork(), fullScenario.getPopulation(), crs, 
				scenarioShrinker.getShrinkedNetwork(), scenarioShrinker.getSignalsBoundingBox(), 
				startTime, endTime, shapeFileDirectory);
		
		
		//convert to KoehlerStrehler2010 file format
		M2KS2010Converter converter = new M2KS2010Converter(scenarioShrinker.getShrinkedNetwork(), 
				scenarioShrinker.getShrinkedLanes(), scenarioShrinker.getShrinkedSignals(), crs);
		String description = createDescription(cellsX, cellsY, startTime, endTime, boundingBoxOffset, 
				matsimPopSampleSize, ksModelCommoditySampleSize, minCommodityFlow);
		converter.setKsModelCommoditySampleSize(ksModelCommoditySampleSize);
		converter.setMinCommodityFlow(minCommodityFlow);
		converter.convertAndWrite(outputDirectory, shapeFileDirectory, ksModelOutputFilename , name, description, zones, startTime, endTime);
		
		
		printStatistics(cellsX, cellsY, boundingBoxOffset, startTime, endTime);
		OutputDirectoryLogging.closeOutputDirLogging();		
	}
	
	private static String createDescription(int cellsX, int cellsY, double startTime, double endTime, double boundingBoxOffset, double matsimPopSampleSize, 
			double ksModelCommoditySampleSize, double minCommodityFlow){
		String description = "offset: " + boundingBoxOffset + " cellsX: " + cellsX + " cellsY: " + cellsY + " startTimeSec: " + startTime + " endTimeSec: " + endTime;
		description += " matsimPopsampleSize: " + matsimPopSampleSize + " ksModelCommoditySampleSize: " + ksModelCommoditySampleSize;
		description += " minimum flow of commodities to be included in conversion: " + minCommodityFlow;
		return description;
	}

	
	private static void printStatistics(int cellsX, int cellsY,double boundingBoxOffset, double startTime, double endTime){
		log.info("Number of Cells:");
		log.info("  X " + cellsX + " Y " + cellsY);
		log.info("Bounding Box: ");
		log.info("  Offset: " + boundingBoxOffset);
		log.info("Time: " );
		log.info("  startTime: " + startTime + " " + Time.writeTime(startTime));
		log.info("  endTime: " + endTime  + " " + Time.writeTime(endTime));
	}
	
	private static String createShapeFileDirectory(String outputDirectory) {
		String shapeDir = outputDirectory + shapeFileDirectoryName;
		File outdir = new File(shapeDir);
		outdir.mkdir();
		return shapeDir;
	}
	
	public static Scenario loadScenario(String net, String pop, String lanesFilename, String signalsFilename,
			String signalGroupsFilename, String signalControlFilename){
		Config c2 = ConfigUtils.createConfig();
		c2.scenario().setUseLanes(true);
		c2.scenario().setUseSignalSystems(true);
		c2.network().setInputFile(net);
		c2.plans().setInputFile(pop);
		c2.network().setLaneDefinitionsFile(lanesFilename);
		c2.signalSystems().setSignalSystemFile(signalsFilename);
		c2.signalSystems().setSignalGroupsFile(signalGroupsFilename);
		c2.signalSystems().setSignalControlFile(signalControlFilename);
		Scenario scenario = ScenarioUtils.loadScenario(c2);
		return scenario;
	}

	
	
}
