/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package peoplemover.stop;

import org.matsim.contrib.dvrp.passenger.PassengerEngine;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic;
import org.matsim.core.mobsim.framework.MobsimTimer;

/**
 * @author Michal Maciejewski (michalm)
 */
public class VariableDurationBusStopQSimModule extends AbstractDvrpModeQSimModule {
	private final BusStopDurationCalculator busStopDurationCalculator;

	public VariableDurationBusStopQSimModule(String mode, BusStopDurationCalculator busStopDurationCalculator) {
		super(mode);
		this.busStopDurationCalculator = busStopDurationCalculator;
	}

	@Override
	protected void configureQSim() {
		bindModal(BusStopDurationCalculator.class).toInstance(busStopDurationCalculator);
		bindModal(VrpAgentLogic.DynActionCreator.class).toProvider(modalProvider(
				getter -> new CustomizedDrtActionCreator(getter.getModal(PassengerEngine.class),
						getter.get(MobsimTimer.class), getter.get(DvrpConfigGroup.class),
						getter.getModal(BusStopDurationCalculator.class)))).asEagerSingleton();
	}
}
