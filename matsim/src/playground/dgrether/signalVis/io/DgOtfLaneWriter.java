/* *********************************************************************** *
 * project: org.matsim.*
 * Writer
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

import java.io.IOException;
import java.nio.ByteBuffer;

import org.apache.log4j.Logger;
import org.matsim.core.mobsim.queuesim.QueueLane;
import org.matsim.core.mobsim.queuesim.QueueLink;
import org.matsim.core.utils.misc.ByteBufferUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.vis.otfvis.data.OTFDataWriter;
import org.matsim.vis.otfvis.data.OTFServerQuad;
import org.matsim.vis.otfvis.data.OTFWriterFactory;

public class DgOtfLaneWriter extends OTFDataWriter<QueueLink> implements OTFWriterFactory<QueueLink>{
	
	
	private static final Logger log = Logger.getLogger(DgOtfLaneWriter.class);
	
	@Override
	public void writeConstData(ByteBuffer out) throws IOException {
//		String id = this.src.getLink().getId().toString();
//		ByteBufferUtils.putString(out, id);
		double linkStartX1 = this.src.getLink().getFromNode().getCoord().getX() - OTFServerQuad.offsetEast;
		double linkStartY1 = this.src.getLink().getFromNode().getCoord().getY() - OTFServerQuad.offsetNorth;
		double linkEndX1 = this.src.getLink().getToNode().getCoord().getX() - OTFServerQuad.offsetEast;
		double linkEndY1 = this.src.getLink().getToNode().getCoord().getY() - OTFServerQuad.offsetNorth;
		
		
		//calculate link width
		double cellWidth = 30.0;
		double quadWidth = cellWidth * this.src.getLink().getNumberOfLanes(Time.UNDEFINED_TIME);
		//calculate length and normal
		double deltaLinkX = linkEndX1 - linkStartX1;
		double deltaLinkY = linkEndY1 - linkStartY1;
		double linkLength = Math.sqrt(Math.pow(deltaLinkX, 2) + Math.pow(deltaLinkY, 2));
		double deltaLinkXNorm = deltaLinkX / linkLength;
		double deltaLinkYNorm = deltaLinkY / linkLength;
		double normalizedOrthogonalX = deltaLinkYNorm;
		double normalizedOrthogonalY = - deltaLinkXNorm;

		//modify x and y coordinates of quad to get a middle line
		double mlinkStartX1 = linkStartX1 + (normalizedOrthogonalX * quadWidth/2);
		double mlinkStartY1 = linkStartY1 + (normalizedOrthogonalY * quadWidth/2);
		double mlinkEndX1 = linkEndX1 + (normalizedOrthogonalX * quadWidth/2);
		double mlinkEndY1 = linkEndY1 + (normalizedOrthogonalY * quadWidth/2);

		int numberOfToNodeQueueLanes = this.src.getToNodeQueueLanes().size();
		out.putInt(numberOfToNodeQueueLanes);
		log.debug("numberoftoNodeQueueLanes: " + numberOfToNodeQueueLanes);

		out.putDouble(mlinkStartX1);
		out.putDouble(mlinkStartY1);
		
		//number of tonodequeuelanes
		
		if (numberOfToNodeQueueLanes == 1) {
			//the branch point is the middle of the link end
			out.putDouble(mlinkEndX1);
			out.putDouble(mlinkEndY1);
		}
		//only write further lane information if there is more than one lane
		else  {
			//write position of branchPoint
			QueueLane ql = this.src.getOriginalLane();
			double meterFromLinkEnd = ql.getMeterFromLinkEnd();
			double branchPointX = mlinkStartX1 + ((linkLength - meterFromLinkEnd) * deltaLinkXNorm);
			double branchPointY = mlinkStartY1 + ((linkLength - meterFromLinkEnd) * deltaLinkYNorm);
			log.debug("meterfromlinkend: " + meterFromLinkEnd);
			log.debug("linkstart x " + linkStartX1 + " y " + linkStartY1);
			log.debug("linkendx: " + linkEndX1 + " linkendy: " + linkEndY1);
			log.debug("branchPointX: " + branchPointX + " Y " + branchPointY);
			out.putDouble(branchPointX);
			out.putDouble(branchPointY);
			
			//write toNodeQueueLanes end points
			double distanceBtwLanes = quadWidth / (numberOfToNodeQueueLanes + 1);
			double linkEndX2 = linkEndX1 + (normalizedOrthogonalX * quadWidth);
			double linkEndY2 = linkEndY1 + (normalizedOrthogonalY * quadWidth);
			log.debug("normOrtho x " + normalizedOrthogonalX + " y " + normalizedOrthogonalY);
			
			double laneEndPointX, laneEndPointY;
			int laneIncrement = 1;
			for (QueueLane l : this.src.getToNodeQueueLanes()){
				ByteBufferUtils.putString(out, l.getLaneId().toString());
				laneEndPointX = linkEndX1 + (normalizedOrthogonalX * distanceBtwLanes * laneIncrement);
				laneEndPointY = linkEndY1 + (normalizedOrthogonalY * distanceBtwLanes * laneIncrement);
				log.debug("laneEndPoint x " + laneEndPointX + " y " + laneEndPointY);
				laneIncrement++;
				out.putDouble(laneEndPointX);
				out.putDouble(laneEndPointY);
			}
		}
	}
	
	
	
	@Override
	public void writeDynData(ByteBuffer out) throws IOException {
		//nothing to do as lanes are non dynamical
	}

	public OTFDataWriter<QueueLink> getWriter() {
		return new DgOtfLaneWriter();
	}
}