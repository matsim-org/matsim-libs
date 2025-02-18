package org.matsim.modechoice;

import com.google.common.collect.Iterables;
import com.google.inject.Inject;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.ControlerListenerManager;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.modechoice.constraints.TripConstraint;
import org.matsim.modechoice.estimators.LegEstimator;
import org.matsim.modechoice.estimators.TripEstimator;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * A service for working with {@link PlanModel} and creating estimates.
 */
@SuppressWarnings("unchecked")
public final class PlanModelService implements StartupListener, IterationEndsListener, ShutdownListener {

	private final InformedModeChoiceConfigGroup config;
	private final Map<String, ModeOptions> options;
	/**
	 * A memory cache for plan models. This is used to avoid re-routing and re-estimation of the same plans.
	 */
	private final Map<Id<Person>, PlanModel> memory = new ConcurrentHashMap<>();
	private final Map<Id<Person>, DoubleList> diffs = new ConcurrentHashMap<>();

	/**
	 * Write estimate statistics.
	 */
	private final BufferedWriter out;

	@Inject
	private EventsManager eventsManager;

	@Inject
	private ControlerListenerManager controlerListenerManager;

	@Inject
	private ScoringParametersForPerson params;

	@Inject
	private Map<String, TripEstimator> tripEstimator;

	@Inject
	private Map<String, LegEstimator> legEstimators;

	@Inject
	private Set<TripConstraint<?>> constraints;

