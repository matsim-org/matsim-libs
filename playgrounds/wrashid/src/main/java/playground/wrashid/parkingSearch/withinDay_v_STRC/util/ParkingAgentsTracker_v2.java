/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.wrashid.parkingSearch.withinDay_v_STRC.util;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.parking.lib.obj.DoubleValueHashMap;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.qsim.agents.PlanBasedWithinDayAgent;

import playground.christoph.parking.core.mobsim.ParkingInfrastructure;
import playground.christoph.parking.withinday.utils.ParkingAgentsTracker;
import playground.wrashid.parkingSearch.withinDay_v_STRC.core.mobsim.ParkingInfrastructure_v2;
import playground.wrashid.parkingSearch.withinDay_v_STRC.strategies.manager.ParkingStrategyManager;

public class ParkingAgentsTracker_v2 extends ParkingAgentsTracker implements IterationEndsListener {

	protected ParkingStrategyManager parkingStrategyManager;
	protected Controler controler;
	private DoubleValueHashMap<Id> firstCarDepartureTimeOfDay;
	
	
	public Scenario getScenario(){
		return scenario;
	}
	
	public ParkingAgentsTracker_v2(Scenario scenario, ParkingInfrastructure parkingInfrastructure, double distance, Controler controler) {
		super(scenario, parkingInfrastructure, distance);
		this.controler = controler;
	}
	
	public ParkingInfrastructure_v2 getInfrastructure_v2(){
		return (ParkingInfrastructure_v2) parkingInfrastructure;
	}
	
	@Override
	public void reset(int iteration) {
		super.reset(iteration);
		firstCarDepartureTimeOfDay=new DoubleValueHashMap<Id>();
	}

	@Override
	public void notifyMobsimInitialized(MobsimInitializedEvent e) {
		super.notifyMobsimInitialized(e);
		for (PlanBasedWithinDayAgent agent:this.agents.values()){
			getParkingStrategyManager().prepareStrategiesForNewIteration(agent, controler.getIterationNumber());
		}
	}

	public ParkingStrategyManager getParkingStrategyManager() {
		return parkingStrategyManager;
	}

	public void setParkingStrategyManager(ParkingStrategyManager parkingStrategyManager) {
		this.parkingStrategyManager = parkingStrategyManager;
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		parkingStrategyManager.printStrategyStatistics();
	}

	public DoubleValueHashMap<Id> getFirstCarDepartureTimeOfDay() {
		return firstCarDepartureTimeOfDay;
	}
	
	@Override
    public void handleEvent(AgentDepartureEvent event) {
    	super.handleEvent(event);
    	
    	Id personId = event.getPersonId();
		if (!firstCarDepartureTimeOfDay.containsKey(personId) && event.getLegMode().equals(TransportMode.car)){
    		firstCarDepartureTimeOfDay.put(personId, event.getTime());
    	}
    }

}

