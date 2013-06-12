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
package playground.dgrether.signalsystems.cottbus.scripts;

import java.io.File;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.dgrether.DgPaths;
import playground.dgrether.koehlerstrehlersignal.PopulationToOd;
import playground.dgrether.koehlerstrehlersignal.Scenario2KoehlerStrehler2010;
import playground.dgrether.koehlerstrehlersignal.ScenarioShrinker;
import playground.dgrether.signalsystems.cottbus.DgCottbusScenarioPaths;


/**
 * @author dgrether
 *
 */
public class DgCb2Ks2010 {

	private static String shapeFileDirectory = "shapes/";

	
	public static void main(String[] args) throws Exception {
		int cellsX = 5;
		int cellsY = 5;
		double boundingBoxOffset = 50.0;
		double startTime = 5.5 * 3600.0;
		double endTime = 9.5 * 3600.0;
	//private static double startTime = 13.5 * 3600.0;
	//private static double endTime = 18.5 * 3600.0;
		double matsimPopSampleSize = 1.0;
		double ksModelCommoditySampleSize = 0.01;
		final String outputDirectory = DgPaths.REPOS + "shared-svn/projects/cottbus/cb2ks2010/test_tt/";
		OutputDirectoryLogging.initLoggingWithOutputDirectory(outputDirectory);
		String shapeFileDirectory = createShapeFileDirectory(outputDirectory);
		
		String networkFilename = "/media/data/work/repos/shared-svn/studies/dgrether/cottbus/cottbus_feb_fix/network_wgs84_utm33n.xml.gz";
		String lanesFilename = DgCottbusScenarioPaths.LANES_FILENAME;
		String signalSystemsFilename = DgPaths.REPOS +  "shared-svn/studies/dgrether/cottbus/cottbus_feb_fix/signal_systems.xml";
		String signalGroupsFilename = DgCottbusScenarioPaths.SIGNAL_GROUPS_FILENAME;
		String signalControlFilename = DgCottbusScenarioPaths.SIGNAL_CONTROL_FIXEDTIME_FILENAME;
		//TODO change to run1712 when finished
		String populationFilename = DgPaths.REPOS + "runs-svn/run1292/1292.output_plans_sample.xml";
//		String populationFile = DgPaths.REPOS + "runs-svn/run1292/1292.output_plans.xml.gz";
		Scenario fullScenario = DgCb2Ks2010.loadScenario(networkFilename, populationFilename, lanesFilename, signalSystemsFilename, signalGroupsFilename, signalControlFilename);
		
		String name = "run 1292 output plans between 05:30 and 09:30";
		CoordinateReferenceSystem crs = MGC.getCRS(TransformationFactory.WGS84_UTM33N);
		
		ScenarioShrinker scenarioShrinker = new ScenarioShrinker(fullScenario,  crs);
		Network shrinkedNetwork = scenarioShrinker.shrinkScenario(outputDirectory, shapeFileDirectory, boundingBoxOffset);
		
		PopulationToOd pop2od = new PopulationToOd();
		pop2od.setMatsimPopSampleSize(matsimPopSampleSize);
		pop2od.matchPopulationToGrid(fullScenario, crs, shrinkedNetwork, scenarioShrinker.getSignalsBoundingBox(), 
				cellsX, cellsY, startTime, endTime, shapeFileDirectory);
		
		System.exit(0);
		
		Scenario scenario = null;
		Scenario2KoehlerStrehler2010 converter = new Scenario2KoehlerStrehler2010(scenario, crs);
		converter.setKsModelCommoditySampleSize(ksModelCommoditySampleSize);
		OutputDirectoryLogging.closeOutputDirLogging();		
	}

	private static String createShapeFileDirectory(String outputDirectory) {
		String shapeDir = outputDirectory + shapeFileDirectory;
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
