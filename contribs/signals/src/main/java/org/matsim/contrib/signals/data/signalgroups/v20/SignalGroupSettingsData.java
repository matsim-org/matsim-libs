/* *********************************************************************** *
 * project: org.matsim.*
 * SignalGroupSettingsData
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
import org.matsim.contrib.signals.model.SignalGroup;


/**
 * @author dgrether
 *
 */
public interface SignalGroupSettingsData {

	public Id<SignalGroup> getSignalGroupId();
	
	public int getOnset();

	public void setOnset(int second);

	public int getDropping();

	public void setDropping(int second);
	
}
