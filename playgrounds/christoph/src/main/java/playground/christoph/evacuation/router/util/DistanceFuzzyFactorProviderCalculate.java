/* *********************************************************************** *
 * project: org.matsim.*
 * DistanceFuzzyFactorProviderCalculate.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.christoph.evacuation.router.util;

import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.geometry.CoordUtils;

/**
 * Calculates the fuzzy factors for link pairs from scratch.
 * 
 * @author cdobler
 */
public class DistanceFuzzyFactorProviderCalculate implements DistanceFuzzyFactorProvider {

	private final Set<Id> observedLinks;
	private final double c = 1 / Math.exp(4);	// constant for fuzzy factor creation
	
	public DistanceFuzzyFactorProviderCalculate(Set<Id> observedLinks) {
		this.observedLinks = observedLinks;
	}
	
	@Override
	public boolean isLinkObserved(Id linkId) {
		return this.observedLinks.contains(linkId);
	}
	
	@Override
	public double getFuzzyFactor(Link fromLink, Link toLink) {
		
		/*
		 * If at least one of both links is not observed, there is no fuzzy factor
		 * stored in the lookup map. Therefore return 0.0. 
		 */
		Id toLinkId = toLink.getId();
		/*
		 * FromLink is now observed in FuzzyTravelTimeEstimator. cdobler, jul'12
		 */
//		if (!observedLinks.contains(fromLinkId) || !observedLinks.contains(toLinkId)) return 0.0;
		if (!observedLinks.contains(toLinkId)) return 0.0;

		if (fromLink.equals(toLinkId)) return 0.0;
		
		double distance = CoordUtils.calcDistance(fromLink.getCoord(), toLink.getCoord());
		
//		double factor = 1 / (1 + Math.exp((-distance/1000.0) + 4.0));
//		double factor = c / (c + Math.exp((-distance/1000.0)));
		double c2 = c * Math.exp(distance/1000.0);
		double factor = c2 / (c2 + 1);
		
		// for backwards compatibility with lookup implementation
		if (factor > 0.98) return 1.0;
		else return factor;
	}
}
