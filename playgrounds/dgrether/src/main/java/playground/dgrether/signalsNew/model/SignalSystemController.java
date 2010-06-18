/* *********************************************************************** *
 * project: org.matsim.*
 * SignalSystemController
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
package playground.dgrether.signalsNew.model;

import java.util.Map;

import org.matsim.api.core.v01.Id;


/**
 * <ul>
 * 		<li>No Id is needed because it is given in the SignalSystemData</li>
 * 		<li>The default cycle time is also given by the SignalSystemData</li>
 * </ul>
 * @author dgrether
 */
public interface SignalSystemController {
	

	public void addSignalGroup(SignalGroup group);
	
	public Map<Id, SignalGroup> getSignalGroups();
	
	public void updateState(double time_seconds);

	
}
