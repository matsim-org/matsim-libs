/* *********************************************************************** *
 * project: org.matsim.*
 * RandomSearchReplannerFactory.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.wrashid.parkingSearch.withindayFW.zhCity.HUPC;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.withinday.mobsim.ReplanningManager;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayDuringLegReplannerFactory;

import playground.wrashid.parkingSearch.withindayFW.core.ParkingAgentsTracker;

public class HUPCReplannerFactory extends WithinDayDuringLegReplannerFactory {

	private final Scenario scenario;
	private final ParkingAgentsTracker parkingAgentsTracker;
	
	public HUPCReplannerFactory(ReplanningManager replanningManager, AbstractMultithreadedModule abstractMultithreadedModule,
			double replanningProbability, Scenario scenario, ParkingAgentsTracker parkingAgentsTracker) {
		super(replanningManager, abstractMultithreadedModule, replanningProbability);
		
		this.scenario = scenario;
		this.parkingAgentsTracker = parkingAgentsTracker;
	}

	@Override
	public HUPCReplanner createReplanner() {
		HUPCReplanner replanner = new HUPCReplanner(super.getId(), scenario, 
				this.getReplanningManager().getInternalInterface(), parkingAgentsTracker);
		super.initNewInstance(replanner);
		return replanner;
	}

}
