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
package org.matsim.lanes;

import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.vis.snapshotwriters.SnapshotLinkWidthCalculator;
import org.matsim.vis.snapshotwriters.VisLink;
import org.matsim.vis.vecmathutils.VectorUtils;

/**
 * @author dgrether
 */
public final class VisLaneModelBuilder {
	
//	private static final Logger log = LogManager.getLogger(VisLaneModelBuilder.class);

	public void recalculatePositions(VisLinkWLanes linkData, SnapshotLinkWidthCalculator linkWidthCalculator) {
//		log.error("recalculatePositions...");
		double linkWidth = linkWidthCalculator.calculateLinkWidth(linkData.getNumberOfLanes()) ;
		linkData.setLinkWidth(linkWidth);
		Point2D.Double linkStartCenter = this.calculatePointOnLink(linkData, 0.0, 0.5);
		linkData.setLinkStartCenterPoint(linkStartCenter);
		if (linkData.getLaneData() == null || linkData.getLaneData().isEmpty()){
			//Calculate end point center
			double x = linkData.getLinkEnd().x + (0.5 * linkWidth * linkData.getLinkOrthogonalVector().x);
			double y = linkData.getLinkEnd().y + (0.5 * linkWidth * linkData.getLinkOrthogonalVector().y);
			linkData.setLinkEndCenterPoint(new Point2D.Double(x, y));
//			log.error("link " + linkData.getLinkId() + " without lanes starts at " + linkData.getLinkStartCenterPoint() + " and ends at " + x + " " + y + " but " + linkData.getLinkEnd());
		}
		else {
			double numberOfLinkParts = (2 * linkData.getMaximalAlignment()) + 2;
//			log.error("link with lanes " + linkData.getLinkId() + " starts at " + linkData.getLinkStart() + " and ends at " + linkData.getLinkEnd());
			for (VisLane lane : linkData.getLaneData().values()){
				double horizontalFraction = 0.5 - (lane.getAlignment() / numberOfLinkParts);
				Point2D.Double laneStart = calculatePointOnLink(linkData, lane.getStartPosition(), horizontalFraction);
				Point2D.Double laneEnd = calculatePointOnLink(linkData, lane.getEndPosition(), horizontalFraction);
				lane.setStartEndPoint(laneStart, laneEnd);
//				log.error("lane " + lane.getId() + " starts at " + laneStart + " ends at " + laneEnd);
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
	
	private Point2D.Double calculatePointOnLink(final VisLinkWLanes laneLinkData, final double position, final double horizontalFraction) {
		Point2D.Double lenghtPoint = this.calcPoint(laneLinkData.getLinkStart(), laneLinkData.getNormalizedLinkVector(), position);
		return this.calcPoint(lenghtPoint, laneLinkData.getLinkOrthogonalVector(), horizontalFraction * laneLinkData.getLinkWidth());
	}
	
	public Point2D.Double calcPoint(Point2D.Double start, Point2D.Double vector, double distance){
		double x = start.getX() + (distance * vector.x);
		double y = start.getY() + (distance * vector.y);
		return new Point2D.Double(x, y);
	}
	
	private VisLane createVisLane(ModelLane qlane, double linkLength, double linkScale, double linkLengthCorrectionFactor) {
		String id = qlane.getLaneData().getId().toString();
		double startPosition = (linkLength -  qlane.getLaneData().getStartsAtMeterFromLinkEnd()) * linkScale * linkLengthCorrectionFactor;
		double endPosition = startPosition + (qlane.getLength() *  linkScale * linkLengthCorrectionFactor);
//		log.error("lane " + qlane.getId() + " starts at: " + startPosition + " and ends at : " +endPosition);
		int alignment = qlane.getLaneData().getAlignment();
		VisLane lane = new VisLane(id);
		lane.setStartPosition(startPosition);
		lane.setEndPosition(endPosition);
		lane.setAlignment(alignment);
		lane.setNumberOfLanes(qlane.getLaneData().getNumberOfRepresentedLanes());
		return lane;
	}


	
	public void connect(Map<String, VisLinkWLanes> otfNetwork){
		for (VisLinkWLanes otfLink : otfNetwork.values()){
			if (otfLink.getLaneData() == null || otfLink.getLaneData().isEmpty()){
				if (otfLink.getToLinkIds() != null){
					for (String toLinkId : otfLink.getToLinkIds()){
						VisLinkWLanes toLink = otfNetwork.get(toLinkId);
						otfLink.addToLink(toLink);
					}
				}
			}
			else {
				for (VisLane otfLane : otfLink.getLaneData().values()){
					if (otfLane.getToLinkIds() != null) {
						for (String toLinkId : otfLane.getToLinkIds()){
							VisLinkWLanes toLink = otfNetwork.get(toLinkId);
							otfLane.addToLink(toLink);
						}
					}
				}
			}
		}
	}

	public VisLinkWLanes createVisLinkLanes(CoordinateTransformation transform, VisLink link, double nodeOffsetMeter, List<ModelLane> lanes) {
//		log.error("");
//		log.error("link " + link.getLink().getId() + " ... ");
//		log.debug("  fromNode: " + link.getLink().getFromNode().getId() + " coord: " + link.getLink().getFromNode().getCoord());
//		log.debug("  toNode: " + link.getLink().getToNode().getId() + " coord: " + link.getLink().getToNode().getCoord());
		Coord linkStartCoord = transform.transform(link.getLink().getFromNode().getCoord());
		Coord linkEndCoord = transform.transform(link.getLink().getToNode().getCoord());
		Point2D.Double linkStart = new Point2D.Double(linkStartCoord.getX(), linkStartCoord.getY());
		Point2D.Double linkEnd =  new Point2D.Double(linkEndCoord.getX(), linkEndCoord.getY());
		
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
//		log.error("scaledLinkStart: " + scaledLinkStart + " end: " + scaledLinkEnd);
		
		VisLinkWLanes lanesLinkData = new VisLinkWLanes(link.getLink().getId().toString());
		lanesLinkData.setLinkStartEndPoint(scaledLinkStart, scaledLinkEnd);
		lanesLinkData.setNormalizedLinkVector(deltaLinkNorm);
		lanesLinkData.setLinkOrthogonalVector(normalizedOrthogonal);
		lanesLinkData.setNumberOfLanes(link.getLink().getNumberOfLanes());

		if (lanes != null){
			int maxAlignment = 0;
			for (ModelLane lane : lanes){
				VisLane visLane = this.createVisLane(lane, link.getLink().getLength(), linkScale, linkLengthCorrectionFactor);
				lanesLinkData.addLaneData(visLane);
				if (visLane.getAlignment() > maxAlignment) {
					maxAlignment = visLane.getAlignment();
				}
			}
			lanesLinkData.setMaximalAlignment(maxAlignment);

			//connect the lanes 
			for (ModelLane lane : lanes){
				VisLane otfLane = lanesLinkData.getLaneData().get(lane.getLaneData().getId().toString());
				if (lane.getToLanes() == null ||  lane.getToLanes().isEmpty()){
					for (Id<Link> id : lane.getLaneData().getToLinkIds()){
						otfLane.addToLinkId(id.toString());
					}
				}
				else {
					for (ModelLane toLane : lane.getToLanes()){
						VisLane otfToLane = lanesLinkData.getLaneData().get(toLane.getLaneData().getId().toString());
						otfLane.addToLane(otfToLane);
					}
				}
			}
		}
		else {
			for (Id<Link> id : link.getLink().getToNode().getOutLinks().keySet()){
				lanesLinkData.addToLinkId(id.toString());
			}
		}
		
		return lanesLinkData;
	}
	
	

  private double calculateEuclideanLinkLength(Point2D.Double deltaLink) {
  	return Math.sqrt(Math.pow(deltaLink.x, 2) + Math.pow(deltaLink.y, 2));
  }

}
