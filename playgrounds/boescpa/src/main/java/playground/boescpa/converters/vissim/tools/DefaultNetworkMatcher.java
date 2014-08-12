/*
 * *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package playground.boescpa.converters.vissim.tools;

import com.vividsolutions.jts.geom.Geometry;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkFactoryImpl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.opengis.feature.simple.SimpleFeature;

import playground.boescpa.converters.vissim.ConvEvents2Anm;
import playground.christoph.evacuation.analysis.CoordAnalyzer;
import playground.christoph.evacuation.withinday.replanning.utils.SHPFileUtil;

import java.util.*;

/**
 * Provides methods that create a key map from a given network to a given mutual base grid.
 *
 * @author boescpa
 */
public class DefaultNetworkMatcher implements ConvEvents2Anm.NetworkMatcher {

	/**
	 * Creates a key-map that maps a MATSimNetwork to a provided mutualBaseGrid (also MATSim-Network-Format).
	 * As a side-job, when the MATSim-network is read in, the network is cut to the zones provided.
	 *
	 * @param path2MATSimNetworkConfig	A matsim config which specifies the network to be used.
	 * @param mutualBaseGrid
	 * @param path2VissimZoneShp
	 * @return A key map that maps the matsim network to the mutual base grid.
	 */
	@Override
	public HashMap<Id, Id[]> mapMsNetwork(String path2MATSimNetworkConfig, Network mutualBaseGrid, String path2VissimZoneShp) {
		Network network = readAndCutMsNetwork(path2MATSimNetworkConfig, path2VissimZoneShp);
		return getKeyMap(mutualBaseGrid, network);
	}

	/**
	 * Read matsim network and cut it to zones.
	 *
	 * @param path2MATSimNetworkConfig	A matsim config which specifies the network to be used.
	 * @param path2VissimZoneShp
	 * @return The prepared matsim network.
	 */
	protected Network readAndCutMsNetwork(String path2MATSimNetworkConfig, String path2VissimZoneShp) {
		// Read network
		Config config = ConfigUtils.loadConfig(path2MATSimNetworkConfig);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Network network = scenario.getNetwork();
		// Prepare zones and identifier.
		Set<SimpleFeature> features = new HashSet<SimpleFeature>();
		features.addAll(ShapeFileReader.getAllFeatures(path2VissimZoneShp));
		SHPFileUtil util = new SHPFileUtil();
		Geometry cuttingArea = util.mergeGeometries(features);
		CoordAnalyzer coordAnalyzer = new CoordAnalyzer(cuttingArea);
		// Identify links not in zones.
		Set<Link> linkSet2Remove = new HashSet<Link>();
		for (Link link : network.getLinks().values()) {
			if (!coordAnalyzer.isLinkAffected(link)) {
				linkSet2Remove.add(link);
			}
		}
		// Remove links not in zones.
		for (Link link : linkSet2Remove) {
			network.removeLink(link.getId());
		}
		return network;
	}

	/**
	 * Creates a key-map that maps a Vissum-Network to a provided mutualBaseGrid (provided in the MATSim-Network-Format).
	 *
	 * @param path2VissimNetworkAnm	Visum-Anm-Format
	 * @param mutualBaseGrid		MATSim-Network-Format
	 * @return	A key map that maps the vissum network to the mutual base grid.
	 */
	@Override
	public HashMap<Id, Id[]> mapAmNetwork(String path2VissimNetworkAnm, Network mutualBaseGrid) {
		Network network = parseAndTransformAmNetwork(path2VissimNetworkAnm);
		return getKeyMap(mutualBaseGrid, network);
	}

