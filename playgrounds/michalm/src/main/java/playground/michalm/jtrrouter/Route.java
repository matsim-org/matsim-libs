/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.michalm.jtrrouter;

import java.util.List;

/**
 * @author michalm
 */
public class Route {
	final Flow inFlow;
	final Flow outFlow;

	final int nodeCount;
	final String nodes;

	final double prob;

	public Route(Flow in, Flow out, List<Integer> nodeList, double prob) {
		this.inFlow = in;
		this.outFlow = out;
		this.prob = prob;
		this.nodeCount = nodeList.size();

		StringBuilder sb = new StringBuilder(nodeCount * 3);

		for (int i = 0; i < nodeCount; i++) {
			sb.append(nodeList.get(i)).append(' ');
		}

		nodes = sb.toString();
	}

	public String toString() {
		return new StringBuilder(nodes.length() + 15).append("IN").append(inFlow.node).append(' ').append(nodes)
				.append("OUT").append(outFlow.node).toString();
	}

	public Flow getInFlow() {
		return inFlow;
	}

	public Flow getOutFlow() {
		return outFlow;
	}

	public String getNodes() {
		return nodes;
	}

	public int getNodeCount() {
		return nodeCount;
	}

	public double getProb() {
		return prob;
	}
}
