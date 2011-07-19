/* *********************************************************************** *
 * project: org.matsim.*
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

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.vis.otfvis.SimulationViewForQueries;
import org.matsim.vis.otfvis.data.OTFServerQuadTree;
import org.matsim.vis.otfvis.interfaces.OTFQuery;
import org.matsim.vis.otfvis.interfaces.OTFQueryRemote;
import org.matsim.vis.otfvis.interfaces.OTFQueryResult;
import org.matsim.vis.snapshots.writers.VisMobsimFeature;

public abstract class AbstractQuery implements OTFQuery, OTFQueryRemote {

	@Override
	public abstract Type getType();

	@Override
	public abstract void installQuery(VisMobsimFeature otfVisQueueSimFeature, EventsManager events, OTFServerQuadTree quad);

	@Override
	public abstract void setId(String id);

	@Override
	public abstract OTFQueryResult query();

	@Override
	public void uninstall() {
		// Default implementation doesn't do anything.
	}

	public void installQuery(SimulationViewForQueries queueModel) {
		
	}

}
