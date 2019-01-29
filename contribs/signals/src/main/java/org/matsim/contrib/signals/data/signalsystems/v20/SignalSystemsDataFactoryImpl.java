/* *********************************************************************** *
 * project: org.matsim.*
 * SignalSystemsDataFactoryImpl
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
package org.matsim.contrib.signals.data.signalsystems.v20;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalData;
import org.matsim.contrib.signals.model.Signal;
import org.matsim.contrib.signals.model.SignalSystem;


/**
 * @author dgrether
 *
 */
public class SignalSystemsDataFactoryImpl implements SignalSystemsDataFactory {

	@Override
	public SignalData createSignalData(Id<Signal> id) {
		return new SignalDataImpl(id);
	}

	@Override
	public SignalSystemData createSignalSystemData(Id<SignalSystem> id) {
		return new SignalSystemDataImpl(id);
	}

}
