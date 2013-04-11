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

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.events.MobsimAfterSimStepEvent;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.agents.ExperimentalBasicWithindayAgent;
import org.matsim.core.mobsim.qsim.agents.PlanBasedWithinDayAgent;

import playground.christoph.parking.core.mobsim.ParkingInfrastructure;
import playground.christoph.parking.withinday.utils.ParkingAgentsTracker;
import playground.wrashid.parkingSearch.withinDay_v_STRC.WithinDayParkingController;
import playground.wrashid.parkingSearch.withinDay_v_STRC.scoring.ParkingScoreManager;
import playground.wrashid.parkingSearch.withinDay_v_STRC.strategies.manager.ParkingStrategyManager;

public class ParkingAgentsTracker_v2 extends ParkingAgentsTracker implements AfterMobsimListener {

	private ParkingStrategyManager parkingStrategyManager;
	protected WithinDayParkingController controler;

	public ParkingAgentsTracker_v2(Scenario scenario, ParkingInfrastructure parkingInfrastructure, double distance, WithinDayParkingController controler) {
		super(scenario, parkingInfrastructure, distance);
		this.controler = controler;
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
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		parkingStrategyManager.printStrategyStatistics();
	}

}

