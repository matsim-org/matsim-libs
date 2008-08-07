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

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.Map;

import org.matsim.events.Events;
import org.matsim.gbl.Gbl;
import org.matsim.mobsim.QueueLink;
import org.matsim.mobsim.QueueNetwork;
import org.matsim.network.Link;
import org.matsim.population.Population;
import org.matsim.utils.collections.QuadTree;
import org.matsim.utils.collections.QuadTree.Executor;
import org.matsim.utils.collections.QuadTree.Rect;
import org.matsim.utils.geometry.shared.Coord;
import org.matsim.utils.vis.otfvis.data.OTFDataWriter;
import org.matsim.utils.vis.otfvis.data.OTFServerQuad;
import org.matsim.utils.vis.otfvis.gui.OTFVisConfig;
import org.matsim.utils.vis.otfvis.interfaces.OTFDrawer;
import org.matsim.utils.vis.otfvis.interfaces.OTFQuery;
import org.matsim.utils.vis.otfvis.opengl.layer.SimpleStaticNetLayer;

 
public class QueryLinkId implements OTFQuery {
	/**
	 * 
	 */
	private static final long serialVersionUID = -1389950511283282110L;
	double sx;
	double sy;
	double width = 0;
	double height = 0;
	
	public Map<Coord, String> linkIds = new HashMap<Coord, String>();

	public QueryLinkId(double x,double y) {
		this.sx = x;
		this.sy = y;
	}

	public QueryLinkId(Rectangle2D.Double rect) {
		this.sx = rect.x;
		this.sy = rect.y;
		this.width = rect.width;
		this.height = rect.height;
	}

	public QueryLinkId(Rect rect) {
		this.sx = rect.minX;
		this.sy = rect.minY;
		this.width = rect.maxX - sx;
		this.height = rect.maxY - sy;
	}

	public void draw(OTFDrawer drawer) {
	}

	class AddIdStringExecutor extends Executor<OTFDataWriter> {
		private final boolean nearestOnly;
		private double minDist = Double.POSITIVE_INFINITY, dist = 0;
		final double epsilon = 0.0001;
		final double cellWidth;
		
		public AddIdStringExecutor(boolean nearestOnly) {
			this.nearestOnly = nearestOnly;
			cellWidth = ((OTFVisConfig)Gbl.getConfig().getModule("otfvis")).getLinkWidth();
		}
		
		@Override
		public void execute(double x, double y, OTFDataWriter writer)  {
			Object src = writer.getSrc();
			if(src instanceof QueueLink) {
				Link link = ((QueueLink)src).getLink();
				double alpha = 0.6;
				double fromX = link.getFromNode().getCoord().getX();
				double fromY = link.getFromNode().getCoord().getY();
				double middleX = alpha*fromX + (1.0-alpha)*link.getToNode().getCoord().getX();
				double middleY = alpha*fromY + (1.0-alpha)*link.getToNode().getCoord().getY();
				if(nearestOnly) {
					
					double xDist = middleX - sx;
					double yDist = middleY - sy;
						// search for NEAREST agent to given POINT
						dist = Math.sqrt(xDist*xDist + yDist*yDist);
						if(dist <= minDist){
							// is  this just about the same distance, then put both into account
							if (minDist - dist > epsilon) linkIds.clear();
							
							minDist = dist;
							Point2D.Float anchor = SimpleStaticNetLayer.SimpleQuadDrawer.calcOrtho(fromX, fromY, middleX, middleY, cellWidth/2.);			
							linkIds.put(new Coord(middleX + anchor.x, middleY + anchor.y), link.getId().toString());
						}
					
				} else {
					Point2D.Float anchor = SimpleStaticNetLayer.SimpleQuadDrawer.calcOrtho(fromX, fromY, middleX, middleY, cellWidth/2.);			
					linkIds.put(new Coord(middleX + anchor.x, middleY + anchor.y), link.getId().toString());
				}
			}
		}
	}
	
	public void query(QueueNetwork net, Population plans, Events events, OTFServerQuad quad) {
		double minDist = Double.POSITIVE_INFINITY, dist = 0, epsilon = 0.0001;
		
		// just look in a certain region around the actual point, 
		double regionWidth = (quad.getMaxEasting()-quad.getMinEasting())*0.1;
		double regionHeight = (quad.getMaxNorthing()-quad.getMinNorthing())*0.1;
		
		QuadTree.Rect rect;
		// The quadtree has its own coord system from (0,0) (max-minXY)
		double qsx = sx - quad.getMinEasting();
		double qsy = sy - quad.getMinNorthing();
		
		if (width == 0) rect = new QuadTree.Rect(qsx-regionWidth, qsy-regionHeight, qsx+regionWidth, qsy+regionHeight);
		else rect = new QuadTree.Rect(qsx,qsy,qsx+width, qsy+height);
		quad.execute(rect, new AddIdStringExecutor(width == 0));

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
