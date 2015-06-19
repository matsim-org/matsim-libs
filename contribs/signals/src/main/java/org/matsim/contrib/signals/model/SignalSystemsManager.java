/* *********************************************************************** *
 * project: org.matsim.*
 * SignalSystemsManager
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
package org.matsim.contrib.signals.model;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.contrib.signals.data.SignalsData;


/**
 * @author dgrether
 *
 */
public interface SignalSystemsManager {

	public SignalsData getSignalsData();
	
	public void setSignalsData(SignalsData signalsData);

	public EventsManager getEventsManager();
	
	public void setEventsManager(EventsManager events);
	
	public void addSignalSystem(SignalSystem system);

	public Map<Id<SignalSystem>, SignalSystem> getSignalSystems();
	
	public void resetModel(Integer iterationNumber);
	
	public void requestControlUpdate(double time_sec);

	public void setAmberLogic(AmberLogic amberLogic);
	
	public AmberLogic getAmberLogic();
	
	public IntergreensLogic getIntergreensLogic();

	public void setIntergreensLogic(IntergreensLogic logic);
	

}
