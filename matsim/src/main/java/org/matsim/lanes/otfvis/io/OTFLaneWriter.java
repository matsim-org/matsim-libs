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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.misc.ByteBufferUtils;
import org.matsim.lanes.data.v20.Lane;
import org.matsim.lanes.data.v20.LaneDefinitions;
import org.matsim.lanes.data.v20.LanesToLinkAssignment;
import org.matsim.lanes.otfvis.OTFLaneModelBuilder;
import org.matsim.ptproject.qsim.qnetsimengine.QLane;
import org.matsim.ptproject.qsim.qnetsimengine.QLinkLanesImpl;
import org.matsim.vis.otfvis.data.OTFDataWriter;
import org.matsim.vis.otfvis.data.OTFServerQuadTree;
import org.matsim.vis.snapshotwriters.VisLink;
import org.matsim.vis.snapshotwriters.VisNetwork;
import org.matsim.vis.vecmathutils.VectorUtils;


/**
 * @author dgrether
 *
 */
public class OTFLaneWriter extends OTFDataWriter<Void> {

//	private static final Logger log = Logger.getLogger(OTFLaneWriter.class);
	
	public static final boolean DRAW_LINK_TO_LINK_LINES = true;
	
	private static final double nodeOffsetMeter = 10.0;

	private final transient VisNetwork network;

	private final transient LaneDefinitions lanes;
	
	private Map<Id, java.lang.Double> linkScaleByLinkIdMap;
	
	private OTFLaneModelBuilder laneModelBuilder = new OTFLaneModelBuilder();
	
	public OTFLaneWriter(VisNetwork visNetwork, LaneDefinitions laneDefinitions){
		this.network = visNetwork;
		this.lanes = laneDefinitions;
		this.linkScaleByLinkIdMap = new HashMap<Id, java.lang.Double>();
	}
	
	@Override
	public void writeConstData(ByteBuffer out) throws IOException {
//		log.error("OffsetEast: " + OTFServerQuad2.offsetEast + " North: " + OTFServerQuad2.offsetNorth);
		Map<Id, java.lang.Double> linkLengthCorrectionFactorsByLinkId = new HashMap<Id, java.lang.Double>();
		//write the data for the links
		out.putInt(this.network.getVisLinks().size());
		for (VisLink visLink : this.network.getVisLinks().values()) {
			double corrFactor = this.writeLinkData(out, visLink);
			linkLengthCorrectionFactorsByLinkId.put(visLink.getLink().getId(), corrFactor);
		}
		//write the data for the lanes 
		if (this.lanes.getLanesToLinkAssignments() != null){
			out.putInt(this.lanes.getLanesToLinkAssignments().size());
			for (LanesToLinkAssignment l2l : this.lanes.getLanesToLinkAssignments().values()){
				VisLink visLink = this.network.getVisLinks().get(l2l.getLinkId());
				this.writeLaneData(out, visLink, l2l, linkLengthCorrectionFactorsByLinkId);
			}
		}
		else {
			out.putInt(0);
		}
	}
	
	private double writeLinkData(ByteBuffer out, VisLink visLink){
		Point2D.Double linkStart = OTFServerQuadTree.transform(visLink.getLink().getFromNode().getCoord());
		Point2D.Double linkEnd = OTFServerQuadTree.transform(visLink.getLink().getToNode().getCoord());
		
		//calculate length and normal
		Point2D.Double deltaLink = new Point2D.Double(linkEnd.x - linkStart.x, linkEnd.y - linkStart.y);
		double euclideanLinkLength = this.calculateEuclideanLinkLength(deltaLink);
		//calculate the correction factor if real link length is different than euclidean distance
		double linkLengthCorrectionFactor = euclideanLinkLength / visLink.getLink().getLength();
		Point2D.Double deltaLinkNorm = new Point2D.Double(deltaLink.x / euclideanLinkLength, deltaLink.y / euclideanLinkLength);
		Point2D.Double normalizedOrthogonal = new Point2D.Double(deltaLinkNorm.y, - deltaLinkNorm.x);
		
		//first calculate the scale of the link based on the node offset, i.e. the link will be shortened at the beginning and the end 
		double linkScale = (euclideanLinkLength - 2.0 * nodeOffsetMeter) / euclideanLinkLength;
		this.linkScaleByLinkIdMap.put(visLink.getLink().getId(), linkScale);
		
		//scale the link 
		Tuple<Double, Double> scaledLink = VectorUtils.scaleVector(linkStart, linkEnd, linkScale);
		Point2D.Double scaledLinkEnd = scaledLink.getSecond();
		Point2D.Double scaledLinkStart = scaledLink.getFirst();
		
//		log.error("Link: " + visLink.getLink().getId() + " start (x,y): (" + scaledLinkStart.x + ", " + scaledLinkStart.y + ") end (x,y): (" + scaledLinkEnd.x + ", " + scaledLinkEnd.y + ")");
		
		//write link data
		ByteBufferUtils.putString(out, visLink.getLink().getId().toString());
		out.putDouble(scaledLinkStart.x);
		out.putDouble(scaledLinkStart.y);
		out.putDouble(scaledLinkEnd.x);
		out.putDouble(scaledLinkEnd.y);
		out.putDouble(deltaLinkNorm.x);
		out.putDouble(deltaLinkNorm.y);
		out.putDouble(normalizedOrthogonal.x);
		out.putDouble(normalizedOrthogonal.y);
		out.putDouble(visLink.getLink().getNumberOfLanes());
		Collection<? extends Link> outlinks = visLink.getLink().getToNode().getOutLinks().values();
		out.putInt(outlinks.size());
		for (Link outLink : outlinks){
			ByteBufferUtils.putString(out, outLink.getId().toString());
		}
		return linkLengthCorrectionFactor;
	}

