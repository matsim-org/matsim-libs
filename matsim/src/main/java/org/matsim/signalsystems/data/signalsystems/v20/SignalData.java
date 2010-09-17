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
package org.matsim.signalsystems.data.signalsystems.v20;

import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;


/**
 * @author dgrether
 *
 */
public interface SignalData extends Identifiable {

	public Id getLinkId();
	
	public void setLinkId(Id id);

	public Set<Id> getLaneIds();
	
	public void addLaneId(Id laneId);
	
	public Set<Id> getTurningMoveRestrictions();
	
	public void addTurningMoveRestriction(Id linkId);

}
