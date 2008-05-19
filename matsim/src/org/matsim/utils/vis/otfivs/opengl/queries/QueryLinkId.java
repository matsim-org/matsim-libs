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

package org.matsim.utils.vis.otfivs.opengl.queries;

import org.matsim.events.Events;
import org.matsim.mobsim.QueueLink;
import org.matsim.mobsim.QueueNetworkLayer;
import org.matsim.network.Link;
import org.matsim.plans.Plans;
import org.matsim.utils.geometry.CoordI;
import org.matsim.utils.vis.otfivs.interfaces.OTFDrawer;
import org.matsim.utils.vis.otfivs.interfaces.OTFQuery;


public class QueryLinkId implements OTFQuery {
	public String linkId = null;;
	double x;
	double y;

	public QueryLinkId(double x,double y) {
		this.x = x;
		this.y = y;
	}

	public void draw(OTFDrawer drawer) {
	}

	public void query(QueueNetworkLayer net, Plans plans, Events events) {
		double minDist = Double.POSITIVE_INFINITY, dist = 0;;
		Link link;
		for( QueueLink qlink : net.getLinks().values()) {
			link = qlink.getLink();
			CoordI middle = link.getCenter();
			double xDist = this.x - middle.getX();
			double yDist = this.y - middle.getY();
			dist = Math.sqrt(xDist*xDist + yDist*yDist);
			if(dist < minDist){
				minDist = dist;
				this.linkId = link.getId().toString();
			}
		}
	}

	public void remove() {
		// TODO Auto-generated method stub

	}

}