	private void writeLaneData(ByteBuffer out, VisLink visLink, LanesToLinkAssignment l2l, Map<Id, java.lang.Double> linkLengthCorrectionFactorsByLinkId){
		double linkLengthCorrectionFactor = linkLengthCorrectionFactorsByLinkId.get(visLink.getLink().getId());
		double linkScale = this.linkScaleByLinkIdMap.get(visLink.getLink().getId());
		//start to write the data
		ByteBufferUtils.putString(out, l2l.getLinkId().toString());
		int maxAlignment = 0;
		//write lane data
		int noLanes = l2l.getLanes().size();
		out.putInt(noLanes);
		//write the lane data
		for (Lane lane : l2l.getLanes().values()){
			QLane ql = this.getQLane(lane.getId(), (QLinkLanesImpl)visLink);
			OTFLane visLane = laneModelBuilder.createOTFLane(lane, ql, visLink.getLink().getLength(), linkScale, linkLengthCorrectionFactor);
			ByteBufferUtils.putObject(out, visLane);
			if (visLane.getAlignment() > maxAlignment) {
				maxAlignment = visLane.getAlignment();
			}
		}
		out.putInt(maxAlignment);

		if (DRAW_LINK_TO_LINK_LINES){
			//write the connections
			for (Lane lane : l2l.getLanes().values()){
				ByteBufferUtils.putString(out, lane.getId().toString());
				//write data for link2link lines
				this.writeLaneToLinkConnections(out, visLink, lane);
				this.writeLaneToLaneConnections(out, visLink, lane);
			} 
		}
	}
	
	private void writeLaneToLaneConnections(ByteBuffer out, VisLink visLink, Lane lane) {
		if (lane.getToLaneIds() != null){
			out.putInt(lane.getToLaneIds().size());
			for (Id toLaneId :  lane.getToLaneIds()){
				ByteBufferUtils.putString(out, toLaneId.toString());
			}
		}
		else {
			out.putInt(0);
		}
	}

	private void writeLaneToLinkConnections(ByteBuffer out, VisLink visLink, Lane lane){
		if (lane.getToLinkIds() != null){
			out.putInt(lane.getToLinkIds().size());
			for (Id toLinkId :  lane.getToLinkIds()){
				ByteBufferUtils.putString(out, toLinkId.toString());
			}
		}
		else {
			out.putInt(0);
		}
	}
	
  private double calculateEuclideanLinkLength(Point2D.Double deltaLink) {
  	return Math.sqrt(Math.pow(deltaLink.x, 2) + Math.pow(deltaLink.y, 2));
  }


	@Override
	public void writeDynData(ByteBuffer out) throws IOException {
		//nothing to do as lanes are not dynamic
	}

	private QLane getQLane(Id laneId, QLinkLanesImpl link){
		for (QLane lane : link.getQueueLanes()){
			if (lane.getId().equals(laneId)){
				return lane;
			}
		}
		throw new IllegalArgumentException("QLane Id " + laneId + " on link Id " + link.getLink().getId() + "  not found. Check configuration!");
	}

	

}
