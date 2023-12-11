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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.IdentityTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.testcases.MatsimTestUtils;

import java.io.ByteArrayInputStream;

/**
 * @author mrieser
 */
public class OsmNetworkReaderTest {

	@RegisterExtension private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void testConversion() {
		String filename = this.utils.getClassInputDirectory() + "adliswil.osm.gz";

		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network net = sc.getNetwork();

		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.CH1903_LV03);

		new OsmNetworkReader(net,ct).parse(filename);

		Assertions.assertEquals(399, net.getNodes().size(), "number of nodes is wrong.");
		Assertions.assertEquals(872, net.getLinks().size(), "number of links is wrong.");

		new NetworkCleaner().run(net);
		Assertions.assertEquals(344, net.getNodes().size(), "number of nodes is wrong.");
		Assertions.assertEquals(794, net.getLinks().size(), "number of links is wrong.");
	}

	@Test
	void testConversionWithDetails() {
		String filename = this.utils.getClassInputDirectory() + "adliswil.osm.gz";

		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network net = sc.getNetwork();

		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.CH1903_LV03);

		OsmNetworkReader reader = new OsmNetworkReader(net,ct);
		reader.setKeepPaths(true);
		reader.parse(filename);

		Assertions.assertEquals(1844, net.getNodes().size(), "number of nodes is wrong.");
		Assertions.assertEquals(3535, net.getLinks().size(), "number of links is wrong.");

		new NetworkCleaner().run(net);
		Assertions.assertEquals(1561, net.getNodes().size(), "number of nodes is wrong.");
		Assertions.assertEquals(3168, net.getLinks().size(), "number of links is wrong.");
	}

	@Test
	void testConversionWithDetails_witMemoryOptimized() {
		String filename = this.utils.getClassInputDirectory() + "adliswil.osm.gz";

		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network net = sc.getNetwork();

		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.CH1903_LV03);

		OsmNetworkReader reader = new OsmNetworkReader(net,ct);
		reader.setKeepPaths(true);
		reader.setMemoryOptimization(true);
		reader.parse(filename);

		Assertions.assertEquals(1844, net.getNodes().size(), "number of nodes is wrong.");
		Assertions.assertEquals(3535, net.getLinks().size(), "number of links is wrong.");

		new NetworkCleaner().run(net);
		Assertions.assertEquals(1561, net.getNodes().size(), "number of nodes is wrong.");
		Assertions.assertEquals(3168, net.getLinks().size(), "number of links is wrong.");
	}

	@Test
	void testConversionWithSettings() {
		String filename = this.utils.getClassInputDirectory() + "adliswil.osm.gz";

		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network net = sc.getNetwork();

		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.CH1903_LV03);

		OsmNetworkReader reader = new OsmNetworkReader(net,ct);
		reader.setHierarchyLayer(47.4, 8.5, 47.2, 8.6, 5);
		reader.setMemoryOptimization(false);
		reader.parse(filename);

		Assertions.assertEquals(67, net.getNodes().size(), "number of nodes is wrong.");
		Assertions.assertEquals(122, net.getLinks().size(), "number of links is wrong.");
		new NetworkCleaner().run(net);
		Assertions.assertEquals(57, net.getNodes().size(), "number of nodes is wrong.");
		Assertions.assertEquals(114, net.getLinks().size(), "number of links is wrong.");
	}

	@Test
	void testConversionWithSettings_withMemoryOptimization() {
		String filename = this.utils.getClassInputDirectory() + "adliswil.osm.gz";

		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network net = sc.getNetwork();

		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.CH1903_LV03);

		OsmNetworkReader reader = new OsmNetworkReader(net,ct);
		reader.setHierarchyLayer(47.4, 8.5, 47.2, 8.6, 5);
		reader.setMemoryOptimization(true);
		reader.parse(filename);

		Assertions.assertEquals(67, net.getNodes().size(), "number of nodes is wrong.");
		Assertions.assertEquals(122, net.getLinks().size(), "number of links is wrong.");
		new NetworkCleaner().run(net);
		Assertions.assertEquals(57, net.getNodes().size(), "number of nodes is wrong.");
		Assertions.assertEquals(114, net.getLinks().size(), "number of links is wrong.");
	}

	@Test
	void testConversionWithSettingsAndDetails() {
		String filename = this.utils.getClassInputDirectory() + "adliswil.osm.gz";

		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network net = sc.getNetwork();

		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.CH1903_LV03);

		OsmNetworkReader reader = new OsmNetworkReader(net,ct);
		reader.setKeepPaths(true);
		reader.setHierarchyLayer(47.4, 8.5, 47.2, 8.6, 5);
		reader.parse(filename);

		Assertions.assertEquals(769, net.getNodes().size(), "number of nodes is wrong.");
		Assertions.assertEquals(1016, net.getLinks().size(), "number of links is wrong.");
		new NetworkCleaner().run(net);
		Assertions.assertEquals(441, net.getNodes().size(), "number of nodes is wrong.");
		Assertions.assertEquals(841, net.getLinks().size(), "number of links is wrong.");
	}

	@Test
	void testConversion_MissingNodeRef() {
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
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
		reader.parse(() -> new ByteArrayInputStream(str.getBytes()));
		Assertions.assertEquals(0, net.getNodes().size(), "incomplete ways should not be converted.");
		Assertions.assertEquals(0, net.getLinks().size(), "incomplete ways should not be converted.");
	}

	@Test
	void testConversion_maxspeeds() {
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network net = sc.getNetwork();
		CoordinateTransformation ct = new IdentityTransformation();

		OsmNetworkReader reader = new OsmNetworkReader(net, ct);
		reader.setKeepPaths(true);
		reader.setHighwayDefaults(1, "motorway", 1, 50.0/3.6, 1.0, 2000.0);

		String str = "<?xml version='1.0' encoding='UTF-8'?>\n" +
				"<osm version=\"0.6\" generator=\"Osmosis 0.36\">\n" +
				"  <bound box=\"0,0,90,180\" origin=\"0.37-SNAPSHOT\"/>\n" +
				"  <node id=\"1\" lat=\"10.0\" lon=\"60.0\"/>\n" +
				"  <node id=\"2\" lat=\"15.0\" lon=\"90.0\"/>\n" +
				"  <node id=\"3\" lat=\"20.0\" lon=\"120.0\"/>\n" +
				"  <node id=\"4\" lat=\"25.0\" lon=\"90.0\"/>\n" +
				"  <node id=\"5\" lat=\"30.0\" lon=\"60.0\"/>\n" +
				"  <way id=\"1\" version=\"6\" timestamp=\"2010-10-14T12:34:56Z\" uid=\"9876\" user=\"MATSim\" changeset=\"123456789\">\n" +
				"    <nd ref=\"1\"/>\n" +
				"    <nd ref=\"2\"/>\n" +
				"    <tag k=\"highway\" v=\"motorway\"/>\n" +
				"  </way>\n" +
				"  <way id=\"2\" version=\"6\" timestamp=\"2010-10-14T12:34:56Z\" uid=\"9876\" user=\"MATSim\" changeset=\"123456789\">\n" +
				"    <nd ref=\"2\"/>\n" +
				"    <nd ref=\"3\"/>\n" +
				"    <tag k=\"highway\" v=\"motorway\"/>\n" +
				"    <tag k=\"maxspeed\" v=\"40\"/>\n" + // lower speed limit than default
				"  </way>\n" +
				"  <way id=\"3\" version=\"6\" timestamp=\"2010-10-14T12:34:56Z\" uid=\"9876\" user=\"MATSim\" changeset=\"123456789\">\n" +
				"    <nd ref=\"3\"/>\n" +
				"    <nd ref=\"4\"/>\n" +
				"    <tag k=\"highway\" v=\"motorway\"/>\n" +
				"    <tag k=\"maxspeed\" v=\"60\"/>\n" + // higher speed limit than default
				"  </way>\n" +
				"</osm>";
		reader.parse(() -> new ByteArrayInputStream(str.getBytes()));

		/* this creates 6 links:
		 * - links 1 & 2: for way 1, in both directions
		 * - links 3 & 4: for way 2, in both directions
		 * - links 5 & 6: for way 3, in both directions
		 */

		Link link1 = net.getLinks().get(Id.create("1", Link.class));
		Link link3 = net.getLinks().get(Id.create("3", Link.class));
		Link link5 = net.getLinks().get(Id.create("5", Link.class));
		Assertions.assertNotNull(link1, "Could not find converted link 1.");
		Assertions.assertNotNull(link3, "Could not find converted link 3");
		Assertions.assertNotNull(link5, "Could not find converted link 5");
		Assertions.assertEquals(50.0/3.6, link1.getFreespeed(), 1e-8);
		Assertions.assertEquals(40.0/3.6, link3.getFreespeed(), 1e-8);
		Assertions.assertEquals(60.0/3.6, link5.getFreespeed(), 1e-8);
	}

	/**
	 * Tests that the conversion does not fail if a way does not contain any node. This might
	 * happen if the osm-file was edited, e.g. with JOSM, and a link was deleted. Then, the way
	 * still exists, but marked as deleted, and all nodes removed from it.
	 * Reported by jjoubert,15nov2012.
	 */
	@Test
	void testConversion_emptyWay() {
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network net = sc.getNetwork();
		CoordinateTransformation ct = new IdentityTransformation();

		OsmNetworkReader reader = new OsmNetworkReader(net, ct);
		reader.setKeepPaths(true);
		reader.setHighwayDefaults(1, "motorway", 1, 50.0/3.6, 1.0, 2000.0);

		String str = "<?xml version='1.0' encoding='UTF-8'?>\n" +
				"<osm version=\"0.6\" generator=\"Osmosis 0.36\">\n" +
				"  <bound box=\"0,0,90,180\" origin=\"0.37-SNAPSHOT\"/>\n" +
				"  <node id=\"1\" lat=\"10.0\" lon=\"60.0\"/>\n" +
				"  <node id=\"2\" lat=\"15.0\" lon=\"90.0\"/>\n" +
				"  <node id=\"3\" lat=\"20.0\" lon=\"120.0\"/>\n" +
				"  <node id=\"4\" lat=\"25.0\" lon=\"90.0\"/>\n" +
				"  <node id=\"5\" lat=\"30.0\" lon=\"60.0\"/>\n" +
				"  <way id=\"1\" version=\"6\" timestamp=\"2010-10-14T12:34:56Z\" uid=\"9876\" user=\"MATSim\" changeset=\"123456789\">\n" +
				"    <nd ref=\"1\"/>\n" +
				"    <nd ref=\"2\"/>\n" +
				"    <tag k=\"highway\" v=\"motorway\"/>\n" +
				"  </way>\n" +
				"  <way id=\"2\" version=\"6\" timestamp=\"2010-10-14T12:34:56Z\" uid=\"9876\" user=\"MATSim\" changeset=\"123456789\">\n" +
				"    <nd ref=\"2\"/>\n" +
				"    <nd ref=\"3\"/>\n" +
				"    <tag k=\"highway\" v=\"motorway\"/>\n" +
				"  </way>\n" +
				"  <way id=\"3\" version=\"6\" timestamp=\"2010-10-14T12:34:56Z\" uid=\"9876\" user=\"MATSim\" changeset=\"123456789\">\n" +
				"    <tag k=\"highway\" v=\"motorway\"/>\n" +
				"  </way>\n" +
				"</osm>";
		reader.parse(() -> new ByteArrayInputStream(str.getBytes()));

		/* this creates 4 links:
		 * - links 1 & 2: for way 1, in both directions
		 * - links 3 & 4: for way 2, in both directions
		 */

		Link link1 = net.getLinks().get(Id.create("1", Link.class));
		Link link3 = net.getLinks().get(Id.create("3", Link.class));
		Assertions.assertNotNull(link1, "Could not find converted link 1.");
		Assertions.assertNotNull(link3, "Could not find converted link 3");
		Assertions.assertNull(net.getLinks().get(Id.create("5", Link.class)));
	}
}
