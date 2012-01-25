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

import org.matsim.lanes.data.v20.Lane;
import org.matsim.lanes.otfvis.io.OTFLane;
import org.matsim.ptproject.qsim.qnetsimengine.QLane;



/**
 * @author dgrether
 *
 */
public class OTFLaneModelBuilder {

	
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



}
