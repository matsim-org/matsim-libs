/* *********************************************************************** *
 * project: org.matsim.*
 * SignalSystemsManagerImpl
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
import java.util.SortedMap;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.core.api.experimental.events.EventsManager;

/**
 * @author dgrether
 */
public class SignalSystemsManagerImpl implements SignalSystemsManager {

	private SortedMap<Id<SignalSystem>, SignalSystem> signalSystems = new TreeMap<>();
	
	private AmberLogic amberLogic = new EmptyAmberLogicImpl();

	private EventsManager eventsManager;

	private SignalsData signalData;

	private IntergreensLogic intergreensLogic = null;
	
	public SignalSystemsManagerImpl(SignalsData signalData, EventsManager eventsManager) {
//		this.signalData = (SignalsData) scenario.getScenarioElement(SignalsData.ELEMENT_NAME);
		this.signalData = signalData;
		this.eventsManager = eventsManager;
	}
	
	@Override
	public EventsManager getEventsManager() {
		return this.eventsManager;
	}
	
	@Override
	public void requestControlUpdate(double time_sec) {
		for (SignalSystem system : this.signalSystems.values()){
			system.updateState(time_sec);
		}
	}

	@Override
	public AmberLogic getAmberLogic(){
		return this.amberLogic;
	}

	@Override
	public void addSignalSystem(SignalSystem system) {
		this.signalSystems.put(system.getId(), system);
	}

	@Override
	public void setAmberLogic(AmberLogic amberLogic) {
		this.amberLogic = amberLogic;
	}

	@Override
	public Map<Id<SignalSystem>, SignalSystem> getSignalSystems() {
		return this.signalSystems;
	}

	@Override
	public SignalsData getSignalsData() {
		return this.signalData;
	}

	@Override
	public IntergreensLogic getIntergreensLogic() {
		return this.intergreensLogic;
	}

	@Override
	public void setIntergreensLogic(IntergreensLogic logic) {
		this.intergreensLogic = logic;
	}

}
