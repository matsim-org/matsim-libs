/* *********************************************************************** *
 * project: org.matsim.*
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

package org.matsim.core.network.algorithms;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.algorithms.NetworkExpandNode.TurnInfo;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;

/**
 * @author mrieser / senozon
 */
public class NetworkExpandNodeTest {

	@Test
	void testExpandNode() {
		Fixture f = new Fixture();
		f.createNetwork_ThreeWayIntersection();
		
		NetworkExpandNode exp = new NetworkExpandNode(f.scenario.getNetwork(), 25, 5);
		ArrayList<TurnInfo> turns = new ArrayList<TurnInfo>();
		turns.add(new TurnInfo(Id.create("1", Link.class), Id.create("6", Link.class)));
		turns.add(new TurnInfo(Id.create("3", Link.class), Id.create("6", Link.class)));
		turns.add(new TurnInfo(Id.create("5", Link.class), Id.create("2", Link.class)));
		turns.add(new TurnInfo(Id.create("5", Link.class), Id.create("4", Link.class)));
		
		exp.expandNode(Id.create("3", Node.class), turns);
		Network n = f.scenario.getNetwork();
		Assertions.assertEquals(12, n.getLinks().size());
		Assertions.assertEquals(10, n.getNodes().size());
		Assertions.assertNotNull(findLinkBetween(n, Id.create("1", Link.class), Id.create("6", Link.class)));
		Assertions.assertNotNull(findLinkBetween(n, Id.create("3", Link.class), Id.create("6", Link.class)));
		Assertions.assertNotNull(findLinkBetween(n, Id.create("5", Link.class), Id.create("2", Link.class)));
		Assertions.assertNotNull(findLinkBetween(n, Id.create("5", Link.class), Id.create("4", Link.class)));
		Assertions.assertNull(findLinkBetween(n, Id.create("1", Link.class), Id.create("2", Link.class)));
		Assertions.assertNull(findLinkBetween(n, Id.create("1", Link.class), Id.create("4", Link.class)));
		Assertions.assertNull(findLinkBetween(n, Id.create("3", Link.class), Id.create("2", Link.class)));
		Assertions.assertNull(findLinkBetween(n, Id.create("3", Link.class), Id.create("4", Link.class)));
		Assertions.assertNull(findLinkBetween(n, Id.create("5", Link.class), Id.create("6", Link.class)));
		
		// test correct attributes on new links
		Link l = findLinkBetween(n, Id.create("1", Link.class), Id.create("6", Link.class));
		Assertions.assertEquals(1800.0, l.getCapacity(), 1e-8, "Capacity attribute is not correct");
		Assertions.assertEquals(2.0, l.getNumberOfLanes(), 1e-8, "Number of lanes is not correct");
		Assertions.assertEquals(10.0, l.getFreespeed(), 1e-8, "Freespeed is not correct");
		Set<String> modes = l.getAllowedModes();
		Assertions.assertEquals(2, modes.size(), "Allowed modes are not correct");
		Assertions.assertTrue(modes.contains(TransportMode.walk));
		Assertions.assertTrue(modes.contains(TransportMode.car));

		// test correct attributes on modified in-links
		l = n.getLinks().get(Id.create("3", Link.class));
		Assertions.assertEquals(1800.0, l.getCapacity(), 1e-8, "Capacity attribute is not correct");
		Assertions.assertEquals(2.0, l.getNumberOfLanes(), 1e-8, "Number of lanes is not correct");
		Assertions.assertEquals(10.0, l.getFreespeed(), 1e-8, "Freespeed is not correct");
		
		modes = l.getAllowedModes();
		Assertions.assertEquals(2, modes.size(), "Allowed modes are not correct");
		Assertions.assertTrue(modes.contains(TransportMode.walk));
		Assertions.assertTrue(modes.contains(TransportMode.car));

		// test correct attributes on modified out-links
		l = n.getLinks().get(Id.create("6", Link.class));
		Assertions.assertEquals(1800.0, l.getCapacity(), 1e-8, "Capacity attribute is not correct");
		Assertions.assertEquals(2.0, l.getNumberOfLanes(), 1e-8, "Number of lanes is not correct");
		Assertions.assertEquals(10.0, l.getFreespeed(), 1e-8, "Freespeed is not correct");
		
		modes = l.getAllowedModes();
		Assertions.assertEquals(2, modes.size(), "Allowed modes are not correct");
		Assertions.assertTrue(modes.contains(TransportMode.walk));
		Assertions.assertTrue(modes.contains(TransportMode.car));
		
		// test coordinates of new nodes
		l = n.getLinks().get(Id.create("1", Link.class));
		Coord c = l.getToNode().getCoord();
		Assertions.assertFalse(Double.isNaN(c.getX()));
		Assertions.assertFalse(Double.isNaN(c.getY()));
		Assertions.assertFalse(Double.isInfinite(c.getX()));
		Assertions.assertFalse(Double.isInfinite(c.getY()));
		Assertions.assertTrue(CoordUtils.calcEuclideanDistance(c, new Coord((double) 1000, (double) 0)) < 30);

		l = n.getLinks().get(Id.create("2", Link.class));
		c = l.getFromNode().getCoord();
		Assertions.assertFalse(Double.isNaN(c.getX()));
		Assertions.assertFalse(Double.isNaN(c.getY()));
		Assertions.assertFalse(Double.isInfinite(c.getX()));
		Assertions.assertFalse(Double.isInfinite(c.getY()));
		Assertions.assertTrue(CoordUtils.calcEuclideanDistance(c, new Coord((double) 1000, (double) 0)) < 30);

		l = n.getLinks().get(Id.create("3", Link.class));
		c = l.getToNode().getCoord();
		Assertions.assertFalse(Double.isNaN(c.getX()));
		Assertions.assertFalse(Double.isNaN(c.getY()));
		Assertions.assertFalse(Double.isInfinite(c.getX()));
		Assertions.assertFalse(Double.isInfinite(c.getY()));
		Assertions.assertTrue(CoordUtils.calcEuclideanDistance(c, new Coord((double) 1000, (double) 0)) < 30);
		
		l = n.getLinks().get(Id.create("4", Link.class));
		c = l.getFromNode().getCoord();
		Assertions.assertFalse(Double.isNaN(c.getX()));
		Assertions.assertFalse(Double.isNaN(c.getY()));
		Assertions.assertFalse(Double.isInfinite(c.getX()));
		Assertions.assertFalse(Double.isInfinite(c.getY()));
		Assertions.assertTrue(CoordUtils.calcEuclideanDistance(c, new Coord((double) 1000, (double) 0)) < 30);
		
		l = n.getLinks().get(Id.create("5", Link.class));
		c = l.getToNode().getCoord();
		Assertions.assertFalse(Double.isNaN(c.getX()));
		Assertions.assertFalse(Double.isNaN(c.getY()));
		Assertions.assertFalse(Double.isInfinite(c.getX()));
		Assertions.assertFalse(Double.isInfinite(c.getY()));
		Assertions.assertTrue(CoordUtils.calcEuclideanDistance(c, new Coord((double) 1000, (double) 0)) < 30);
		
		l = n.getLinks().get(Id.create("6", Link.class));
		c = l.getFromNode().getCoord();
		Assertions.assertFalse(Double.isNaN(c.getX()));
		Assertions.assertFalse(Double.isNaN(c.getY()));
		Assertions.assertFalse(Double.isInfinite(c.getX()));
		Assertions.assertFalse(Double.isInfinite(c.getY()));
		Assertions.assertTrue(CoordUtils.calcEuclideanDistance(c, new Coord((double) 1000, (double) 0)) < 30);
	}

