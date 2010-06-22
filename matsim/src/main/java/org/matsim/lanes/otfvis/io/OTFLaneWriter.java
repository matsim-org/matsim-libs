/* *********************************************************************** *
 * project: org.matsim.*
 * OTFLaneWriter2
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

import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.misc.ByteBufferUtils;
import org.matsim.ptproject.qsim.netsimengine.QLane;
import org.matsim.ptproject.qsim.netsimengine.QLinkInternalI;
import org.matsim.ptproject.qsim.netsimengine.QLinkLanesImpl;
import org.matsim.vis.otfvis.data.OTFDataWriter;
import org.matsim.vis.otfvis.data.OTFServerQuad2;
import org.matsim.vis.otfvis.data.OTFWriterFactory;
import org.matsim.vis.vecmathutils.VectorUtils;


/**
 * @author dgrether
 *
 */
public class OTFLaneWriter extends OTFDataWriter<QLinkInternalI> implements OTFWriterFactory<QLinkInternalI>{
	
	private static final Logger log = Logger.getLogger(OTFLaneWriter.class);
	
	public static final boolean DRAW_LINK_TO_LINK_LINES = true;
	
	private double linkScale = 1.0;

	private boolean isQLinkLanesReader;
	
	public OTFLaneWriter(){
	}
	
	@Override
	public void writeConstData(ByteBuffer out) throws IOException {
		if (! (this.src instanceof QLinkLanesImpl)) {
			out.putShort((short)0);
			this.isQLinkLanesReader = false;
		}
		else {
			out.putShort((short)1);
			this.isQLinkLanesReader = true;
		}
		
		Point2D.Double.Double linkStart = new Point2D.Double.Double(this.src.getLink().getFromNode().getCoord().getX() - OTFServerQuad2.offsetEast,
					this.src.getLink().getFromNode().getCoord().getY() - OTFServerQuad2.offsetNorth);
			
			Point2D.Double.Double linkEnd = new Point2D.Double.Double(this.src.getLink().getToNode().getCoord().getX() - OTFServerQuad2.offsetEast,
					this.src.getLink().getToNode().getCoord().getY() - OTFServerQuad2.offsetNorth);
			//calculate length and normal
			Point2D.Double.Double deltaLink = new Point2D.Double.Double(linkEnd.x - linkStart.x, linkEnd.y - linkStart.y);
			double euclideanLinkLength = this.calculateEuclideanLinkLength(deltaLink);
			Point2D.Double.Double deltaLinkNorm = new Point2D.Double.Double(deltaLink.x / euclideanLinkLength, deltaLink.y / euclideanLinkLength);
			Point2D.Double normalizedOrthogonal = new Point2D.Double(deltaLinkNorm.y, - deltaLinkNorm.x);
			
			//calculate the correction factor if real link length is different than euclidean distance
			double linkLengthCorrectionFactor = euclideanLinkLength / this.src.getLink().getLength();
			
			//scale the link (will be rewritten)
			Tuple<Double, Double> scaledLink = VectorUtils.scaleVector(linkStart, linkEnd, this.linkScale);
			Point2D.Double scaledLinkEnd = scaledLink.getSecond();
			Point2D.Double scaledLinkStart = scaledLink.getFirst();
			
			//write link data
			out.putDouble(scaledLinkStart.x);
			out.putDouble(scaledLinkStart.y);
			out.putDouble(scaledLinkEnd.x);
			out.putDouble(scaledLinkEnd.y);
			out.putDouble(deltaLinkNorm.x);
			out.putDouble(deltaLinkNorm.y);
			out.putDouble(normalizedOrthogonal.x);
			out.putDouble(normalizedOrthogonal.y);
			out.putDouble(this.src.getLink().getNumberOfLanes());
			
		if (this.isQLinkLanesReader){
			int maxAlignment = 0;
			//write lane data
			List<QLane> qLanes = ((QLinkLanesImpl)this.src).getQueueLanes();
			out.putInt(qLanes.size());
			for (QLane qLane : qLanes){
				String id = qLane.getId().toString();
				double startPoint = (qLane.getQLink().getLink().getLength() -  qLane.getLane().getStartsAtMeterFromLinkEnd()) * linkLengthCorrectionFactor;
//				log.error("lane " + qLane.getId() + " starts at: " + startPoint);
				double endPoint = startPoint + (qLane.getLength() * linkLengthCorrectionFactor);
				int alignment = qLane.getLane().getAlignment();
				ByteBufferUtils.putString(out, id);
				out.putDouble(startPoint);
				out.putDouble(endPoint);
				out.putInt(alignment);
				out.putDouble(qLane.getLane().getNumberOfRepresentedLanes());
				if (alignment > maxAlignment){
					maxAlignment = alignment;
				}
				//write data for link2link lines
				if (DRAW_LINK_TO_LINK_LINES){
					if (qLane.getLane().getToLinkIds() != null){
						out.putInt(qLane.getLane().getToLinkIds().size());
						log.error("link " + qLane.getQLink().getLink().getId() + " link2link for lane " + qLane.getId());
						for (Id toLinkId :  qLane.getLane().getToLinkIds()){
							log.error("2linkid: " + toLinkId);
							Link toLink = ((Network) this.src.getLink().getLayer()).getLinks().get(toLinkId);
							if (toLink == null) {
								throw new IllegalStateException("No Link found with id: " + toLinkId + " this is set as toLink of Lane " + qLane.getId() + " on Link " + qLane.getQLink().getLink().getId());
							}
							Point2D.Double normalOfToLink = calculateNormalOfLink(toLink);
							out.putDouble(toLink.getFromNode().getCoord().getX() - OTFServerQuad2.offsetEast);
							out.putDouble(toLink.getFromNode().getCoord().getY() - OTFServerQuad2.offsetNorth);
							out.putDouble(normalOfToLink.x);
							out.putDouble(normalOfToLink.y);
							out.putDouble(toLink.getNumberOfLanes());
						}
					}
					else {
						out.putInt(0);
					}
				}
			}
			out.putInt(maxAlignment);
		}	
	}
	
