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

package playground.mzilske.vis;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.vis.otfvis.OTFVisQSimFeature;
import org.matsim.vis.otfvis.data.OTFServerQuad2;
import org.matsim.vis.otfvis.handler.OTFLinkAgentsHandler;
import org.matsim.vis.otfvis.interfaces.OTFDrawer;
import org.matsim.vis.otfvis.interfaces.OTFQuery;
import org.matsim.vis.otfvis.interfaces.OTFQueryResult;
import org.matsim.vis.otfvis.opengl.queries.AbstractQuery;
import org.matsim.vis.otfvis.opengl.queries.QueryQueueModel;
import org.matsim.vis.snapshots.writers.AgentSnapshotInfo;
import org.matsim.vis.snapshots.writers.AgentSnapshotInfo.AgentState;

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
public class EventBasedQueryAgentId extends AbstractQuery {

	private static final long serialVersionUID = -4466967514266968254L;
	private final double x;
	private final double y;
	private double width = 0;
	private double height = 0;
	private Result result;

	public EventBasedQueryAgentId(double x,double y) {
		this.x = x;
		this.y = y;
	}

	public EventBasedQueryAgentId(Rectangle2D.Double rect) {
		this.x = rect.x;
		this.y = rect.y;
		this.width = rect.width;
		this.height = rect.height;
	}

	@Override
	public void installQuery(OTFVisQSimFeature queueSimulation, EventsManager events, OTFServerQuad2 quad) {

	}

	@Override
	public void installQuery(Scenario scenario, QueryQueueModel queueModel) {
		this.result = new Result();
		double minDist = Double.POSITIVE_INFINITY;
		double dist = 0;
		for(AgentSnapshotInfo info : queueModel.getSnapshot()) {
			if ((info.getAgentState()== AgentState.PERSON_AT_ACTIVITY) && !OTFLinkAgentsHandler.showParked) continue;
			double xDist = info.getEasting() - this.x;
			double yDist = info.getNorthing() - this.y;
			if (this.width == 0) {
				// search for NEAREST agent to given POINT
				dist = Math.sqrt(xDist*xDist + yDist*yDist);
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

		private static final long serialVersionUID = 1L;

		public List<String> agentIds = new ArrayList<String>();

		public void remove() {

		}

		public boolean isAlive() {
			return false;
		}

		public void draw(OTFDrawer drawer) {

		}

	}

}
