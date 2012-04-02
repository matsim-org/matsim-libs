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
package org.matsim.lanes.otfvis.io;

import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.core.mobsim.qsim.qnetsimengine.OTFLane;
import org.matsim.core.mobsim.qsim.qnetsimengine.OTFLinkWLanes;
import org.matsim.core.mobsim.qsim.qnetsimengine.QLane;
import org.matsim.core.mobsim.qsim.qnetsimengine.QLinkLanesImpl;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.lanes.data.v20.LaneData20;
import org.matsim.lanes.data.v20.LanesToLinkAssignment20;
import org.matsim.vis.otfvis.data.OTFServerQuadTree;
import org.matsim.vis.snapshotwriters.SnapshotLinkWidthCalculator;
import org.matsim.vis.snapshotwriters.VisLink;
import org.matsim.vis.vecmathutils.VectorUtils;

/**
 * @author dgrether
 */
public class OTFLaneModelBuilder {

	public void recalculatePositions(OTFLinkWLanes linkData, SnapshotLinkWidthCalculator linkWidthCalculator) {
		double linkWidth = linkWidthCalculator.calculateLinkWidth(linkData.getNumberOfLanes()) ;
		linkData.setLinkWidth(linkWidth);
		Point2D.Double linkStartCenter = this.calculatePointOnLink(linkData, 0.0, 0.5);
		linkData.setLinkStartCenterPoint(linkStartCenter);
		if (linkData.getLaneData() == null || linkData.getLaneData().isEmpty()){
			//Calculate end point center
			double x = linkData.getLinkEnd().x + (0.5 * linkWidth * linkData.getLinkOrthogonalVector().x);
			double y = linkData.getLinkEnd().y + (0.5 * linkWidth * linkData.getLinkOrthogonalVector().y);
			linkData.setLinkEndCenterPoint(new Point2D.Double(x, y));
		}
		else {
			double numberOfLinkParts = (2 * linkData.getMaximalAlignment()) + 2;
			for (OTFLane lane : linkData.getLaneData().values()){
				double horizontalFraction = 0.5 - (lane.getAlignment() / numberOfLinkParts);
				Point2D.Double laneStart = calculatePointOnLink(linkData, lane.getStartPosition(), horizontalFraction);
				Point2D.Double laneEnd = calculatePointOnLink(linkData, lane.getEndPosition(), horizontalFraction);
				lane.setStartEndPoint(laneStart, laneEnd);
				if (lane.getNumberOfLanes() >= 2.0){
					double noLanesFloor = Math.floor(lane.getNumberOfLanes());
					double laneOffset = - noLanesFloor / 2 * linkWidthCalculator.getLaneWidth();
					if (noLanesFloor % 2 == 0){
						laneOffset = laneOffset + (linkWidthCalculator.getLaneWidth()/2);
					}
					for (int i = 1; i <= noLanesFloor; i++){
						Point2D.Double drivingLaneStart = this.calcPoint(laneStart, linkData.getLinkOrthogonalVector(), laneOffset);
						Point2D.Double drivingLaneEnd = this.calcPoint(laneEnd, linkData.getLinkOrthogonalVector(), laneOffset);
						lane.addDrivingLane(i, drivingLaneStart, drivingLaneEnd);
						laneOffset = laneOffset + linkWidthCalculator.getLaneWidth();
					}
				}
			}
		}
	}
	
	private Point2D.Double calculatePointOnLink(final OTFLinkWLanes laneLinkData, final double position, final double horizontalFraction) {
		Point2D.Double lenghtPoint = this.calcPoint(laneLinkData.getLinkStart(), laneLinkData.getNormalizedLinkVector(), position);
		return this.calcPoint(lenghtPoint, laneLinkData.getLinkOrthogonalVector(), horizontalFraction * laneLinkData.getLinkWidth());
	}
	
