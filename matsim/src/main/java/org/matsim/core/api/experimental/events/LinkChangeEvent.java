/* *********************************************************************** *
 * project: org.matsim.*
 * LinkChangeEvent.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package org.matsim.core.api.experimental.events;

import org.matsim.api.core.v01.Id;
import org.matsim.core.network.NetworkChangeEvent.ChangeValue;

public interface LinkChangeEvent extends Event {

	public static final String CHANGETYPE = "changetype";
	public static final String CHANGETYPEABSOLUTE = "absolute";
	public static final String CHANGETYPEFACTOR = "factor";
	public static final String CHANGEVALUE = "changevalue";
	public static final String ATTRIBUTE_LINK = "link";
	
	public Id getLinkId();
	
	public ChangeValue getChangeValue();
}
