/* *********************************************************************** *
 * project: org.matsim.*
 * IntergreenTimesDataImpl
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package org.matsim.contrib.signals.data.intergreens.v10;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.signals.data.ambertimes.v10.IntergreenTimesData;
import org.matsim.contrib.signals.data.ambertimes.v10.IntergreenTimesDataFactory;
import org.matsim.contrib.signals.data.ambertimes.v10.IntergreensForSignalSystemData;
import org.matsim.contrib.signals.model.SignalSystem;


/**
 * @author dgrether
 *
 */
public class IntergreenTimesDataImpl implements IntergreenTimesData {

	private IntergreenTimesDataFactory factory = new IntergreenTimesDataFactoryImpl();

	private Map<Id<SignalSystem>, IntergreensForSignalSystemData> intergreensForSystemByIdMap = new HashMap<>();
	
	@Override
	public IntergreensForSignalSystemData addIntergreensForSignalSystem(
			IntergreensForSignalSystemData intergreens) {
		return this.intergreensForSystemByIdMap.put(intergreens.getSignalSystemId(), intergreens);
	}

	@Override
	public Map<Id<SignalSystem>, IntergreensForSignalSystemData> getIntergreensForSignalSystemDataMap() {
		return this.intergreensForSystemByIdMap;
	}

	@Override
	public IntergreenTimesDataFactory getFactory() {
		return this.factory;
	}

	@Override
	public void setFactory(IntergreenTimesDataFactory factory) {
		this.factory = factory;
	}

}
