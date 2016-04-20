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
public class PseudoRoutePath {

	private final Tuple<PseudoRouteStop, PseudoRouteStop> id;
	private final PseudoRouteStop from;
	private final PseudoRouteStop to;
	private final double weight;

	public PseudoRoutePath(PseudoRouteStop fromStop, PseudoRouteStop toStop, double travelTime) {
		this(fromStop, toStop, travelTime, false);
	}

	public PseudoRoutePath(PseudoRouteStop fromStop, PseudoRouteStop toStop, double travelTime, boolean dummy) {
		this.id = new Tuple<>(fromStop, toStop);
		this.from = fromStop;
		this.to = toStop;

		this.weight = travelTime + (dummy ? 0 : 0.5 * fromStop.getLinkCandidate().getLinkTravelTime() + 0.5 * toStop.getLinkCandidate().getLinkTravelTime());
	}

	public Tuple<PseudoRouteStop, PseudoRouteStop> getId() {
		return id;
	}

	public double getWeight() {
		return weight;
	}

	@Override
	public String toString() {
		return to.getName()+";"+from.getName();
	}

	public PseudoRouteStop getFromPseudoStop() {
		return from;
	}

	public PseudoRouteStop getToPseudoStop() {
		return to;
	}

}