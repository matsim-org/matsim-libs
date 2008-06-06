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

package org.matsim.utils.vis.otfvis.opengl.queries;

import org.matsim.events.Events;
import org.matsim.mobsim.QueueNetworkLayer;
import org.matsim.plans.Plans;
import org.matsim.utils.vis.otfvis.handler.OTFLinkAgentsHandler;
import org.matsim.utils.vis.otfvis.interfaces.OTFDrawer;
import org.matsim.utils.vis.otfvis.interfaces.OTFQuery;


public class QueryToggleShowParking implements OTFQuery {

	// This is not a real query it just toggles the rendering of vehicles while activities
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -3558773539361553004L;

	public void draw(OTFDrawer drawer) {
		
	}

	public void query(QueueNetworkLayer net, Plans plans, Events events) {
		OTFLinkAgentsHandler.showParked = !OTFLinkAgentsHandler.showParked;
	}

	public void remove() {
		
	}

}
