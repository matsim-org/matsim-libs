/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkUtils.java
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

package org.matsim.core.utils.misc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NodeImpl;

/**
 * Contains several helper methods for working with {@link NetworkLayer networks}. 
 *
 * @author mrieser
 */
public class NetworkUtils {

	/**
	 * @param nodes
	 * @return The bounding box of all the given nodes as <code>double[] = {minX, minY, maxX, maxY}</code>
	 */
	public static double[] getBoundingBox(final Collection<? extends NodeImpl> nodes) {
		double[] bBox = new double[4];
		bBox[0] = Double.MIN_VALUE;
		bBox[1] = Double.MAX_VALUE;
		bBox[2] = Double.MIN_VALUE;
		bBox[3] = Double.MAX_VALUE;

		for (NodeImpl n : nodes) {
			if (n.getCoord().getX() > bBox[0]) {
				bBox[0] = n.getCoord().getX();
			}
			if (n.getCoord().getX() < bBox[1]) {
				bBox[1] = n.getCoord().getX();
			}
			if (n.getCoord().getY() > bBox[2]) {
				bBox[2] = n.getCoord().getY();
			}
			if (n.getCoord().getY() < bBox[3]) {
				bBox[3] = n.getCoord().getY();
			}
		}

		return bBox;
	}

	/**
	 * @param network
	 * @param nodes list of node ids, separated by one or multiple whitespace (space, \t, \n)
	 * @return list containing the specified nodes.
	 * @throws IllegalArgumentException if a specified node is not found in the network
	 */
	public static List<NodeImpl> getNodes(final NetworkLayer network, final String nodes) {
		if (nodes == null) {
			return new ArrayList<NodeImpl>(0);
		}
		String trimmed = nodes.trim();
		if (trimmed.length() == 0) {
			return new ArrayList<NodeImpl>(0);
		}
		String[] parts = trimmed.split("[ \t\n]+");
		final List<NodeImpl> nodesList = new ArrayList<NodeImpl>(parts.length);

		for (String id : parts) {
			NodeImpl node = network.getNode(new IdImpl(id));
			if (node == null) {
				throw new IllegalArgumentException("no node with id " + id);
			}
			nodesList.add(node);
		}
		return nodesList;
	}

	/**
	 * @param network
	 * @param links list of link ids, separated by one or multiple whitespace (space, \t, \n)
	 * @return list containing the specified links.
	 * @throws IllegalArgumentException if a specified node is not found in the network
	 */
	public static List<LinkImpl> getLinks(final NetworkLayer network, final String links) {
		if (links == null) {
			return new ArrayList<LinkImpl>(0);
		}
		String trimmed = links.trim();
		if (trimmed.length() == 0) {
			return new ArrayList<LinkImpl>(0);
		}
		String[] parts = trimmed.split("[ \t\n]+");
		final List<LinkImpl> linksList = new ArrayList<LinkImpl>(parts.length);
		
		for (String id : parts) {
			LinkImpl link = network.getLink(new IdImpl(id));
			if (link == null) {
				throw new IllegalArgumentException("no node with id " + id);
			}
			linksList.add(link);
		}
		return linksList;
	}
	
}
