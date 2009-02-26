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

package org.matsim.network.algorithms;

import org.matsim.gbl.Gbl;
import org.matsim.interfaces.core.v01.Node;
import org.matsim.network.NetworkLayer;
import org.matsim.network.NodeImpl;

/* See "http://www.ivt.ethz.ch/vpl/publications/reports/ab283.pdf"
 * for a description of node types. It's the graph matching paper. */

public class NetworkCalcTopoType {

	public NetworkCalcTopoType() {
		super();
	}

	public void run(final NetworkLayer network) {
		System.out.println("    running " + this.getClass().getName() + " algorithm...");

		for (Node n : network.getNodes().values()) {
			if (n.getIncidentLinks().size() == 0) { n.setTopoType(NodeImpl.EMPTY); }
			else if (n.getInLinks().size() == 0) { n.setTopoType(NodeImpl.SOURCE); }
			else if (n.getOutLinks().size() == 0) {n.setTopoType(NodeImpl.SINK); }
			else if (n.getIncidentNodes().size() == 1) { n.setTopoType(NodeImpl.DEADEND); }
			else if (n.getIncidentNodes().size() == 2) {
				if ((n.getOutLinks().size() == 1) && (n.getInLinks().size() == 1)) { n.setTopoType(NodeImpl.PASS1WAY); }
				else if ((n.getOutLinks().size() == 2) && (n.getInLinks().size() == 2)) { n.setTopoType(NodeImpl.PASS2WAY); }
				else if ((n.getOutLinks().size() == 2) && (n.getInLinks().size() == 1)) { n.setTopoType(NodeImpl.START1WAY); }
				else if ((n.getOutLinks().size() == 1) && (n.getInLinks().size() == 2)) { n.setTopoType(NodeImpl.END1WAY); }
				else { Gbl.errorMsg("Node=" + n.toString() + " cannot be assigned to a topo type!"); }
			}
			else { // more than two neighbour nodes and no sink or source
				n.setTopoType(NodeImpl.INTERSECTION);
			}
		}

		int [] cnt = {0,0,0,0,0,0,0,0,0};
		for (Node n : network.getNodes().values()) {
			cnt[n.getTopoType()] = cnt[n.getTopoType()]+1;
		}

		System.out.println("      #nodes        = " + network.getNodes().size());
		System.out.println("      #EMTPY        = " + cnt[NodeImpl.EMPTY]);
		System.out.println("      #SOURCE       = " + cnt[NodeImpl.SOURCE]);
		System.out.println("      #SINK         = " + cnt[NodeImpl.SINK]);
		System.out.println("      #DEADEND      = " + cnt[NodeImpl.DEADEND]);
		System.out.println("      #PASS1WAY     = " + cnt[NodeImpl.PASS1WAY]);
		System.out.println("      #PASS2WAY     = " + cnt[NodeImpl.PASS2WAY]);
		System.out.println("      #START1WAY    = " + cnt[NodeImpl.START1WAY]);
		System.out.println("      #END1WAY      = " + cnt[NodeImpl.END1WAY]);
		System.out.println("      #INTERSECTION = " + cnt[NodeImpl.INTERSECTION]);

		System.out.println("    done.");
	}
}