	@Test
	void testExpandNode_sameCoordinateLinks() {
		Fixture f = new Fixture();
		f.createNetwork_ThreeWayIntersection();
		Coord c = f.scenario.getNetwork().getNodes().get(Id.create("3", Node.class)).getCoord();
//		f.scenario.getNetwork().getNodes().get(Id.create("1", Node.class)).getCoord().setXY(c.getX(), c.getY()); // move it on top of node 3
		f.scenario.getNetwork().getNodes().get(Id.create("1", Node.class)).setCoord(c); // move it on top of node 3
		
		
		NetworkExpandNode exp = new NetworkExpandNode(f.scenario.getNetwork(), 25, 5);
		ArrayList<TurnInfo> turns = new ArrayList<TurnInfo>();
		turns.add(new TurnInfo(Id.create("1", Link.class), Id.create("6", Link.class)));
		turns.add(new TurnInfo(Id.create("3", Link.class), Id.create("6", Link.class)));
		turns.add(new TurnInfo(Id.create("5", Link.class), Id.create("2", Link.class)));
		turns.add(new TurnInfo(Id.create("5", Link.class), Id.create("4", Link.class)));
		
		exp.expandNode(Id.create("3", Node.class), turns);
		Network n = f.scenario.getNetwork();
		Assertions.assertEquals(12, n.getLinks().size());
		Assertions.assertEquals(10, n.getNodes().size());
		Assertions.assertNotNull(findLinkBetween(n, Id.create("1", Link.class), Id.create("6", Link.class)));
		Assertions.assertNotNull(findLinkBetween(n, Id.create("3", Link.class), Id.create("6", Link.class)));
		Assertions.assertNotNull(findLinkBetween(n, Id.create("5", Link.class), Id.create("2", Link.class)));
		Assertions.assertNotNull(findLinkBetween(n, Id.create("5", Link.class), Id.create("4", Link.class)));
		Assertions.assertNull(findLinkBetween(n, Id.create("1", Link.class), Id.create("2", Link.class)));
		Assertions.assertNull(findLinkBetween(n, Id.create("1", Link.class), Id.create("4", Link.class)));
		Assertions.assertNull(findLinkBetween(n, Id.create("3", Link.class), Id.create("2", Link.class)));
		Assertions.assertNull(findLinkBetween(n, Id.create("3", Link.class), Id.create("4", Link.class)));
		Assertions.assertNull(findLinkBetween(n, Id.create("5", Link.class), Id.create("6", Link.class)));
		
		// test correct attributes on new links
		Link l = findLinkBetween(n, Id.create("1", Link.class), Id.create("6", Link.class));
		Assertions.assertEquals(1800.0, l.getCapacity(), 1e-8, "Capacity attribute is not correct");
		Assertions.assertEquals(2.0, l.getNumberOfLanes(), 1e-8, "Number of lanes is not correct");
		Assertions.assertEquals(10.0, l.getFreespeed(), 1e-8, "Freespeed is not correct");
		Set<String> modes = l.getAllowedModes();
		Assertions.assertEquals(2, modes.size(), "Allowed modes are not correct");
		Assertions.assertTrue(modes.contains(TransportMode.walk));
		Assertions.assertTrue(modes.contains(TransportMode.car));
		
		// test correct attributes on modified in-links
		l = n.getLinks().get(Id.create("3", Link.class));
		Assertions.assertEquals(1800.0, l.getCapacity(), 1e-8, "Capacity attribute is not correct");
		Assertions.assertEquals(2.0, l.getNumberOfLanes(), 1e-8, "Number of lanes is not correct");
		Assertions.assertEquals(10.0, l.getFreespeed(), 1e-8, "Freespeed is not correct");
		
		modes = l.getAllowedModes();
		Assertions.assertEquals(2, modes.size(), "Allowed modes are not correct");
		Assertions.assertTrue(modes.contains(TransportMode.walk));
		Assertions.assertTrue(modes.contains(TransportMode.car));
		
		// test correct attributes on modified out-links
		l = n.getLinks().get(Id.create("6", Link.class));
		Assertions.assertEquals(1800.0, l.getCapacity(), 1e-8, "Capacity attribute is not correct");
		Assertions.assertEquals(2.0, l.getNumberOfLanes(), 1e-8, "Number of lanes is not correct");
		Assertions.assertEquals(10.0, l.getFreespeed(), 1e-8, "Freespeed is not correct");
		
		modes = l.getAllowedModes();
		Assertions.assertEquals(2, modes.size(), "Allowed modes are not correct");
		Assertions.assertTrue(modes.contains(TransportMode.walk));
		Assertions.assertTrue(modes.contains(TransportMode.car));
		
		// test coordinates of new nodes
		l = n.getLinks().get(Id.create("1", Link.class));
		c = l.getToNode().getCoord();
		Assertions.assertFalse(Double.isNaN(c.getX()));
		Assertions.assertFalse(Double.isNaN(c.getY()));
		Assertions.assertFalse(Double.isInfinite(c.getX()));
		Assertions.assertFalse(Double.isInfinite(c.getY()));
		Assertions.assertTrue(CoordUtils.calcEuclideanDistance(c, new Coord((double) 1000, (double) 0)) < 30);
		
		l = n.getLinks().get(Id.create("2", Link.class));
		c = l.getFromNode().getCoord();
		Assertions.assertFalse(Double.isNaN(c.getX()));
		Assertions.assertFalse(Double.isNaN(c.getY()));
		Assertions.assertFalse(Double.isInfinite(c.getX()));
		Assertions.assertFalse(Double.isInfinite(c.getY()));
		Assertions.assertTrue(CoordUtils.calcEuclideanDistance(c, new Coord((double) 1000, (double) 0)) < 30);
		
		l = n.getLinks().get(Id.create("3", Link.class));
		c = l.getToNode().getCoord();
		Assertions.assertFalse(Double.isNaN(c.getX()));
		Assertions.assertFalse(Double.isNaN(c.getY()));
		Assertions.assertFalse(Double.isInfinite(c.getX()));
		Assertions.assertFalse(Double.isInfinite(c.getY()));
		Assertions.assertTrue(CoordUtils.calcEuclideanDistance(c, new Coord((double) 1000, (double) 0)) < 30);
		
		l = n.getLinks().get(Id.create("4", Link.class));
		c = l.getFromNode().getCoord();
		Assertions.assertFalse(Double.isNaN(c.getX()));
		Assertions.assertFalse(Double.isNaN(c.getY()));
		Assertions.assertFalse(Double.isInfinite(c.getX()));
		Assertions.assertFalse(Double.isInfinite(c.getY()));
		Assertions.assertTrue(CoordUtils.calcEuclideanDistance(c, new Coord((double) 1000, (double) 0)) < 30);
		
		l = n.getLinks().get(Id.create("5", Link.class));
		c = l.getToNode().getCoord();
		Assertions.assertFalse(Double.isNaN(c.getX()));
		Assertions.assertFalse(Double.isNaN(c.getY()));
		Assertions.assertFalse(Double.isInfinite(c.getX()));
		Assertions.assertFalse(Double.isInfinite(c.getY()));
		Assertions.assertTrue(CoordUtils.calcEuclideanDistance(c, new Coord((double) 1000, (double) 0)) < 30);
		
		l = n.getLinks().get(Id.create("6", Link.class));
		c = l.getFromNode().getCoord();
		Assertions.assertFalse(Double.isNaN(c.getX()));
		Assertions.assertFalse(Double.isNaN(c.getY()));
		Assertions.assertFalse(Double.isInfinite(c.getX()));
		Assertions.assertFalse(Double.isInfinite(c.getY()));
		Assertions.assertTrue(CoordUtils.calcEuclideanDistance(c, new Coord((double) 1000, (double) 0)) < 30);
	}

