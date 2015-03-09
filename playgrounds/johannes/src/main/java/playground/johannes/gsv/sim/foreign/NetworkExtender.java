/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.johannes.gsv.sim.foreign;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.log4j.Logger;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.FacilitiesWriter;
import org.matsim.facilities.MatsimFacilitiesReader;
import org.opengis.referencing.FactoryException;

import playground.johannes.coopsim.util.MatsimCoordUtils;
import playground.johannes.gsv.zones.Zone;
import playground.johannes.gsv.zones.io.Zone2GeoJSON;
import playground.johannes.sna.util.ProgressLogger;
import playground.johannes.socialnetworks.gis.CartesianDistanceCalculator;
import playground.johannes.socialnetworks.gis.DistanceCalculator;
import playground.johannes.socialnetworks.utils.XORShiftRandom;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.TopologyException;
import com.vividsolutions.jts.geom.prep.PreparedGeometry;
import com.vividsolutions.jts.geom.prep.PreparedGeometryFactory;

/**
 * @author johannes
 * 
 */
public class NetworkExtender {

	private static final Logger logger = Logger.getLogger(NetworkExtender.class);

	private static final GeometryFactory factory = JTSFactoryFinder.getGeometryFactory(null);

	private static final DistanceCalculator distCalc = CartesianDistanceCalculator.getInstance();

	// private static MathTransform transform;

	static private Set<Link> getIntersectingLinks(Network network, PreparedGeometry boundary) {
		Set<Link> links = new HashSet<>();

		ProgressLogger.init(network.getLinks().size(), 2, 10);

		for (Link link : network.getLinks().values()) {
			if (link.getCapacity() >= 1000) {
				Point p1 = MatsimCoordUtils.coordToPoint(link.getFromNode().getCoord());
				Point p2 = MatsimCoordUtils.coordToPoint(link.getToNode().getCoord());

				boolean in1 = boundary.contains(p1);
				boolean in2 = boundary.contains(p2);

				if (in1 != in2) {
					links.add(link);
				}
			}
			ProgressLogger.step();
		}
		ProgressLogger.termiante();
		return links;
	}

	static private Set<Node> getNearestNodes(Point centroid, Set<Link> links) {
		final Map<Node, Double> distances = new HashMap<>(links.size());
		/*
		 * get from or to node
		 */
		for (Link link : links) {
			double fromDist = calcDistance(centroid, link.getFromNode());
			double toDist = calcDistance(centroid, link.getToNode());

			if (fromDist < toDist) {
				distances.put(link.getFromNode(), fromDist);
			} else {
				distances.put(link.getToNode(), toDist);
			}
		}
		/*
		 * sort nodes
		 */
		List<Node> sortedNodes = new LinkedList<>(distances.keySet());
		Collections.sort(sortedNodes, new Comparator<Node>() {

			@Override
			public int compare(Node o1, Node o2) {
				int result = Double.compare(distances.get(o1), distances.get(o2));
				if (result == 0) {
					return distances.get(o1).hashCode() - distances.get(o2).hashCode();
				} else {
					return result;
				}
			}
		});
		/*
		 * get nearest nodes
		 */
		int cnt = 0;
		int N = 10; // links.size() / 2;
		Set<Node> nearest = new HashSet<>(N);
		for (Node node : sortedNodes) {
			nearest.add(node);
			cnt++;
			if (cnt >= N) {
				break;
			}
		}

		return nearest;
	}

	static private double calcDistance(Point centroid, Node node) {
		return distCalc.distance(centroid, MatsimCoordUtils.coordToPoint(node.getCoord()));

	}

	static private void connectZone(Point centroid, Collection<Node> nodes, Network network, ActivityFacilities facilities, String id) {
		/*
		 * create nodes
		 */
		Id<Node> nodeId = Id.createNodeId(String.format("%s.n%s", id, 0));
		Node centroidNode = network.getFactory().createNode(nodeId, MatsimCoordUtils.pointToCoord(centroid));
		network.addNode(centroidNode);

		nodeId = Id.createNodeId(String.format("%s.n%s", id, 1));
		Coord c = MatsimCoordUtils.pointToCoord(centroid);
		c.setX(c.getX() + 1);
		c.setY(c.getY() + 1);

		Node neighbourNode = network.getFactory().createNode(nodeId, c);
		network.addNode(neighbourNode);
		/*
		 * create facility
		 */
		Id<ActivityFacility> facId = Id.create(String.format("%s.f", id), ActivityFacility.class);
		ActivityFacility fac = facilities.getFactory().createActivityFacility(facId, neighbourNode.getCoord());
		facilities.addActivityFacility(fac);
		/*
		 * create facility link
		 */
		Id<Link> linkId = Id.createLinkId(String.format("%s.l%s", id, 0));
		Link fLink1 = network.getFactory().createLink(linkId, centroidNode, neighbourNode);
		fLink1.setLength(1);
		fLink1.setCapacity(99999);
		fLink1.setFreespeed(120 / 3.6);

		linkId = Id.createLinkId(String.format("%s.l%s", id, 1));
		Link fLink2 = network.getFactory().createLink(linkId, neighbourNode, centroidNode);
		fLink2.setLength(1);
		fLink2.setCapacity(99999);
		fLink2.setFreespeed(120 / 3.6);

		network.addLink(fLink1);
		network.addLink(fLink2);
		/*
		 * create connecting links
		 */
		int cnt = 2;
		Point nPoint = MatsimCoordUtils.coordToPoint(neighbourNode.getCoord());
		for (Node node : nodes) {
			double dist = distCalc.distance(MatsimCoordUtils.coordToPoint(node.getCoord()), nPoint);

			linkId = Id.createLinkId(String.format("%s.l%s", id, cnt));
			Link toLink = network.getFactory().createLink(linkId, neighbourNode, node);
			toLink.setLength(dist);
			toLink.setCapacity(99999);
			toLink.setFreespeed(120 / 3.6);
			cnt++;

			linkId = Id.createLinkId(String.format("%s.l%s", id, cnt));
			Link fromLink = network.getFactory().createLink(linkId, node, neighbourNode);
			fromLink.setLength(dist);
			fromLink.setCapacity(99999);
			fromLink.setFreespeed(120 / 3.6);
			cnt++;

			network.addLink(toLink);
			network.addLink(fromLink);
		}
	}

