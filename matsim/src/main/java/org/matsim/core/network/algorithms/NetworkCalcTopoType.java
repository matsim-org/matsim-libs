/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkCalcTopoType.java
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

package org.matsim.core.network.algorithms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.internal.NetworkRunnable;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.misc.NetworkUtils;

/** See "http://www.ivt.ethz.ch/vpl/publications/reports/ab283.pdf"
 * for a description of node types. It's the graph matching paper. */
public class NetworkCalcTopoType implements NetworkRunnable {

	public final static Integer EMPTY        = Integer.valueOf(0);
	public final static Integer SOURCE       = Integer.valueOf(1);
	public final static Integer SINK         = Integer.valueOf(2);
	public final static Integer DEADEND      = Integer.valueOf(3);
	public final static Integer PASS1WAY     = Integer.valueOf(4);
	public final static Integer PASS2WAY     = Integer.valueOf(5);
	public final static Integer START1WAY    = Integer.valueOf(6);
	public final static Integer END1WAY      = Integer.valueOf(7);
	public final static Integer INTERSECTION = Integer.valueOf(8);

	private final Map<Node, Integer> topoTypePerNode = new HashMap<Node, Integer>();

	public void run(final Network network) {
		System.out.println("    running " + this.getClass().getName() + " algorithm...");

		for (Node n : network.getNodes().values()) {
			if (NetworkUtils.getIncidentLinks(n).size() == 0) { setTopoType(n, EMPTY); }
			else if (n.getInLinks().size() == 0) { setTopoType(n, SOURCE); }
			else if (n.getOutLinks().size() == 0) {setTopoType(n, SINK); }
			else if (getIncidentNodes(n).size() == 1) { setTopoType(n, DEADEND); }
			else if (getIncidentNodes(n).size() == 2) {
				if ((n.getOutLinks().size() == 1) && (n.getInLinks().size() == 1)) { setTopoType(n, PASS1WAY); }
				else if ((n.getOutLinks().size() == 2) && (n.getInLinks().size() == 2)) { setTopoType(n, PASS2WAY); }
				else if ((n.getOutLinks().size() == 2) && (n.getInLinks().size() == 1)) { setTopoType(n, START1WAY); }
				else if ((n.getOutLinks().size() == 1) && (n.getInLinks().size() == 2)) { setTopoType(n, END1WAY); }
				// The following case is not covered by the paper, but quite common, e.g. parallel roads connecting the same nodes.
				else if ((n.getOutLinks().size() >= 1) && (n.getInLinks().size() >= 1)) { setTopoType(n, INTERSECTION); }
				else { Gbl.errorMsg("Node=" + n.toString() + " cannot be assigned to a topo type!"); }
			}
			else { // more than two neighbour nodes and no sink or source
				setTopoType(n, INTERSECTION);
			}
		}

		int [] cnt = {0,0,0,0,0,0,0,0,0};
		for (Node n : network.getNodes().values()) {
			cnt[getTopoType(n)]++;
		}

		System.out.println("      #nodes        = " + network.getNodes().size());
		System.out.println("      #EMTPY        = " + cnt[EMPTY.intValue()]);
		System.out.println("      #SOURCE       = " + cnt[SOURCE.intValue()]);
		System.out.println("      #SINK         = " + cnt[SINK.intValue()]);
		System.out.println("      #DEADEND      = " + cnt[DEADEND.intValue()]);
		System.out.println("      #PASS1WAY     = " + cnt[PASS1WAY.intValue()]);
		System.out.println("      #PASS2WAY     = " + cnt[PASS2WAY.intValue()]);
		System.out.println("      #START1WAY    = " + cnt[START1WAY.intValue()]);
		System.out.println("      #END1WAY      = " + cnt[END1WAY.intValue()]);
		System.out.println("      #INTERSECTION = " + cnt[INTERSECTION.intValue()]);

		System.out.println("    done.");
	}

	private void setTopoType(final Node node, final Integer topoType) {
		this.topoTypePerNode.put(node, topoType);
	}

	public int getTopoType(final Node node) {
		Integer i = this.topoTypePerNode.get(node);
		if (i == null) {
			return Integer.MIN_VALUE;
		}
		return i.intValue();
	}

	private List<Node> getIncidentNodes(final Node node) {
		List<Node> nodes = new ArrayList<Node>();
		for (Link link : node.getInLinks().values()) {
			nodes.add(link.getFromNode());
		}
		for (Link link : node.getOutLinks().values()) {
			nodes.add(link.getToNode());
		}
		return nodes;
	}
}
