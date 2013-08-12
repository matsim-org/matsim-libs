/* *********************************************************************** *
 * project: org.matsim.*
 * DgAnalyseCottbusBasecase
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.dgrether.koehlerstrehlersignal.analysis;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.signalsystems.data.SignalsScenarioWriter;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.dgrether.DgPaths;
import playground.dgrether.events.EventsFilterManager;
import playground.dgrether.events.EventsFilterManagerImpl;
import playground.dgrether.events.GeospatialEventTools;
import playground.dgrether.events.filters.TimeEventFilter;
import playground.dgrether.koehlerstrehlersignal.run.Cottbus2KS2010;
import playground.dgrether.signalsystems.cottbus.CottbusUtils;
import playground.dgrether.signalsystems.utils.DgScenarioUtils;


public class DgAnalyseCottbusBasecase {

	
	private static final Logger log = Logger.getLogger(DgAnalyseCottbusBasecase.class);
	
	public static void main(String[] args) {
		String runId = "1722";
		String runDirectory = DgPaths.REPOS + "runs-svn/run1722/";
		String outputDirectory = DgPaths.REPOS + "runs-svn/run1722/analysis/";
		int iteration = 1000;
		double startTime = 13.5 * 3600.0;
		double endTime = 18.5 * 3600.0;
		OutputDirectoryHierarchy outputDir = new OutputDirectoryHierarchy(runDirectory, runId, false, false);
		String networkFilename = outputDir.getOutputFilename("output_network.xml.gz");
		String eventsFilename = outputDir.getIterationFilename(iteration, "events.xml.gz");
		String populationFilename = outputDir.getOutputFilename("output_plans.xml.gz");
		String lanesFilename = outputDir.getOutputFilename("output_lanes.xml.gz");
		String signalsystemsFilename = outputDir.getOutputFilename(SignalsScenarioWriter.FILENAME_SIGNAL_SYSTEMS);
		String signalgroupsFilename = outputDir.getOutputFilename(SignalsScenarioWriter.FILENAME_SIGNAL_GROUPS);
		String signalcontrolFilename = outputDir.getOutputFilename(SignalsScenarioWriter.FILENAME_SIGNAL_CONTROL);
		String ambertimesFilename = outputDir.getOutputFilename(SignalsScenarioWriter.FILENAME_AMBER_TIMES);
		String cottbusFeatureFile = DgPaths.REPOS
				+ "shared-svn/studies/countries/de/brandenburg_gemeinde_kreisgrenzen/kreise/dlm_kreis.shp";
		Tuple<CoordinateReferenceSystem, SimpleFeature> cottbusFeatureTuple = CottbusUtils.loadCottbusFeature(cottbusFeatureFile);
//		if (! Cottbus2KS2010.CRS.equals(cottbusFeatureTuple.getFirst())) throw new RuntimeException();
		
		Scenario scenario = DgScenarioUtils.loadScenario(networkFilename, populationFilename, lanesFilename, 
				signalsystemsFilename, signalgroupsFilename, signalcontrolFilename);

		GeospatialEventTools geotools = new GeospatialEventTools(scenario.getNetwork(), Cottbus2KS2010.CRS);
		geotools.addCrsFeatureTuple(cottbusFeatureTuple);
		
		EventsFilterManager eventsManager = new EventsFilterManagerImpl();
		TimeEventFilter tef = new TimeEventFilter();
		tef.setStartTime(startTime);
		tef.setEndTime(endTime);
		eventsManager.addFilter(tef);
		
		//Average traveltime
		// Average speed
		DgAverageTravelTimeSpeed avgTtSpeed = new DgAverageTravelTimeSpeed(geotools);
		eventsManager.addHandler(avgTtSpeed);
		
		MatsimEventsReader reader = new MatsimEventsReader(eventsManager);
		reader.readFile(eventsFilename);
		double averageTT = avgTtSpeed.getTravelTime() / avgTtSpeed.getNumberOfPersons();
		log.info("Average travel time : " + avgTtSpeed.getTravelTime() + " Average tt: " + averageTT);
		
		log.info("done.");

		
		// Macroscopic fundamental diagram
		// Leg histogram, see DgCottbusLegHistogram or LHI
		// Traffic difference qgis
	}
}
