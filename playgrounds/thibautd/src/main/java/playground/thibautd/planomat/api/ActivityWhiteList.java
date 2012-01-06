/* *********************************************************************** *
 * project: org.matsim.*
 * ActivityWhiteList.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.thibautd.planomat.api;

/**
 * Defines the activity types which are modifiable by planomat.
 *
 * @author thibautd
 */
public interface ActivityWhiteList {
	/**
	 * Says whether an activity is modifiable or not.
	 *
	 * @param type the type to check
	 * @return true if Planomat is allowed to modify activities of this type
	 */
	public boolean isModifiableType(String type);
}

