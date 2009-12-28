/******************************************************************************
 *project: org.matsim.*
 * PopulationPoint.java
 *                                                                            *
 * ************************************************************************** *
 *                                                                            *
 * copyright       : (C) 2009 by the members listed in the COPYING,           *
 *                   LICENSE and WARRANTY file.                               *
 * email           : info at matsim dot org                                   *
 *                                                                            *
 * ************************************************************************** *
 *                                                                            *
 *   This program is free software; you can redistribute it and/or modify     *
 *   it under the terms of the GNU General Public License as published by     *
 *   the Free Software Foundation; either version 2 of the License, or        *
 *   (at your option) any later version.                                      *
 *   See also COPYING, LICENSE and WARRANTY file                              *
 *                                                                            *
 ******************************************************************************/


package playground.rost.graph.populationpoint;

import playground.rost.graph.Point;

public class PopulationPoint {
	public Point point;
	public int population;
	
	public PopulationPoint(Point point, int population)
	{
		this.point = point;
		this.population = population;
	}
	
	public PopulationPoint(double x, double y, int population)
	{
		this.point = new Point(x,y);
		this.population = population;
	}
}