	@Test
	void testExpandNode_specificModes() {
		Fixture f = new Fixture();
		f.createNetwork_ThreeWayIntersection();
		
		Set<String> carOnly = new HashSet<String>();
		carOnly.add(TransportMode.car);
		Set<String> walkOnly = new HashSet<String>();
		walkOnly.add(TransportMode.walk);
		
		NetworkExpandNode exp = new NetworkExpandNode(f.scenario.getNetwork(), 25, 5);
		ArrayList<TurnInfo> turns = new ArrayList<TurnInfo>();
		turns.add(new TurnInfo(Id.create("1", Link.class), Id.create("6", Link.class)));
		turns.add(new TurnInfo(Id.create("3", Link.class), Id.create("6", Link.class)));
		turns.add(new TurnInfo(Id.create("5", Link.class), Id.create("2", Link.class), walkOnly));
		turns.add(new TurnInfo(Id.create("5", Link.class), Id.create("4", Link.class), carOnly));
		
		exp.expandNode(Id.create("3", Node.class), turns);
		Network n = f.scenario.getNetwork();
		Assertions.assertEquals(12, n.getLinks().size());
		Assertions.assertEquals(10, n.getNodes().size());
		Assertions.assertNotNull(findLinkBetween(n, Id.create("1", Link.class), Id.create("6", Link.class)));
		Assertions.assertNotNull(findLinkBetween(n, Id.create("3", Link.class), Id.create("6", Link.class)));
		Assertions.assertNotNull(findLinkBetween(n, Id.create("5", Link.class), Id.create("2", Link.class)));
		Assertions.assertNotNull(findLinkBetween(n, Id.create("5", Link.class), Id.create("4", Link.class)));
		Assertions.assertNull(findLinkBetween(n, Id.create("1", Link.class), Id.create("2", Link.class)));
		Assertions.assertNull(findLinkBetween(n, Id.create("1", Link.class), Id.create("4", Link.class)));
		Assertions.assertNull(findLinkBetween(n, Id.create("3", Link.class), Id.create("2", Link.class)));
		Assertions.assertNull(findLinkBetween(n, Id.create("3", Link.class), Id.create("4", Link.class)));
		Assertions.assertNull(findLinkBetween(n, Id.create("5", Link.class), Id.create("6", Link.class)));
		
		// test correct attributes on new links
		Link l = findLinkBetween(n, Id.create("1", Link.class), Id.create("6", Link.class));
		Assertions.assertEquals(1800.0, l.getCapacity(), 1e-8, "Capacity attribute is not correct");
		Assertions.assertEquals(2.0, l.getNumberOfLanes(), 1e-8, "Number of lanes is not correct");
		Assertions.assertEquals(10.0, l.getFreespeed(), 1e-8, "Freespeed is not correct");
		Set<String> modes = l.getAllowedModes();
		Assertions.assertEquals(2, modes.size(), "Allowed modes are not correct");
		Assertions.assertTrue(modes.contains(TransportMode.walk));
		Assertions.assertTrue(modes.contains(TransportMode.car));

		l = findLinkBetween(n, Id.create("5", Link.class), Id.create("2", Link.class));
		modes = l.getAllowedModes();
		Assertions.assertEquals(1, modes.size(), "Allowed modes are not correct");
		Assertions.assertTrue(modes.contains(TransportMode.walk));

		l = findLinkBetween(n, Id.create("5", Link.class), Id.create("4", Link.class));
		modes = l.getAllowedModes();
		Assertions.assertEquals(1, modes.size(), "Allowed modes are not correct");
		Assertions.assertTrue(modes.contains(TransportMode.car));

		// test correct attributes on modified in-links
		l = n.getLinks().get(Id.create("3", Link.class));
		Assertions.assertEquals(1800.0, l.getCapacity(), 1e-8, "Capacity attribute is not correct");
		Assertions.assertEquals(2.0, l.getNumberOfLanes(), 1e-8, "Number of lanes is not correct");
		Assertions.assertEquals(10.0, l.getFreespeed(), 1e-8, "Freespeed is not correct");
		
		modes = l.getAllowedModes();
		Assertions.assertEquals(2, modes.size(), "Allowed modes are not correct");
		Assertions.assertTrue(modes.contains(TransportMode.walk));
		Assertions.assertTrue(modes.contains(TransportMode.car));

		// test correct attributes on modified out-links
		l = n.getLinks().get(Id.create("6", Link.class));
		Assertions.assertEquals(1800.0, l.getCapacity(), 1e-8, "Capacity attribute is not correct");
		Assertions.assertEquals(2.0, l.getNumberOfLanes(), 1e-8, "Number of lanes is not correct");
		Assertions.assertEquals(10.0, l.getFreespeed(), 1e-8, "Freespeed is not correct");
		
		modes = l.getAllowedModes();
		Assertions.assertEquals(2, modes.size(), "Allowed modes are not correct");
		Assertions.assertTrue(modes.contains(TransportMode.walk));
		Assertions.assertTrue(modes.contains(TransportMode.car));
		
		// test coordinates of new nodes
		l = n.getLinks().get(Id.create("1", Link.class));
		Coord c = l.getToNode().getCoord();
		Assertions.assertFalse(Double.isNaN(c.getX()));
		Assertions.assertFalse(Double.isNaN(c.getY()));
		Assertions.assertFalse(Double.isInfinite(c.getX()));
		Assertions.assertFalse(Double.isInfinite(c.getY()));
		Assertions.assertTrue(CoordUtils.calcEuclideanDistance(c, new Coord((double) 1000, (double) 0)) < 30);

		l = n.getLinks().get(Id.create("2", Link.class));
		c = l.getFromNode().getCoord();
		Assertions.assertFalse(Double.isNaN(c.getX()));
		Assertions.assertFalse(Double.isNaN(c.getY()));
		Assertions.assertFalse(Double.isInfinite(c.getX()));
		Assertions.assertFalse(Double.isInfinite(c.getY()));
		Assertions.assertTrue(CoordUtils.calcEuclideanDistance(c, new Coord((double) 1000, (double) 0)) < 30);

		l = n.getLinks().get(Id.create("3", Link.class));
		c = l.getToNode().getCoord();
		Assertions.assertFalse(Double.isNaN(c.getX()));
		Assertions.assertFalse(Double.isNaN(c.getY()));
		Assertions.assertFalse(Double.isInfinite(c.getX()));
		Assertions.assertFalse(Double.isInfinite(c.getY()));
		Assertions.assertTrue(CoordUtils.calcEuclideanDistance(c, new Coord((double) 1000, (double) 0)) < 30);
		
		l = n.getLinks().get(Id.create("4", Link.class));
		c = l.getFromNode().getCoord();
		Assertions.assertFalse(Double.isNaN(c.getX()));
		Assertions.assertFalse(Double.isNaN(c.getY()));
		Assertions.assertFalse(Double.isInfinite(c.getX()));
		Assertions.assertFalse(Double.isInfinite(c.getY()));
		Assertions.assertTrue(CoordUtils.calcEuclideanDistance(c, new Coord((double) 1000, (double) 0)) < 30);
		
		l = n.getLinks().get(Id.create("5", Link.class));
		c = l.getToNode().getCoord();
		Assertions.assertFalse(Double.isNaN(c.getX()));
		Assertions.assertFalse(Double.isNaN(c.getY()));
		Assertions.assertFalse(Double.isInfinite(c.getX()));
		Assertions.assertFalse(Double.isInfinite(c.getY()));
		Assertions.assertTrue(CoordUtils.calcEuclideanDistance(c, new Coord((double) 1000, (double) 0)) < 30);
		
		l = n.getLinks().get(Id.create("6", Link.class));
		c = l.getFromNode().getCoord();
		Assertions.assertFalse(Double.isNaN(c.getX()));
		Assertions.assertFalse(Double.isNaN(c.getY()));
		Assertions.assertFalse(Double.isInfinite(c.getX()));
		Assertions.assertFalse(Double.isInfinite(c.getY()));
		Assertions.assertTrue(CoordUtils.calcEuclideanDistance(c, new Coord((double) 1000, (double) 0)) < 30);
	}

