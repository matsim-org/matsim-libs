/* *********************************************************************** *
 * project: org.matsim.*
 * SignalGroupSettingsDataImpl
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
package org.matsim.contrib.signals.data.signalcontrol.v20;

import org.matsim.api.core.v01.Id;
import org.matsim.signals.data.signalcontrol.v20.SignalGroupSettingsData;
import org.matsim.signals.model.SignalGroup;


/**
 * @author dgrether
 *
 */
public class SignalGroupSettingsDataImpl implements SignalGroupSettingsData {

	private int dropping;
	private int onset;
	private Id<SignalGroup> signalGroupId;

	public SignalGroupSettingsDataImpl(Id<SignalGroup> signalGroupId){
		this.signalGroupId = signalGroupId;
	}
	
	@Override
	public int getDropping() {
		return this.dropping;
	}

	@Override
	public int getOnset() {
		return this.onset;
	}

	@Override
	public Id<SignalGroup> getSignalGroupId() {
		return this.signalGroupId;
	}

	@Override
	public void setDropping(int second) {
		this.dropping = second;
	}

	@Override
	public void setOnset(int second) {
		this.onset= second;
	}

}
