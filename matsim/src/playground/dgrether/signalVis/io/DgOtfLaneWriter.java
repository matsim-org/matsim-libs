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

import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.misc.ByteBufferUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.ptproject.qsim.QueueLane;
import org.matsim.ptproject.qsim.QueueLink;
import org.matsim.vis.otfvis.data.OTFDataWriter;
import org.matsim.vis.otfvis.data.OTFServerQuad2;
import org.matsim.vis.otfvis.data.OTFWriterFactory;
import org.matsim.vis.otfvis.gui.OTFVisConfig;
import org.matsim.vis.otfvis.handler.OTFDefaultLinkHandler;
import org.matsim.vis.vecmathutils.VectorUtils;

public class DgOtfLaneWriter extends OTFDataWriter<QueueLink> implements OTFWriterFactory<QueueLink>{
	
	private static final Logger log = Logger.getLogger(DgOtfLaneWriter.class);
	
  public static final boolean DRAW_LINK_TO_LINK_LINES = true;	
	
  private double linkScale = OTFDefaultLinkHandler.LINK_SCALE;
  
  private double calculateLinkLength(Point2D.Double deltaLink) {
  	return Math.sqrt(Math.pow(deltaLink.x, 2) + Math.pow(deltaLink.y, 2));
  }
  
	@Override
	public void writeConstData(ByteBuffer out) throws IOException {
//		String id = this.src.getLink().getId().toString();
//		ByteBufferUtils.putString(out, id);
		Point2D.Double.Double linkStart = new Point2D.Double.Double(this.src.getLink().getFromNode().getCoord().getX() - OTFServerQuad2.offsetEast, 
				this.src.getLink().getFromNode().getCoord().getY() - OTFServerQuad2.offsetNorth);
		
		Point2D.Double.Double linkEnd = new Point2D.Double.Double(this.src.getLink().getToNode().getCoord().getX() - OTFServerQuad2.offsetEast,
				this.src.getLink().getToNode().getCoord().getY() - OTFServerQuad2.offsetNorth);

		
		//calculate link width
		double cellWidth = ((OTFVisConfig)Gbl.getConfig().getModule("otfvis")).getLinkWidth();
		double quadWidth = cellWidth * this.src.getLink().getNumberOfLanes(Time.UNDEFINED_TIME);
		//calculate length and normal
		Point2D.Double.Double deltaLink = new Point2D.Double.Double(linkEnd.x - linkStart.x, linkEnd.y - linkStart.y);
		double linkLength = this.calculateLinkLength(deltaLink);
		Point2D.Double.Double deltaLinkNorm = new Point2D.Double.Double(deltaLink.x / linkLength, deltaLink.y / linkLength);
		Point2D.Double normalizedOrthogonal = new Point2D.Double(deltaLinkNorm.y, - deltaLinkNorm.x);

		Tuple<Double, Double> scaledLink = VectorUtils.scaleVector(linkStart, linkEnd, linkScale);
		Point2D.Double scaledLinkEnd = scaledLink.getSecond();
		Point2D.Double scaledLinkStart = scaledLink.getFirst();
		
		
		//modify x and y coordinates of quad to get a middle line
		Point2D.Double mlinkStart = this.calculateMiddleOfLink(linkStart, normalizedOrthogonal, quadWidth);
		Point2D.Double mscaledLinkStart = this.calculateMiddleOfLink(scaledLinkStart, normalizedOrthogonal, quadWidth);
//		Point2D.Double mlinkEnd = this.calculateMiddleOfLink(linkEnd, normalizedOrthogonal, quadWidth);
		
		int numberOfToNodeQueueLanes = this.src.getToNodeQueueLanes().size();
		out.putInt(numberOfToNodeQueueLanes);

		out.putDouble(mscaledLinkStart.x);
		out.putDouble(mscaledLinkStart.y);
		
		//number of tonodequeuelanes
		
		if (numberOfToNodeQueueLanes == 1) {
			Double mscaledLinkEnd = this.calculateMiddleOfLink(scaledLinkEnd, normalizedOrthogonal, quadWidth);
			//the branch point is the middle of the link end
			out.putDouble(mscaledLinkEnd.x);
			out.putDouble(mscaledLinkEnd.y);
			if (DRAW_LINK_TO_LINK_LINES){
				out.putInt(this.src.getLink().getToNode().getOutLinks().size());
				for (Link toLink :  this.src.getLink().getToNode().getOutLinks().values()){
					Double mscaledToLinkStart = this.calculateMiddleOfToLink(toLink, cellWidth);
					out.putDouble(mscaledToLinkStart.x);
					out.putDouble(mscaledToLinkStart.y);
				}
			}
		}
		//only write further lane information if there is more than one lane
		else  {
			//write position of branchPoint
			QueueLane ql = this.src.getOriginalLane();
			double meterFromLinkEnd = ql.getMeterFromLinkEnd();
			
			Point2D.Double branchPoint = new Point2D.Double(mscaledLinkStart.x + ((linkLength - meterFromLinkEnd) * linkScale * deltaLinkNorm.x),
					mscaledLinkStart.y + ((linkLength - meterFromLinkEnd) * deltaLinkNorm.y * linkScale));
			out.putDouble(branchPoint.x);
			out.putDouble(branchPoint.y);
			
			//write toNodeQueueLanes end points
			double distanceBtwLanes = quadWidth / (numberOfToNodeQueueLanes + 1);
//			Point2D.Double linkEnd2 = new Point2D.Double(linkEnd.x + (normalizedOrthogonal.x * quadWidth), 
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
						Double mscaledToLinkStart = this.calculateMiddleOfToLink(toLink, cellWidth);
						out.putDouble(mscaledToLinkStart.x);
						out.putDouble(mscaledToLinkStart.y);
					}
				}
			}
		}
	}
	
	private Point2D.Double calculateMiddleOfToLink(Link toLink, double cellWidth) {
		//get coordinates
		Point2D.Double toLinkStart = new Point2D.Double(toLink.getFromNode().getCoord().getX() - OTFServerQuad2.offsetEast, 
				toLink.getFromNode().getCoord().getY() - OTFServerQuad2.offsetNorth);
		Point2D.Double toLinkEnd = new Point2D.Double(toLink.getToNode().getCoord().getX()  - OTFServerQuad2.offsetEast, 
				toLink.getToNode().getCoord().getY() - OTFServerQuad2.offsetNorth);
		//scale
		Tuple<Double, Double> scaledTuple = VectorUtils.scaleVector(toLinkStart, toLinkEnd, linkScale);
		toLinkStart = scaledTuple.getFirst();
		toLinkEnd = scaledTuple.getSecond();
		
		//calculate middle of toLink start
		Point2D.Double deltaToLink = new Point2D.Double(toLinkEnd.x - toLinkStart.x, toLinkEnd.y - toLinkStart.y);
		double toLinkLength = this.calculateLinkLength(deltaToLink);
		Point2D.Double deltaToLinkNorm = new Point2D.Double(deltaToLink.x / toLinkLength, deltaToLink.y / toLinkLength);
		Point2D.Double normalizedToLinkOrthogonal = new Point2D.Double(deltaToLinkNorm.y, - deltaToLinkNorm.x);
		Point2D.Double mToLinkStart = this.calculateMiddleOfLink(toLinkStart, normalizedToLinkOrthogonal, cellWidth * toLink.getNumberOfLanes(Time.UNDEFINED_TIME));
		return mToLinkStart;
	}
	
	
	private Point2D.Double calculateMiddleOfLink(Point2D.Double linkStart, Point2D.Double normalizedOrthogonal, double quadWidth) {
		Point2D.Double ret = new Point2D.Double(linkStart.x + (normalizedOrthogonal.x  * quadWidth/2), 
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