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

import com.google.common.base.Verify;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.internal.NetworkRunnable;
import org.matsim.core.network.turnRestrictions.DisallowedNextLinks;
import org.matsim.core.network.turnRestrictions.DisallowedNextLinksUtils;
import org.matsim.core.network.NetworkUtils;

import java.util.*;

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

		Map<Id<Link>, Replacement> replacedLinks = new HashMap<>();
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
							replacedLinks.put(l2.getId(), new Replacement(l2, l1));
							// restart
							l1_it = n.getOutLinks().values().iterator();
							l2_it = n.getOutLinks().values().iterator();
						}
					}
				}
			}
		}

		handleTurnRestrictions(network, replacedLinks);
	}

	private void handleTurnRestrictions(Network network, Map<Id<Link>, Replacement> replacedLinks) {

		for (Replacement mergedLinks : replacedLinks.values()) {
			mergeTurnRestrictions(mergedLinks.substitute, mergedLinks.orginal, replacedLinks);
		}

		for (Link link : network.getLinks().values()) {
			DisallowedNextLinks disallowedNextLinks = NetworkUtils.getDisallowedNextLinks(link);

			if(disallowedNextLinks != null) {

				Map<String, List<List<Id<Link>>>> restrictionsPerMode = disallowedNextLinks.getAsMap();
				Map<String, List<List<Id<Link>>>> restrictionsPerModeCopy = new HashMap<>();

				for (Map.Entry<String, List<List<Id<Link>>>> restrictions : restrictionsPerMode.entrySet()) {
					List<List<Id<Link>>> restrictionsCopy = new ArrayList<>();
					for (List<Id<Link>> restriction : restrictions.getValue()) {
						List<Id<Link>> restrictionCopy = new ArrayList<>();
						for (Id<Link> linkId : restriction) {
							if(replacedLinks.containsKey(linkId)) {
                            	restrictionCopy.add(replacedLinks.get(linkId).substitute.getId());
							} else {
								restrictionCopy.add(linkId);
							}
						}
						restrictionsCopy.add(restrictionCopy);
					}
					restrictionsPerModeCopy.put(restrictions.getKey(), restrictionsCopy);
				}

				disallowedNextLinks.clear();
				restrictionsPerModeCopy.forEach((mode, sequences) ->
						sequences.forEach(sequence ->
								disallowedNextLinks.addDisallowedLinkSequence(mode, sequence)
						)
				);
			}
		}

		Verify.verify(DisallowedNextLinksUtils.isValid(network));
	}

	private static void mergeTurnRestrictions(Link link1, Link link2, Map<Id<Link>, Replacement> replacedLinks) {
		DisallowedNextLinks disallowedNextLinks1 = NetworkUtils.getDisallowedNextLinks(link1);
		DisallowedNextLinks disallowedNextLinks2 = NetworkUtils.getDisallowedNextLinks(link2);
		if (disallowedNextLinks1 != null) {
			if (disallowedNextLinks2 != null) {

				Map<String, List<List<Id<Link>>>> perMode1 = disallowedNextLinks1.getAsMap();
				Map<String, List<List<Id<Link>>>> perMode2 = disallowedNextLinks2.getAsMap();
				Map<String, List<List<Id<Link>>>> merged = new HashMap<>();

				for (Map.Entry<String, List<List<Id<Link>>>> restrictionsPerMode : perMode1.entrySet()) {
					if (perMode2.containsKey(restrictionsPerMode.getKey())) {
						for (List<Id<Link>> forbiddenSequence : restrictionsPerMode.getValue()) {
							Id<Link> target = forbiddenSequence.getLast();
							if(replacedLinks.containsKey(target)) {
								target = replacedLinks.get(target).substitute.getId();
							}

							for (List<Id<Link>> forbiddenSequence2 : perMode2.get(restrictionsPerMode.getKey())) {
								Id<Link> target2 = forbiddenSequence2.getLast();
								if(replacedLinks.containsKey(target2)) {
									target2 = replacedLinks.get(target2).substitute.getId();
								}
								if(target.equals(target2)) {
									merged.computeIfAbsent(restrictionsPerMode.getKey(), k -> new ArrayList<>()).add(forbiddenSequence);
								}
							}
						}
					}
				}
				disallowedNextLinks1.clear();
				merged.forEach((mode, sequences) -> sequences.forEach(sequence -> disallowedNextLinks1.addDisallowedLinkSequence(mode, sequence)));
			} else {
				NetworkUtils.removeDisallowedNextLinks(link1);
			}
		}
	}

	private record Replacement(Link orginal, Link substitute){}

}
