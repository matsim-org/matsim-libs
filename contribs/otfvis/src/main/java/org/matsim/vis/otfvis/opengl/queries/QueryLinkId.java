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

package org.matsim.vis.otfvis.opengl.queries;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.collections.QuadTree.Executor;
import org.matsim.vis.otfvis.OTFClientControl;
import org.matsim.vis.otfvis.SimulationViewForQueries;
import org.matsim.vis.otfvis.data.OTFDataWriter;
import org.matsim.vis.otfvis.data.OTFServerQuadTree;
import org.matsim.vis.otfvis.handler.OTFLinkAgentsHandler;
import org.matsim.vis.otfvis.interfaces.OTFQuery;
import org.matsim.vis.otfvis.interfaces.OTFQueryResult;
import org.matsim.vis.otfvis.opengl.drawer.OTFOGLDrawer;
import org.matsim.vis.snapshotwriters.VisLink;

/**
 * QueryLinkId find the most likely link (or links) to a given coordinate (or rectangle).
 * The meaning of "nearest" is that of the smallest distance between the middle point of the link
 * and the given coordinate. 
 * TODO This might lead to unexpected behavior with long links. 
 * 
 * @author dstrippgen
 *
 */
public class QueryLinkId extends AbstractQuery {

	public static class Result implements OTFQueryResult {

		public Map<Coord, String> linkIds = new HashMap<Coord, String>();

		@Override
		public void remove() {

		}
		
		@Override
		public boolean isAlive() {
			return false;
		}

		@Override
		public void draw(OTFOGLDrawer drawer) {

		}

	}

	private final double sx;
	private final double sy;
	private double width = 0;
	private double height = 0;
	
	private Result result;

	public QueryLinkId(Rectangle2D.Double rect) {
		this.sx = rect.x;
		this.sy = rect.y;
		this.width = rect.width;
		this.height = rect.height;
	}

	class AddIdStringExecutor implements Executor<OTFDataWriter> {
		private final boolean nearestOnly;
		private double minDist = Double.POSITIVE_INFINITY;
		private static final double epsilon = 0.0001;
		private final double cellWidth;
		
		public AddIdStringExecutor(boolean nearestOnly) {
			this.nearestOnly = nearestOnly;
			cellWidth = OTFClientControl.getInstance().getOTFVisConfig().getLinkWidth();
		}
		
		@Override
		public void execute(double x, double y, OTFDataWriter writer)  {
			Object src = writer.getSrc();
			if(src instanceof VisLink) {
				Link link = ((VisLink)src).getLink();
				double alpha = 0.6;
				Coord from = link.getFromNode().getCoord();
				java.awt.geom.Point2D.Double transformedFrom = OTFServerQuadTree.transform(from);
				double fromX = transformedFrom.getX();
				double fromY = transformedFrom.getY();
				Coord to = link.getToNode().getCoord();
				java.awt.geom.Point2D.Double transformedTo = OTFServerQuadTree.transform(to);
				double middleX = alpha*fromX + (1.0-alpha)*transformedTo.getX();
				double middleY = alpha*fromY + (1.0-alpha)*transformedTo.getY();
				if (nearestOnly) {
					double xDist = middleX - sx;
					double yDist = middleY - sy;
					// search for NEAREST agent to given POINT
					double dist = Math.sqrt(xDist*xDist + yDist*yDist);
					if(dist <= minDist){
						// is this just about the same distance, then put both into account
						if (minDist - dist > epsilon) result.linkIds.clear();

						minDist = dist;
						Point2D.Float anchor = OTFLinkAgentsHandler.calcOrtho(fromX, fromY, middleX, middleY, cellWidth/2.);
						result.linkIds.put(new Coord(middleX + anchor.x, middleY + anchor.y), link.getId().toString());
					}

				} else {
					Point2D.Float anchor = OTFLinkAgentsHandler.calcOrtho(fromX, fromY, middleX, middleY, cellWidth/2.);
					result.linkIds.put(new Coord(middleX + anchor.x, middleY + anchor.y), link.getId().toString());
				}
			}
		}
	}
	
	@Override
	public void installQuery(SimulationViewForQueries simulationView) {
		this.result = new Result();
		OTFServerQuadTree quad = simulationView.getNetworkQuadTree();
		double regionWidth = (quad.getMaxEasting()-quad.getMinEasting())*0.1;
		double regionHeight = (quad.getMaxNorthing()-quad.getMinNorthing())*0.1;

		QuadTree.Rect rect;
		if (width == 0) {
			rect = new QuadTree.Rect(sx-regionWidth, sy-regionHeight, sx+regionWidth, sy+regionHeight);
		} else {
			rect = new QuadTree.Rect(sx,sy,sx+width,sy+height);
		}
		quad.execute(rect, new AddIdStringExecutor(width == 0));
	}

	@Override
	public Type getType() {
		return OTFQuery.Type.OTHER;
	}

	@Override
	public void setId(String id) {
	}

	@Override
	public OTFQueryResult query() {
		return result;
	}

}
