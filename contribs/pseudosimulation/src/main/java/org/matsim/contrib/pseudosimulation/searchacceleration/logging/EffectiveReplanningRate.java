package org.matsim.contrib.pseudosimulation.searchacceleration.logging;

import floetteroed.utilities.statisticslogging.Statistic;

public class EffectiveReplanningRate implements Statistic<LogDataWrapper> {

	public static final String EFFECTIVE_REPLANNING_RATE = "EffectiveReplanningRate";
	
	@Override
	public String label() {
		return EFFECTIVE_REPLANNING_RATE;
	}

	@Override
	public String value(LogDataWrapper arg0) {
		return Statistic.toString(arg0.getEffectiveReplanninRate());
	}

}
