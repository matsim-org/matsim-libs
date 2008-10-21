/* *********************************************************************** *
 * project: org.matsim.*
 * Action.java
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

import org.matsim.network.Link;

public abstract class Option {

	protected double conf;
	protected Link link;
		
	
	
	public double getConfidence() {
		return this.conf;
	}
	
	public void setConfidence(final double conf) {
		this.conf = conf;
	}
	
	public Link getNextLink(){
		return this.link;
	}
}
