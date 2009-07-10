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
package playground.dgrether.signalVis;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.apache.log4j.Logger;
import org.matsim.core.mobsim.queuesim.QueueLane;
import org.matsim.core.mobsim.queuesim.QueueLink;
import org.matsim.core.utils.misc.ByteBufferUtils;
import org.matsim.vis.otfvis.data.OTFDataWriter;
import org.matsim.vis.otfvis.data.OTFServerQuad;
import org.matsim.vis.otfvis.handler.OTFLinkAgentsNoParkingHandler;


/**
 * @author dgrether
 *
 */
public class DgOtfLinkLanesAgentsNoParkingHandler extends OTFLinkAgentsNoParkingHandler {

	private static final Logger log = Logger.getLogger(DgOtfLinkLanesAgentsNoParkingHandler.class);
	
	public DgOtfLinkLanesAgentsNoParkingHandler() {
		log.debug("using DgOtfLinkLanesAgentsNoParkingHandler");
	}

	static public class Writer extends OTFLinkAgentsNoParkingHandler.Writer {
		
		
		@Override
		public void writeConstData(ByteBuffer out) throws IOException {
			String id = this.src.getLink().getId().toString();
			ByteBufferUtils.putString(out, id);
			out.putFloat((float)(this.src.getLink().getFromNode().getCoord().getX() - OTFServerQuad.offsetEast)); //subtract minEasting/Northing somehow!
			out.putFloat((float)(this.src.getLink().getFromNode().getCoord().getY() - OTFServerQuad.offsetNorth));
			out.putFloat((float)(this.src.getLink().getToNode().getCoord().getX() - OTFServerQuad.offsetEast)); //subtract minEasting/Northing somehow!
			out.putFloat((float)(this.src.getLink().getToNode().getCoord().getY() - OTFServerQuad.offsetNorth));
			out.putInt(this.src.getLink().getLanesAsInt(0));
			
			//number of queuelanes
			out.putInt(this.src.getQueueLanes().size());
			for (QueueLane l : this.src.getQueueLanes()){
				ByteBufferUtils.putString(out, l.getLaneId().toString());
				out.putDouble(l.getMeterFromLinkEnd());
			}
		}

		@Override
		public OTFDataWriter<QueueLink> getWriter() {
			return new Writer();
		}
	}
	
	@Override
	public void readConstData(ByteBuffer in) throws IOException {
		String id = ByteBufferUtils.getString(in);

		this.quadReceiver.setQuad(in.getFloat(), in.getFloat(),in.getFloat(), in.getFloat(), in.getInt());
		this.quadReceiver.setId(id.toCharArray());
		
		int nrLanes = in.getInt();
		((DgSimpleQuadDrawer)this.quadReceiver).setNumberOfLanes(nrLanes);
		for (int i = 0; i < nrLanes; i++){
			((DgSimpleQuadDrawer)this.quadReceiver).addQueueLaneData(ByteBufferUtils.getString(in), in.getDouble());
		}
	}
	
	
}
