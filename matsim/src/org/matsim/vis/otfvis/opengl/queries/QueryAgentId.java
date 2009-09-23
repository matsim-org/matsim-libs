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
import java.util.LinkedList;
import java.util.List;

import org.matsim.core.api.experimental.events.Events;
import org.matsim.core.mobsim.queuesim.QueueLink;
import org.matsim.core.mobsim.queuesim.QueueNetwork;
import org.matsim.core.population.PopulationImpl;
import org.matsim.vis.otfvis.data.OTFServerQuad;
import org.matsim.vis.otfvis.handler.OTFLinkAgentsHandler;
import org.matsim.vis.otfvis.interfaces.OTFDrawer;
import org.matsim.vis.otfvis.interfaces.OTFQuery;
import org.matsim.vis.snapshots.writers.PositionInfo;
import org.matsim.vis.snapshots.writers.PositionInfo.VehicleState;

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
public class QueryAgentId implements OTFQuery {

	private static final long serialVersionUID = -4466967514266968254L;
	private final double x;
	private final double y;
	private double width = 0;
	private double height = 0;
	public List<String> agentIds = new ArrayList<String>();

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

	public void draw(OTFDrawer drawer) {
	}

	public OTFQuery query(QueueNetwork net, PopulationImpl plans, Events events, OTFServerQuad quad) {
		double minDist = Double.POSITIVE_INFINITY;
		double dist = 0;
		for( QueueLink qlink : net.getLinks().values()) {
			List<PositionInfo> positions = new LinkedList<PositionInfo>();
			qlink.getVisData().getVehiclePositions(positions);
			for(PositionInfo info : positions) {
				
				if ((info.getVehicleState()== VehicleState.Parking) && !OTFLinkAgentsHandler.showParked) continue;

				double xDist = info.getEasting() - this.x;
				double yDist = info.getNorthing() - this.y;
				if (this.width == 0) {
					// search for NEAREST agent to given POINT
					dist = Math.sqrt(xDist*xDist + yDist*yDist);
					if(dist < minDist){
						minDist = dist;
						this.agentIds.clear();
						this.agentIds.add(info.getAgentId().toString());
					}
				} else {
					// search for all agents in given RECT
					if( (xDist < width) && (yDist < height) && (xDist >= 0) && (yDist >= 0) ) {
						this.agentIds.add(info.getAgentId().toString());
					}
				}
			}
		}
		return this;
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
