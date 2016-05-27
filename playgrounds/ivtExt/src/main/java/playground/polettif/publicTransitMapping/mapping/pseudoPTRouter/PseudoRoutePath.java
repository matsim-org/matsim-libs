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


package playground.polettif.publicTransitMapping.mapping.pseudoPTRouter;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.core.utils.collections.Tuple;
import playground.polettif.publicTransitMapping.config.PublicTransitMappingConfigGroup;

/**
 * Describes the path between two pseudoRouteStops for the pseudoGraph (used by the
 * Dijkstra algorithm).
 *
 * @author polettif
 */
@Deprecated
public class PseudoRoutePath implements Identifiable<PseudoRoutePath> {

	private static PublicTransitMappingConfigGroup config = null;

	private final Id<PseudoRoutePath> id;
	private final PseudoRouteStop from;
	private final PseudoRouteStop to;
	private final double weight;
	private boolean dummy;

	public static void setConfig(PublicTransitMappingConfigGroup configGroup) {
		config = configGroup;
	}

	public PseudoRoutePath(PseudoRouteStop fromStop, PseudoRouteStop toStop, double weight) {
		this(fromStop, toStop, weight, false);
	}

	public PseudoRoutePath(PseudoRouteStop fromStop, PseudoRouteStop toStop, double weight, boolean dummy) {
		this.id = Id.create(fromStop.getId() + "->" + toStop.getId(), PseudoRoutePath.class);
		this.from = fromStop;
		this.to = toStop;
		this.dummy = dummy;

		this.weight = weight + (dummy ? 0 : 0.5 * fromStop.getLinkWeight() + 0.5 * toStop.getLinkWeight());
	}

	@Override
	public Id<PseudoRoutePath> getId() {
		return id;
	}

	public double getWeight() {
		return weight;
	}

	@Override
	public String toString() {
		return id.toString();
	}

	public PseudoRouteStop getFromPseudoStop() {
		return from;
	}

	public PseudoRouteStop getToPseudoStop() {
		return to;
	}

	public boolean isDummy() {
		return dummy;
	}

	@Override
	public boolean equals(Object obj) {
		if(this == obj)
			return true;
		if(obj == null)
			return false;
		if(getClass() != obj.getClass())
			return false;

		PseudoRoutePath other = (PseudoRoutePath) obj;
		if(id == null) {
			if(other.id != null)
				return false;
		} else if(!id.equals(other.id))
			return false;
		return true;
	}

}