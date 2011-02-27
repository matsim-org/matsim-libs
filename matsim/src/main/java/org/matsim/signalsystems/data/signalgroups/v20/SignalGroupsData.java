/* *********************************************************************** *
 * project: org.matsim.*
 * SignalGroupsData
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
package org.matsim.signalsystems.data.signalgroups.v20;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.internal.MatsimToplevelContainer;


/**
 * @author dgrether
 * @author jbischoff
 */
public interface SignalGroupsData extends MatsimToplevelContainer {

	@Override
	public SignalGroupsDataFactory getFactory();
	
	public Map<Id, SignalGroupData> getSignalGroupDataBySystemId(Id signalSystemId);
	
	public void addSignalGroupData(SignalGroupData signalGroupData);
	
	public Map<Id,Map<Id,SignalGroupData>> getSignalGroupDataBySignalSystemId();
	
	public void setFactory(SignalGroupsDataFactory factory);

}
