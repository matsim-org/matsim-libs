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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.utils.geometry.CoordUtils;

import playground.boescpa.converters.vissim.ConvEvents;

/**
 * Provides methods that create a key map from a given network to a given mutual base grid.
 *
 * @author boescpa
 */
public abstract class AbstractNetworkMapper implements ConvEvents.NetworkMapper {

	/**
	 * Creates a key-map that maps a Network to a provided mutualBaseGrid (provided in the MATSim-Network-Format).
	 *
	 * @param path2Network
	 * @param mutualBaseGrid		MATSim-Network-Format
	 * @param path2VissimZoneShp
	 * @return	A key map that maps the vissum network to the mutual base grid.
	 */
	@Override
	public HashMap<Id<Link>, Id<Node>[]> mapNetwork(String path2Network, Network mutualBaseGrid, String path2VissimZoneShp) {
		Network network = providePreparedNetwork(path2Network, path2VissimZoneShp);
		return getKeyMap(mutualBaseGrid, network);
	}

	protected abstract Network providePreparedNetwork(String path2Network, String path2VissimZoneShp);

	/**
	 * Maps a provided Network to a mutual base grid.
	 *
	 * @param mutualBaseGrid
	 * @param network
	 * @return
	 */
	protected final HashMap<Id<Link>, Id<Node>[]> getKeyMap(Network mutualBaseGrid, Network network) {
		HashMap<Id<Link>, Id<Node>[]> mapKey = new HashMap<>();
		// follow all links and check which "zones" of mutual base grid are passed
		for (Link link : network.getLinks().values()) {
			List<Id<Node>> passedZones = new LinkedList<>();
			Coord start = new Coord(link.getFromNode().getCoord().getX(), link.getFromNode().getCoord().getY());
			Coord end = new Coord(link.getToNode().getCoord().getX(), link.getToNode().getCoord().getY());
			double[] deltas = calculateDeltas(start, end);
			for (int i = 0; i <= (int)deltas[2]; i++) {
				Id<Node> presentSmallest = findZone(mutualBaseGrid, start, deltas, i);
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

	private Id<Node> findZone(Network mutualBaseGrid, Coord start, double[] deltas, int i) {
		int gridcellsize = BaseGridCreator.getGridcellsize();
		Id<Node> presentSmallest = null;
		double presentSmallestDist = Double.MAX_VALUE;
		for (Node zone : mutualBaseGrid.getNodes().values()) {
			Double dist = CoordUtils.calcEuclideanDistance(zone.getCoord(),
					new Coord(start.getX() + (i * deltas[0]), start.getY() + (i * deltas[1])));
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
		int gridcellsize = BaseGridCreator.getGridcellsize();
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
