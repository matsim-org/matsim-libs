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

package org.matsim.core.network.algorithms;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.internal.NetworkRunnable;
import org.matsim.core.network.NetworkUtils;

import java.util.Iterator;

public final class NetworkMergeDoubleLinks implements NetworkRunnable {

	public enum MergeType {
		/** no merge, instead remove additionals (random) */
		REMOVE,
		/** additive merge (sum cap, max freespeed, sum lanes, max length) */
		ADDITIVE,
		/** max merge (max cap, max freespeed, max langes, max length */
		MAXIMUM }

	public enum LogInfoLevel {
		/** do not print any info on merged links */
		NOINFO,
		/** print info for every merged link */
		MAXIMUM
	}

	private final static Logger log = LogManager.getLogger(NetworkMergeDoubleLinks.class);

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final MergeType mergetype;
	private final LogInfoLevel logInfoLevel;

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public NetworkMergeDoubleLinks() {
		this(MergeType.MAXIMUM, LogInfoLevel.MAXIMUM);
	}

	public NetworkMergeDoubleLinks(final MergeType mergetype) {
		this(mergetype, LogInfoLevel.MAXIMUM);
	}

	public NetworkMergeDoubleLinks(final MergeType mergetype, final LogInfoLevel logInfoLevel) {
		this.mergetype = mergetype;
		this.logInfoLevel = logInfoLevel;
	}

	//////////////////////////////////////////////////////////////////////
	// private methods
	//////////////////////////////////////////////////////////////////////

	private void mergeLink2IntoLink1(Link link1, Link link2, Network network) {
		switch (this.mergetype) {
			case REMOVE:
				if (logInfoLevel.equals(LogInfoLevel.MAXIMUM)) {
					log.info("        Link id=" + link2.getId() + " removed because of Link id=" + link1.getId());
				}

				network.removeLink(link2.getId());
				break;
			case ADDITIVE:
			{
				if (logInfoLevel.equals(LogInfoLevel.MAXIMUM)) {
					log.info("        Link id=" + link2.getId() + " merged (additive) into Link id=" + link1.getId());
				}

				double cap = link1.getCapacity() + link2.getCapacity();
				double fs = Math.max(link1.getFreespeed(),link2.getFreespeed());
				int lanes = NetworkUtils.getNumberOfLanesAsInt(link1) + NetworkUtils.getNumberOfLanesAsInt(link2);
				double length = Math.max(link1.getLength(),link2.getLength());
				//			String origid = "add-merge(" + link1.getId() + "," + link2.getId() + ")";
				link1.setCapacity(cap);
				link1.setFreespeed(fs);
				link1.setNumberOfLanes(lanes);
				link1.setLength(length);
				network.removeLink(link2.getId());
			}
			break;
			case MAXIMUM:
				if (logInfoLevel.equals(LogInfoLevel.MAXIMUM)) {
					log.info("        Link id=" + link2.getId() + " merged (maximum) into Link id=" + link1.getId());
				}

				{
					double cap = Math.max(link1.getCapacity(),link2.getCapacity());
					double fs = Math.max(link1.getFreespeed(),link2.getFreespeed());
					int lanes = Math.max(NetworkUtils.getNumberOfLanesAsInt(link1), NetworkUtils.getNumberOfLanesAsInt(link2));
					double length = Math.max(link1.getLength(),link2.getLength());
					//			String origid = "max-merge(" + link1.getId() + "," + link2.getId() + ")";
					link1.setCapacity(cap);
					link1.setFreespeed(fs);
					link1.setNumberOfLanes(lanes);
					link1.setLength(length);
					network.removeLink(link2.getId());
				}
				break;
			default:
				throw new IllegalArgumentException("'mergetype' not known!");
		}
	}

	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public void run(Network network) {
		for (Node n : network.getNodes().values()) {
			Iterator<? extends Link> l1_it = n.getOutLinks().values().iterator();
			while (l1_it.hasNext()) {
				Link l1 = l1_it.next();
				Iterator<? extends Link> l2_it = n.getOutLinks().values().iterator();
				while (l2_it.hasNext()) {
					Link l2 = l2_it.next();
					if (!l2.equals(l1)) {
						if (l2.getToNode().equals(l1.getToNode())) {
							if (logInfoLevel.equals(LogInfoLevel.MAXIMUM)) {
								log.info("      Node id=" + n.getId());
							}

							this.mergeLink2IntoLink1(l1, l2, network);
							// restart
							l1_it = n.getOutLinks().values().iterator();
							l2_it = n.getOutLinks().values().iterator();
						}
					}
				}
			}
		}
	}
}
