package org.matsim.contrib.pseudosimulation.searchacceleration.logging;

import floetteroed.utilities.statisticslogging.Statistic;

public class DriversInPhysicalSim implements Statistic<LogDataWrapper> {

	public static final String PHYSICAL_DRIVERS = "PhysicalDrivers";

	@Override
	public String label() {
		return PHYSICAL_DRIVERS;
	}

	@Override
	public String value(LogDataWrapper arg0) {
		return Statistic.toString(arg0.getDriversInPhysicalSim());
	}

}
