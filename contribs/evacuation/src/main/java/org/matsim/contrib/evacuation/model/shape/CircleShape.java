/* *********************************************************************** *
 * project: org.matsim.*
 * MyMapViewer.java
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

package org.matsim.contrib.evacuation.model.shape;

import java.awt.Point;
import java.awt.geom.Point2D;

public class CircleShape extends Shape
{
	private Point2D origin;
	private Point2D destination;
	private double radius;
	
	private Point pixelOrigin;
	private Point pixelDestination;
	private int pixelRadius;
	
	public CircleShape(int layerID, Point2D origin, Point2D destination)
	{
		this("",layerID,origin,destination);
	}
	
	public CircleShape(String circleID, int layerID, Point2D origin, Point2D destination)
	{
		this.layerID = layerID;
		this.origin = origin;
		this.destination = destination;
		
		//calculate radius
		double x1 = origin.getX();
		double y1 = origin.getY();
		double x2 = destination.getX();
		double y2 = destination.getY();
		this.radius = Math.sqrt((x1-x2)*(x1-x2) + (y1-y2)*(y1-y2) + .00001);
		
		if ((circleID == null) || (circleID.equals("")))
			this.id = "circle_" + (++Shape.currentNumberId);
		else
			this.id = circleID;

	}
	
	public Point2D getOrigin()
	{
		return origin;
	}
	
	public void setOrigin(Point2D origin)
	{
		this.origin = origin;
	}
	
	public void setDiameter(double diameter)
	{
		this.radius = diameter;
	}
	
	public double getDiameter()
	{
		return radius;
	}
	
	public int getPixelRadius()
	{
		return pixelRadius;
	}
	
	public Point getPixelOrigin()
	{
		return pixelOrigin;
	}
	
	public void setPixelRadius(int pixelRadius)
	{
		this.pixelRadius = pixelRadius;
	}
	
	public void setPixelOrigin(Point pixelOrigin)
	{
		this.pixelOrigin = pixelOrigin;
	}
	
	public Point2D getDestination()
	{
		return destination;
	}
	
	public void setDestination(Point2D destination)
	{
		this.destination = destination;
	}
	
	public Point getPixelDestination()
	{
		return pixelDestination;
	}
	
	public void setPixelDestination(Point pixelDestination)
	{
		this.pixelDestination = pixelDestination;
	}
	

}
