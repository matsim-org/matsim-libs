package org.matsim.core.scoring.functions;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.population.PopulationUtils;
import org.matsim.testcases.MatsimTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests commercial activity scoring for partially bounded first and last activities.
 */
class ActivityScoringForCommercialActivitiesTest {

	// Expects boundary activities without maximum durations to contribute no score.
	@Test
	void ignoresUndefinedMorningAndEveningBoundaryTime() {
		var scoringParameters = createScoringParameters("freight_start", "freight_end");

		var scoring = new ActivityScoringForCommercialActivities(scoringParameters, -1.0);

		scoring.handleFirstActivity(createActivity("freight_start", null, 100.0));
		scoring.handleLastActivity(createActivity("freight_end", 200.0, null));
		scoring.finish();

		assertEquals(0.0, scoring.getScore(), MatsimTestUtils.EPSILON);
	}

	// Expects explicit maximum durations to determine the scores of both boundary activities.
	@Test
	void scoresExplicitBoundaryActivityDurations() {
		var scoringParameters = createScoringParameters("freight_start", "freight_end");

		var scoring = new ActivityScoringForCommercialActivities(scoringParameters, -1.0);

		scoring.handleFirstActivity(createActivity("freight_start", null, 100.0, 60.0));
		scoring.handleLastActivity(createActivity("freight_end", 200.0, null, 60.0));
		scoring.finish();

		assertEquals(-120.0, scoring.getScore(), MatsimTestUtils.EPSILON);
	}

	// Checks that an intermediate activity uses the person-specific utility per second.
	@Test
	void scoresIntermediateActivityDuration() {
		var scoringParameters = createScoringParameters("service");
		var scoring = new ActivityScoringForCommercialActivities(scoringParameters, -1.0);

		scoring.handleActivity(createActivity("service", 120.0, 180.0));
		scoring.finish();

		assertEquals(-60.0, scoring.getScore(), MatsimTestUtils.EPSILON);
	}

	private static ScoringParameters createScoringParameters(String... activityTypes) {
		var config = ConfigUtils.createConfig();
		for (String activityType : activityTypes) {
			config.scoring().addDefaultActivityParams(new ScoringConfigGroup.ActivityParams(activityType).setTypicalDuration(3600.0));
		}
		ScoringConfigGroup.ScoringParameterSet scoringParameterSet = config.scoring().getScoringParametersOrDefault(null);
		return new ScoringParameters.Builder(config.scoring(), scoringParameterSet, config.scenario()).build();
	}

	private static Activity createActivity(String type, Double startTime, Double endTime) {
		return createActivity(type, startTime, endTime, null);
	}

	private static Activity createActivity(String type, Double startTime, Double endTime, Double maximumDuration) {
		Activity activity = PopulationUtils.createActivityFromLinkId(type, Id.create(type, Link.class));
		if (startTime != null) {
			activity.setStartTime(startTime);
		}
		if (endTime != null) {
			activity.setEndTime(endTime);
		}
		if (maximumDuration != null) {
			activity.setMaximumDuration(maximumDuration);
		}
		return activity;
	}
}
