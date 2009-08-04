/* *********************************************************************** *
 * project: org.matsim.*
 * RawRouteImpl.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.johannes.plans.plain.impl;

import java.util.Collections;
import java.util.List;

import playground.johannes.plans.ModCount;
import playground.johannes.plans.plain.PlainRoute;

/**
 * @author illenberger
 *
 */
public class PlainRouteImpl implements PlainRoute, ModCount {

	private List<String> linkIds;
	
	private long modCount = 0;
	
	public List<String> getLinkIds() {
		return linkIds;
	}

	public long getModCount() {
		return modCount;
	}

	public void setLinkIds(List<String> linkIds) {
		this.linkIds = Collections.unmodifiableList(linkIds);
		modCount++;
	}

}
