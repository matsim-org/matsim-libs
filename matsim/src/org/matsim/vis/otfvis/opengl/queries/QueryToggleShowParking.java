/* *********************************************************************** *
 * project: org.matsim.*
 * QueryToggleShowParking.java
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

import org.matsim.core.events.Events;
import org.matsim.core.mobsim.queuesim.QueueNetwork;
import org.matsim.core.population.PopulationImpl;
import org.matsim.vis.otfvis.data.OTFServerQuad;
import org.matsim.vis.otfvis.handler.OTFLinkAgentsHandler;
import org.matsim.vis.otfvis.interfaces.OTFDrawer;
import org.matsim.vis.otfvis.interfaces.OTFQuery;

/**
 * QueryToggleShowParking is not a real query, it just toggles the 
 * rendering of vehicles while the respective agent attends an activity.
 * 
 */
public class QueryToggleShowParking implements OTFQuery {
	
	private static final long serialVersionUID = -3558773539361553004L;

	public void draw(OTFDrawer drawer) {
	}

	public OTFQuery query(QueueNetwork net, PopulationImpl plans, Events events, OTFServerQuad quad) {
		OTFLinkAgentsHandler.showParked = !OTFLinkAgentsHandler.showParked;
		return this;
	}

	public void remove() {
	}
	
	public boolean isAlive() {
		return false;
	}
	public Type getType() {
		return OTFQuery.Type.OTHER;
	}

	public void setId(String id) {
	}

}
