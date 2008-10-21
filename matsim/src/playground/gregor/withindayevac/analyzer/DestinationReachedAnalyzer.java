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

package playground.gregor.withindayevac.analyzer;

import playground.gregor.withindayevac.Beliefs;
import playground.gregor.withindayevac.Intentions;

public class DestinationReachedAnalyzer implements Analyzer {

	private final Beliefs beliefs;
	private final Intentions intentions;
	private double coef;

	public DestinationReachedAnalyzer(final Beliefs beliefs, final Intentions intentions) {
		this.beliefs = beliefs;
		this.intentions = intentions;
	}
	
	public NextLinkOption getAction(final double now) {
		if (this.beliefs.getCurrentLink().getToNode().getId() == this.intentions.getDestination().getId()) {
			return new NextLinkOption(null,1);
		}

		return null;
	}

	public void setCoefficient(final double coef) {
		this.coef = coef;
	}

}
