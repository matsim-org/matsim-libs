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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkLayer;

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
	public static double[] getBoundingBox(final Collection<? extends Node> nodes) {
		double[] bBox = new double[4];
		bBox[0] = Double.MIN_VALUE;
		bBox[1] = Double.MAX_VALUE;
		bBox[2] = Double.MIN_VALUE;
		bBox[3] = Double.MAX_VALUE;

		for (Node n : nodes) {
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
	public static List<Node> getNodes(final Network network, final String nodes) {
		if (nodes == null) {
			return new ArrayList<Node>(0);
		}
		String trimmed = nodes.trim();
		if (trimmed.length() == 0) {
			return new ArrayList<Node>(0);
		}
		String[] parts = trimmed.split("[ \t\n]+");
		final List<Node> nodesList = new ArrayList<Node>(parts.length);

		for (String id : parts) {
			Node node = network.getNodes().get(new IdImpl(id));
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
	public static List<Link> getLinks(final Network network, final String links) {
		if (links == null) {
			return new ArrayList<Link>(0);
		}
		String trimmed = links.trim();
		if (trimmed.length() == 0) {
			return new ArrayList<Link>(0);
		}
		String[] parts = trimmed.split("[ \t\n]+");
		final List<Link> linksList = new ArrayList<Link>(parts.length);

		for (String id : parts) {
			Link link = network.getLinks().get(new IdImpl(id));
			if (link == null) {
				throw new IllegalArgumentException("no link with id " + id);
			}
			linksList.add(link);
		}
		return linksList;
	}

	public static List<Id> getLinkIds(final String links) {
		if (links == null) {
			return new ArrayList<Id>(0);
		}
		String trimmed = links.trim();
		if (trimmed.length() == 0) {
			return new ArrayList<Id>(0);
		}
		String[] parts = trimmed.split("[ \t\n]+");
		final List<Id> linkIdsList = new ArrayList<Id>(parts.length);

		for (String id : parts) {
			linkIdsList.add(new IdImpl(id));
		}
		return linkIdsList;
	}

	public static List<Link> getLinks(final Network network, final List<Id> linkIds) {
		List<Link> links = new ArrayList<Link>();
		for (Id linkId : linkIds) {
			Link link = network.getLinks().get(linkId);
			if (link == null) {
				throw new IllegalArgumentException("no link with id " + linkId);
			}
			links.add(link);
		}
		return links;
	}

	public static List<Id> getLinkIds(final List<Link> links) {
		List<Id> linkIds = new ArrayList<Id>();
		for (Link link : links) {
			linkIds.add(link.getId());
		}
		return linkIds;
	}

	/**
	 * @return the maximum of 1 and the mathematically rounded number of lanes attribute's value at time "time" of the link given as parameter
	 */
	public static int getNumberOfLanesAsInt(final double time, final Link link) {
		return Math.round((float)Math.max(link.getNumberOfLanes(time), 1.0d));
	}

}
