/* *********************************************************************** *
 * project: org.matsim.*
 * ChooseRandomLink.java
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

import org.matsim.gbl.MatsimRandom;
import org.matsim.network.Link;

import playground.gregor.withinday_evac.Beliefs;

public class ChooseRandomLinkAnalyzer implements Analyzer {

	private double coef;
	private final Beliefs beliefs;
	public ChooseRandomLinkAnalyzer(final Beliefs beliefs) {
		this.beliefs = beliefs;
	}
	
	
	
	public NextLinkOption getAction(final double now) {
		Link link = this.beliefs.getCurrentLink();
		double selnum = link.getToNode().getOutLinks().size() * MatsimRandom.random.nextDouble();
		for (Link l : link.getToNode().getOutLinks().values()) {
			selnum -= 1.0;
			if (selnum <= 0) {
				return new NextLinkOption(l,1 * this.coef);
			}
			
			
		}
		
		return null;
	}
	
	public void setCoefficient(final double coef) {
		this.coef = coef;
		
	}

}
