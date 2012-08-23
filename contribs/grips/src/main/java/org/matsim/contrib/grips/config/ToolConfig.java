/* *********************************************************************** *
 * project: org.matsim.*
 * EvacuationPTLinesEditor.java
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

package org.matsim.contrib.grips.config;

import java.awt.Color;

public abstract class ToolConfig {
	
	//colors
	public static Color COLOR_EVAC_AREA = new Color(235,30,0,80);
	public static Color COLOR_EVAC_AREA_BORDER = new Color(235,80,0);
	public static Color COLOR_POP_AREA = new Color(0,180,50,100);
	public static Color COLOR_POP_AREA_BORDER = new Color(0,130,50,170);
	
	public static Color COLOR_ROAD_1 = new Color(255,0,0);
	public static Color COLOR_ROAD_2 = new Color(0,255,0);
	
	//hovering
	public static Color COLOR_ROAD_HOVER = new Color(255,255,0,100);
	public static Color COLOR_AREA_HOVER = new Color(0,255,100,128);
	
	//selected
	public static Color COLOR_ROAD_SELECTED = new Color(0,0,255,190);
	public static Color COLOR_AREA_SELECTED = new Color(0,255,111,170);
	
	//misc
	public static Color COLOR_ROAD_CLOSED = new Color(150,0,0,220);
}
