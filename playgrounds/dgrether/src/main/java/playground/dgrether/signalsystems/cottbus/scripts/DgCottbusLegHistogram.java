/* *********************************************************************** *
 * project: org.matsim.*
 * DgCottbusLegHistogram
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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.dgrether.DgPaths;
import playground.dgrether.events.handlers.DgGeoFilteredLegHistogram;
import playground.dgrether.signalsystems.cottbus.CottbusUtils;


/**
 * @author dgrether
 *
 */
public class DgCottbusLegHistogram {
	
	private static final Logger log = Logger.getLogger(DgCottbusLegHistogram.class);
	
	public static void main(String[] args) {
		String runNumber = "1732";
		String iterationNumber = "1000";
		String runDirectory = DgPaths.RUNSSVN + "run" + runNumber + "/";
		String networkFile = runDirectory + "" + runNumber + ".output_network.xml.gz";
		String outputDirectory = runDirectory + "ITERS/it." + iterationNumber + "/";
		String eventsFile = outputDirectory + "" + runNumber + "." + iterationNumber + ".events.xml.gz";
		String histoFile = outputDirectory + "" + runNumber + "." + iterationNumber + ".cottbus_leg_histogram.txt";
		String histoGraphicFile = outputDirectory + "" + runNumber + "." + iterationNumber + ".cottbus_leg_histogram_all.png";
		String cottbusFeatureFile = DgPaths.REPOS
				+ "shared-svn/studies/countries/de/brandenburg_gemeinde_kreisgrenzen/kreise/dlm_kreis.shp";
		CoordinateReferenceSystem netCrs = MGC.getCRS(TransformationFactory.WGS84_UTM33N);
		Tuple<CoordinateReferenceSystem, SimpleFeature> cottbusFeatureTuple = CottbusUtils.loadCottbusFeature(cottbusFeatureFile);
		
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario.getNetwork()).readFile(networkFile);
		Network network = scenario.getNetwork();
		
		
		EventsManager events = EventsUtils.createEventsManager();
		DgGeoFilteredLegHistogram histo = new DgGeoFilteredLegHistogram(network, netCrs, 300);
		histo.addCrsFeatureTuple(cottbusFeatureTuple);
		events.addHandler(histo);

		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventsFile);
		histo.write(histoFile);
		histo.writeGraphic(histoGraphicFile);
		log.info("done.");
	}

}
