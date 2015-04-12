/* *********************************************************************** *
 * project: org.matsim.*
 * SignalDefinitionData
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
package org.matsim.signals.data.signalsystems.v20;

import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.api.core.v01.network.Link;
import org.matsim.lanes.data.v20.Lane;
import org.matsim.signals.model.Signal;


/**
 * Holds data modelling the physics of a signal on one link/lane. 
 * @author dgrether
 *
 */
public interface SignalData extends Identifiable<Signal> {
	/**
	 * @return the Id of the Signal 
	 */
	@Override
	public Id<Signal> getId();
	/**
	 * @return the Id of the Link the signal is located on.
	 */
	public Id<Link> getLinkId();
	
	public void setLinkId(Id<Link> id);
	/**
	 * @return A Set of Lane Ids, if the signal is placed on cerain lanes. Maybe null or empty if the signal is
	 * on the link.
	 */
	public Set<Id<Lane>> getLaneIds();
	
	public void addLaneId(Id<Lane> laneId);
	/**
	 * @return Returns a Set of link Ids to that driving is allowed when the signal 
	 * is activated. May return null if no turning  move restrictions are set.
	 */
	public Set<Id<Link>> getTurningMoveRestrictions();
	
	public void addTurningMoveRestriction(Id<Link> linkId);

}
