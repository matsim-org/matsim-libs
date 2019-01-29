/* *********************************************************************** *
 * project: org.matsim.*
 * QueryAgentId.java
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

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.vis.otfvis.SimulationViewForQueries;
import org.matsim.vis.otfvis.data.OTFServerQuadTree;
import org.matsim.vis.otfvis.handler.OTFLinkAgentsHandler;
import org.matsim.vis.otfvis.interfaces.OTFQuery;
import org.matsim.vis.otfvis.interfaces.OTFQueryResult;
import org.matsim.vis.otfvis.opengl.drawer.OTFOGLDrawer;
import org.matsim.vis.snapshotwriters.AgentSnapshotInfo;
import org.matsim.vis.snapshotwriters.AgentSnapshotInfo.AgentState;
import org.matsim.vis.snapshotwriters.VisLink;

/**
 * QueryAgentId is an internal query.
 * For a given coordinate it tries to find the most likely agent.
 * It does that by first finding the most likely link to the clicked coordinate and
 * then the agent nearest to the coordinate on that link.
 * TODO This might yield unexpected results. It would probably be a better solution to collect
 * the agents of all surrounding link and then chose the nearest one.
 *
 * @author dstrippgen
 *
 */
public class QueryAgentId extends AbstractQuery {

	private final double x;
	private final double y;
	private double width = 0;
	private double height = 0;
	private Result result;

	public QueryAgentId(double x,double y) {
		this.x = x;
		this.y = y;
	}

	public QueryAgentId(Rectangle2D.Double rect) {
		this.x = rect.x;
		this.y = rect.y;
		this.width = rect.width;
		this.height = rect.height;
	}

	@Override
	public void installQuery(SimulationViewForQueries simulationView) {
		this.result = new Result();
		double minDist = Double.POSITIVE_INFINITY;
		double dist = 0;
		Collection<AgentSnapshotInfo> positions = new LinkedList<AgentSnapshotInfo>();
		for(VisLink qlink : simulationView.getVisNetwork().getVisLinks().values()) {
			qlink.getVisData().addAgentSnapshotInfo(positions);
		}
		positions = simulationView.getNonNetwokAgentSnapshots().addAgentSnapshotInfo(positions);
		for(AgentSnapshotInfo info : positions) {
			if ((info.getAgentState()== AgentState.PERSON_AT_ACTIVITY) && !OTFLinkAgentsHandler.showParked) continue;
			java.awt.geom.Point2D.Double xy = OTFServerQuadTree.transform(new Coord(info.getEasting(), info.getNorthing()));
			double xDist = xy.getX() - this.x;
			double yDist = xy.getY() - this.y;
			if (this.width == 0) {
				// search for NEAREST agent to given POINT
				dist = Math.sqrt(xDist*xDist + yDist*yDist);//"dist_2 = xDist*xDist + yDist*yDist" will work here (no need for "sqrt"), michalm
				if(dist < minDist){
					minDist = dist;
					this.result.agentIds.clear();
					this.result.agentIds.add(info.getId().toString());
				}
			} else {
				// search for all agents in given RECT
				if( (xDist < this.width) && (yDist < this.height) && (xDist >= 0) && (yDist >= 0) ) {
					this.result.agentIds.add(info.getId().toString());
				}
			}
		}
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
		return this.result;
	}

	public static class Result implements OTFQueryResult {

		public List<String> agentIds = new ArrayList<String>();

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

}