	@Test
	void testTurnsAreSameAsSingleNode_IncludeUTurns() {
		Fixture f = new Fixture();
		f.createNetwork_ThreeWayIntersection();
		
		Set<String> carOnly = new HashSet<String>();
		carOnly.add(TransportMode.car);
		Set<String> walkOnly = new HashSet<String>();
		walkOnly.add(TransportMode.walk);
		Set<String> walkCar = new HashSet<String>();
		walkCar.add(TransportMode.walk);
		walkCar.add(TransportMode.car);

		NetworkExpandNode exp = new NetworkExpandNode(f.scenario.getNetwork(), 25, 5);

		ArrayList<TurnInfo> turns = new ArrayList<TurnInfo>();
		turns.add(new TurnInfo(Id.create("1", Link.class), Id.create("6", Link.class)));
		turns.add(new TurnInfo(Id.create("3", Link.class), Id.create("6", Link.class)));
		turns.add(new TurnInfo(Id.create("5", Link.class), Id.create("2", Link.class), walkOnly));
		turns.add(new TurnInfo(Id.create("5", Link.class), Id.create("4", Link.class), carOnly));

		Id<Node> nodeId = Id.create("3", Node.class);
		Assertions.assertFalse(exp.turnsAreSameAsSingleNode(nodeId, turns, false));

		turns.clear();
		turns.add(new TurnInfo(Id.create("1", Link.class), Id.create("2", Link.class)));
		turns.add(new TurnInfo(Id.create("1", Link.class), Id.create("4", Link.class)));
		turns.add(new TurnInfo(Id.create("1", Link.class), Id.create("6", Link.class)));
		turns.add(new TurnInfo(Id.create("3", Link.class), Id.create("2", Link.class), walkCar));
		turns.add(new TurnInfo(Id.create("3", Link.class), Id.create("4", Link.class), walkCar));
		turns.add(new TurnInfo(Id.create("3", Link.class), Id.create("6", Link.class), walkCar));
		turns.add(new TurnInfo(Id.create("5", Link.class), Id.create("2", Link.class)));
		turns.add(new TurnInfo(Id.create("5", Link.class), Id.create("4", Link.class)));
		turns.add(new TurnInfo(Id.create("5", Link.class), Id.create("6", Link.class)));

		Assertions.assertTrue(exp.turnsAreSameAsSingleNode(nodeId, turns, false));
	}

