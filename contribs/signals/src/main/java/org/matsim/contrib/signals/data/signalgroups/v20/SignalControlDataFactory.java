/* *********************************************************************** *
 * project: org.matsim.*
 * SignalSystemsControlDataFactory
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
package org.matsim.contrib.signals.data.signalgroups.v20;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.internal.MatsimFactory;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.contrib.signals.model.SignalPlan;
import org.matsim.contrib.signals.model.SignalSystem;


/**
 * @author dgrether
 *
 */
public interface SignalControlDataFactory extends MatsimFactory {

	public SignalSystemControllerData createSignalSystemControllerData(Id<SignalSystem> signalSystemId);
	
	public SignalPlanData createSignalPlanData(Id<SignalPlan> id);
	
	public SignalGroupSettingsData createSignalGroupSettingsData(Id<SignalGroup> signalGroupId);
}
