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

package org.matsim.contrib.grips.model.shape;

import com.vividsolutions.jts.geom.Polygon;


public class PolygonShape extends Shape
{
	private Polygon polygon;
	private java.awt.Polygon pixelPolygon;
	
	public PolygonShape(int layerID, Polygon polygon)
	{
		this.layerID = layerID;
		this.polygon = polygon;
		
		this.id = (++Shape.currentNumberId) + "_poly";

	}
	
	public Polygon getPolygon()
	{
		return polygon;
	}
	
	public java.awt.Polygon getPixelPolygon()
	{
		return pixelPolygon;
	}
	
	public void setPixelPolygon(java.awt.Polygon pixelPolygon)
	{
		this.pixelPolygon = pixelPolygon;
	}

	public void setPolygon(Polygon polygon)
	{
		this.polygon = polygon;
	}
	


}
