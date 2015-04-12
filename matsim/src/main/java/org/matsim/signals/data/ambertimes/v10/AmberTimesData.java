/* *********************************************************************** *
 * project: org.matsim.*
 * AmberTimes
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
package org.matsim.signals.data.ambertimes.v10;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.internal.MatsimToplevelContainer;
import org.matsim.signals.model.SignalSystem;


/**
 * @author dgrether
 *
 */
public interface AmberTimesData extends MatsimToplevelContainer {
	
	@Override
	public AmberTimesDataFactory getFactory();
	
	public Double getDefaultAmberTimeGreen();
	
	public Integer getDefaultRedAmber();
	
	public Integer getDefaultAmber();
	
	public Map<Id<SignalSystem>, AmberTimeData> getAmberTimeDataBySystemId();
	
	public void addAmberTimeData(AmberTimeData amberTimeData);
	
	public void setDefaultRedAmber(Integer seconds);
	
	public void setDefaultAmber(Integer seconds);
	
	public void setDefaultAmberTimeGreen(Double proportion);
	
	public void setFactory(AmberTimesDataFactory factory);
}

