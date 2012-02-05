/* *********************************************************************** *
 * project: org.matsim.*
 * OTFLaneModelBuilder
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package org.matsim.lanes.otfvis;

import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.lanes.data.v20.Lane;
import org.matsim.lanes.otfvis.io.OTFLane;
import org.matsim.lanes.otfvis.io.OTFLinkWLanes;
import org.matsim.ptproject.qsim.qnetsimengine.QLane;
import org.matsim.vis.otfvis.data.OTFServerQuadTree;
import org.matsim.vis.vecmathutils.VectorUtils;

/**
 * @author dgrether
 */
public class OTFLaneModelBuilder {

	private Map<Id, java.lang.Double> linkScaleByLinkIdMap= new HashMap<Id, java.lang.Double>();

	public  Map<Id, java.lang.Double>  getLinkScaleByLinkIdMap(){
		return linkScaleByLinkIdMap;
	}
	
	//create OTFNetwork should be:
	//create OTFLinks
	//create OTFLanes
	//connect everything
	
	public OTFLane createOTFLane(Lane laneData, QLane qlane, double linkLength, double linkScale, double linkLengthCorrectionFactor) {
		String id = laneData.getId().toString();
		double startPosition = (linkLength -  laneData.getStartsAtMeterFromLinkEnd()) * linkScale * linkLengthCorrectionFactor;
//		log.error("lane " + qLane.getId() + " starts at: " + startPoint);
		double endPosition = startPosition + (qlane.getLength() *  linkScale * linkLengthCorrectionFactor);
		int alignment = laneData.getAlignment();
		OTFLane lane = new OTFLane(id);
		lane.setStartPosition(startPosition);
		lane.setEndPosition(endPosition);
		lane.setAlignment(alignment);
		lane.setNumberOfLanes(laneData.getNumberOfRepresentedLanes());
		return lane;
	}

	public OTFLinkWLanes createOTFLinkWLanesWithOTFLanes(Link link, double nodeOffsetMeter) {
		Point2D.Double linkStart = OTFServerQuadTree.transform(link.getFromNode().getCoord());
		Point2D.Double linkEnd = OTFServerQuadTree.transform(link.getToNode().getCoord());

		//calculate length and normal
		Point2D.Double deltaLink = new Point2D.Double(linkEnd.x - linkStart.x, linkEnd.y - linkStart.y);
		double euclideanLinkLength = this.calculateEuclideanLinkLength(deltaLink);
		//calculate the correction factor if real link length is different than euclidean distance
		double linkLengthCorrectionFactor = euclideanLinkLength / link.getLength();
		Point2D.Double deltaLinkNorm = new Point2D.Double(deltaLink.x / euclideanLinkLength, deltaLink.y / euclideanLinkLength);
		Point2D.Double normalizedOrthogonal = new Point2D.Double(deltaLinkNorm.y, - deltaLinkNorm.x);
		
		//first calculate the scale of the link based on the node offset, i.e. the link will be shortened at the beginning and the end 
		double linkScale = (euclideanLinkLength - 2.0 * nodeOffsetMeter) / euclideanLinkLength;
		this.linkScaleByLinkIdMap.put(link.getId(), linkScale);
		
		//scale the link 
		Tuple<Double, Double> scaledLink = VectorUtils.scaleVector(linkStart, linkEnd, linkScale);
		Point2D.Double scaledLinkEnd = scaledLink.getSecond();
		Point2D.Double scaledLinkStart = scaledLink.getFirst();
		
		OTFLinkWLanes lanesLinkData = new OTFLinkWLanes(link.getId().toString());
		lanesLinkData.setLinkStart(scaledLinkStart);
		lanesLinkData.setLinkEnd(scaledLinkEnd);
		lanesLinkData.setNormalizedLinkVector(deltaLinkNorm);
		lanesLinkData.setLinkOrthogonalVector(normalizedOrthogonal);
		lanesLinkData.setNumberOfLanes(link.getNumberOfLanes());
		return lanesLinkData;
	}

	
	
	public OTFLinkWLanes createOTFLinkWLanes(Link link, Point2D.Double scaledLinkStart, Point2D.Double scaledLinkEnd,
			Point2D.Double deltaLinkNorm, Point2D.Double normalizedOrthogonal) {
		OTFLinkWLanes lanesLinkData = new OTFLinkWLanes(link.getId().toString());
		lanesLinkData.setLinkStart(scaledLinkStart);
		lanesLinkData.setLinkEnd(scaledLinkEnd);
		lanesLinkData.setNormalizedLinkVector(deltaLinkNorm);
		lanesLinkData.setLinkOrthogonalVector(normalizedOrthogonal);
		lanesLinkData.setNumberOfLanes(link.getNumberOfLanes());
		return lanesLinkData;
	}

  private double calculateEuclideanLinkLength(Point2D.Double deltaLink) {
  	return Math.sqrt(Math.pow(deltaLink.x, 2) + Math.pow(deltaLink.y, 2));
  }

}
