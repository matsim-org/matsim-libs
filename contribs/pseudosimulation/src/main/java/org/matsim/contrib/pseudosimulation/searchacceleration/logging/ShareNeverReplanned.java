package org.matsim.contrib.pseudosimulation.searchacceleration.logging;

import floetteroed.utilities.statisticslogging.Statistic;

public class ShareNeverReplanned implements Statistic<LogDataWrapper> {

	public static final String SHARE_NEVER_REPLANNED = "ShareNeverReplanned";

	@Override
	public String label() {
		return SHARE_NEVER_REPLANNED;
	}

	@Override
	public String value(LogDataWrapper arg0) {
		return Statistic.toString(arg0.getShareNeverReplanned());
	}

}
