/* *********************************************************************** *
 * project: org.matsim.*
 * SignalsDataImpl
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
package org.matsim.signalsystems.data;

import org.matsim.signalsystems.data.ambertimes.v10.AmberTimesData;
import org.matsim.signalsystems.data.ambertimes.v10.AmberTimesDataImpl;
import org.matsim.signalsystems.data.signalcontrol.v20.SignalControlData;
import org.matsim.signalsystems.data.signalcontrol.v20.SignalControlDataImpl;
import org.matsim.signalsystems.data.signalgroups.v20.SignalGroupsData;
import org.matsim.signalsystems.data.signalgroups.v20.SignalGroupsDataImpl;
import org.matsim.signalsystems.data.signalsystems.v20.SignalSystemsData;
import org.matsim.signalsystems.data.signalsystems.v20.SignalSystemsDataImpl;


/**
 * @author dgrether
 *
 */
public class SignalsDataImpl implements SignalsData {

	private SignalSystemsData signalsystemsdata;
	private AmberTimesData ambertimesdata;
	private SignalGroupsData signalgroupsdata;
	private SignalControlData signalcontroldata;
	
	public SignalsDataImpl(){
		this.initContainers();
	}
	
	private void initContainers(){
		this.signalsystemsdata = new SignalSystemsDataImpl();
		this.signalgroupsdata = new SignalGroupsDataImpl();
		this.signalcontroldata = new SignalControlDataImpl();
		this.ambertimesdata = new AmberTimesDataImpl();
	}
	
	
	@Override
	public AmberTimesData getAmberTimesData() {
		return this.ambertimesdata;
	}

//	@Override
//	public IntergreenTimesData getIntergreenTimesData() {
//		return null;
//	}

	@Override
	public SignalGroupsData getSignalGroupsData() {
		return this.signalgroupsdata;
	}

	@Override
	public SignalControlData getSignalSystemControlData() {
		return this.signalcontroldata;
	}

	@Override
	public SignalSystemsData getSignalSystemsData() {
		return this.signalsystemsdata;
	}

}
