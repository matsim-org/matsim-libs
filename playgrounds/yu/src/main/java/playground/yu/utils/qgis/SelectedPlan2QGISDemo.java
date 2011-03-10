/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.yu.utils.qgis;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.geotools.referencing.CRS;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.utils.gis.matsim2esri.plans.SelectedPlans2ESRIShape;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * This class is a copy of main() from
 * org.matsim.utils.gis.matsim2esri.plans.SelectedPlans2ESRIShape and can
 * convert a MATSim-population to a QGIS .shp-file (acts or legs)
 *
 * @author ychen
 *
 */
public class SelectedPlan2QGISDemo implements X2QGIS {
	private final static Logger log = Logger.getLogger(SelectedPlan2QGISDemo.class);
	public static void main(final String[] args) throws FactoryException {
		// final String populationFilename = "./examples/equil/plans100.xml";
		// final String populationFilename =
		// "../runs/run628/it.500/500.plans.xml.gz";
		// final String populationFilename = "output/bvg/245.xml.gz";
		final String populationFilename = "input/bse/760.plans.xml.gz";
		// final String networkFilename = "./examples/equil/network.xml";
		// final String networkFilename =
		// "test/scenarios/berlin/network.xml.gz";
		final String networkFilename = "../schweiz-ivtch-SVN/baseCase/network/ivtch-osm.xml";
		// final String networkFilename =
		// "../schweiz-ivtch-SVN/baseCase/network/ivtch-osm.xml";
		// final String outputDir = "../runs/run628/it.500/";
		// final String outputDir = "output/bvg";
		final String outputDir = "output/bse";

		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		NetworkImpl network = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(networkFilename);

		Population population = scenario.getPopulation();
		new MatsimPopulationReader(scenario).readFile(populationFilename);
		/*
		 * ----------------------------------------------------------------------
		 */
		CoordinateReferenceSystem crs = CRS.parseWKT(ch1903);
		SelectedPlans2ESRIShape sp = new SelectedPlans2ESRIShape(population, network,
				crs, outputDir);
		sp.setOutputSample(
		// 0.05
				0.2);
		sp.setActBlurFactor(100);
		sp.setLegBlurFactor(100);
		sp.setWriteActs(true);
		sp.setWriteLegs(true);

		try {
			sp.write();
		} catch (IOException e) {
			log.error(e);
		}
	}
}