	private static Point getPosition(Geometry geometry, Random random) {
		Point centroid = geometry.getCentroid();
		try {
			if (geometry.contains(centroid)) {
				return centroid;
			} else {
				PreparedGeometry prepGeo = PreparedGeometryFactory.prepare(geometry);
				Envelope env = geometry.getEnvelopeInternal();
				double dx = env.getMaxX() - env.getMinX();
				double dy = env.getMaxY() - env.getMinY();
				Point p = null;
				boolean inbounds = false;
				while (!inbounds) {
					double x = (random.nextDouble() * dx) + env.getMinX();
					double y = (random.nextDouble() * dy) + env.getMinY();
					Coordinate c = new Coordinate(x, y);
					p = factory.createPoint(c);
					inbounds = prepGeo.contains(p);
				}

				return p;
			}
		} catch (TopologyException e) {
			logger.warn(e.getMessage());
			return centroid;
		}
	}

	public static void main(String args[]) throws IOException, FactoryException {
		String netFile = args[0];
		String facFile = args[1];
		String zonesFile = args[2];
		String boundaryFile = args[3];
		String outDir = args[4];

		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);

		logger.info("Loading network...");
		MatsimNetworkReader netReader = new MatsimNetworkReader(scenario);
		netReader.readFile(netFile);

		logger.info("Loading facilities....");
		MatsimFacilitiesReader facReader = new MatsimFacilitiesReader(scenario);
		facReader.readFile(facFile);

		logger.info("Loading geometries...");
		String data = new String(Files.readAllBytes(Paths.get(zonesFile)));
		Set<Zone> zones = Zone2GeoJSON.parseFeatureCollection(data);

		logger.info("Extracting boundary...");
		Set<Geometry> deGeometries = new HashSet<>();
		Set<Zone> deZones = new HashSet<>();
		Set<Zone> euZones = new HashSet<>();
		for (Zone zone : zones) {
			if (zone.getAttribute("NUTS0_CODE").equalsIgnoreCase("DE")) {
				deGeometries.add(zone.getGeometry());
				deZones.add(zone);
			} else {
				euZones.add(zone);
			}
		}

		logger.info("Loading boundary...");
		data = new String(Files.readAllBytes(Paths.get(boundaryFile)));
		Set<Zone> tmp = Zone2GeoJSON.parseFeatureCollection(data);
		Geometry boundary = tmp.iterator().next().getGeometry();

		logger.info("Extracting intersecting links...");
		Set<Link> links = getIntersectingLinks(scenario.getNetwork(), PreparedGeometryFactory.prepare(boundary));

		logger.info(String.format("Connecting %s zones..", euZones.size()));
		Random random = new XORShiftRandom();
		ProgressLogger.init(euZones.size(), 2, 10);
		for (Zone zone : euZones) {
			Point centroid = zone.getGeometry().getCentroid();
			// logger.info("get nearest Nodes");
			Set<Node> nodes = getNearestNodes(centroid, links);
			// logger.info("connect zones");
			Point p = getPosition(zone.getGeometry(), random);
			connectZone(p, nodes, scenario.getNetwork(), scenario.getActivityFacilities(), zone.getAttribute("NO"));
			ProgressLogger.step();
		}
		ProgressLogger.termiante();

		logger.info("Writing network...");
		NetworkWriter netWriter = new NetworkWriter(scenario.getNetwork());
		netWriter.write(String.format("%s/network.xml.gz", outDir));

		logger.info("Writing facilities...");
		FacilitiesWriter facWriter = new FacilitiesWriter(scenario.getActivityFacilities());
		facWriter.write(String.format("%s/facilities.xml.gz", outDir));

		logger.info("Done.");

	}
}
