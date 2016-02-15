/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.ivt.analysis.scoretracking;

import org.matsim.core.scoring.SumScoringFunction.BasicScoring;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author thibautd
 */
class PersonScoreTracker {
	private final Map<String, BasicScoring> scoringElements = new HashMap<>();

	public void addScoringFunction( final String name, final BasicScoring scoring ) {
		this.scoringElements.put(name, scoring);
	}

	public Map<String, Double> getDecomposedScoring() {
		final Map<String,Double> map = new LinkedHashMap<>();

		for (Map.Entry<String,BasicScoring> s :scoringElements.entrySet()) {
			map.put( s.getKey() , s.getValue().getScore() );
		}

		return map;
	}
}
