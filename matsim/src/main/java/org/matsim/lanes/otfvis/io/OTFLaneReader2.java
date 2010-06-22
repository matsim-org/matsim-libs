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
package org.matsim.lanes.otfvis.io;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.apache.log4j.Logger;
import org.matsim.core.utils.misc.ByteBufferUtils;
import org.matsim.lanes.otfvis.drawer.OTFLaneSignalDrawer2;
import org.matsim.vis.otfvis.caching.SceneGraph;
import org.matsim.vis.otfvis.data.OTFDataReceiver;
import org.matsim.vis.otfvis.interfaces.OTFDataReader;


/**
 * @author dgrether
 *
 */
public class OTFLaneReader2 extends OTFDataReader {
	
	private static final Logger log = Logger.getLogger(OTFLaneReader2.class);
	
	protected OTFLaneSignalDrawer2 drawer;
	protected boolean isQLinkLanesReader;

	public OTFLaneReader2(){
	}
	
	@Override
	public void readConstData(ByteBuffer in) throws IOException {
		short isQLinkLanesIndicator = in.getShort();
		if (isQLinkLanesIndicator == 1){
			this.isQLinkLanesReader = true;
		}
		else {
			this.isQLinkLanesReader = false;
		}
		
		//read link data
		drawer.getLanesLinkData().setLinkStart(in.getDouble(), in.getDouble());
		drawer.getLanesLinkData().setLinkEnd(in.getDouble(), in.getDouble());
		drawer.getLanesLinkData().setNormalizedLinkVector(in.getDouble(), in.getDouble());
		drawer.getLanesLinkData().setLinkOrthogonalVector(in.getDouble(), in.getDouble());
		drawer.getLanesLinkData().setNumberOfLanes(in.getDouble());
		
		//read lane data
		if (this.isQLinkLanesReader) {
			int nrToNodeLanes = in.getInt();
			for (int i = 0; i < nrToNodeLanes; i++){
				OTFLaneData2 data = new OTFLaneData2();
				data.setId(ByteBufferUtils.getString(in));
				data.setStartPoint(in.getDouble());
				data.setEndPoint(in.getDouble());
				data.setAlignment(in.getInt());
				data.setNumberOfLanes(in.getDouble());
				
				if (OTFLaneWriter2.DRAW_LINK_TO_LINK_LINES){
					int numberOfToLinks = in.getInt();
					for (int j = 0; j < numberOfToLinks; j++){
						double toLinkStartX = in.getDouble();
						double toLinkStartY = in.getDouble();
						double normalX = in.getDouble();
						double normalY = in.getDouble();
						double toLinkNumberOfLanes = in.getDouble();
						data.addToLinkData(toLinkStartX, toLinkStartY, normalX, normalY, toLinkNumberOfLanes);
					}
				}
				this.drawer.addLaneData(data);
			}
			drawer.getLanesLinkData().setMaximalAlignment(in.getInt());
		}
	}
	
	
	@Override
	public void connect(OTFDataReceiver receiver) {
		this.drawer = (OTFLaneSignalDrawer2) receiver;
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
