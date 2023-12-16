/* *********************************************************************** *
 * project: org.matsim.*
 * SelectedPlans2ESRIShapeTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.utils.gis.matsim2esri.plans;

import java.io.IOException;
import java.util.Collection;
import java.util.zip.GZIPInputStream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.testcases.MatsimTestUtils;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class SelectedPlans2ESRIShapeTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();


	@Test
	void testSelectedPlansActsShape() throws IOException {
		String outputDir = utils.getOutputDirectory();

		String outShp = utils.getOutputDirectory() + "acts.shp";

		Scenario scenario = ScenarioUtils.createScenario(utils.loadConfig((String)null));
		Network network = scenario.getNetwork();
		new MatsimNetworkReader(scenario.getNetwork()).parse(new GZIPInputStream(getClass().getResourceAsStream("/test/scenarios/berlin/network.xml.gz")));

		Population population = scenario.getPopulation();
		new PopulationReader(scenario).parse(new GZIPInputStream(getClass().getResourceAsStream("/test/scenarios/berlin/plans_hwh_1pct.xml.gz")));

		CoordinateReferenceSystem crs = MGC.getCRS("DHDN_GK4");
		SelectedPlans2ESRIShape sp = new SelectedPlans2ESRIShape(population, network, crs, outputDir);
		sp.setOutputSample(0.05);
		sp.setActBlurFactor(100);
		sp.setWriteActs(true);
		sp.setWriteLegs(false);
		sp.write();

		Collection<SimpleFeature> writtenFeatures = ShapeFileReader.getAllFeatures(outShp);
		Assertions.assertEquals(2235, writtenFeatures.size());
	}

	@Test
	void testSelectedPlansLegsShape() throws IOException {
		String outputDir = utils.getOutputDirectory();

		String outShp = utils.getOutputDirectory() + "legs.shp";

		Scenario scenario = ScenarioUtils.createScenario(utils.loadConfig((String)null));
		Network network = scenario.getNetwork();
		new MatsimNetworkReader(scenario.getNetwork()).parse(new GZIPInputStream(getClass().getResourceAsStream("/test/scenarios/berlin/network.xml.gz")));

		Population population = scenario.getPopulation();
		new PopulationReader(scenario).parse(new GZIPInputStream(getClass().getResourceAsStream("/test/scenarios/berlin/plans_hwh_1pct.xml.gz")));

		CoordinateReferenceSystem crs = MGC.getCRS("DHDN_GK4");
		SelectedPlans2ESRIShape sp = new SelectedPlans2ESRIShape(population, network, crs, outputDir);
		sp.setOutputSample(0.05);
		sp.setLegBlurFactor(100);
		sp.setWriteActs(false);
		sp.setWriteLegs(true);
		sp.write();

		Collection<SimpleFeature> writtenFeatures = ShapeFileReader.getAllFeatures(outShp);
		Assertions.assertEquals(1431, writtenFeatures.size());
	}

}
