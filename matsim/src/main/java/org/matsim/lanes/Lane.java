
/* *********************************************************************** *
 * project: org.matsim.*
 * Lane.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.utils.objectattributes.attributable.Attributable;

import java.util.List;

/**
 * Models a lane on a link.
 * 
 * The position is specified via the distance the lane starts measured from the
 * downstream node of the link and the ids of the downstream lanes. If the
 * distance to the downstream node equals the link length, this implies that
 * this is the most upstream (first) lane on a link. More than one first lane is
 * permitted. If the first or any other lane shall model more than one lane that
 * exists in the reality to be modeled, increase the number of represented lanes
 * attribute.
 * 
 * The alignment is used to visualize parallel lanes in the correct order. The
 * higher the alignment the more right the lane is visualized on a link. Lanes
 * leading straight on are assigned with an alignment of zero, lanes leading
 * left with an negative and lanes leading right with an positive alignment.
 * 
 * For an easy modeling use the v11 lane model and the conversion class in the
 * package.
 * 
 * @author dgrether
 * 
 */
public interface Lane extends Attributable {

	public Id<Lane> getId();

	public double getNumberOfRepresentedLanes();

	public void setNumberOfRepresentedLanes(double number);

	public void setStartsAtMeterFromLinkEnd(double meter);

	public double getStartsAtMeterFromLinkEnd();

	public double getCapacityVehiclesPerHour();

	public void setCapacityVehiclesPerHour(double capacity);

	public void addToLinkId(Id<Link> id);

	public void addToLaneId(Id<Lane> id);
	/**
	 *
	 * @return List may be null if nothing is set
	 */
	public List<Id<Link>> getToLinkIds();
	/**
	 *
	 * @return List may be null if nothing is set
	 */
	public List<Id<Lane>> getToLaneIds();

	public void setAlignment(int alignment);

	public int getAlignment();

}
