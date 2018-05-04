/* *********************************************************************** *
 * project: org.matsim.*
 * IntergreenTimes
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
package org.matsim.contrib.signals.data.intergreens.v10;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.internal.MatsimToplevelContainer;
import org.matsim.contrib.signals.model.SignalSystem;



/**
 * @author dgrether
 *
 */
public interface IntergreenTimesData extends MatsimToplevelContainer  {
	
	public IntergreensForSignalSystemData addIntergreensForSignalSystem(IntergreensForSignalSystemData intergreens);
	
	public Map<Id<SignalSystem>, IntergreensForSignalSystemData> getIntergreensForSignalSystemDataMap();
	
	@Override
	public IntergreenTimesDataFactory getFactory() ;
	
	public void setFactory(IntergreenTimesDataFactory factory);
	
}
