/* *********************************************************************** *
 * project: org.matsim.*
 * Plansgenerator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package playground.droeder.eMobility.v2.fleet;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.GenericEvent;
import org.matsim.core.events.GenericEventImpl;
import org.matsim.core.mobsim.framework.events.MobsimAfterSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimAfterSimStepListener;

/**
 * @author droeder
 *
 */
public class EmobFleet implements MobsimAfterSimStepListener{
	
	private Map<Id, EmobVehicle> fleet;
	private EventsManager manager;

	public EmobFleet(EventsManager manager){
		this.fleet = new HashMap<Id, EmobVehicle>();
		this.manager = manager;
	}
	
	public EmobVehicle getVehicle(Id  id){
		return this.fleet.get(id);
	}
	
	public void addVehicle(EmobVehicle veh){
		this.fleet.put(veh.getId(), veh);
	}
	
	public Map<Id, EmobVehicle> getFleet(){
		return this.fleet;
	}

	@Override
	public void notifyMobsimAfterSimStep(MobsimAfterSimStepEvent e) {
		for(EmobVehicle veh : this.fleet.values()){
			GenericEvent ge;
			if(veh.changedSoC()){
				ge = new GenericEventImpl("SoCChange", e.getSimulationTime());
				ge.getAttributes().put("SoC", String.valueOf(veh.getCurrentSoC()));
				ge.getAttributes().put("link", String.valueOf(veh.getPositionLinkId()));
				this.manager.processEvent(ge);
			}
		}
	}

}
