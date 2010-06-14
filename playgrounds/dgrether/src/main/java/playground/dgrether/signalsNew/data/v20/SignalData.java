/* *********************************************************************** *
 * project: org.matsim.*
 * SignalData
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
package playground.dgrether.signalsNew.data.v20;

import java.util.Set;

import org.matsim.api.core.v01.Id;


/**
 * @author dgrether
 *
 */
public interface SignalData {

	/**
	 * @return The Id of the Signal
	 */
	public Id getId();
	/**
	 * @return The Id of the Link the SignalGroup is located on
	 */
	public Id getLinkRefId();
	
	
	public void addTurningMoveRestrictionToLinkId(Id toLinkId);
	
	public Set<Id> getTurningMoveRestrictionsToLinkIds();
	
  /**
   * @return The Ids of the Lanes controlled by this signal, null if the complete Link is controlled.
   */
  public Set<Id> getLaneIds();
	
	/**
	 * Add the Id of a Lane that is controlled by this Signal
	 * @param laneId
	 */
	public void addLaneId(Id laneId);

	
}
