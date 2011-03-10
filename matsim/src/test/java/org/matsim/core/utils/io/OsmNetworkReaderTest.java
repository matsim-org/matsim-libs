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

package org.matsim.core.utils.io;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import junit.framework.Assert;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.IdentityTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.xml.sax.SAXException;

/**
 * @author mrieser
 */
public class OsmNetworkReaderTest {

	@Rule public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testConversion() throws SAXException, ParserConfigurationException, IOException {
		String filename = this.utils.getClassInputDirectory() + "adliswil.osm.gz";

		Scenario sc = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network net = sc.getNetwork();

		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.CH1903_LV03);

		new OsmNetworkReader(net,ct).parse(filename);

		Assert.assertEquals("number of nodes is wrong.", 399, net.getNodes().size());
		Assert.assertEquals("number of links is wrong.", 874, net.getLinks().size());

		new NetworkCleaner().run(net);
		Assert.assertEquals("number of nodes is wrong.", 344, net.getNodes().size());
		Assert.assertEquals("number of links is wrong.", 794, net.getLinks().size());
	}

	@Test
	public void testConversionWithDetails() throws SAXException, ParserConfigurationException, IOException {
		String filename = this.utils.getClassInputDirectory() + "adliswil.osm.gz";

		Scenario sc = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network net = sc.getNetwork();

		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.CH1903_LV03);

		OsmNetworkReader reader = new OsmNetworkReader(net,ct);
		reader.setKeepPaths(true);
		reader.parse(filename);

		Assert.assertEquals("number of nodes is wrong.", 1844, net.getNodes().size());
		Assert.assertEquals("number of links is wrong.", 3537, net.getLinks().size());

		new NetworkCleaner().run(net);
		Assert.assertEquals("number of nodes is wrong.", 1561, net.getNodes().size());
		Assert.assertEquals("number of links is wrong.", 3168, net.getLinks().size());
	}

	@Test
	public void testConversionWithDetails_witMemoryOptimized() throws SAXException, ParserConfigurationException, IOException {
		String filename = this.utils.getClassInputDirectory() + "adliswil.osm.gz";

		Scenario sc = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network net = sc.getNetwork();

		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.CH1903_LV03);

		OsmNetworkReader reader = new OsmNetworkReader(net,ct);
		reader.setKeepPaths(true);
		reader.setMemoryOptimization(true);
		reader.parse(filename);

		Assert.assertEquals("number of nodes is wrong.", 1844, net.getNodes().size());
		Assert.assertEquals("number of links is wrong.", 3537, net.getLinks().size());

		new NetworkCleaner().run(net);
		Assert.assertEquals("number of nodes is wrong.", 1561, net.getNodes().size());
		Assert.assertEquals("number of links is wrong.", 3168, net.getLinks().size());
	}

	@Test
	public void testConversionWithSettings() throws SAXException, ParserConfigurationException, IOException {
		String filename = this.utils.getClassInputDirectory() + "adliswil.osm.gz";

		Scenario sc = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network net = sc.getNetwork();

		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.CH1903_LV03);

		OsmNetworkReader reader = new OsmNetworkReader(net,ct);
		reader.setHierarchyLayer(47.4, 8.5, 47.2, 8.6, 5);
		reader.setMemoryOptimization(false);
		reader.parse(filename);

		Assert.assertEquals("number of nodes is wrong.", 67, net.getNodes().size());
		Assert.assertEquals("number of links is wrong.", 122, net.getLinks().size());
		new NetworkCleaner().run(net);
		Assert.assertEquals("number of nodes is wrong.", 57, net.getNodes().size());
		Assert.assertEquals("number of links is wrong.", 114, net.getLinks().size());
	}

	@Test
	public void testConversionWithSettings_withMemoryOptimization() throws SAXException, ParserConfigurationException, IOException {
		String filename = this.utils.getClassInputDirectory() + "adliswil.osm.gz";

		Scenario sc = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network net = sc.getNetwork();

		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.CH1903_LV03);

		OsmNetworkReader reader = new OsmNetworkReader(net,ct);
		reader.setHierarchyLayer(47.4, 8.5, 47.2, 8.6, 5);
		reader.setMemoryOptimization(true);
		reader.parse(filename);

		Assert.assertEquals("number of nodes is wrong.", 67, net.getNodes().size());
		Assert.assertEquals("number of links is wrong.", 122, net.getLinks().size());
		new NetworkCleaner().run(net);
		Assert.assertEquals("number of nodes is wrong.", 57, net.getNodes().size());
		Assert.assertEquals("number of links is wrong.", 114, net.getLinks().size());
	}

	@Test
	public void testConversionWithSettingsAndDetails() throws SAXException, ParserConfigurationException, IOException {
		String filename = this.utils.getClassInputDirectory() + "adliswil.osm.gz";

		Scenario sc = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network net = sc.getNetwork();

		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.CH1903_LV03);

		OsmNetworkReader reader = new OsmNetworkReader(net,ct);
		reader.setKeepPaths(true);
		reader.setHierarchyLayer(47.4, 8.5, 47.2, 8.6, 5);
		reader.parse(filename);

		Assert.assertEquals("number of nodes is wrong.", 769, net.getNodes().size());
		Assert.assertEquals("number of links is wrong.", 1016, net.getLinks().size());
		new NetworkCleaner().run(net);
		Assert.assertEquals("number of nodes is wrong.", 441, net.getNodes().size());
		Assert.assertEquals("number of links is wrong.", 841, net.getLinks().size());
	}

	@Test
	public void testConversion_MissingNodeRef() throws SAXException, ParserConfigurationException, IOException {
		Scenario sc = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network net = sc.getNetwork();
		CoordinateTransformation ct = new IdentityTransformation();

		OsmNetworkReader reader = new OsmNetworkReader(net, ct);
		reader.setKeepPaths(true);

		String str = "<?xml version='1.0' encoding='UTF-8'?>\n" +
				"<osm version=\"0.6\" generator=\"Osmosis 0.36\">\n" +
				"  <bound box=\"0,0,90,180\" origin=\"0.37-SNAPSHOT\"/>\n" +
				"  <node id=\"1\" lat=\"10.0\" lon=\"60.0\"/>\n" +
				"  <node id=\"2\" lat=\"15.0\" lon=\"90.0\"/>\n" +
				"  <node id=\"3\" lat=\"20.0\" lon=\"120.0\"/>\n" +
				"  <node id=\"5\" lat=\"30.0\" lon=\"170.0\"/>\n" +
				"  <way id=\"1234\" version=\"6\" timestamp=\"2010-10-14T12:34:56Z\" uid=\"9876\" user=\"MATSim\" changeset=\"123456789\">\n" +
				"    <nd ref=\"0\"/>\n" +
				"    <nd ref=\"1\"/>\n" +
				"    <nd ref=\"2\"/>\n" +
				"    <nd ref=\"3\"/>\n" +
				"    <nd ref=\"4\"/>\n" +
				"    <nd ref=\"5\"/>\n" +
				"    <tag k=\"highway\" v=\"motorway\"/>\n" +
				"  </way>\n" +
				"</osm>";
		reader.parse(new ByteArrayInputStream(str.getBytes()));
		Assert.assertEquals("incomplete ways should not be converted.", 0, net.getNodes().size());
		Assert.assertEquals("incomplete ways should not be converted.", 0, net.getLinks().size());

	}
}
