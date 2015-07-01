/* *********************************************************************** *
 * project: org.matsim.*
 * LaneDataV1
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
package org.matsim.lanes.data.v11;

import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.lanes.data.v20.Lane;


/**
 * Models a lane that ends at the downstream node of a link.
 * 
 * The position is specified via the distance the lane starts measured from the downstream node
 * of the link. The 'starts at meter from link end' attribute corresponds to the 'length' attribute in
 * the laneDefinitions_v1.1.xsd.
 * 
 * All downstream links that can be reached by vehicles leaving this lane must be completely given
 * by their id.
 * 
 * If a lane shall model more than one lane existing in the reality to be modeled, increase the number of represented lanes attribute.
 * 
 * @author dgrether
 *
 */
public interface LaneData11 {

	public Id<Lane> getId();

	public void setNumberOfRepresentedLanes(double number);

	public double getNumberOfRepresentedLanes();

	public void setStartsAtMeterFromLinkEnd(double meter);

	public double getStartsAtMeterFromLinkEnd();

	public void addToLinkId(Id<Link> id);

	public List<Id<Link>> getToLinkIds();

	
}
