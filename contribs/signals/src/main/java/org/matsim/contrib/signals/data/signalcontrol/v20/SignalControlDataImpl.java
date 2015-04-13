/* *********************************************************************** *
 * project: org.matsim.*
 * SignalControlDataImpl
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

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.signals.data.signalcontrol.v20.SignalControlData;
import org.matsim.signals.data.signalcontrol.v20.SignalControlDataFactory;
import org.matsim.signals.data.signalcontrol.v20.SignalSystemControllerData;
import org.matsim.signals.model.SignalSystem;


/**
 * @author dgrether
 *
 */
public class SignalControlDataImpl implements SignalControlData {

	private SignalControlDataFactory factory = new SignalControlDataFactoryImpl();
	private Map<Id<SignalSystem>, SignalSystemControllerData> signalControllerDataBySystemId = new HashMap<>();

	@Override
	public void addSignalSystemControllerData(SignalSystemControllerData controllerData) {
		this.signalControllerDataBySystemId.put(controllerData.getSignalSystemId(), controllerData);
	}

	@Override
	public SignalControlDataFactory getFactory() {
		return this.factory ;
	}

	@Override
	public Map<Id<SignalSystem>, SignalSystemControllerData> getSignalSystemControllerDataBySystemId() {
		return this.signalControllerDataBySystemId;
	}

	@Override
	public void setFactory(SignalControlDataFactory factory) {
		this.factory = factory;		
	}

}
