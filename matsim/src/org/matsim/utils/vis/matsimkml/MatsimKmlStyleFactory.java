/* *********************************************************************** *
 * project: org.matsim.*
 * MatsimKmlStyleFactory.java
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

package org.matsim.utils.vis.matsimkml;

import org.matsim.utils.vis.kml.ColorStyle;
import org.matsim.utils.vis.kml.Icon;
import org.matsim.utils.vis.kml.IconStyle;
import org.matsim.utils.vis.kml.LineStyle;
import org.matsim.utils.vis.kml.Style;
import org.matsim.utils.vis.kml.fields.Color;


/**
 * @author dgrether
 *
 */
public class MatsimKmlStyleFactory {
	/**
	 * 
	 */
	public static final String DEFAULTNODEICON = "res/icon18.png";
	/**
	 * the scale for the icons
	 */
	private static final double ICONSCALE = 0.5;
	/**
	 * some colors frequently used in matsim: bgr: 15,15,190
	 */
	public static final Color MATSIMRED = new Color("ff","0f","0f","be");
	/**
	 * some colors frequently used in matsim
	 */
	public static final Color MATSIMBLUE = new Color(190, 10, 80, 190);
	/**
	 * some colors frequently used in matsim
	 */
	public static final Color MATSIMGREY = new Color(210, 50, 50, 70);
	/**
	 * some colors frequently used in matsim
	 */
	public static final Color MATSIMWHITE = new Color(230, 230, 230, 230);
	
	
	
	
	public Style createDefaultNetworkStyle() {
		Style style = new Style("defaultnetworkstyle");
		Icon icon = new Icon(DEFAULTNODEICON);
		IconStyle iStyle = new IconStyle(icon, MATSIMWHITE, IconStyle.DEFAULT_COLOR_MODE, ICONSCALE);
		style.setIconStyle(iStyle);
		LineStyle lineStyle = new LineStyle(MATSIMGREY, ColorStyle.DEFAULT_COLOR_MODE, 12);
		style.setLineStyle(lineStyle);
		return style;
	}

	
	
	
	
	
	
}