	/**
	 * Parses the provided Visum-Anm-File (xml-format) and transform the network into a matsim network.
	 *
	 * @param path2VissimNetworkAnm Path to a Visum-Anm-File
	 * @return
	 */
	protected Network parseAndTransformAmNetwork(String path2VissimNetworkAnm) {
		final Network network = NetworkUtils.createNetwork();
		final NetworkFactory networkFactory = new NetworkFactoryImpl(network);
		final Set<SimpleAnmParser.AnmLink> links = new HashSet<SimpleAnmParser.AnmLink>();
		
		// parse anm-file:
		MatsimXmlParser xmlParser = new SimpleAnmParser(new NodeAndLinkParser() {
			@Override
			public void handleLink(SimpleAnmParser.AnmLink anmLink) {
				links.add(anmLink);
			}
			@Override
			public void handleNode(SimpleAnmParser.AnmNode anmNode) {
				network.addNode(networkFactory.createNode(anmNode.id, anmNode.coord));
			}
		});
		xmlParser.parse(path2VissimNetworkAnm);

		// create links:
		int countErrLinks = 0;
		for (SimpleAnmParser.AnmLink link : links) {
			try {
				Node fromNode = network.getNodes().get(link.fromNode);
				Node toNode = network.getNodes().get(link.toNode);
				network.addLink(networkFactory.createLink(link.id, fromNode, toNode));
			} catch (NullPointerException e) {
				System.out.println("Link " + link.id.toString() + " lacks one or both nodes.");
				countErrLinks++;
			}
		}
		System.out.print("\n" + countErrLinks + " links found with one or both nodes lacking.\nThey were dropped.\n");

		return network;
	}
	private interface NodeAndLinkParser extends SimpleAnmParser.AnmNodeHandler, SimpleAnmParser.AnmLinkHandler {}

	private HashMap<Id, Id[]> getKeyMap(Network mutualBaseGrid, Network network) {
		HashMap<Id, Id[]> mapKey = new HashMap<Id, Id[]>();
		// follow all links and check which "zones" of mutual base grid are passed
		for (Link link : network.getLinks().values()) {
			List<Id> passedZones = new LinkedList<Id>();
			Coord start = new CoordImpl(link.getFromNode().getCoord().getX(), link.getFromNode().getCoord().getY());
			Coord end = new CoordImpl(link.getToNode().getCoord().getX(), link.getToNode().getCoord().getY());
			double[] deltas = calculateDeltas(start, end);
			for (int i = 0; i <= (int)deltas[2]; i++) {
				Id presentSmallest = findZone(mutualBaseGrid, start, deltas, i);
				if (presentSmallest != null) {
					if (passedZones.isEmpty()) {
						passedZones.add(presentSmallest);
					} else if (passedZones.get(passedZones.size() - 1) != presentSmallest) {
						passedZones.add(presentSmallest);
					}
				} else {
					throw new NullPointerException("For a coordinate no closest zone was found.");
				}
			}
			mapKey.put(link.getId(), passedZones.toArray(new Id[passedZones.size()]));
		}
		return mapKey;
	}

	private Id findZone(Network mutualBaseGrid, Coord start, double[] deltas, int i) {
		int gridcellsize = DefaultBaseGridCreator.getGridcellsize();
		Id presentSmallest = null;
		double presentSmallestDist = gridcellsize;
		for (Node zone : mutualBaseGrid.getNodes().values()) {
			Double dist = CoordUtils.calcDistance(zone.getCoord(),
					new CoordImpl(start.getX() + (i * deltas[0]), start.getY() + (i * deltas[1])));
			if (dist < presentSmallestDist) {
				presentSmallestDist = dist;
				presentSmallest = zone.getId();
			}
			if (dist < gridcellsize/2) {
				break;
			}
		}
		return presentSmallest;
	}

	private double[] calculateDeltas(Coord start, Coord end) {
		int gridcellsize = DefaultBaseGridCreator.getGridcellsize();
		double factor = 1;
		double[] delta = new double[3];
		do {
			factor *= 10;
			delta[0] = Math.abs((end.getX() - start.getX())/factor);
			delta[1] = Math.abs((end.getY() - start.getY())/factor);
		} while (delta[0] >= (gridcellsize/10) && delta[1] >= (gridcellsize/10));
		delta[0] = (end.getX() - start.getX())/factor;
		delta[1] = (end.getY() - start.getY())/factor;
		delta[2] = factor;
		return delta;
	}
}
