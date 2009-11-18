/******************************************************************************
 *project: org.matsim.*
 * Point.java
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


package playground.rost.graph;

public class Point {
	public double x = 0;
	public double y = 0;
	
	public Point()
	{
		
	}
	
	public Point(double x, double y)
	{
		this.x = x;
		this.y = y;
	
	}
	
	@Override
	public boolean equals(Object other)
	{
		if(other instanceof Point)
		{
			Point pother = (Point)other;
			return (this.x == pother.x && this.y == pother.y);
		}
		return false;
	}
	
	
	@Override
	public int hashCode()
	{
		long bits = java.lang.Double.doubleToLongBits(x);
		bits ^= java.lang.Double.doubleToLongBits(y) * 31;
		return (((int) bits) ^ ((int) (bits >> 32)));
	}
	
}
