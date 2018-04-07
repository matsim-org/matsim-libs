/* *********************************************************************** *
 * project: org.matsim.*
 * Network2ESRIShapeTest.java
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

package org.matsim.utils.gis.matsim2esri.network;

import java.util.Collection;

import org.junit.Assert;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.testcases.MatsimTestCase;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class Network2ESRIShapeTest extends MatsimTestCase  {

	public void testPolygonCapacityShape() {
		String netFileName = "test/scenarios/equil/network.xml";
		String outputFileP = getOutputDirectory() + "./network.shp";

		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		scenario.getConfig().global().setCoordinateSystem("DHDN_GK4");

		final Network network = scenario.getNetwork();
		new MatsimNetworkReader(scenario.getNetwork()).readFile(netFileName);

		FeatureGeneratorBuilderImpl builder = new FeatureGeneratorBuilderImpl(network, "DHDN_GK4");
		CoordinateReferenceSystem crs = MGC.getCRS("DHDN_GK4");
		builder.setWidthCoefficient(0.001);
		builder.setFeatureGeneratorPrototype(PolygonFeatureGenerator.class);
		builder.setWidthCalculatorPrototype(CapacityBasedWidthCalculator.class);
		builder.setCoordinateReferenceSystem(crs);
		new Links2ESRIShape(network,outputFileP, builder).write();

		Collection<SimpleFeature> writtenFeatures = ShapeFileReader.getAllFeatures(outputFileP);
		Assert.assertEquals(network.getLinks().size(), writtenFeatures.size());
	}

	public void testPolygonLanesShape() {
		String netFileName = "test/scenarios/equil/network.xml";
		String outputFileP = getOutputDirectory() + "./network.shp";

		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		scenario.getConfig().global().setCoordinateSystem("DHDN_GK4");

		final Network network = scenario.getNetwork();
		new MatsimNetworkReader(scenario.getNetwork()).readFile(netFileName);

		FeatureGeneratorBuilderImpl builder = new FeatureGeneratorBuilderImpl(network, "DHDN_GK4");
		CoordinateReferenceSystem crs = MGC.getCRS("DHDN_GK4");
		builder.setWidthCoefficient(1);
		builder.setFeatureGeneratorPrototype(PolygonFeatureGenerator.class);
		builder.setWidthCalculatorPrototype(LanesBasedWidthCalculator.class);
		builder.setCoordinateReferenceSystem(crs);
		new Links2ESRIShape(network,outputFileP, builder).write();
		
		Collection<SimpleFeature> writtenFeatures = ShapeFileReader.getAllFeatures(outputFileP);
		Assert.assertEquals(network.getLinks().size(), writtenFeatures.size());
	}

	public void testPolygonFreespeedShape() {
		String netFileName = "test/scenarios/equil/network.xml";
		String outputFileP = getOutputDirectory() + "./network.shp";

		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		scenario.getConfig().global().setCoordinateSystem("DHDN_GK4");

		final Network network = scenario.getNetwork();
		new MatsimNetworkReader(scenario.getNetwork()).readFile(netFileName);

		FeatureGeneratorBuilderImpl builder = new FeatureGeneratorBuilderImpl(network, "DHDN_GK4");
		CoordinateReferenceSystem crs = MGC.getCRS("DHDN_GK4");
		builder.setWidthCoefficient(1);
		builder.setFeatureGeneratorPrototype(PolygonFeatureGenerator.class);
		builder.setWidthCalculatorPrototype(LanesBasedWidthCalculator.class);
		builder.setCoordinateReferenceSystem(crs);
		new Links2ESRIShape(network,outputFileP, builder).write();
		
		Collection<SimpleFeature> writtenFeatures = ShapeFileReader.getAllFeatures(outputFileP);
		Assert.assertEquals(network.getLinks().size(), writtenFeatures.size());
	}

	public void testLineStringShape() {
		String netFileName = "test/scenarios/equil/network.xml";
		String outputFileShp = getOutputDirectory() + "./network.shp";

		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		scenario.getConfig().global().setCoordinateSystem("DHDN_GK4");

		final Network network = scenario.getNetwork();
		new MatsimNetworkReader(scenario.getNetwork()).readFile(netFileName);

		FeatureGeneratorBuilderImpl builder = new FeatureGeneratorBuilderImpl(network, "DHDN_GK4");
		CoordinateReferenceSystem crs = MGC.getCRS("DHDN_GK4");
		builder.setWidthCoefficient(1);
		builder.setFeatureGeneratorPrototype(LineStringBasedFeatureGenerator.class);
		builder.setWidthCalculatorPrototype(LanesBasedWidthCalculator.class);
		builder.setCoordinateReferenceSystem(crs);
		new Links2ESRIShape(network,outputFileShp, builder).write();
		
		Collection<SimpleFeature> writtenFeatures = ShapeFileReader.getAllFeatures(outputFileShp);
		Assert.assertEquals(network.getLinks().size(), writtenFeatures.size());
	}

	public void testNodesShape() {
		String netFileName = "test/scenarios/equil/network.xml";
		String outputFileShp = getOutputDirectory() + "./network.shp";

		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());

		final Network network = scenario.getNetwork();
		new MatsimNetworkReader(scenario.getNetwork()).readFile(netFileName);

		new Nodes2ESRIShape(network,outputFileShp, "DHDN_GK4").write();
		
		Collection<SimpleFeature> writtenFeatures = ShapeFileReader.getAllFeatures(outputFileShp);
		Assert.assertEquals(network.getNodes().size(), writtenFeatures.size());
	}
}
