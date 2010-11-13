/* *********************************************************************** *
 * project: org.matsim.*
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

package org.matsim.ptproject.qsim.qnetsimengine;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.ptproject.qsim.interfaces.NetsimEngine;
import org.matsim.ptproject.qsim.interfaces.NetsimLink;

public abstract class QLinkInternalI extends QBufferItem implements NetsimLink {
	// yyyy this class needs to be public with some of the traffic signal code, but I do not understand why.  kai, aug'10

	// for Customizable
	private Map<String, Object> customAttributes = new HashMap<String, Object>();

	abstract void setQSimEngine(NetsimEngine qsimEngine);

	protected abstract boolean moveLink(double now);

	abstract boolean hasSpace();

	abstract void clearVehicles();

	abstract QVehicle removeParkedVehicle(Id vehicleId);
	// in contrast to "addParkedVehicle", this here does not need to be public since it is only used internally.  kai, aug'10

	abstract void activateLink();

	abstract void addFromIntersection(final QVehicle veh);

	abstract QSimEngineInternalI getQSimEngine() ;

//	@Deprecated // not needed here (I think)
//	public abstract LinkedList<QVehicle> getVehQueue() ;

	@Override
	public Map<String, Object> getCustomAttributes() {
		return customAttributes;
	}
}
