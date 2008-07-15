/* *********************************************************************** *
 * project: org.matsim.*
 * DestinationReachedAnalyzer.java
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

package playground.gregor.withinday_evac.analyzer;

import playground.gregor.withinday_evac.Beliefs;
import playground.gregor.withinday_evac.Intentions;

public class DestinationReachedAnalyzer implements Analyzer {

	private final Beliefs beliefs;
	private final Intentions intentions;

	public DestinationReachedAnalyzer(final Beliefs beliefs, final Intentions intentions) {
		this.beliefs = beliefs;
		this.intentions = intentions;
	}
	
	public Action getAction(final double now) {
		if (this.beliefs.getCurrentLink().getToNode().getId() == this.intentions.getDestination().getId()) {
			return new NextLinkAction(null,1);
		}

		return null;
	}

}
