/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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


package playground.polettif.multiModalMap.mapping.pseudoPTRouter;

import org.matsim.core.utils.collections.Tuple;

/**
 * Describes the path between two link candidates for the pseudo network.
 */
public class LinkCandidatePath {

	private final Tuple<LinkCandidate, LinkCandidate> id;
	private final LinkCandidate from;
	private final LinkCandidate to;
	private final double weight;

	public LinkCandidatePath(LinkCandidate fromLinkCandidate, LinkCandidate toLinkCandidate, double travelTime) {
		this(fromLinkCandidate, toLinkCandidate, travelTime, false, false);
	}

	public LinkCandidatePath(LinkCandidate fromLinkCandidate, LinkCandidate toLinkCandidate, double travelTime, boolean firstPath, boolean lastPath) {
		this.id = new Tuple<>(fromLinkCandidate, toLinkCandidate);
		this.from = fromLinkCandidate;
		this.to = toLinkCandidate;

		if(firstPath) {
			this.weight = travelTime + 0.5*fromLinkCandidate.getLinkTravelTime();
		} else if(lastPath) {
			this.weight = travelTime + 0.5*toLinkCandidate.getLinkTravelTime();
		} else {
			this.weight = travelTime + 0.5*fromLinkCandidate.getLinkTravelTime() + 0.5*toLinkCandidate.getLinkTravelTime();
		}
	}

	public Tuple<LinkCandidate, LinkCandidate> getId() {
		return id;
	}
	public LinkCandidate getToLinkCandidate() {
		return to;
	}

	public LinkCandidate getFromLinkCandidate() {
		return from;
	}
	public double getWeight() {
		return weight;
	}

	@Override
	public String toString() {
		return from + " " + to;
	}
}