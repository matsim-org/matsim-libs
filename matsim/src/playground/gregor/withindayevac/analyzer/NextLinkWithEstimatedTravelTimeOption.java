/* *********************************************************************** *
 * project: org.matsim.*
 * NextLinkWithEstimatedTravelTimeOption.java
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

public class NextLinkWithEstimatedTravelTimeOption extends Option {

	private final double estTime;

	public NextLinkWithEstimatedTravelTimeOption(final Link link, final double conf, final double estTime){
		this.link = link;
		this.conf = conf;
		this.estTime = estTime;
	}
	
	public double getEstTTime() {
		return this.estTime;
	}

}
