/* *********************************************************************** *
 * project: org.matsim.*
 * NextLinkAction.java
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

import org.matsim.network.Link;

public class NextLinkOption extends Option {

	private final Link link;
	

	public NextLinkOption(final Link link, final double conf) {
		this.link = link;
		this.conf = conf;
	}
	
	public Link getNextLink() {
		return this.link;
	}
	
	
	
}
