/* *********************************************************************** *
 * project: org.matsim.*
 * RawActivityImpl.java
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

import playground.johannes.plans.ModCount;
import playground.johannes.plans.plain.PlainActivity;

/**
 * @author illenberger
 *
 */
public class PlainActivityImpl implements PlainActivity, ModCount {

	public String getLinkId() {
		return "this is a link id";
	}

	public String getFacilityId() {
		return "this is a facility id";
	}

	/* (non-Javadoc)
	 * @see playground.johannes.plans.ModCount#getModCount()
	 */
	public long getModCount() {
		// TODO Auto-generated method stub
		return 0;
	}

}
