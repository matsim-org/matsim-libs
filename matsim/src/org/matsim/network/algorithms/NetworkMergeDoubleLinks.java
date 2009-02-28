/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkMergeDoubleLinks.java
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

import java.util.Iterator;

import org.matsim.gbl.Gbl;
import org.matsim.interfaces.core.v01.Link;
import org.matsim.interfaces.core.v01.Network;
import org.matsim.interfaces.core.v01.Node;
import org.matsim.utils.misc.Time;

public class NetworkMergeDoubleLinks {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	// 0 := no merge, instead remove additionals (random)
	// 1 := additive merge (sum cap, max freespeed, sum lanes, max length)
	// 2 := max merge (max cap, max freespeed, max lanes, max length)
	private static final int mergetype = 2;

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public NetworkMergeDoubleLinks() {
		super();
	}

	//////////////////////////////////////////////////////////////////////
	// private methods
	//////////////////////////////////////////////////////////////////////

	private final void mergeLink2IntoLink1(Link link1, Link link2, Network network) {
		if (mergetype == 0) {
			System.out.println("        Link id=" + link2.getId() + " removed because of Link id=" + link1.getId());
			network.removeLink(link2);
		}
		else if (mergetype == 1) {
			System.out.println("        Link id=" + link2.getId() + " merged (additive) into Link id=" + link1.getId());
			double cap = link1.getCapacity(org.matsim.utils.misc.Time.UNDEFINED_TIME) + link2.getCapacity(org.matsim.utils.misc.Time.UNDEFINED_TIME);
			double fs = Math.max(link1.getFreespeed(Time.UNDEFINED_TIME),link2.getFreespeed(Time.UNDEFINED_TIME));
			int lanes = link1.getLanesAsInt(org.matsim.utils.misc.Time.UNDEFINED_TIME) + link2.getLanesAsInt(org.matsim.utils.misc.Time.UNDEFINED_TIME);
			double length = Math.max(link1.getLength(),link2.getLength());
//			String origid = "add-merge(" + link1.getId() + "," + link2.getId() + ")";
			link1.setCapacity(cap);
			link1.setFreespeed(fs);
			link1.setLanes(lanes);
			link1.setLength(length);
			network.removeLink(link2);
		}
		else if (mergetype == 2) {
			System.out.println("        Link id=" + link2.getId() + " merged (maximum) into Link id=" + link1.getId());
			double cap = Math.max(link1.getCapacity(org.matsim.utils.misc.Time.UNDEFINED_TIME),link2.getCapacity(org.matsim.utils.misc.Time.UNDEFINED_TIME));
			double fs = Math.max(link1.getFreespeed(Time.UNDEFINED_TIME),link2.getFreespeed(Time.UNDEFINED_TIME));
			int lanes = Math.max(link1.getLanesAsInt(org.matsim.utils.misc.Time.UNDEFINED_TIME),link2.getLanesAsInt(org.matsim.utils.misc.Time.UNDEFINED_TIME));
			double length = Math.max(link1.getLength(),link2.getLength());
//			String origid = "max-merge(" + link1.getId() + "," + link2.getId() + ")";
			link1.setCapacity(cap);
			link1.setFreespeed(fs);
			link1.setLanes(lanes);
			link1.setLength(length);
			network.removeLink(link2);
		}
		else {
			Gbl.errorMsg("'mergetype' not known!");
		}
	}

	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	public void run(Network network) {
		System.out.println("    running " + this.getClass().getName() + " algorithm...");

		for (Node n : network.getNodes().values()) {
			Iterator<?> l1_it = n.getOutLinks().values().iterator();
			while (l1_it.hasNext()) {
				Link l1 = (Link)l1_it.next();
				Iterator<?> l2_it = n.getOutLinks().values().iterator();
				while (l2_it.hasNext()) {
					Link l2 = (Link)l2_it.next();
					if (!l2.equals(l1)) {
						if (l2.getToNode().equals(l1.getToNode())) {
							System.out.println("      Node id=" + n.getId());
							this.mergeLink2IntoLink1(l1, l2, network);
							// restart
							l1_it = n.getOutLinks().values().iterator();
							l2_it = n.getOutLinks().values().iterator();
						}
					}
				}
			}
		}

		System.out.println("    done.");
	}
}
