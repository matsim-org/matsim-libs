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

import javax.vecmath.Point2d;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.queuesim.QueueLane;
import org.matsim.core.mobsim.queuesim.QueueLink;
import org.matsim.core.utils.misc.ByteBufferUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.vis.otfvis.data.OTFDataWriter;
import org.matsim.vis.otfvis.data.OTFServerQuad;
import org.matsim.vis.otfvis.data.OTFWriterFactory;

public class DgOtfLaneWriter extends OTFDataWriter<QueueLink> implements OTFWriterFactory<QueueLink>{
	
	private static final Logger log = Logger.getLogger(DgOtfLaneWriter.class);
	
  public static final boolean DRAW_LINK_TO_LINK_LINES = true;	
	
  private double linkScale = 0.8;
  
  private double calculateLinkLength(Point2d deltaLink) {
  	return Math.sqrt(Math.pow(deltaLink.x, 2) + Math.pow(deltaLink.y, 2));
  }
  
	@Override
	public void writeConstData(ByteBuffer out) throws IOException {
//		String id = this.src.getLink().getId().toString();
//		ByteBufferUtils.putString(out, id);
		Point2d linkStart = new Point2d(this.src.getLink().getFromNode().getCoord().getX() - OTFServerQuad.offsetEast, 
				this.src.getLink().getFromNode().getCoord().getY() - OTFServerQuad.offsetNorth);
		
		Point2d linkEnd = new Point2d(this.src.getLink().getToNode().getCoord().getX() - OTFServerQuad.offsetEast,
				this.src.getLink().getToNode().getCoord().getY() - OTFServerQuad.offsetNorth);


		
		//calculate link width
		double cellWidth = 30.0;
		double quadWidth = cellWidth * this.src.getLink().getNumberOfLanes(Time.UNDEFINED_TIME);
		//calculate length and normal
		Point2d deltaLink = new Point2d(linkEnd.x - linkStart.x, linkEnd.y - linkStart.y);
		double linkLength = this.calculateLinkLength(deltaLink);
		Point2d deltaLinkNorm = new Point2d(deltaLink.x / linkLength, deltaLink.y / linkLength);
		Point2d normalizedOrthogonal = new Point2d(deltaLinkNorm.y, - deltaLinkNorm.x);
		
		Point2d scaledLinkEnd = new Point2d(linkStart.x +  ((linkLength * linkScale) * deltaLinkNorm.x),
				linkStart.y +  ((linkLength * linkScale) * deltaLinkNorm.y));

		
		//modify x and y coordinates of quad to get a middle line
		Point2d mlinkStart = this.calculateMiddleOfLink(linkStart, normalizedOrthogonal, quadWidth);
		Point2d mlinkEnd = this.calculateMiddleOfLink(linkEnd, normalizedOrthogonal, quadWidth);
//		Point2d mlinkEnd = new Point2d(mlinkStart.x + ((linkLength * linkScale) * deltaLinkNorm.x),
//				mlinkStart.y + ((linkLength * linkScale) * deltaLinkNorm.y));
		
		int numberOfToNodeQueueLanes = this.src.getToNodeQueueLanes().size();
		out.putInt(numberOfToNodeQueueLanes);

		out.putDouble(mlinkStart.x);
		out.putDouble(mlinkStart.y);
		
		//number of tonodequeuelanes
		
		if (numberOfToNodeQueueLanes == 1) {
			//the branch point is the middle of the link end
			out.putDouble(mlinkEnd.x);
			out.putDouble(mlinkEnd.y);
		}
		//only write further lane information if there is more than one lane
		else  {
			//write position of branchPoint
			QueueLane ql = this.src.getOriginalLane();
			double meterFromLinkEnd = ql.getMeterFromLinkEnd();
			Point2d branchPoint = new Point2d(mlinkStart.x + ((linkLength - meterFromLinkEnd) * linkScale * deltaLinkNorm.x),
					mlinkStart.y + ((linkLength - meterFromLinkEnd) * deltaLinkNorm.y * linkScale));
			out.putDouble(branchPoint.x);
			out.putDouble(branchPoint.y);
			
			//write toNodeQueueLanes end points
			double distanceBtwLanes = quadWidth / (numberOfToNodeQueueLanes + 1);
//			Point2d linkEnd2 = new Point2d(linkEnd.x + (normalizedOrthogonal.x * quadWidth), 
//					linkEnd.y + (normalizedOrthogonal.y * quadWidth));
//			log.debug("normOrtho x " + normalizedOrthogonalX + " y " + normalizedOrthogonalY);
			
			double laneEndPointX, laneEndPointY;
			int laneIncrement = 1;
			for (QueueLane l : this.src.getToNodeQueueLanes()){
				ByteBufferUtils.putString(out, l.getLaneId().toString());
				laneEndPointX = scaledLinkEnd.x + (normalizedOrthogonal.x * distanceBtwLanes * laneIncrement);
				laneEndPointY = scaledLinkEnd.y + (normalizedOrthogonal.y * distanceBtwLanes * laneIncrement);
//				log.debug("laneEndPoint x " + laneEndPointX + " y " + laneEndPointY);
				laneIncrement++;
				out.putDouble(laneEndPointX);
				out.putDouble(laneEndPointY);
				
				if (DRAW_LINK_TO_LINK_LINES){
					out.putInt(l.getDestinationLinks().size());
					for (Link toLink :  l.getDestinationLinks()){
						log.debug(toLink.getFromNode());
						log.debug(toLink.getToNode());
						//calculate middle of toLink start
						Point2d toLinkStart = new Point2d(toLink.getFromNode().getCoord().getX() - OTFServerQuad.offsetEast, 
								toLink.getFromNode().getCoord().getY() - OTFServerQuad.offsetNorth);
						Point2d toLinkEnd = new Point2d(toLink.getToNode().getCoord().getX()  - OTFServerQuad.offsetEast, 
								toLink.getToNode().getCoord().getY() - OTFServerQuad.offsetNorth);
						Point2d deltaToLink = new Point2d(toLinkEnd.x - toLinkStart.x, toLinkEnd.y - toLinkStart.y);
						double toLinkLength = this.calculateLinkLength(deltaToLink);
						Point2d deltaToLinkNorm = new Point2d(deltaToLink.x / toLinkLength, deltaToLink.y / toLinkLength);
						Point2d normalizedToLinkOrthogonal = new Point2d(deltaToLinkNorm.y, - deltaToLinkNorm.x);
						Point2d mToLinkStart = this.calculateMiddleOfLink(toLinkStart, normalizedToLinkOrthogonal, cellWidth * toLink.getNumberOfLanes(Time.UNDEFINED_TIME));
						//write it
						log.debug("Middle to x: " + mToLinkStart.x);
						log.debug("Middle toLink y: " + mToLinkStart.y);
						
						out.putDouble(mToLinkStart.x);
						out.putDouble(mToLinkStart.y);
					}
				}
			}
		}
	}
	
	
	
	private Point2d calculateMiddleOfLink(Point2d linkStart, Point2d normalizedOrthogonal, double quadWidth) {
		Point2d ret = new Point2d(linkStart.x + (normalizedOrthogonal.x  * quadWidth/2), 
				linkStart.y + (normalizedOrthogonal.y * quadWidth/2));
		return ret;
	}



	@Override
	public void writeDynData(ByteBuffer out) throws IOException {
		//nothing to do as lanes are non dynamical
	}

	public OTFDataWriter<QueueLink> getWriter() {
		return new DgOtfLaneWriter();
	}
}