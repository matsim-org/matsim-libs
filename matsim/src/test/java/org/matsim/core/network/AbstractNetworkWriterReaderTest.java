/* *********************************************************************** *
 * project: org.matsim.*
 * AbstractNetworkWriterReaderTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package org.matsim.core.network;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

/**
 * An abstract class that tests if certain features of networks (e.g. specific
 * settings/attributes for links or nodes) are written to file and afterwards
 * correctly read in. The class is abstract so the tests can be reused with
 * different file formats.
 *
 * @author mrieser
 */
public abstract class AbstractNetworkWriterReaderTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();


	/**
	 * Writes the given network to the specified file.
	 *
	 * @param network
	 * @param filename
	 */
	protected abstract void writeNetwork(final Network network, final String filename);

	/**
	 * Reads a network from the specified file into the given network data structure.
	 *
	 * @param scenario
	 * @param filename
	 */
	protected abstract void readNetwork(final Scenario scenario, final String filename);

	/**
	 * Writes the given network to the specified file.
	 *
	 * @param network
	 * @param stream
	 */
	protected abstract void writeNetwork(final Network network, final OutputStream stream);

	/**
	 * Reads a network from the specified file into the given network data structure.
	 *
	 * @param scenario
	 * @param stream
	 */
	protected abstract void readNetwork(final Scenario scenario, final InputStream stream);

	@Test
	void testAllowedModes_manyUnsortedModes() {
		doTestAllowedModes(createHashSet("train", "drt", "car", "bus", "walk", "freight"),
				utils.getOutputDirectory() + "network.xml");
	}

	@Test
	void testAllowedModes_multipleModes() {
		doTestAllowedModes(createHashSet("bus", "train"), utils.getOutputDirectory() + "network.xml");
	}

	@Test
	void testAllowedModes_singleMode() {
		doTestAllowedModes(createHashSet("miv"), utils.getOutputDirectory() + "network.xml");
	}

	@Test
	void testAllowedModes_noMode() {
		doTestAllowedModes(new HashSet<String>(), utils.getOutputDirectory() + "network.xml");
	}

	@Test
	void testNodes_withoutElevation(){
		List<Node> nodes = new ArrayList<>(2);
		Node n1 = NetworkUtils.createNode(
				Id.create("1", Node.class),
				new Coord((double) 0, (double) 0));
		Node n2 = NetworkUtils.createNode(
				Id.create("2", Node.class),
				new Coord((double) 1000, (double) 0));
		nodes.add(n1);
		nodes.add(n2);
		doTestNodes(nodes, utils.getOutputDirectory() + "network.xml");
	}

	@Test
	void testNodes_withElevation(){
		List<Node> nodes = new ArrayList<>(2);
		Node n1 = NetworkUtils.createNode(
				Id.create("1", Node.class),
				new Coord((double) 0, (double) 0, (double) 0));
		Node n2 = NetworkUtils.createNode(
				Id.create("2", Node.class),
				new Coord((double) 1000, (double) 0, (double) 0));
		nodes.add(n1);
		nodes.add(n2);
		doTestNodes(nodes, utils.getOutputDirectory() + "network.xml");
	}

	@Test
	void testNodes_withAndWithoutElevation(){
		List<Node> nodes = new ArrayList<>(2);
		Node n1 = NetworkUtils.createNode(
				Id.create("1", Node.class),
				new Coord((double) 0, (double) 0));
		Node n2 = NetworkUtils.createNode(
				Id.create("2", Node.class),
				new Coord((double) 1000, (double) 0, (double) 0));
		nodes.add(n1);
		nodes.add(n2);
		doTestNodes(nodes, utils.getOutputDirectory() + "network.xml");
	}

	@Test
	void testNodes_IdSpecialCharacters() {
		Network network1 = NetworkUtils.createNetwork();
		NetworkFactory nf = network1.getFactory();
		Node nodeA1 = nf.createNode(Id.create("A & 1 <a>\"'aa", Node.class), new Coord(100, 200));
		Node nodeB1 = nf.createNode(Id.create("B & 1 <b>\"'bb", Node.class), new Coord(100, 200));
		network1.addNode(nodeA1);
		network1.addNode(nodeB1);

		Network network2 = doIOTest(network1);
		Node nodeA2 = network2.getNodes().get(nodeA1.getId());
		Node nodeB2 = network2.getNodes().get(nodeB1.getId());

		Assertions.assertNotNull(nodeA2);
		Assertions.assertNotNull(nodeB2);
		Assertions.assertNotSame(nodeA1, nodeA2);
		Assertions.assertNotSame(nodeB1, nodeB2);
	}

	@Test
	void testLinks_IdSpecialCharacters() {
		Network network1 = NetworkUtils.createNetwork();
		NetworkFactory nf = network1.getFactory();
		Node nodeA1 = nf.createNode(Id.create("A & 1 <a>\"'aa", Node.class), new Coord(100, 200));
		Node nodeB1 = nf.createNode(Id.create("B & 1 <b>\"'bb", Node.class), new Coord(100, 200));
		network1.addNode(nodeA1);
		network1.addNode(nodeB1);

		Link linkA1 = nf.createLink(Id.create("aa & 1 <A>\"'AA", Link.class), nodeA1, nodeB1);
		Link linkB1 = nf.createLink(Id.create("bb & 1 <B>\"'BB", Link.class), nodeB1, nodeA1);
		NetworkUtils.setType(linkA1, "my&special<type>\"'");
		NetworkUtils.setOrigId(linkB1, "my&special<origId>\"'");

		network1.addLink(linkA1);
		network1.addLink(linkB1);

		Network network2 = doIOTest(network1);
		Link linkA2 = network2.getLinks().get(linkA1.getId());
		Link linkB2 = network2.getLinks().get(linkB1.getId());

		Assertions.assertNotNull(linkA2);
		Assertions.assertNotNull(linkB2);
		Assertions.assertNotSame(linkA1, linkA2);
		Assertions.assertNotSame(linkB1, linkB2);
//		Assert.assertEquals(NetworkUtils.getType(linkA1), NetworkUtils.getType(linkA2)); // type is not supported anymore in v2
//		Assert.assertEquals(NetworkUtils.getOrigId(linkB1), NetworkUtils.getOrigId(linkB2)); // origId is not supported anymore in v2
	}

	@Test
	void testNetwork_NameSpecialCharacters() {
		Network network1 = NetworkUtils.createNetwork();
		network1.setName("Special & characters < are > in \" this ' name.");
		NetworkFactory nf = network1.getFactory();
		Node nodeA1 = nf.createNode(Id.create("1", Node.class), new Coord(100, 200));
		Node nodeB1 = nf.createNode(Id.create("2", Node.class), new Coord(100, 200));
		network1.addNode(nodeA1);
		network1.addNode(nodeB1);

		Link linkA1 = nf.createLink(Id.create("A", Link.class), nodeA1, nodeB1);
		Link linkB1 = nf.createLink(Id.create("B", Link.class), nodeB1, nodeA1);

		network1.addLink(linkA1);
		network1.addLink(linkB1);

		Network network2 = doIOTest(network1);

		Assertions.assertNotSame(network1, network2);
		Assertions.assertEquals(network1.getName(), network2.getName());
	}

	private void doTestAllowedModes(final Set<String> modes, final String filename) {
		Network network1 = NetworkUtils.createNetwork();
		Node n1 = NetworkUtils.createAndAddNode(network1, Id.create("1", Node.class), new Coord((double) 0, (double) 0));
		Node n2 = NetworkUtils.createAndAddNode(network1, Id.create("2", Node.class), new Coord((double) 1000, (double) 0));
		final Node fromNode = n1;
		final Node toNode = n2;
		Link l1 = NetworkUtils.createAndAddLink(network1,Id.create("1", Link.class), fromNode, toNode, 1000.0, 10.0, 3600.0, 1.0 );
		l1.setAllowedModes(modes);

		writeNetwork(network1, filename);

		File networkFile = new File(filename);
		assertTrue(networkFile.exists(), "written network file doesn't exist.");
		assertTrue(networkFile.length() > 0, "written network file seems to be empty.");

		Scenario scenario2 = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network network2 = scenario2.getNetwork();
		readNetwork(scenario2, filename);

		Link link1 = network2.getLinks().get(Id.create("1", Link.class));
		assertNotNull(link1, "link not found in read-in network.");

		Set<String> modes2 = link1.getAllowedModes();
		assertEquals(modes.size(), modes2.size(), "wrong number of allowed modes.");
		assertTrue(modes2.containsAll(modes), "wrong mode.");

		assertModesSorted(modes2);
	}

	private static void assertModesSorted(Set<String> modes) {
		List<String> originalModes = new ArrayList<>(modes);
		List<String> sortedModes = new ArrayList<>(modes);
		Collections.sort(sortedModes);

		for (int i = 0; i < originalModes.size(); i++) {
			assertEquals(sortedModes.get(i), originalModes.get(i),
					"modes are not sorted. expected '" + sortedModes + "'' but got '" + originalModes + "'");
		}
	}

	private static <T> Set<T> createHashSet(T... mode) {
		HashSet<T> set = new HashSet<T>();
        Collections.addAll(set, mode);
		return set;
	}

	private void doTestNodes(List<Node> nodes, final String filename) {
		Network network1 = NetworkUtils.createNetwork();
		for(Node n : nodes){
			network1.addNode(n);
		}

		writeNetwork(network1, filename);

		File networkFile = new File(filename);
		assertTrue(networkFile.exists(), "written network file doesn't exist.");
		assertTrue(networkFile.length() > 0, "written network file seems to be empty.");

		Scenario scenario2 = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network network2 = scenario2.getNetwork();
		readNetwork(scenario2, filename);

		for(Node n : nodes){
			assertEquals(n.getCoord(), network2.getNodes().get(n.getId()).getCoord(), "Coordinates are not equal.");
		}
	}

	private Network doIOTest(Network network1) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		writeNetwork(network1, out);

		ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());

		Scenario scenario2 = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network network2 = scenario2.getNetwork();
		readNetwork(scenario2, in);

		return network2;
	}

}