	@Inject
	private PlanModelService(InformedModeChoiceConfigGroup config, OutputDirectoryHierarchy io,
							 Map<String, ModeOptions> options) {
		this.config = config;
		this.options = options;

		for (String mode : config.getModes()) {
			if (!options.containsKey(mode))
				throw new IllegalArgumentException(String.format("No estimators configured for mode %s", mode));
		}

		this.out = IOUtils.getBufferedWriter(io.getOutputFilename("score_estimates_stats.csv"));
		try {
			this.out.write("iteration,mean_estimate_mae,median_estimate_mae,p95_estimate_mae,p99_estimate_mae,mean_corrected_estimate_mae,p95_corrected_estimate_mae,p99_corrected_estimate_mae,mean_estimate_bias\n");
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Override
	public double priority() {
		// Run after scoring listeners
		return -10;
	}

	@Override
	public void notifyStartup(StartupEvent event) {

		// Estimators are registered as event handlers if needed
		Set<Object> registered = new HashSet<>();
		for (Object v : Iterables.concat(legEstimators.values(), tripEstimator.values())) {
			if (v instanceof EventHandler ev && !registered.contains(v)) {
				eventsManager.addHandler(ev);
				registered.add(v);
			}
		}
		registered.clear();

		// also register as controler listener
		for (Object v : Iterables.concat(legEstimators.values(), tripEstimator.values())) {
			if (v instanceof ControlerListener ev && !registered.contains(v)) {
				controlerListenerManager.addControlerListener(ev);
				registered.add(v);
			}
		}
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {

		if (config.getProbaEstimate() >= 1) {
			memory.clear();
		} else if (config.getProbaEstimate() > 0) {

			Random rnd = MatsimRandom.getLocalInstance();

			// Remove plans from memory with a certain probability
			memory.keySet().removeIf(id -> rnd.nextDouble() < config.getProbaEstimate());
		}

		Population population = event.getServices().getScenario().getPopulation();

		SummaryStatistics bias = new SummaryStatistics();
		DescriptiveStatistics mae = new DescriptiveStatistics();

		// The corrected mae subtracts the systematically bias from the estimate
		DescriptiveStatistics correctedMae = new DescriptiveStatistics();

		for (Person person : population.getPersons().values()) {

			Plan executedPlan = person.getSelectedPlan();

			// Remove the estimate so it is only used once
			Object estimate = executedPlan.getAttributes().removeAttribute(PlanCandidate.ESTIMATE_ATTR);
			Double score = executedPlan.getScore();

			DoubleList diffs = this.diffs.computeIfAbsent(person.getId(), k -> new DoubleArrayList());

			if (estimate instanceof Double est && score != null) {
				double diff = est - score;
				bias.addValue(diff);
				mae.addValue(Math.abs(diff));

				diffs.add(diff);

				if (diffs.size() > event.getServices().getConfig().replanning().getMaxAgentPlanMemorySize()) {
					diffs.removeDouble(0);
				}
			}

			if (diffs.size() >= event.getServices().getConfig().replanning().getMaxAgentPlanMemorySize()) {

				double minDiff = Double.POSITIVE_INFINITY;
				for (double d : diffs) {
					if (d < minDiff)
						minDiff = d;
				}

				double sumDiff = 0;
				for (double d : diffs) {
					sumDiff += Math.abs(d - minDiff);
				}

				correctedMae.addValue(sumDiff / diffs.size());
			}
		}

		this.writeStats(event.getIteration(), mae, correctedMae, bias);
	}

	private void writeStats(int iteration, DescriptiveStatistics mae, DescriptiveStatistics correctedMae, SummaryStatistics bias) {
		try {
			out.write(String.format(Locale.ENGLISH, "%d,%f,%f,%f,%f,%f,%f,%f,%f\n",
				iteration,
				mae.getMean(),
				mae.getPercentile(50),
				mae.getPercentile(95),
				mae.getPercentile(99),
				correctedMae.getMean(),
				correctedMae.getPercentile(95),
				correctedMae.getPercentile(99),
				bias.getMean()
			));
			out.flush();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Override
	public void notifyShutdown(ShutdownEvent event) {
		try {
			out.close();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * Get a plan model from memory for a person. If not present, a new one is created.
	 */
	public PlanModel getPlanModel(Plan plan) {

		Id<Person> id = plan.getPerson().getId();
		PlanModel model = memory.get(id);
		if (model == null) {
			model = PlanModel.newInstance(plan);
			initEstimates(model);
			memory.put(id, model);
		} else
			model.setPlan(plan);

		return model;
	}

	/**
	 * Initialized {@link ModeEstimate} for all available options in the {@link PlanModel}. No routing or estimation is performed yet.
	 */
	void initEstimates(PlanModel planModel) {

		EstimatorContext context = new EstimatorContext(planModel.getPerson(), params.getScoringParameters(planModel.getPerson()));

		for (String mode : config.getModes()) {

			ModeOptions t = this.options.get(mode);

			List<ModeAvailability> modeOptions = t.get(planModel.getPerson());

			List<ModeEstimate> c = new ArrayList<>();

			for (ModeAvailability modeOption : modeOptions) {
				TripEstimator te = tripEstimator.get(mode);

				boolean usable = t.allowUsage(modeOption);

				// Check if min estimate is needed
				if (te != null && te.providesMinEstimate(context, mode, modeOption)) {

					c.add(new ModeEstimate(mode, modeOption, planModel.trips(), usable, true, false));
					c.add(new ModeEstimate(mode, modeOption, planModel.trips(), usable, true, true));

				} else {

					c.add(new ModeEstimate(mode, modeOption, planModel.trips(), usable, te != null, false));

				}
			}

			planModel.putEstimate(mode, c);
		}
	}

	/**
	 * Allowed modes of a plan.
	 */
	public List<String> allowedModes(PlanModel planModel) {

		List<String> modes = new ArrayList<>();

		for (String mode : config.getModes()) {

			ModeOptions t = this.options.get(mode);
			List<ModeAvailability> modeOptions = t.get(planModel.getPerson());

			for (ModeAvailability modeOption : modeOptions) {
				boolean usable = t.allowUsage(modeOption);

				if (usable) {
					modes.add(mode);
					break;
				}
			}
		}
		return modes;
	}

	/**
	 * Return whether any constraints are registered.
	 */
	public boolean hasConstraints() {
		return !constraints.isEmpty();
	}

	/**
	 * Check whether all registered constraints are met.
	 *
	 * @param modes array of used modes for each trip of the day
	 */
	public boolean isValidOption(PlanModel model, String[] modes) {

		if (constraints.isEmpty())
			return true;

		// Scoring is null here as it should not be needed
		EstimatorContext context = new EstimatorContext(model.getPerson(), null);

		for (TripConstraint<?> c : this.constraints) {
			PlanModelService.ConstraintHolder<?> h = new PlanModelService.ConstraintHolder<>(
				(TripConstraint<Object>) c,
				c.getContext(context, model)
			);

			if (!h.test(modes))
				return false;

		}

		return true;
	}

	/**
	 * Return the trip estimator for one specific mode.
	 */
	public TripEstimator getTripEstimator(String mode) {
		return tripEstimator.get(mode);
	}

	public record ConstraintHolder<T>(TripConstraint<T> constraint, T context) implements Predicate<String[]> {

		@Override
		public boolean test(String[] modes) {
			return constraint.isValid(context, modes);
		}

		public boolean testMode(String[] currentModes, ModeEstimate mode, boolean[] mask) {
			return constraint.isValidMode(context, currentModes, mode, mask);
		}

	}

}
