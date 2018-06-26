/* *********************************************************************** *
 * project: org.matsim.*
 * OTFLaneReader2
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package org.matsim.contrib.signals.otfvis;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.matsim.core.utils.misc.ByteBufferUtils;
import org.matsim.lanes.VisLaneModelBuilder;
import org.matsim.lanes.VisLinkWLanes;
import org.matsim.vis.otfvis.caching.SceneGraph;
import org.matsim.vis.otfvis.interfaces.OTFDataReader;


/**
 * @author dgrether
 *
 */
public class OTFLaneReader extends OTFDataReader {
	
	protected OTFLaneSignalDrawer drawer = new OTFLaneSignalDrawer();

	private VisLaneModelBuilder laneModelBuilder = new VisLaneModelBuilder();
	
	public OTFLaneReader(){
	}
	
	@Override
	public void readConstData(ByteBuffer in) throws IOException {
		int noLinks = in.getInt();
		for (int i = 0; i < noLinks; i++){
			//read link data
			VisLinkWLanes lanesLinkData = (VisLinkWLanes) ByteBufferUtils.getObject(in);
			this.drawer.addLaneLinkData(lanesLinkData);
		}
		this.laneModelBuilder.connect(this.drawer.getLanesLinkData());
	}
	
	@Override
	public void invalidate(SceneGraph graph) {
		this.drawer.addToSceneGraph(graph);
	}

	@Override
	public void readDynData(ByteBuffer in, SceneGraph graph) throws IOException {
		// nothing to do as lanes are non dynamical data
	}
}
