/* *********************************************************************** *
 * project: org.matsim.*
 * OTFTeleportAgentsDataReader
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
package org.matsim.vis.otfvis.data.teleportation;

import java.awt.geom.Point2D;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.apache.log4j.Logger;
import org.matsim.core.utils.misc.ByteBufferUtils;
import org.matsim.vis.otfvis.caching.SceneGraph;
import org.matsim.vis.otfvis.data.OTFDataReceiver;
import org.matsim.vis.otfvis.data.OTFServerQuad;
import org.matsim.vis.otfvis.interfaces.OTFDataReader;


/**
 * @author dgrether
 *
 */
public class OTFTeleportAgentsDataReader extends OTFDataReader {
  
	private static final Logger log = Logger.getLogger(OTFTeleportAgentsDataReader.class);
	
	private OTFTeleportAgentsDrawer drawer;

	public OTFTeleportAgentsDataReader() {
	}

	@Override
	public void connect(OTFDataReceiver receiver) {
		this.drawer = (OTFTeleportAgentsDrawer)receiver;
	}

	@Override
	public void invalidate(SceneGraph graph) {
		this.drawer.invalidate(graph);
	}

	@Override
	public void readConstData(ByteBuffer in) throws IOException {}

	@Override
	public void readDynData(ByteBuffer in, SceneGraph graph) throws IOException {
		int numberOfAgents = in.getInt();
		drawer.getPositions().clear();
		for (int i = 0; i < numberOfAgents; i++){
			
			String id = ByteBufferUtils.getString(in);
			double x = in.getDouble() - OTFServerQuad.offsetEast;
			double y = in.getDouble() - OTFServerQuad.offsetNorth;
//			log.error("id: " + id + " x: " + x + " y: " + y);
			drawer.getPositions().put(id, new Point2D.Double(x, y));
		}
	}

}
