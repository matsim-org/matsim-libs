/* *********************************************************************** *
 * project: org.matsim.*
 * DgOtfLinkLanesAgentsNoParkingHandler
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
package org.matsim.lanes.otfvis.io;

import java.awt.geom.Point2D;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.apache.log4j.Logger;
import org.matsim.core.utils.misc.ByteBufferUtils;
import org.matsim.lanes.otfvis.drawer.OTFLaneSignalDrawer;
import org.matsim.vis.otfvis.caching.SceneGraph;
import org.matsim.vis.otfvis.data.OTFDataReceiver;
import org.matsim.vis.otfvis.interfaces.OTFDataReader;



/**
 * @author dgrether
 */
public class OTFLaneReader extends OTFDataReader {

  private static final Logger log = Logger.getLogger(OTFLaneReader.class);
	
  protected OTFLaneSignalDrawer drawer;

	public OTFLaneReader() {
	}

	@Override
	public void readConstData(ByteBuffer in) throws IOException {
		int nrToNodeLanes = in.getInt();
		drawer.setNumberOfLanes(nrToNodeLanes);

		this.drawer.setMiddleOfLinkStart(in.getDouble(), in.getDouble());
		this.drawer.setBranchPoint(in.getDouble(), in.getDouble());

		if (nrToNodeLanes == 1){
			if (OTFLaneWriter.DRAW_LINK_TO_LINK_LINES){
				OTFLaneData data = new OTFLaneData();
				int numberOfToLinks = in.getInt();
				for (int j = 0; j < numberOfToLinks; j++) {
					data.getToLinkStartPoints().add(new Point2D.Double(in.getDouble(), in.getDouble()));
				}
				this.drawer.setOriginalLaneData(data);
			}
		}
		else {
			for (int i = 0; i < nrToNodeLanes; i++){
				OTFLaneData data = new OTFLaneData();
				data.setId(ByteBufferUtils.getString(in));
				data.setEndPoint(in.getDouble(), in.getDouble());
				log.error("adding lane data for id : " + data.getId() + " and drawer " + this.drawer);
				this.drawer.getLaneData().put(data.getId(), data);

				if (OTFLaneWriter.DRAW_LINK_TO_LINK_LINES){
					int numberOfToLinks = in.getInt();
					for (int j = 0; j < numberOfToLinks; j++) {
						data.getToLinkStartPoints().add(new Point2D.Double(in.getDouble(), in.getDouble()));
					}
				}
			}
		}
	}

	@Override
	public void connect(OTFDataReceiver receiver) {
		this.drawer = (OTFLaneSignalDrawer) receiver;
	}

	@Override
	public void invalidate(SceneGraph graph) {
		this.drawer.invalidate(graph);
	}

	@Override
	public void readDynData(ByteBuffer in, SceneGraph graph) throws IOException {
		// nothing to do as lanes are non dynamical data
	}




}
