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
package org.matsim.contrib.signals.data;

import org.matsim.contrib.signals.SignalSystemsConfigGroup;
import org.matsim.contrib.signals.data.ambertimes.v10.AmberTimesDataImpl;
import org.matsim.contrib.signals.data.intergreens.v10.IntergreenTimesData;
import org.matsim.contrib.signals.data.intergreens.v10.IntergreenTimesDataImpl;
import org.matsim.contrib.signals.data.signalcontrol.v20.SignalControlDataImpl;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupsDataImpl;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemsDataImpl;
import org.matsim.contrib.signals.data.ambertimes.v10.AmberTimesData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalControlData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupsData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemsData;


/**
 * @author dgrether
 *
 */
public class SignalsDataImpl implements SignalsData {

	private SignalSystemsData signalsystemsdata;
	private SignalGroupsData signalgroupsdata;
	private SignalControlData signalcontroldata;
	private AmberTimesData ambertimesdata = null;
	private IntergreenTimesData intergreensdata = null;
	
	public SignalsDataImpl(SignalSystemsConfigGroup signalConfig){
		this.initContainers(signalConfig);
	}
	
	private void initContainers(SignalSystemsConfigGroup signalConfig){
		this.signalsystemsdata = new SignalSystemsDataImpl();
		this.signalgroupsdata = new SignalGroupsDataImpl();
		this.signalcontroldata = new SignalControlDataImpl();
		if (signalConfig.isUseAmbertimes()){
			this.ambertimesdata = new AmberTimesDataImpl();
		}
		if (signalConfig.isUseIntergreenTimes()) {
			this.intergreensdata = new IntergreenTimesDataImpl();
		}
	}
	
	
	@Override
	public AmberTimesData getAmberTimesData() {
		return this.ambertimesdata;
	}

	@Override
	public IntergreenTimesData getIntergreenTimesData() {
		return this.intergreensdata;
	}

	@Override
	public SignalGroupsData getSignalGroupsData() {
		return this.signalgroupsdata;
	}

	@Override
	public SignalControlData getSignalControlData() {
		return this.signalcontroldata;
	}

	@Override
	public SignalSystemsData getSignalSystemsData() {
		return this.signalsystemsdata;
	}

}
