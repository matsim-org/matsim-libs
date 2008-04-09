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
import org.matsim.network.LinkImpl;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.utils.misc.Time;

public class NetworkMergeDoubleLinks extends NetworkAlgorithm {

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

	private final void mergeLink2IntoLink1(LinkImpl link1, LinkImpl link2, NetworkLayer network) {
		if (mergetype == 0) {
			System.out.println("        Link id=" + link2.getId() + " removed because of Link id=" + link1.getId());
			network.removeLink(link2);
		}
		else if (mergetype == 1) {
			System.out.println("        Link id=" + link2.getId() + " merged (additive) into Link id=" + link1.getId());
			double cap = link1.getCapacity() + link2.getCapacity();
			double fs = Math.max(link1.getFreespeed(Time.UNDEFINED_TIME),link2.getFreespeed(Time.UNDEFINED_TIME));
			int lanes = link1.getLanesAsInt() + link2.getLanesAsInt();
			double length = Math.max(link1.getLength(),link2.getLength());
			String origid = "add-merge(" + link1.getId() + "," + link2.getId() + ")";
			link1.setCapacity(cap);
			link1.setFreespeed(fs);
			link1.setLanes(lanes);
			link1.setLength(length);
			link1.setOrigId(origid);
			network.removeLink(link2);
		}
		else if (mergetype == 2) {
			System.out.println("        Link id=" + link2.getId() + " merged (maximum) into Link id=" + link1.getId());
			double cap = Math.max(link1.getCapacity(),link2.getCapacity());
			double fs = Math.max(link1.getFreespeed(Time.UNDEFINED_TIME),link2.getFreespeed(Time.UNDEFINED_TIME));
			int lanes = Math.max(link1.getLanesAsInt(),link2.getLanesAsInt());
			double length = Math.max(link1.getLength(),link2.getLength());
			String origid = "max-merge(" + link1.getId() + "," + link2.getId() + ")";
			link1.setCapacity(cap);
			link1.setFreespeed(fs);
			link1.setLanes(lanes);
			link1.setLength(length);
			link1.setOrigId(origid);
			network.removeLink(link2);
		}
		else {
			Gbl.errorMsg("'mergetype' not known!");
		}
	}

	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public void run(NetworkLayer network) {
		System.out.println("    running " + this.getClass().getName() + " algorithm...");

		for (Node n : network.getNodes().values()) {
			Iterator<?> l1_it = n.getOutLinks().values().iterator();
			while (l1_it.hasNext()) {
				LinkImpl l1 = (LinkImpl)l1_it.next();
				Iterator<?> l2_it = n.getOutLinks().values().iterator();
				while (l2_it.hasNext()) {
					LinkImpl l2 = (LinkImpl)l2_it.next();
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
