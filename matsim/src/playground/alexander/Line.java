/* *********************************************************************** *
 * project: org.matsim.*
 * Line.java
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

package playground.alexander;

import java.util.ArrayList;
import java.util.List;

import com.vividsolutions.jts.geom.Coordinate;

public class Line {

	private List points = new ArrayList();
	private List neighbours = new ArrayList();
	private List widths = new ArrayList();
	private double width;
	
	public double getWidth(){return width;} 
	public void setWidth(double tmpWidth){width = tmpWidth;}
	public void addPoint(Coordinate coordinate){points.add(coordinate);}
	public List getPoints(){return points;}
	public void addWidth(double tmpWidth){widths.add(tmpWidth);}
	public List getWidths(){return widths;}

}