	private Point2D.Double calculateNormalOfLink(Link link) {
		log.error(link);
		//get coordinates
		Point2D.Double linkStartPoint = new Point2D.Double(link.getFromNode().getCoord().getX() - OTFServerQuad2.offsetEast,
				link.getFromNode().getCoord().getY() - OTFServerQuad2.offsetNorth);
		Point2D.Double linkEndPoint = new Point2D.Double(link.getToNode().getCoord().getX()  - OTFServerQuad2.offsetEast,
				link.getToNode().getCoord().getY() - OTFServerQuad2.offsetNorth);
		//scale
		Tuple<Double, Double> scaledTuple = VectorUtils.scaleVector(linkStartPoint, linkEndPoint, this.linkScale);
		linkStartPoint = scaledTuple.getFirst();
		linkEndPoint = scaledTuple.getSecond();

		//calculate middle of toLink start
		Point2D.Double deltaLink = new Point2D.Double(linkEndPoint.x - linkStartPoint.x, linkEndPoint.y - linkStartPoint.y);
		double euclideanLinkLength = this.calculateEuclideanLinkLength(deltaLink);
		Point2D.Double deltaLinkNorm = new Point2D.Double(deltaLink.x / euclideanLinkLength, deltaLink.y / euclideanLinkLength);
		Point2D.Double normalizedLinkOrthogonal = new Point2D.Double(deltaLinkNorm.y, - deltaLinkNorm.x);
		return normalizedLinkOrthogonal;
	}
	
	
  private double calculateEuclideanLinkLength(Point2D.Double deltaLink) {
  	return Math.sqrt(Math.pow(deltaLink.x, 2) + Math.pow(deltaLink.y, 2));
  }


	@Override
	public void writeDynData(ByteBuffer out) throws IOException {
		//nothing to do as lanes are not dynamic
	}

	@Override
	public OTFDataWriter<QLinkInternalI> getWriter() {
		return new OTFLaneWriter();
	}


}
