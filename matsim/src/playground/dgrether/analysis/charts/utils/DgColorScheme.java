/* *********************************************************************** *
 * project: org.matsim.*
 * DgColorScheme
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
package playground.dgrether.analysis.charts.utils;

import java.awt.Color;


/**
 * @author dgrether
 *
 */
public class DgColorScheme {

	//firebrick
	public Color COLOR1A = new Color(178, 34, 34, 255);
	
	public Color COLOR1B = new Color(178, 34, 34, 60);
	//forest green
	public Color COLOR2A = new Color(34, 139, 34, 255);

	public Color COLOR2B = new Color(34, 139, 34, 60);
	//midnight blue
	public Color COLOR3A = new Color(0, 0, 205, 255);

	public Color COLOR3B = new Color(0, 0, 205, 100);
	//chocolate
	public Color COLOR4A = new Color(210, 105, 30, 255);

	public Color COLOR4B = new Color(210, 105, 30, 100);

	public Color getColor(int i, String a){
		if ((i == 1) && a.equalsIgnoreCase("a")){
			return COLOR1A;
		}
		else if ((i == 1) && a.equalsIgnoreCase("b")){
			return COLOR1B;
		}
		else if ((i == 2) && a.equalsIgnoreCase("a")){
			return COLOR2A;
		}
		else if ((i == 2) && a.equalsIgnoreCase("b")){
			return COLOR2B;
		}
		else if ((i == 3) && a.equalsIgnoreCase("a")){
			return COLOR3A;
		}
		else if ((i == 3) && a.equalsIgnoreCase("b")){
			return COLOR3B;
		}
		else if ((i == 4) && a.equalsIgnoreCase("a")){
			return COLOR4A;
		}
		else if ((i == 4) && a.equalsIgnoreCase("b")){
			return COLOR4B;
		}
		throw new IllegalArgumentException("wrong arguments: " + i + " " + a);
	}
	
	
}