	@Test
	void testTurnsAreSameAsSingleNode_IgnoreUTurns() {
		Fixture f = new Fixture();
		f.createNetwork_ThreeWayIntersection();
		
		Set<String> emptySet = new HashSet<String>();
		Set<String> carOnly = new HashSet<String>();
		carOnly.add(TransportMode.car);
		Set<String> walkOnly = new HashSet<String>();
		walkOnly.add(TransportMode.walk);
		Set<String> walkCar = new HashSet<String>();
		walkCar.add(TransportMode.walk);
		walkCar.add(TransportMode.car);
		
		NetworkExpandNode exp = new NetworkExpandNode(f.scenario.getNetwork(), 25, 5);
		
		ArrayList<TurnInfo> turns = new ArrayList<TurnInfo>();
		turns.add(new TurnInfo(Id.create("1", Link.class), Id.create("6", Link.class)));
		turns.add(new TurnInfo(Id.create("3", Link.class), Id.create("6", Link.class)));
		turns.add(new TurnInfo(Id.create("5", Link.class), Id.create("2", Link.class), walkOnly));
		turns.add(new TurnInfo(Id.create("5", Link.class), Id.create("4", Link.class), carOnly));
		
		Id<Node> nodeId = Id.create("3", Node.class);
		Assertions.assertFalse(exp.turnsAreSameAsSingleNode(nodeId, turns, true));
		
		turns.clear();
		turns.add(new TurnInfo(Id.create("1", Link.class), Id.create("2", Link.class))); // u-turn
		turns.add(new TurnInfo(Id.create("1", Link.class), Id.create("4", Link.class)));
		turns.add(new TurnInfo(Id.create("1", Link.class), Id.create("6", Link.class)));
		turns.add(new TurnInfo(Id.create("3", Link.class), Id.create("2", Link.class), walkCar));
		turns.add(new TurnInfo(Id.create("3", Link.class), Id.create("4", Link.class), walkCar)); // u-turn
		turns.add(new TurnInfo(Id.create("3", Link.class), Id.create("6", Link.class), walkCar));
		turns.add(new TurnInfo(Id.create("5", Link.class), Id.create("2", Link.class)));
		turns.add(new TurnInfo(Id.create("5", Link.class), Id.create("4", Link.class)));
		turns.add(new TurnInfo(Id.create("5", Link.class), Id.create("6", Link.class), emptySet)); // u-turn
		
		Assertions.assertTrue(exp.turnsAreSameAsSingleNode(nodeId, turns, true));
	}

