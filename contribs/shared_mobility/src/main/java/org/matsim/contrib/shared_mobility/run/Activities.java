package org.matsim.contrib.shared_mobility.run;

import org.matsim.core.config.Config;
import org.matsim.core.config.groups.ScoringConfigGroup;

/**
 * Defines available activity types.
 */
public enum Activities {
	home,
	other,
	outside_recreation,
	transport,
	visit,
	edu_kiga(7, 17),
	edu_primary(7, 17),
	edu_secondary(7, 17),
	edu_higher(7, 19),
	edu_other(7, 22),

	work(6, 21),
	work_business(8, 21),
	personal_business(8, 20),
	leisure(9, 27),
	dining(8, 27),
	shop_daily(8, 20),
	shop_other(8, 20),

	// Commercial traffic types
	service;

	/**
	 * Start time of an activity in hours, can be -1 if not defined.
	 */
	private final double start;

	/**
	 * End time of an activity in hours, can be -1 if not defined.
	 */
	private final double end;

	Activities(double start, double end) {
		this.start = start;
		this.end = end;
	}

	Activities() {
		this.start = -1;
		this.end = -1;
	}


	/**
	 * Apply start and end time to params.
	 */
	public ScoringConfigGroup.ActivityParams apply(ScoringConfigGroup.ActivityParams params) {
		if (start >= 0)
			params = params.setOpeningTime(start * 3600.);
		if (end >= 0)
			params = params.setClosingTime(end * 3600.);

		return params;
	}

	/**
	 * Add required activity params for the scenario.
	 */
	public static void addScoringParams(Config config, boolean splitTypes) {

		for (Activities value : Activities.values()) {
			// Default length if none is given
			config.scoring().addActivityParams(value.apply(new ScoringConfigGroup.ActivityParams(value.name())).setTypicalDuration(6 * 3600));

			if (splitTypes)
				for (long ii = 600; ii <= 97200; ii += 600) {
					config.scoring().addActivityParams(value.apply(new ScoringConfigGroup.ActivityParams(value.name() + "_" + ii).setTypicalDuration(ii)));
				}
		}

		config.scoring().addActivityParams(new ScoringConfigGroup.ActivityParams("commercial_start").setTypicalDuration(3600));
		config.scoring().addActivityParams(new ScoringConfigGroup.ActivityParams("commercial_end").setTypicalDuration(3600));

		config.scoring().addActivityParams(new ScoringConfigGroup.ActivityParams("freight_start").setTypicalDuration(3600));
		config.scoring().addActivityParams(new ScoringConfigGroup.ActivityParams("freight_end").setTypicalDuration(3600));

	}

}
