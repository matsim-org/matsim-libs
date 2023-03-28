package org.matsim.contrib.vsp.scenario;

import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;

/**
 * Defines available activities and open- and closing times in Snz scenarios at vsp.
 */
public enum SnzActivities {

	home,
	other,
	visit,

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
	public PlanCalcScoreConfigGroup.ActivityParams apply(PlanCalcScoreConfigGroup.ActivityParams params) {
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
				config.planCalcScore().addActivityParams(value.apply(new PlanCalcScoreConfigGroup.ActivityParams(value.name() + "_" + ii).setTypicalDuration(ii)));
			}
		}

		config.planCalcScore().addActivityParams(new PlanCalcScoreConfigGroup.ActivityParams("car interaction").setTypicalDuration(60));
		config.planCalcScore().addActivityParams(new PlanCalcScoreConfigGroup.ActivityParams("ride interaction").setTypicalDuration(60));
		config.planCalcScore().addActivityParams(new PlanCalcScoreConfigGroup.ActivityParams("bike interaction").setTypicalDuration(60));

		config.planCalcScore().addActivityParams(new PlanCalcScoreConfigGroup.ActivityParams("other").setTypicalDuration(600 * 3));

		config.planCalcScore().addActivityParams(new PlanCalcScoreConfigGroup.ActivityParams("freight_start").setTypicalDuration(60 * 15));
		config.planCalcScore().addActivityParams(new PlanCalcScoreConfigGroup.ActivityParams("freight_end").setTypicalDuration(60 * 15));

	}
}
