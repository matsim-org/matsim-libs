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
import org.matsim.core.utils.misc.Time;
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
			double linkStartX1 = this.src.getLink().getFromNode().getCoord().getX() - OTFServerQuad.offsetEast;
			double linkStartY1 = this.src.getLink().getFromNode().getCoord().getY() - OTFServerQuad.offsetNorth;
			double linkEndX1 = this.src.getLink().getToNode().getCoord().getX() - OTFServerQuad.offsetEast;
			double linkEndY1 = this.src.getLink().getToNode().getCoord().getY() - OTFServerQuad.offsetNorth;
			
			out.putFloat((float)linkStartX1); //subtract minEasting/Northing somehow!
			out.putFloat((float)(linkStartY1));
			out.putFloat((float)(linkEndX1)); //subtract minEasting/Northing somehow!
			out.putFloat((float)(linkEndY1));
			out.putInt(this.src.getLink().getLanesAsInt(0));
			
			
			//number of tonodequeuelanes
			int numberOfToNodeQueueLanes = this.src.getToNodeQueueLanes().size();
			out.putInt(numberOfToNodeQueueLanes);
			//only write further lane information if there is more than one lane
			if (numberOfToNodeQueueLanes != 1) {
				//write position of branchPoint
				QueueLane ql = this.src.getToNodeQueueLanes().get(0);
				double meterFromLinkEnd = ql.getMeterFromLinkEnd();
				double deltaLinkX = linkEndX1 - linkStartX1;
				double deltaLinkY = linkEndY1 - linkStartY1;
				double linkLength = Math.sqrt(Math.pow(deltaLinkX, 2) + Math.pow(deltaLinkY, 2));
				double deltaLinkXNorm = deltaLinkX / linkLength;
				double deltaLinkYNorm = deltaLinkY / linkLength;
				double branchPointX = linkStartX1 + ((linkLength - meterFromLinkEnd) * deltaLinkXNorm);
				double branchPointY = linkStartY1 + ((linkLength - meterFromLinkEnd) * deltaLinkYNorm);
				out.putDouble(branchPointX);
				out.putDouble(branchPointY);
				
				//write toNodeQueueLanes end points
				//first calculate link width
				double cellWidth = 30.0;
				double quadWidth = cellWidth * this.src.getLink().getNumberOfLanes(Time.UNDEFINED_TIME);
				double distanceBtwLanes = quadWidth / (numberOfToNodeQueueLanes + 2);
				//this could be optimized is however more transparent as it is
				double normalizedOrthogonalX = deltaLinkXNorm;
				double normalizedOrthogonalY = - deltaLinkYNorm;
				double linkEndX2 = linkEndX1 + (normalizedOrthogonalX * quadWidth);
				double linkEndY2 = linkEndY1 + (normalizedOrthogonalY * quadWidth);
				
				double laneEndPointX, laneEndPointY;
				int laneIncrement = 1;
				for (QueueLane l : this.src.getToNodeQueueLanes()){
					ByteBufferUtils.putString(out, l.getLaneId().toString());
					laneEndPointX = linkEndX1 + (normalizedOrthogonalX * distanceBtwLanes * laneIncrement);
					laneEndPointY = linkEndY1 + (normalizedOrthogonalY * distanceBtwLanes * laneIncrement);
					laneIncrement++;
					out.putDouble(laneEndPointX);
					out.putDouble(laneEndPointY);
				}
			}
		}
		
		@Override
		public void writeDynData(ByteBuffer out) throws IOException {
			super.writeDynData(out);
			
			
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

		DgSimpleQuadDrawer drawer = (DgSimpleQuadDrawer) this.quadReceiver;
		
		int nrToNodeLanes = in.getInt();
		drawer.setNumberOfLanes(nrToNodeLanes);
		
		if (nrToNodeLanes != 1) {
			drawer.setBranchPoint(in.getDouble(), in.getDouble());
			for (int i = 0; i < nrToNodeLanes; i++){
				drawer.addNewQueueLaneData(ByteBufferUtils.getString(in), in.getDouble(), in.getDouble());
			}
		}
		
	}
	

	
	
}
