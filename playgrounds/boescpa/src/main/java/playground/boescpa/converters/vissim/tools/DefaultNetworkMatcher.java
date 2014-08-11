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

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordUtils;
import playground.boescpa.converters.vissim.ConvEvents2Anm;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * Creates a square grid around considered area. The grid is represented by nodes in the network.
 *
 * @author boescpa
 */
public class DefaultNetworkMatcher implements ConvEvents2Anm.NetworkMatcher {



	/**
	 * Creates a key-map that maps a MATSimNetwork to a provided mutualBaseGrid (also MATSim-Network-Format).
	 *
	 * @param path2MATSimNetwork
	 * @param mutualBaseGrid
	 * @param path2VissimZoneShp
	 * @return
	 */
	@Override
	public HashMap<Id, Id[]> mapMsNetwork(String path2MATSimNetwork, Network mutualBaseGrid, String path2VissimZoneShp) {
		Network network = readAndCutMsNetwork(path2MATSimNetwork, path2VissimZoneShp);
		return getKeyMap(mutualBaseGrid, network);
	}

	/**
	 * Read network MATSim-Network and cut it to zones.
	 *
	 * @param path2MATSimNetwork
	 * @param path2VissimZoneShp
	 * @return
	 */
	protected Network readAndCutMsNetwork(String path2MATSimNetwork, String path2VissimZoneShp) {
		return null;
	}

	@Override
	public HashMap<Id, Long[]> mapAmNetwork(String path2VissimNetworkLinks, Network mutualBaseGrid) {
		return null;
	}


	private HashMap<Id, Id[]> getKeyMap(Network mutualBaseGrid, Network network) {
		HashMap<Id, Id[]> mapKey = new HashMap<Id, Id[]>();
		// follow all links and check which "zones" of mutual base grid are passed
		for (Link link : network.getLinks().values()) {
			List<Id> passedZones = new LinkedList<Id>();
			Coord start = new CoordImpl(link.getFromNode().getCoord().getX(), link.getFromNode().getCoord().getY());
			Coord end = new CoordImpl(link.getToNode().getCoord().getX(), link.getToNode().getCoord().getY());
			double[] deltas = calculateDeltas(start, end);
			for (int i = 0; i <= (int)deltas[2]; i++) {
				Id presentSmallest = null;
				presentSmallest = findZone(mutualBaseGrid, start, deltas, i, presentSmallest);
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

	private Id findZone(Network mutualBaseGrid, Coord start, double[] deltas, int i, Id presentSmallest) {
		int gridcellsize = DefaultBaseGridCreator.getGridcellsize();
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
