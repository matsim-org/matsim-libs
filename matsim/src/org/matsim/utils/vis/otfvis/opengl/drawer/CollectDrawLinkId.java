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

package org.matsim.utils.vis.otfvis.opengl.drawer;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.Map;

import org.matsim.events.Events;
import org.matsim.gbl.Gbl;
import org.matsim.interfaces.core.v01.Link;
import org.matsim.interfaces.core.v01.Population;
import org.matsim.mobsim.queuesim.QueueLink;
import org.matsim.mobsim.queuesim.QueueNetwork;
import org.matsim.utils.collections.QuadTree;
import org.matsim.utils.collections.QuadTree.Executor;
import org.matsim.utils.collections.QuadTree.Rect;
import org.matsim.utils.geometry.CoordImpl;
import org.matsim.utils.vis.otfvis.data.OTFClientQuad;
import org.matsim.utils.vis.otfvis.data.OTFDataWriter;
import org.matsim.utils.vis.otfvis.data.OTFServerQuad;
import org.matsim.utils.vis.otfvis.gui.OTFVisConfig;
import org.matsim.utils.vis.otfvis.handler.OTFDefaultLinkHandler;
import org.matsim.utils.vis.otfvis.interfaces.OTFDataReader;
import org.matsim.utils.vis.otfvis.interfaces.OTFDrawer;
import org.matsim.utils.vis.otfvis.interfaces.OTFQuery;
import org.matsim.utils.vis.otfvis.opengl.layer.SimpleStaticNetLayer;
import org.matsim.utils.vis.otfvis.opengl.layer.SimpleStaticNetLayer.SimpleQuadDrawer;

public class CollectDrawLinkId {

	private static final long serialVersionUID = -1389950511283282110L;
	private double sx;
	private double sy;
	private double width = 0;
	private double height = 0;
	
	public Map<CoordImpl, String> linkIds = new HashMap<CoordImpl, String>();

	public CollectDrawLinkId(double x,double y) {
		this.sx = x;
		this.sy = y;
	}

	public CollectDrawLinkId(Rectangle2D.Double rect) {
		this.sx = rect.x;
		this.sy = rect.y;
		this.width = rect.width;
		this.height = rect.height;
	}

	public CollectDrawLinkId(Rect rect) {
		this.sx = rect.minX;
		this.sy = rect.minY;
		this.width = rect.maxX - sx;
		this.height = rect.maxY - sy;
	}

	public void draw(OTFDrawer drawer) {
	}

	class AddIdStringExecutor implements Executor<OTFDataReader> {
		private final boolean nearestOnly;
		private double minDist = Double.POSITIVE_INFINITY;
		private static final double epsilon = 0.0001;
		private final double cellWidth;
		
		public AddIdStringExecutor(boolean nearestOnly) {
			this.nearestOnly = nearestOnly;
			cellWidth = ((OTFVisConfig)Gbl.getConfig().getModule("otfvis")).getLinkWidth();
		}
		
		public void execute(double x, double y, OTFDataReader reader)  {
			if(reader instanceof OTFDefaultLinkHandler) {
				
				SimpleQuadDrawer drawer = (SimpleQuadDrawer)((OTFDefaultLinkHandler)reader).getQuadReceiver();
				if(drawer != null) drawer.prepareLinkId(linkIds);
			}
		}
	}
	
	public void prepare(OTFClientQuad quad) {
		
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
}
