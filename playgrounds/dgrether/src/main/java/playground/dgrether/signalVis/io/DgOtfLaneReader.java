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
package playground.dgrether.signalVis.io;

import java.awt.geom.Point2D;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.apache.log4j.Logger;
import org.matsim.core.utils.misc.ByteBufferUtils;
import org.matsim.vis.otfvis.caching.SceneGraph;
import org.matsim.vis.otfvis.data.OTFDataReceiver;
import org.matsim.vis.otfvis.interfaces.OTFDataReader;

import playground.dgrether.signalVis.drawer.DgLaneSignalDrawer;
import playground.dgrether.signalVis.drawer.DgOtfLaneData;


/**
 * @author dgrether
 *
 */
public class DgOtfLaneReader extends OTFDataReader {

	private static final Logger log = Logger.getLogger(DgOtfLaneReader.class);
	
	protected DgLaneSignalDrawer drawer;
	
	public DgOtfLaneReader() {
	}

	@Override
	public void readConstData(ByteBuffer in) throws IOException {
		int nrToNodeLanes = in.getInt();
		drawer.setNumberOfLanes(nrToNodeLanes);

		this.drawer.setMiddleOfLinkStart(in.getDouble(), in.getDouble());
		this.drawer.setBranchPoint(in.getDouble(), in.getDouble());

		if (nrToNodeLanes == 1){
			if (DgOtfLaneWriter.DRAW_LINK_TO_LINK_LINES){
				DgOtfLaneData data = new DgOtfLaneData();
				int numberOfToLinks = in.getInt();
				for (int j = 0; j < numberOfToLinks; j++) {
					data.getToLinkStartPoints().add(new Point2D.Double(in.getDouble(), in.getDouble()));
				}
				this.drawer.setOriginalLaneData(data);
			}
		}
		else {
			for (int i = 0; i < nrToNodeLanes; i++){
				DgOtfLaneData data = new DgOtfLaneData();
				data.setId(ByteBufferUtils.getString(in));
				data.setEndPoint(in.getDouble(), in.getDouble());
				this.drawer.getLaneData().put(data.getId(), data);
				
				if (DgOtfLaneWriter.DRAW_LINK_TO_LINK_LINES){
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
		this.drawer = (DgLaneSignalDrawer) receiver;
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
