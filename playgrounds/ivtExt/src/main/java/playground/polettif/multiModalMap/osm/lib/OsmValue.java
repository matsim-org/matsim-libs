/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.polettif.multiModalMap.osm.lib;

import playground.polettif.multiModalMap.osm.core.OsmParser;

public class OsmValue {
	
	public final static String BUS = "bus";
	public final static String TROLLEYBUS = "trolleybus";
	public static final String RAIL = "rail";
	public static final String TRAM = "tram";
	public static final String LIGHT_RAIL = "light_rail";
	public static final String FUNICULAR = "funicular";
	public static final String MONORAIL = "monorail";
	public static final String SUBWAY = "subway";
	public static final String FERRY = "ferry";

	public static final String STOP = "stop";
	public static final String STOP_FORWARD = "stop_forward";
	public static final String STOP_BACKWARD = "stop_backward";

	public static final String STOP_POSITION = "stop_position";
	public static final String STOP_AREA = "stop_area";
	public static final String NODE = "node";
	public static final String BACKWARD = "backward";
}