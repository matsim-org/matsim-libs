/* *********************************************************************** *
 * project: matsim
 * AdditionalTeleportationDepartureEvent.java
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

package org.matsim.core.events;

import org.matsim.api.core.v01.Id;

/**
 * @author nagel
 *
 */
@Deprecated // this is a possibly temporary fix to remove the MobsimFeatures.  do not use.  kai, aug'10
public class AdditionalTeleportationDepartureEvent extends EventImpl {

	private final Id agentId;
	private final Id linkId;
	private final String mode;
	private final Id destinationLinkId;
	private final double travelTime;

	/**
	 * @param time
	 * @param travelTime 
	 * @param destinationLinkId 
	 * @param mode 
	 * @param linkId 
	 * @param agentId 
	 */
	@Deprecated // this is a possibly temporary fix to remove the MobsimFeatures.  do not use.  kai, aug'10
	public AdditionalTeleportationDepartureEvent(double now, Id agentId, Id linkId, String mode, Id destinationLinkId, double travelTime) {
		super(now);
		this.agentId = agentId;
		this.linkId = linkId;
		this.mode = mode;
		this.destinationLinkId = destinationLinkId;
		this.travelTime = travelTime;
	}

	/* (non-Javadoc)
	 * @see org.matsim.core.events.EventImpl#getEventType()
	 */
	@Override
	public String getEventType() {
		return "teleportation departure" ;
	}

	public Id getAgentId() {
		return agentId;
	}

	public Id getLinkId() {
		return linkId;
	}

	public String getMode() {
		return mode;
	}

	public Id getDestinationLinkId() {
		return destinationLinkId;
	}

	public double getTravelTime() {
		return travelTime;
	}

}
