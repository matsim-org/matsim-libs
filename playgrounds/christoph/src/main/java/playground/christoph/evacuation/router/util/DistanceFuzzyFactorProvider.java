/* *********************************************************************** *
 * project: org.matsim.*
 * DistanceFuzzyFactorProvider.java
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

import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;

public class DistanceFuzzyFactorProvider {

	private final Map<Id, Map<Id, Double>> distanceFuzzyFactors;
	private final Set<Id> observedLinks;
	
	public DistanceFuzzyFactorProvider(Map<Id, Map<Id, Double>> distanceFuzzyFactors, Set<Id> observedLinks) {
		this.distanceFuzzyFactors = distanceFuzzyFactors;
		this.observedLinks = observedLinks;
	}
	
	public double getFuzzyFactor(Id fromLinkId, Id toLinkId) {
		
		/*
		 * If at least one of both links is not observed, there is no fuzzy factor
		 * stored in the lookup map. Therefore return 0.0. 
		 */
		if (!observedLinks.contains(fromLinkId) || !observedLinks.contains(toLinkId)) return 0.0;
		
		int cmp = fromLinkId.compareTo(toLinkId);
		if (cmp < 0) {
			Double factor = this.distanceFuzzyFactors.get(fromLinkId).get(toLinkId);
			if (factor == null) return 1.0;
			else return factor;
		} else if (cmp > 0) {
			Double factor = this.distanceFuzzyFactors.get(toLinkId).get(fromLinkId);
			if (factor == null) return 1.0;
			else return factor;
		} else return 0.0;
	}
}