	@Test
	void testTurnInfo_equals() {
		Set<String> modes1 = new HashSet<String>();
		Set<String> modes2 = new HashSet<String>();
		modes2.add(TransportMode.car);
		Id<Link> id1 = Id.create("1", Link.class);
		Id<Link> id2 = Id.create("2", Link.class);
		
		TurnInfo ti1 = new TurnInfo(id1, id2);
		TurnInfo ti2 = new TurnInfo(id1, id2, modes1);
		TurnInfo ti3 = new TurnInfo(id1, id2, modes2);
		TurnInfo ti4 = new TurnInfo(id2, id1);
		TurnInfo ti5 = new TurnInfo(id2, id1, modes1);
		TurnInfo ti6 = new TurnInfo(id2, id1, modes2);

		TurnInfo ti22 = new TurnInfo(id1, id2, modes1);
		TurnInfo ti44 = new TurnInfo(id2, id1);
		
		Assertions.assertNotNull(ti1);
		Assertions.assertFalse(ti1.equals(ti2));
		Assertions.assertFalse(ti1.equals(ti3));
		Assertions.assertFalse(ti1.equals(ti4));
		Assertions.assertFalse(ti1.equals(ti5));
		Assertions.assertFalse(ti1.equals(ti6));
		
		Assertions.assertNotNull(ti2);
		Assertions.assertFalse(ti2.equals(ti1));
		Assertions.assertFalse(ti2.equals(ti3));
		Assertions.assertFalse(ti2.equals(ti4));
		Assertions.assertFalse(ti2.equals(ti5));
		Assertions.assertFalse(ti2.equals(ti6));
		
		Assertions.assertTrue(ti2.equals(ti22));
		Assertions.assertTrue(ti4.equals(ti44));
	}
	
