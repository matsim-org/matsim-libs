package org.matsim.core.scoring.functions;

import com.google.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripStructureUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * With the typical durations taken from the person attribute ({@link PersonAttributeTypicalDurationCalculator}),
 * reports at startup how many persons of each subpopulation have one, and checks the attribute against all plans of
 * the person, so that an attribute that does not fit aborts the run before the first iteration rather than in its
 * scoring.
 */
final class PersonTypicalDurationsStartupCheck implements StartupListener {

	private static final Logger log = LogManager.getLogger(PersonTypicalDurationsStartupCheck.class);

	@Inject
	private TypicalDurationCalculator typicalDurationCalculator;

	@Override
	public void notifyStartup(StartupEvent event) {
		if (!(typicalDurationCalculator instanceof PersonAttributeTypicalDurationCalculator calculator)) {
			// replaced by a binding
			return;
		}

		// subpopulation -> {persons with the attribute, persons}
		Map<String, int[]> counts = new TreeMap<>(Comparator.nullsFirst(Comparator.naturalOrder()));
		for (Person person : event.getServices().getScenario().getPopulation().getPersons().values()) {
			int[] count = counts.computeIfAbsent(PopulationUtils.getSubpopulation(person), subpopulation -> new int[2]);
			count[1]++;
			if (PersonUtils.getTypicalDurations(person) == null) {
				continue;
			}
			count[0]++;
			for (Plan plan : person.getPlans()) {
				List<Activity> activities = TripStructureUtils.getActivities(plan, TripStructureUtils.StageActivityHandling.ExcludeStageActivities);
				for (int i = 0; i < activities.size(); i++) {
					calculator.getTypicalDuration(person, activities.get(i), i);
				}
			}
		}

		counts.forEach((subpopulation, count) -> log.info("Subpopulation {}: {} of {} persons have their own typical durations ({} attribute).",
			subpopulation, count[0], count[1], PersonUtils.TYPICAL_DURATIONS));
		if (counts.values().stream().allMatch(count -> count[0] == 0)) {
			log.warn("The typical durations are taken from the {} person attribute, but no person has one; all activities are scored "
				+ "against the typical durations of their types.", PersonUtils.TYPICAL_DURATIONS);
		}
	}

}
