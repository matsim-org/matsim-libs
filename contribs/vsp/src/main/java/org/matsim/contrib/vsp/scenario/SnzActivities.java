package org.matsim.contrib.vsp.scenario;

import org.matsim.core.config.Config;
import org.matsim.core.config.groups.ScoringConfigGroup;

/**
 * Defines available activities and open- and closing times in Snz scenarios at vsp.
 */
public enum SnzActivities {

	home,
	other,
	visit,
	accomp_children,
	accomp_other,

	educ_kiga(7, 17),
	educ_primary(7, 16),
	educ_secondary(7, 17),
	educ_tertiary(7, 22),
	educ_higher(7, 19),
	educ_other(7, 22),

	work(6, 20),
	business(8, 20),
	errands(8, 20),

	leisure(9, 27),
	restaurant(8, 27),
	shop_daily(8, 20),
	shop_other(8, 20);

	/**
	 * Start time of an activity in hours, can be -1 if not defined.
	 */
	private final double start;

	/**
	 * End time of an activity in hours, can be -1 if not defined.
	 */
	private final double end;

	SnzActivities(double start, double end) {
		this.start = start;
		this.end = end;
	}

	SnzActivities() {
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
	 * Add activity params for the scenario config.
	 */
	public static void addScoringParams(Config config) {

		for (SnzActivities value : SnzActivities.values()) {
			for (long ii = 600; ii <= 97200; ii += 600) {
				config.scoring().addActivityParams(value.apply(new ScoringConfigGroup.ActivityParams(value.name() + "_" + ii).setTypicalDuration(ii)));
			}
		}

		config.scoring().addActivityParams(new ScoringConfigGroup.ActivityParams("other").setTypicalDuration(600 * 3));

		config.scoring().addActivityParams(new ScoringConfigGroup.ActivityParams("freight_start").setTypicalDuration(60 * 15));
		config.scoring().addActivityParams(new ScoringConfigGroup.ActivityParams("freight_end").setTypicalDuration(60 * 15));

	}
}
