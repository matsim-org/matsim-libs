/* *********************************************************************** *
 * project: org.matsim.*
 * DgOtfLaneData
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
package playground.dgrether.signalVis.drawer;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import playground.dgrether.signalVis.io.DgOtfLaneWriter;

public class DgOtfLaneData {
	private String id;
	private Point2D.Double endPoint;
	private boolean isGreen = false;
	private List<Point2D.Double> toLinkStartPoints;

	public DgOtfLaneData() {
		if (DgOtfLaneWriter.DRAW_LINK_TO_LINK_LINES) {
			this.toLinkStartPoints = new ArrayList<Point2D.Double>();
		}
	}
	
	public void setId(String id){
		this.id = id;
	}

	public void setEndPoint(double endx, double endy) {
		this.endPoint = new Point2D.Double(endx, endy);
	}

	public void setGreen(boolean isGreen) {
		this.isGreen = isGreen;
	}
	
	public boolean isGreen(){
		return this.isGreen ;
	}
	
	public Point2D.Double getEndPoint() {
		return endPoint;
	}
	
	public String getId() {
		return id;
	}

	public List<Point2D.Double> getToLinkStartPoints(){
		return this.toLinkStartPoints;
	}
	
}