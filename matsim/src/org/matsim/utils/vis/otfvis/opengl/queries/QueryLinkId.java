/* *********************************************************************** *
 * project: org.matsim.*
 * QueryLinkId.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.utils.vis.otfvis.opengl.queries;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import org.matsim.events.Events;
import org.matsim.mobsim.QueueLink;
import org.matsim.mobsim.QueueNetworkLayer;
import org.matsim.network.Link;
import org.matsim.plans.Plans;
import org.matsim.utils.vis.otfvis.interfaces.OTFDrawer;
import org.matsim.utils.vis.otfvis.interfaces.OTFQuery;


public class QueryLinkId implements OTFQuery {
	/**
	 * 
	 */
	private static final long serialVersionUID = -1389950511283282110L;
	
	double x;
	double y;
	double width = 0;
	double height = 0;
	public List<String> linkIds = new ArrayList<String>();

	public QueryLinkId(double x,double y) {
		this.x = x;
		this.y = y;
	}

	public QueryLinkId(Rectangle2D.Double rect) {
		this.x = rect.x;
		this.y = rect.y;
		this.width = rect.width;
		this.height = rect.height;
	}

	public void draw(OTFDrawer drawer) {
	}

	public void query(QueueNetworkLayer net, Plans plans, Events events) {
		double minDist = Double.POSITIVE_INFINITY, dist = 0, epsilon = 0.0001;
		Link link;
		for( QueueLink qlink : net.getLinks().values()) {
			link = qlink.getLink();
			
			double alpha = 0.6;
			double middleX = alpha*link.getFromNode().getCoord().getX() + (1.0-alpha)*link.getToNode().getCoord().getX();
			double middleY = alpha*link.getFromNode().getCoord().getY() + (1.0-alpha)*link.getToNode().getCoord().getY();
			
			double xDist = middleX - this.x;
			double yDist = middleY - this.y;
			if (this.width == 0) {
				// search for NEAREST agent to given POINT
				dist = Math.sqrt(xDist*xDist + yDist*yDist);
				if(dist <= minDist){
					// is  this just about the same distance, then put both into account
					if (minDist - dist > epsilon) this.linkIds.clear();
					
					minDist = dist;
					this.linkIds.add(link.getId().toString());
				}
			} else {
				// search for all agents in given RECT
				if( (xDist < width) && (yDist < height) && (xDist >= 0) && (yDist >= 0) ) {
					this.linkIds.add(link.getId().toString());
				}
			}
		}
	}

	public void remove() {
		// TODO Auto-generated method stub

	}
	
	public boolean isAlive() {
		return false;
	}

	public Type getType() {
		return OTFQuery.Type.OTHER;
	}

	public void setId(String id) {
		// TODO Auto-generated method stub
		
	}

}
