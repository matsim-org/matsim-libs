/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.johannes.gsv.synPop;

/**
 * @author johannes
 *
 */
public interface ActivityType {

	public static final String HOME = "home";

	public static final String WORK = "work";
	
	public static final String BUISINESS = "buisiness";
	
	public static final String LEISURE = "leisure";
	
	public static final String EDUCATION = "edu";
	
	public static final String SHOP = "shop";
	
	public static final String MISC = "misc";

	String VACATIONS_SHORT = "vacations_short";

	String VACATIONS_LONG = "vacations_long";
	
}
