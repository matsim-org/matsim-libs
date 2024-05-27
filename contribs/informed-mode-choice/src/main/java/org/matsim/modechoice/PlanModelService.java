package org.matsim.modechoice;

import com.google.common.collect.Iterables;
import com.google.inject.Inject;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.ControlerListenerManager;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.modechoice.constraints.TripConstraint;
import org.matsim.modechoice.estimators.ActivityEstimator;
import org.matsim.modechoice.estimators.LegEstimator;
import org.matsim.modechoice.estimators.MinMaxEstimate;
import org.matsim.modechoice.estimators.TripEstimator;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * A service for working with {@link PlanModel} and creating estimates.
 */
@SuppressWarnings("unchecked")
public final class PlanModelService implements StartupListener {

	@Inject
	private Map<String, LegEstimator> legEstimators;

	@Inject
	private Map<String, TripEstimator> tripEstimator;

	@Inject
	private Set<TripConstraint<?>> constraints;

	@Inject
	private ActivityEstimator actEstimator;

	@Inject
	private TimeInterpretation timeInterpretation;

	@Inject
	private EventsManager eventsManager;

	@Inject
	private ControlerListenerManager controlerListenerManager;

	private final InformedModeChoiceConfigGroup config;
	private final Map<String, ModeOptions> options;

	@Inject
	private PlanModelService(InformedModeChoiceConfigGroup config, Map<String, ModeOptions> options) {
		this.config = config;
		this.options = options;

		for (String mode : config.getModes()) {

			if (!options.containsKey(mode))
				throw new IllegalArgumentException(String.format("No estimators configured for mode %s", mode));
		}
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

	/**
	 * Initialized {@link ModeEstimate} for all available options in the {@link PlanModel}. No routing or estimation is performed yet.
	 */
	public void initEstimates(EstimatorContext context, PlanModel planModel) {

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

					c.add(new ModeEstimate(mode, modeOption, planModel.trips(), usable, false, false));

				}
			}

			planModel.putEstimate(mode, c);
		}
	}

	/**
	 * Return the modes an estimator was registered for.
	 */
	public List<String> modesForEstimator(LegEstimator est) {
		return legEstimators.entrySet().stream().filter(e -> e.getValue().equals(est))
				.map(Map.Entry::getKey)
				.distinct()
				.collect(Collectors.toList());
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
	 * Calculate the estimates for all options. Note that plan model has to be routed before computing estimates.
	 */
	@SuppressWarnings("rawtypes")
	public void calculateEstimates(EstimatorContext context, PlanModel planModel) {

		for (Map.Entry<String, List<ModeEstimate>> e : planModel.getEstimates().entrySet()) {

			for (ModeEstimate c : e.getValue()) {

				if (!c.isUsable())
					continue;

				double[] values = c.getEstimates();
				double[] tValues = c.getTripEstimates();

				// Collect all estimates
				for (int i = 0; i < planModel.trips(); i++) {

					List<Leg> legs = planModel.getLegs(c.getMode(), i);

					// This mode could not be applied
					if (legs == null) {
						values[i] = Double.NEGATIVE_INFINITY;
						continue;
					}

					TripEstimator tripEst =  tripEstimator.get(c.getMode());

					// some options may produce equivalent results, but are re-estimated
					// however, the more expensive computation is routing and only done once

					double estimate = 0;
					if (tripEst != null) {
						MinMaxEstimate minMax = tripEst.estimate(context, c.getMode(), planModel, legs, c.getOption());
						double tripEstimate = c.isMin() ? minMax.getMin() : minMax.getMax();

						// Only store if required
						if (tValues != null)
							tValues[i] = tripEstimate;

						estimate += tripEstimate;
					}

					double tt = 0;

					for (Leg leg : legs) {
						String legMode = leg.getMode();

						tt += timeInterpretation.decideOnLegTravelTime(leg).orElse(0);

						// Already scored with the trip estimator
						if (tripEst != null && legMode.equals(c.getMode()))
							continue;

						LegEstimator legEst = legEstimators.get(legMode);

						if (legEst == null)
							throw new IllegalStateException("No leg estimator defined for mode: " + legMode);

						// TODO: add delay estimate? (e.g waiting time for drt, currently not respected)
						estimate += legEst.estimate(context, legMode, leg, c.getOption());
					}

					TripStructureUtils.Trip trip = planModel.getTrip(i);

					// early or late arrival can also have an effect on the activity scores which is potentially considered here
					estimate += actEstimator.estimate(context, planModel.getStartTimes()[i] + tt, trip.getDestinationActivity());

					values[i] = estimate;
				}
			}
		}
	}

	/**
	 * Return whether any constraints are registered.
	 */
	public boolean hasConstraints() {
		return !constraints.isEmpty();
	}

	/**
	 * Check whether all registered constraints are met.
	 * @param modes array of used modes for each trip of the day
	 */
	public boolean isValidOption(PlanModel model, String[] modes) {

		if (constraints.isEmpty())
			return true;

		// Scoring is null here as it should not be needed
		EstimatorContext context = new EstimatorContext(model.getPerson(), null);

		for (TripConstraint<?> c : this.constraints) {
			ConstraintHolder<?> h = new ConstraintHolder<>(
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
