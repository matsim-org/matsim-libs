package org.matsim.contrib.pseudosimulation.searchacceleration.logging;

import floetteroed.utilities.statisticslogging.Statistic;

public class DriversInPseudoSim implements Statistic<LogDataWrapper> {

	public static final String PSIM_DRIVERS = "PseudoSimDrivers";

	@Override
	public String label() {
		return PSIM_DRIVERS;
	}

	@Override
	public String value(LogDataWrapper arg0) {
		return Statistic.toString(arg0.getDriversInPseudoSim());
	}

}
