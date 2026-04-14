package org.matsim.contrib.vsp.scenario;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class Activities{
	private static final Logger log = LogManager.getLogger( Activities.class );

	private Activities(){}

	/**
	 * Disable wrap-around scoring of first and last act of the day by setting them to different subtypes "_morning" and "_evening".
	 */
	public static void changeWrapAroundActsIntoMorningAndEveningActs( Scenario scenario ) {
		Set<String> firstActTypes = new HashSet<>();
		Set<String> lastActTypes = new HashSet<>();

		for ( Person p : scenario.getPopulation().getPersons().values()) {
//			ignore freight / commercial traffic agents and stay home agents
			if (!p.getAttributes().getAttribute("subpopulation").equals("person") ||
			p.getSelectedPlan().getPlanElements().size() == 1) {
				continue;
			}

			for ( Plan plan : p.getPlans()) {
				Activity first = (Activity) plan.getPlanElements().getFirst();
				Activity last = (Activity) plan.getPlanElements().getLast();

				String[] splitFirst = first.getType().split("_");
				String typeFirst = String.join("_", Arrays.copyOfRange(splitFirst, 0, splitFirst.length - 1 ) );
				int orginalTimeBinFirst = Integer.parseInt(splitFirst[splitFirst.length - 1]);
				firstActTypes.add(typeFirst);

				String[] splitLast = last.getType().split("_");
				String typeLast = String.join("_", Arrays.copyOfRange(splitLast, 0, splitLast.length - 1));
				int orginalTimeBinLast = Integer.parseInt(splitLast[splitLast.length - 1]);
				lastActTypes.add(typeLast);

				if (!typeFirst.equals(typeLast)) {
//					if first and last act do not have the same type, we will not change anything.
//					this is the pragmatic version. There are last acts with without startTime, endTime or maxDuration.
//					this needs to be repaired upstream (in the makefile process). -sm0226
					continue;
				}

				Double durationFirst = null;
				if (first.getEndTime().isDefined()) {
//					use act end time if defined
					durationFirst = first.getEndTime().seconds();
				}

				if (durationFirst == null && first.getMaximumDuration().isDefined()) {
					durationFirst = first.getMaximumDuration().seconds();
				}

				if (durationFirst == null) {
					log.fatal("Neither duration nor end time is defined for activity {} of agent {}. This should not happen, aborting!", first, p.getId() );
					throw new IllegalStateException("");
				}

				int durationBinFirst = getDurationBin(durationFirst);

				first.setType(String.format("%s_%d", SnzActivities.createMorningActivityType(typeFirst ), durationBinFirst ) );

	//			act types of first and last act the same
				if (orginalTimeBinFirst != orginalTimeBinLast) {
					log.fatal("typical duration of first and last activity of person {} with the same act type {} are not the same. This should not happen, aborting!", p.getId(), typeLast );
					throw new IllegalStateException("");
				}
				double durationLast = orginalTimeBinLast - durationFirst;

				last.setType(String.format("%s_%d", SnzActivities.createEveningActivityType(typeLast ), getDurationBin(durationLast ) ) );
				last.setMaximumDuration(durationLast);
				last.setEndTimeUndefined();
				last.setStartTimeUndefined();
			}
		}
		log.info("Activity types of first activity in plans: {}", firstActTypes );
		log.info("Activity types of last activity in plans: {}", lastActTypes );
	}
	private static int getDurationBin( Double duration ) {
		final int maxCategories = 86400 / 600;

		int durationCategoryNr = (int) Math.round(duration / 600);

		if (durationCategoryNr <= 0) {
			durationCategoryNr = 1;
		}

		if (durationCategoryNr >= maxCategories) {
			durationCategoryNr = maxCategories;
		}
		return durationCategoryNr * 600;
	}
}