	private static Link findLinkBetween(final Network network, final Id<Link> fromLinkId, final Id<Link> toLinkId) {
		Link fromLink = network.getLinks().get(fromLinkId);
		Link toLink = network.getLinks().get(toLinkId);
		Node from = fromLink.getToNode();
		Node to = toLink.getFromNode();
		for (Link link : from.getOutLinks().values()) {
			if (link.getToNode() == to) {
				return link;
			}
		}
		return null;
	}
	
	private static class Fixture {
		private final Scenario scenario;
		
		public Fixture() {
			this.scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		}
		
		/**
		 * Creates the following, simple network for testing purposes:
		 * <pre>
		 *           (1)
		 *           | ^
		 *           1 |
		 *           | 2
		 *           v |
		 *  (2)<-4---(3)<--5--(4)<--7--(5)
		 *  ( )--3-->( )---6->( )---8->( )
		 * </pre>
		 * Each link has 1 lane, is 1000 long, has a capacity of 1800 and a freespeed of 10.0 and is
		 * open for car and walk.
		 */
		public void createNetwork_ThreeWayIntersection() {
			Network n = this.scenario.getNetwork();
			NetworkFactory nf = n.getFactory();
			Node node1 = nf.createNode(Id.create("1", Node.class), new Coord((double) 1000, (double) 1000));
			Node node2 = nf.createNode(Id.create("2", Node.class), new Coord((double) 0, (double) 0));
			Node node3 = nf.createNode(Id.create("3", Node.class), new Coord((double) 1000, (double) 0));
			Node node4 = nf.createNode(Id.create("4", Node.class), new Coord((double) 2000, (double) 0));
			Node node5 = nf.createNode(Id.create("5", Node.class), new Coord((double) 3000, (double) 0));
			n.addNode(node1);
			n.addNode(node2);
			n.addNode(node3);
			n.addNode(node4);
			n.addNode(node5);
			n.addLink(createLink(nf, "1", node1, node3));
			n.addLink(createLink(nf, "2", node3, node1));
			n.addLink(createLink(nf, "3", node2, node3));
			n.addLink(createLink(nf, "4", node3, node2));
			n.addLink(createLink(nf, "5", node4, node3));
			n.addLink(createLink(nf, "6", node3, node4));
			n.addLink(createLink(nf, "7", node5, node4));
			n.addLink(createLink(nf, "8", node4, node5));
		}

		private Link createLink(final NetworkFactory nf, final String id, final Node fromNode, final Node toNode) {
			Link l = nf.createLink(Id.create(id, Link.class), fromNode, toNode);
			l.setLength(1000.0);
			l.setCapacity(1800.0);
			l.setNumberOfLanes(2);
			l.setFreespeed(10.0);
			HashSet<String> modes = new HashSet<String>();
			modes.add(TransportMode.car);
			modes.add(TransportMode.walk);
			l.setAllowedModes(modes);
			return l;
		}
	}
}
