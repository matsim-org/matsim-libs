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
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.misc.Time;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.dgrether.DgPaths;
import playground.dgrether.koehlerstrehlersignal.conversion.M2KS2010Converter;
import playground.dgrether.koehlerstrehlersignal.demand.PopulationToOd;
import playground.dgrether.koehlerstrehlersignal.demand.ZoneBuilder;
import playground.dgrether.koehlerstrehlersignal.network.NetLanesSignalsShrinker;
import playground.dgrether.signalsystems.utils.DgScenarioUtils;
import playground.dgrether.utils.zones.DgZones;

/**
 * @author dgrether
 * @author tthunig
 * 
 */
public class Cottbus2KS2010 {

	private static final Logger log = Logger.getLogger(Cottbus2KS2010.class);

	private static final String shapeFileDirectoryName = "shapes/";

	public static final String SIGNAL_SYSTEMS_FILENAME = DgPaths.REPOS
			+ "shared-svn/projects/cottbus/cottbus_scenario/signal_systems_no_13.xml";
	public static final String SIGNAL_GROUPS_FILENAME = DgPaths.REPOS
			+ "shared-svn/projects/cottbus/cottbus_scenario/signal_groups_no_13.xml";
	public static final String SIGNAL_CONTROL_FILENAME = DgPaths.REPOS
			+ "shared-svn/projects/cottbus/cottbus_scenario/signal_control_no_13.xml";

	public static final String NETWORK_FILENAME = DgPaths.REPOS
			+ "shared-svn/projects/cottbus/cottbus_scenario/network_wgs84_utm33n.xml.gz";

	public static final String LANES_FILENAME = DgPaths.REPOS
			+ "shared-svn/projects/cottbus/cottbus_scenario/lanes.xml";

	// change run number here to use another base case
	public static final String POPULTATION_FILENAME = DgPaths.REPOS
			+ "runs-svn/run1728/1728.output_plans.xml.gz";

	public static final CoordinateReferenceSystem CRS = MGC
			.getCRS(TransformationFactory.WGS84_UTM33N);

