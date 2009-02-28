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
import org.matsim.interfaces.core.v01.Network;
import org.matsim.interfaces.core.v01.Node;

/* See "http://www.ivt.ethz.ch/vpl/publications/reports/ab283.pdf"
 * for a description of node types. It's the graph matching paper. */

public class NetworkCalcTopoType {

	public final static int EMPTY        = 0;
	public final static int SOURCE       = 1;
	public final static int SINK         = 2;
	public final static int DEADEND      = 3;
	public final static int PASS1WAY     = 4;
	public final static int PASS2WAY     = 5;
	public final static int START1WAY    = 6;
	public final static int END1WAY      = 7;
	public final static int INTERSECTION = 8;
	
	public void run(final Network network) {
		System.out.println("    running " + this.getClass().getName() + " algorithm...");

		for (Node n : network.getNodes().values()) {
			if (n.getIncidentLinks().size() == 0) { n.setTopoType(EMPTY); }
			else if (n.getInLinks().size() == 0) { n.setTopoType(SOURCE); }
			else if (n.getOutLinks().size() == 0) {n.setTopoType(SINK); }
			else if (n.getIncidentNodes().size() == 1) { n.setTopoType(DEADEND); }
			else if (n.getIncidentNodes().size() == 2) {
				if ((n.getOutLinks().size() == 1) && (n.getInLinks().size() == 1)) { n.setTopoType(PASS1WAY); }
				else if ((n.getOutLinks().size() == 2) && (n.getInLinks().size() == 2)) { n.setTopoType(PASS2WAY); }
				else if ((n.getOutLinks().size() == 2) && (n.getInLinks().size() == 1)) { n.setTopoType(START1WAY); }
				else if ((n.getOutLinks().size() == 1) && (n.getInLinks().size() == 2)) { n.setTopoType(END1WAY); }
				else { Gbl.errorMsg("Node=" + n.toString() + " cannot be assigned to a topo type!"); }
			}
			else { // more than two neighbour nodes and no sink or source
				n.setTopoType(INTERSECTION);
			}
		}

		int [] cnt = {0,0,0,0,0,0,0,0,0};
		for (Node n : network.getNodes().values()) {
			cnt[n.getTopoType()]++;
		}

		System.out.println("      #nodes        = " + network.getNodes().size());
		System.out.println("      #EMTPY        = " + cnt[EMPTY]);
		System.out.println("      #SOURCE       = " + cnt[SOURCE]);
		System.out.println("      #SINK         = " + cnt[SINK]);
		System.out.println("      #DEADEND      = " + cnt[DEADEND]);
		System.out.println("      #PASS1WAY     = " + cnt[PASS1WAY]);
		System.out.println("      #PASS2WAY     = " + cnt[PASS2WAY]);
		System.out.println("      #START1WAY    = " + cnt[START1WAY]);
		System.out.println("      #END1WAY      = " + cnt[END1WAY]);
		System.out.println("      #INTERSECTION = " + cnt[INTERSECTION]);

		System.out.println("    done.");
	}
}
