package org.matsim.application.analysis.population;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.api.core.v01.messages.ComputeNode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scoring.ExperiencedPlansService;
import org.matsim.simwrapper.dashboard.VttsDashboard;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.matsim.application.analysis.population.VttsAttributes.*;
import static org.matsim.core.router.TripStructureUtils.StageActivityHandling.ExcludeStageActivities;

/** Evaluates the final experienced plans using services from the scenario's actual injector. */
final class VttsAnalysisListener implements IterationEndsListener, PersonStuckEventHandler {

	private final Population population;
	private final ExperiencedPlansService experiencedPlans;
	private final VttsAnalysis analysis;
	private final OutputDirectoryHierarchy output;
	private final VttsModule.Options options;
	// Events can arrive from different simulation threads; reset() limits this set to one iteration.
	private final Set<Id<Person>> stuck = ConcurrentHashMap.newKeySet();

	@Inject
	VttsAnalysisListener(Population population, ExperiencedPlansService experiencedPlans, VttsAnalysis analysis,
						 OutputDirectoryHierarchy output, VttsModule.Options options, ComputeNode computeNode) {
		if (computeNode.isDistributed())
			throw new IllegalArgumentException("VTTS currently requires single-process experienced plans; distributed collection is not implemented");
		this.population = population;
		this.experiencedPlans = experiencedPlans;
		this.analysis = analysis;
		this.output = output;
		this.options = options;
	}

	@Override
	public double priority() {
		// Core scoring/experienced-plan listeners run first. Export finishes before shutdown/SimWrapper.
		return -100;
	}

	@Override
	public void handleEvent(PersonStuckEvent event) {
		stuck.add(event.getPersonId());
	}

	@Override
	public void reset(int iteration) {
		stuck.clear();
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		// Only the final iteration supplies dashboard observations. Earlier iterations can still
		// change the person's selected plan, and must not leave obsolete analysis attributes.
		if (!event.isLastIteration()) return;
		// These plans carry actual experienced times. Selected plans supply the original person
		// and scoring attributes but are not substituted for what was actually executed.
		var plans = experiencedPlans.getExperiencedPlans();
		for (Person person : population.getPersons().values()) {
			Plan plan = plans.get(person.getId());
			if (!options.personFilter().test(person)) {
				skip(person, plan, "excluded");
			} else if (stuck.contains(person.getId())) {
				skip(person, plan, "stuck");
			} else if (plan != null && !copyActivityAttributes(person, plan)) {
				skip(person, plan, "activity_mismatch");
			} else {
				analysis.analyze(person, plan);
			}
		}
		// Export only after every person has either results or a coverage status. The writer
		// reads these attributes and does not run any scoring a second time.
		try {
			new VttsCsvWriter().write(Path.of(output.getOutputPath()).resolve(VttsDashboard.DEFAULT_DIRECTORY), population, plans);
		} catch (IOException e) {
			throw new UncheckedIOException("Could not export VTTS results", e);
		}
	}

	private static void skip(Person person, Plan plan, String status) {
		// Clear previous numeric results as well as recording the reason for exclusion, so that
		// a later export cannot accidentally include stale finite values in its distributions.
		resetSummary(person, status);
		if (plan == null) return;
		resetSummary(plan, status);
		for (var activity : TripStructureUtils.getActivities(plan, ExcludeStageActivities)) {
			for (String key : new String[]{MUSE, MONEY_UTILITY, VSE, VTTS, TRAVEL_UTILITY})
				activity.getAttributes().putAttribute(key, Double.NaN);
			activity.getAttributes().removeAttribute(MODE);
			activity.getAttributes().putAttribute(STATUS, status);
		}
	}

	/** Experienced callbacks may omit attributes needed by a scenario's activity scorer. */
	private static boolean copyActivityAttributes(Person person, Plan experienced) {
		var planned = TripStructureUtils.getActivities(person.getSelectedPlan(), ExcludeStageActivities);
		var actual = TripStructureUtils.getActivities(experienced, ExcludeStageActivities);
		// Match by position only after checking the full sequence of main-activity types.
		// A diverging sequence cannot safely inherit attributes from the selected plan.
		if (planned.size() != actual.size()) return false;
		for (int i = 0; i < planned.size(); i++)
			if (!planned.get(i).getType().equals(actual.get(i).getType())) return false;
		// Fill only missing attributes: experienced times and any existing experienced
		// attributes stay authoritative. Stage interactions do not take part in this matching.
		for (int i = 0; i < planned.size(); i++) {
			var attributes = actual.get(i).getAttributes();
			planned.get(i).getAttributes().getAsMap().forEach((key, value) -> {
				if (attributes.getAttribute(key) == null) attributes.putAttribute(key, value);
			});
		}
		return true;
	}
}
