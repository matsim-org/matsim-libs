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
import org.geotools.feature.Feature;
import org.matsim.analysis.LegHistogram;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.dgrether.DgPaths;
import playground.dgrether.events.EventsFilterManager;
import playground.dgrether.events.EventsFilterManagerImpl;
import playground.dgrether.events.filters.GeospatialEventFilter;
import playground.dgrether.signalsystems.cottbus.CottbusUtils;


/**
 * @author dgrether
 *
 */
public class DgCottbusLegHistogram {

	
	private static final Logger log = Logger.getLogger(DgCottbusLegHistogram.class);
	
	public static void main(String[] args) {
		String runDirectory = DgPaths.RUNSSVN + "run1291/";
		String networkFile = runDirectory + "1291.output_network.xml.gz";
		String outputDirectory = runDirectory + "ITERS/it.1000/";
		String eventsFile = outputDirectory + "1291.1000.events.xml.gz";
		String histoFile = outputDirectory + "1291.1000.cottbus_leg_histogram.txt";
		String histoGraphicFile = outputDirectory + "1291.1000.cottbus_leg_histogram_all.png";
		String cottbusFeatureFile = DgPaths.REPOS
				+ "shared-svn/studies/countries/de/brandenburg_gemeinde_kreisgrenzen/kreise/dlm_kreis.shp";
		CoordinateReferenceSystem netCrs = MGC.getCRS(TransformationFactory.WGS84_UTM33N);
		Tuple<CoordinateReferenceSystem, Feature> cottbusFeatureTuple = CottbusUtils.loadCottbusFeature(cottbusFeatureFile);
		
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario).readFile(networkFile);
		Network network = scenario.getNetwork();
		GeospatialEventFilter filter = new GeospatialEventFilter(network, netCrs);
		filter.addCrsFeatureTuple(cottbusFeatureTuple);
		
		
		EventsFilterManager events = new EventsFilterManagerImpl();
		events.addFilter(filter);
		MatsimEventsReader reader = new MatsimEventsReader(events);
		LegHistogram histo = new LegHistogram(300);
		events.addHandler(histo);
		reader.readFile(eventsFile);
		histo.write(histoFile);
		histo.writeGraphic(histoGraphicFile);
		log.info("done.");
	}

}