	public static void main(String[] args) throws Exception {
		/* parameters for the time interval*/
		double startTime = 5.5 * 3600.0;
		double endTime = 9.5 * 3600.0;
		// double startTime = 13.5 * 3600.0;
		// double endTime = 18.5 * 3600.0;
		/* parameters for the network area */
		double signalsBoundingBoxOffset = 500.0;
		double cuttingBoundingBoxOffset = 5000.0; // an offset >= 31000.0 results in a bounding box that contains the hole network
		/* parameters for the interior link filter */
		double freeSpeedFilter = 15.0; // the minimal free speed value for the interior link filter in m/s
		boolean useFreeSpeedTravelTime = true; // a flag for dijkstras cost function: if true, dijkstra will use the free speed travel time, if false, dijkstra will use the travel distance as cost function
		double maximalLinkLength = Double.MAX_VALUE; // restricts the NetworkSimplifier. Double.MAX_VALUE is no restriction.
		/* parameters for the demand filter */
		double matsimPopSampleSize = 1.0; // 100% sample
		double ksModelCommoditySampleSize = 1.0; // 1 vehicle is equivalent to 1 unit of flow
		double minCommodityFlow = 50.0; // only commodities with at least this demand will be optimized in the BTU model
		int cellsX = 5;
		int cellsY = 5;	
		/* other parameters */
		String name = "run run1728 output plans between 05:30 and 09:30";
		// String name = "run run1728 output plans between 13:30 and 18:30";
		String spCost = "tt";
		if (!useFreeSpeedTravelTime) spCost = "dist";
		final String outputDirectory = DgPaths.REPOS + "shared-svn/projects/cottbus/cb2ks2010/2015-02-02_test_" 
				+ "_minflow_" + minCommodityFlow + "_morning_peak_speedFilter" + freeSpeedFilter + "_SP_" + spCost 
				+ "_cBB" + cuttingBoundingBoxOffset + "_sBB" + signalsBoundingBoxOffset + "_doubleLightTest/";
		String ksModelOutputFilename = "ks2010_model_";
		ksModelOutputFilename += Double.toString(minCommodityFlow) + "_"
				+ Double.toString(startTime) + "_" + Double.toString(cuttingBoundingBoxOffset) + ".xml";

		// run
		OutputDirectoryLogging.initLoggingWithOutputDirectory(outputDirectory);
		String shapeFileDirectory = createShapeFileDirectory(outputDirectory);
		Scenario fullScenario = DgScenarioUtils.loadScenario(NETWORK_FILENAME,
				POPULTATION_FILENAME, LANES_FILENAME, SIGNAL_SYSTEMS_FILENAME,
				SIGNAL_GROUPS_FILENAME, SIGNAL_CONTROL_FILENAME);

		// reduce the size of the scenario
		NetLanesSignalsShrinker scenarioShrinker = new NetLanesSignalsShrinker(
				fullScenario, CRS);
		scenarioShrinker.shrinkScenario(outputDirectory, shapeFileDirectory,
				cuttingBoundingBoxOffset, freeSpeedFilter, useFreeSpeedTravelTime, maximalLinkLength);

		// create the geometry for zones. The geometry itself is not used, but
		// the object serves as container for the link -> link OD pairs
		ZoneBuilder zoneBuilder = new ZoneBuilder(CRS);
		DgZones zones = zoneBuilder.createAndWriteZones(
				scenarioShrinker.getShrinkedNetwork(),
				scenarioShrinker.getCuttingBoundingBox(), cellsX, cellsY,
				shapeFileDirectory);

		// match population to the small network and convert to od, results are
		// stored in the DgZones object
		PopulationToOd pop2od = new PopulationToOd();
		pop2od.setMatsimPopSampleSize(matsimPopSampleSize);
		pop2od.setOriginalToSimplifiedLinkMapping(scenarioShrinker
				.getOriginalToSimplifiedLinkIdMatching());
		pop2od.convertPopulation2OdPairs(zones, fullScenario.getNetwork(),
				fullScenario.getPopulation(), CRS,
				scenarioShrinker.getShrinkedNetwork(),
				scenarioShrinker.getCuttingBoundingBox(), startTime, endTime,
				shapeFileDirectory);

		// convert to KoehlerStrehler2010 file format
		M2KS2010Converter converter = new M2KS2010Converter(
				scenarioShrinker.getShrinkedNetwork(),
				scenarioShrinker.getShrinkedLanes(),
				scenarioShrinker.getShrinkedSignals(), 
				signalsBoundingBoxOffset, CRS);
		String description = createDescription(cellsX, cellsY, startTime,
				endTime, cuttingBoundingBoxOffset, matsimPopSampleSize,
				ksModelCommoditySampleSize, minCommodityFlow);
		converter.setKsModelCommoditySampleSize(ksModelCommoditySampleSize);
		converter.setMinCommodityFlow(minCommodityFlow);
		converter.convertAndWrite(outputDirectory, shapeFileDirectory,
				ksModelOutputFilename, name, description, zones, startTime,
				endTime);

		printStatistics(cellsX, cellsY, cuttingBoundingBoxOffset, startTime, endTime);
		log.info("output ist written to " + outputDirectory);
		OutputDirectoryLogging.closeOutputDirLogging();
	}

	private static String createDescription(int cellsX, int cellsY,
			double startTime, double endTime, double boundingBoxOffset,
			double matsimPopSampleSize, double ksModelCommoditySampleSize,
			double minCommodityFlow) {
		String description = "offset: " + boundingBoxOffset + " cellsX: "
				+ cellsX + " cellsY: " + cellsY + " startTimeSec: " + startTime
				+ " endTimeSec: " + endTime;
		description += " matsimPopsampleSize: " + matsimPopSampleSize
				+ " ksModelCommoditySampleSize: " + ksModelCommoditySampleSize;
		description += " minimum flow of commodities to be included in conversion: "
				+ minCommodityFlow;
		return description;
	}

	private static void printStatistics(int cellsX, int cellsY,
			double boundingBoxOffset, double startTime, double endTime) {
		log.info("Number of Cells:");
		log.info("  X " + cellsX + " Y " + cellsY);
		log.info("Bounding Box: ");
		log.info("  Offset: " + boundingBoxOffset);
		log.info("Time: ");
		log.info("  startTime: " + startTime + " " + Time.writeTime(startTime));
		log.info("  endTime: " + endTime + " " + Time.writeTime(endTime));
	}

	private static String createShapeFileDirectory(String outputDirectory) {
		String shapeDir = outputDirectory + shapeFileDirectoryName;
		File outdir = new File(shapeDir);
		outdir.mkdir();
		return shapeDir;
	}

}