	public Point2D.Double calcPoint(Point2D.Double start, Point2D.Double vector, double distance){
		double x = start.getX() + (distance * vector.x);
		double y = start.getY() + (distance * vector.y);
		return new Point2D.Double(x, y);
	}
	
	
	private OTFLane createOTFLane(LaneData20 laneData, QLane qlane, double linkLength, double linkScale, double linkLengthCorrectionFactor) {
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
	
	public void connect(Map<String, OTFLinkWLanes> otfNetwork){
		for (OTFLinkWLanes otfLink : otfNetwork.values()){
			if (otfLink.getLaneData() == null || otfLink.getLaneData().isEmpty()){
				if (otfLink.getToLinkIds() != null){
					for (Id toLinkId : otfLink.getToLinkIds()){
						OTFLinkWLanes toLink = otfNetwork.get(toLinkId.toString());
						otfLink.addToLink(toLink);
					}
				}
			}
			else {
				for (OTFLane otfLane : otfLink.getLaneData().values()){
					if (otfLane.getToLinkIds() != null) {
						for (Id toLinkId : otfLane.getToLinkIds()){
							OTFLinkWLanes toLink = otfNetwork.get(toLinkId.toString());
							otfLane.addToLink(toLink);
						}
					}
				}
			}
		}
	}

	public OTFLinkWLanes createOTFLinkWLanes(VisLink link, double nodeOffsetMeter, LanesToLinkAssignment20 l2l) {
		Point2D.Double linkStart = OTFServerQuadTree.transform(link.getLink().getFromNode().getCoord());
		Point2D.Double linkEnd = OTFServerQuadTree.transform(link.getLink().getToNode().getCoord());

//		Point2D.Double linkStart = new Point2D.Double(link.getLink().getFromNode().getCoord().getX(), link.getLink().getFromNode().getCoord().getY());
//		Point2D.Double linkEnd =  new Point2D.Double(link.getLink().getToNode().getCoord().getX(), link.getLink().getToNode().getCoord().getY());
		
		//calculate length and normal
		Point2D.Double deltaLink = new Point2D.Double(linkEnd.x - linkStart.x, linkEnd.y - linkStart.y);
		double euclideanLinkLength = this.calculateEuclideanLinkLength(deltaLink);
		//calculate the correction factor if real link length is different than euclidean distance
		double linkLengthCorrectionFactor = euclideanLinkLength / link.getLink().getLength();
		Point2D.Double deltaLinkNorm = new Point2D.Double(deltaLink.x / euclideanLinkLength, deltaLink.y / euclideanLinkLength);
		Point2D.Double normalizedOrthogonal = new Point2D.Double(deltaLinkNorm.y, - deltaLinkNorm.x);
		
		//first calculate the scale of the link based on the node offset, i.e. the link will be shortened at the beginning and the end 
		double linkScale = 1.0;
		if ((euclideanLinkLength * 0.2) > (2.0 * nodeOffsetMeter)){ // 2* nodeoffset is more than 20%
			linkScale = (euclideanLinkLength - (2.0 * nodeOffsetMeter)) / euclideanLinkLength;
		}
		else { // use 80 % as euclidean length
			linkScale = euclideanLinkLength * 0.8 / euclideanLinkLength;
		}
		
		//scale the link 
		Tuple<Double, Double> scaledLink = VectorUtils.scaleVector(linkStart, linkEnd, linkScale);
		Point2D.Double scaledLinkEnd = scaledLink.getSecond();
		Point2D.Double scaledLinkStart = scaledLink.getFirst();
		
		OTFLinkWLanes lanesLinkData = new OTFLinkWLanes(link.getLink().getId().toString());
		lanesLinkData.setLinkStartEndPoint(scaledLinkStart, scaledLinkEnd);
		lanesLinkData.setNormalizedLinkVector(deltaLinkNorm);
		lanesLinkData.setLinkOrthogonalVector(normalizedOrthogonal);
		lanesLinkData.setNumberOfLanes(link.getLink().getNumberOfLanes());

		if (l2l != null){
			int maxAlignment = 0;
			for (LaneData20 lane : l2l.getLanes().values()){
				QLane ql = this.getQLane(lane.getId(), (QLinkLanesImpl)link);
				OTFLane visLane = this.createOTFLane(lane, ql, link.getLink().getLength(), linkScale, linkLengthCorrectionFactor);
				lanesLinkData.addLaneData(visLane);
				if (visLane.getAlignment() > maxAlignment) {
					maxAlignment = visLane.getAlignment();
				}
			}
			lanesLinkData.setMaximalAlignment(maxAlignment);
			//connect the lanes
			for (LaneData20 lane : l2l.getLanes().values()){
				OTFLane otfLane = lanesLinkData.getLaneData().get(lane.getId().toString());
				if (lane.getToLaneIds() != null){
					for (Id toLaneId : lane.getToLaneIds()){
						OTFLane otfToLane = lanesLinkData.getLaneData().get(toLaneId.toString());
						otfLane.addToLane(otfToLane);
					}
				}
				else if (lane.getToLinkIds() != null){
					for (Id id : lane.getToLinkIds()){
						otfLane.addToLinkId(id);
					}
				}
			}
		}
		else {
			for (Id id : link.getLink().getToNode().getOutLinks().keySet()){
				lanesLinkData.addToLinkId(id);
			}
		}
		
		return lanesLinkData;
	}

	private QLane getQLane(Id laneId, QLinkLanesImpl link){
		for (QLane lane : link.getQueueLanes()){
			if (lane.getId().equals(laneId)){
				return lane;
			}
		}
		throw new IllegalArgumentException("QLane Id " + laneId + " on link Id " + link.getLink().getId() + "  not found. Check configuration!");
	}
	

  private double calculateEuclideanLinkLength(Point2D.Double deltaLink) {
  	return Math.sqrt(Math.pow(deltaLink.x, 2) + Math.pow(deltaLink.y, 2));
  }

}
