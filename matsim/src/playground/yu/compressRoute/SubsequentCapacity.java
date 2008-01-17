/* *********************************************************************** *
 * project: org.matsim.*
 * SubsequentCapacity.java
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

package playground.yu.compressRoute;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.utils.identifiers.IdI;
import org.matsim.utils.geometry.shared.Coord;
import org.matsim.writer.MatsimXmlWriter;

/**
 * (like Subsequent) calculates the "default next" linkId of a current link in
 * MATSim networkfile with respect to Capacity (i.e. BEAT=1) and Geometry
 *
 * @author ychen
 *
 */
public class SubsequentCapacity extends MatsimXmlWriter {

	private Map<IdI, ? extends Link> links;

	/**
	 * (arg0) - ssLinkId
	 * (arg1) - linkId
	 */
	private TreeMap<String, String> ssLinks = new TreeMap<String, String>();

	Map<String, Link> outLinksMap = new TreeMap<String, Link>();

	public SubsequentCapacity(NetworkLayer network) {
		links = network.getLinks();
	}

	/**
	 * @return Returns the ssLinks.
	 */
	public TreeMap<String, String> getSsLinks() {
		return ssLinks;
	}

	/**
	 *
	 */
	public void compute() {
		Map<String, Double> caps = new TreeMap<String, Double>();
		List<String> toCompareAngles = new ArrayList<String>();

		for (Link l : links.values()) {
			caps.clear();
			toCompareAngles.clear();
			outLinksMap.clear();

			Node to = l.getToNode();
			Collection<? extends Link> outLinks = to.getOutLinks().values();

			for (Link outLink : outLinks) {
				String outLinkId = outLink.getId().toString();
				caps.put(outLinkId, outLink.getCapacity());
				outLinksMap.put(outLinkId, outLink);
				// TODO: can man delete outLinksMap?
			}
			String nextOutLinkId = "";
			if (caps.size() == 1) {
				nextOutLinkId = outLinksMap.keySet().iterator().next();
			} else if (caps.size() > 1) {
				Collection<Double> capsValues = caps.values();
				double maxCap = Collections.max(capsValues);
				double minCap = Collections.min(capsValues);
				double lCap = l.getCapacity();

				if (lCap >= maxCap) {
					// choose the max, when over 1 max, compare angles.
					for (String linkId : caps.keySet())
						if (caps.get(linkId).doubleValue() == maxCap)
							toCompareAngles.add(linkId);
					// compare angles
					nextOutLinkId = compareAngles(l, toCompareAngles);
				} else if (lCap <= minCap) {
					toCompareAngles.addAll(caps.keySet());
					// compare angles
					nextOutLinkId = compareAngles(l, toCompareAngles);
				} else {
					// choose the links with bigger Capacities than l has, then
					// compare angles.
					for (String linkId : caps.keySet())
						if (caps.get(linkId).doubleValue() >= lCap)
							toCompareAngles.add(linkId);
					// compare angles
					nextOutLinkId = compareAngles(l, toCompareAngles);
				}
			}

			ssLinks.put(nextOutLinkId, l.getId().toString());
		}
	}

	/**
	 * Calculates the "default next" LinkId with respect to Geometry i.e.
	 * angles, if there is 2 outLinks with the same Capacity
	 *
	 * @param l - the current link
	 * @param nextLinksIds - a list of outLinks with the same Capacity, size <= 2
	 * @return the "default next" LinkId
	 */
	public String compareAngles(Link l, List<String> nextLinksIds) {
		Map<String, Double> thetas = new TreeMap<String, Double>();
		List<String> minThetas = new ArrayList<String>();
		String resultLinkId = "";
		Node to = l.getToNode();
		Node from = l.getFromNode();

		Coord cFrom = from.getCoord();
		Coord cTo = to.getCoord();
		double xTo = cTo.getX();
		double yTo = cTo.getY();
		double crtLinkTheta = Math
				.atan2(yTo - cFrom.getY(), xTo - cFrom.getX());

		if (nextLinksIds.size() == 1)
			resultLinkId = nextLinksIds.get(0);
		else if (nextLinksIds.size() > 1) {
			for (String nextLinkId : nextLinksIds) {
				Coord cNextTo = outLinksMap.get(nextLinkId).getToNode()
						.getCoord();
				double outLinkTheta = Math.atan2(cNextTo.getY() - yTo, cNextTo
						.getX()
						- xTo);
				double deltaTheta = outLinkTheta - crtLinkTheta;
				while (deltaTheta < -Math.PI)
					deltaTheta += 2.0 * Math.PI;
				while (deltaTheta > Math.PI)
					deltaTheta -= 2.0 * Math.PI;
				thetas.put(nextLinkId, Math.abs(deltaTheta));
			}
			double minTheta = Collections.min(thetas.values());
			for (String outLinkId : thetas.keySet())
				if (thetas.get(outLinkId).doubleValue() == minTheta)
					minThetas.add(outLinkId);

			if (minThetas.size() == 1)
				resultLinkId = minThetas.get(0);
			else if (minThetas.size() == 2)
				resultLinkId = (Math.random() < 0.5) ? minThetas.get(0)
						: minThetas.get(1);
		}
		return resultLinkId;
	}

	/**
	 * see also: org.matsim.playground.yu.Subsequent
	 * @param filename
	 * @throws IOException
	 */
	public void writeFile(final String filename) throws IOException {
		System.out.println("@write beginning");
		openFile(filename);
		writeXmlHead();
		write();
		close();
	}

	/**
	 * see also: org.matsim.playground.yu.Subsequent
	 * @throws IOException
	 */
	private void write() throws IOException {
		writer.write("<subsequent>\n");
		// links
		writer.write("\t<links>\n");
		for (Iterator<Entry<String, String>> it = ssLinks.entrySet().iterator(); it.hasNext();) {
			Entry<String, String> next = it.next();
			writer.write("\t\t<link id=\"" + next.getValue()
					+ "\" subsequentLinkId=\"" + next.getKey() + "\" />\n");
		}
		writer.write("\t</links>\n" + "</subsequent>");
		System.out.println("@write done.");
	}
}