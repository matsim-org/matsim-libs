/* *********************************************************************** *
 * project: org.matsim.*
 * QueryLinkZoom.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.ptproject.qsim.QueueNetwork;
import org.matsim.vis.otfvis.data.OTFServerQuad2;
import org.matsim.vis.otfvis.interfaces.OTFDrawer;
import org.matsim.vis.otfvis.interfaces.OTFQuery;

@Deprecated
public class QueryLinkZoom implements OTFQuery {

	public void draw(OTFDrawer drawer) {
	}

	public Type getType() {
		return Type.OTHER;
	}

	public boolean isAlive() {
		return false;
	}

	public OTFQuery query(QueueNetwork net, Population plans, EventsManager events,
			OTFServerQuad2 quad) {
		return this;

	}

	public void remove() {
	}

	public void setId(String id) {
	}

}

