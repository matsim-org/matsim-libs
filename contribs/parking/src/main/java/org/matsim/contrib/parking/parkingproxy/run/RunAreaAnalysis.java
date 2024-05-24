/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2020 by the members listed in the COPYING,        *
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

package org.matsim.contrib.parking.parkingproxy.run;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;

import org.geotools.api.feature.simple.SimpleFeature;
import org.matsim.contrib.parking.parkingproxy.analysis.RegionModeshareAnalyzer;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.StreamingPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.gis.GeoFileReader;

public class RunAreaAnalysis {

	public static void main(String[] args) {
		Path shapefile = Paths.get(args[0]);
		Path experiencedPlansPath = Paths.get(args[1]);
		Path networkFile = Paths.get(args[2]);

		Collection<SimpleFeature> features = GeoFileReader.getAllFeatures(shapefile.toString());

		Config conf = ConfigUtils.createConfig();
		conf.network().setInputFile(networkFile.toString());
		RegionModeshareAnalyzer modeshares = new RegionModeshareAnalyzer(features);
		StreamingPopulationReader reader = new StreamingPopulationReader(ScenarioUtils.loadScenario(conf));
		reader.addAlgorithm(modeshares);
		reader.readFile(experiencedPlansPath.toString());
		modeshares.write(new File(experiencedPlansPath.getParent().toString(), "areaModeShare.csv"));
	}

}
