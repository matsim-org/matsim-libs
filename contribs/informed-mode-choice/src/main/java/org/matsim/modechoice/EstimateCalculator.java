package org.matsim.modechoice;

import com.google.inject.Inject;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import it.unimi.dsi.fastutil.objects.ReferenceSet;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.core.utils.timing.TimeTracker;
import org.matsim.modechoice.constraints.TripConstraint;
import org.matsim.modechoice.estimators.*;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Predicate;

/**
 * Provides methods for computing estimates.
 */
public final class EstimateCalculator {

	@Inject
	private QSimConfigGroup qsim;

	@Inject
	private EstimateRouter router;

	@Inject
	private Map<String, LegEstimator> legEstimators;

	@Inject
	private Map<String, TripEstimator> tripEstimator;

	@Inject
	private Set<TripConstraint<?>> constraints;

	@Inject
	private Set<TripScoreEstimator> tripScores;

	@Inject
	private ActivityEstimator actEstimator;

	@Inject
	private TimeInterpretation timeInterpretation;

	@Inject
	private Map<String, FixedCostsEstimator> fixedCosts;

	@Inject
	private PlanModelService service;

	/**
	 * Route and prepare estimates for all trips, expect those that are not considered.
	 */
	public void prepareEstimates(PlanModel planModel, EstimatorContext context, @Nullable Set<String> consideredModes, @Nullable boolean[] mask) {

		if (planModel.getEstimates().isEmpty())
			service.initEstimates(planModel);

		router.routeModes(planModel, consideredModes == null ? planModel.filterModes(ModeEstimate::isUsable) : consideredModes, (mode, tripIdx) -> mask == null || mask[tripIdx]);
		calculateEstimates(context, planModel);
	}

	/**
	 * Prepare estimates only for the list of mode combinations.
	 */
	public void prepareEstimates(PlanModel planModel, EstimatorContext context, List<String[]> modes) {

		if (planModel.getEstimates().isEmpty())
			service.initEstimates(planModel);

		Set<String> all = new HashSet<>();
		Set<String>[] perTrip = new Set[planModel.trips()];

		for (String[] ms : modes) {
			for (int i = 0; i < ms.length; i++) {
				all.add(ms[i]);
				perTrip[i] = perTrip[i] == null ? new HashSet<>() : perTrip[i];
				perTrip[i].add(ms[i]);
			}
		}

		router.routeModes(planModel, all, (mode, tripIdx) -> perTrip[tripIdx].contains(mode));
		calculateEstimates(context, planModel);
	}

	/**
	 * Prepare estimates for a single trip.
	 */
	public void prepareEstimates(PlanModel planModel, EstimatorContext context, Predicate<ModeEstimate> useModes, int tripId) {

		if (planModel.getEstimates().isEmpty())
			service.initEstimates(planModel);


		router.routeModes(planModel, planModel.filterModes(useModes), (mode, tripIdx) -> tripIdx == tripId);
		calculateEstimates(context, planModel);
	}

	/**
	 * Computes the estimate of a whole plan. Note that this does not check for constraints.
	 */
	public double calculatePlanEstimate(EstimatorContext context, PlanModel planModel, String[] modes) {

		if (planModel.getEstimates().isEmpty())
			throw new IllegalArgumentException("Plan model contains no estimates. Use .prepareEstimates() first.");

		ReferenceSet<String> usedModes = new ReferenceOpenHashSet<>();

		Map<String, ModeEstimate> singleOptions = new HashMap<>();
		// reduce to single options that are usable
		for (Map.Entry<String, List<ModeEstimate>> e : planModel.getEstimates().entrySet()) {
			for (ModeEstimate o : e.getValue()) {
				// check if a mode can be used at all
				if (!o.isUsable() || o.isMin())
					continue;

				singleOptions.put(e.getKey(), o);
				break;
			}
		}

		TimeTracker tt = new TimeTracker(timeInterpretation);
		tt.setTime(planModel.getStartTimes()[0]);

		double estimate = 0;
		for (int i = 0; i < modes.length; i++) {

			String mode = modes[i];

			// If no mode is selected, estimates are skipped
			if (mode == null) {
				if (i < modes.length - 1)
					tt.setTime(planModel.getStartTimes()[i + 1]);

				continue;
			}

			List<Leg> legs = planModel.getLegs(mode, i);

			if (tt.getTime().isUndefined())
				tt.setTime(planModel.getStartTimes()[i]);

			if (legs == null)
				return Double.NEGATIVE_INFINITY;

			for (Leg leg : legs) {
				tt.addLeg(leg);
			}

			ModeEstimate opt = singleOptions.get(mode);

			// Some illegal option without estimates
			if (opt == null) {
				return Double.NEGATIVE_INFINITY;
			}

			// This estimated is unrouted and skipped
			if (opt.getLegEstimates()[i] == Double.NEGATIVE_INFINITY) {
				if (i < modes.length - 1)
					tt.setTime(planModel.getStartTimes()[i + 1]);

				continue;
			}

			estimate += opt.getLegEstimates()[i];

			TripEstimator t = tripEstimator.get(mode);

			// This trip estimator can be used directly
			if (t != null && !t.providesMinEstimate(context, mode, opt.getOption()))
				estimate += opt.getTripEstimates()[i];

			// Store modes that have been used
			if (!opt.getNoRealUsage()[i])
				usedModes.add(mode);

			TripStructureUtils.Trip trip = planModel.getTrip(i);

			for (TripScoreEstimator tripScore : tripScores) {
				estimate += tripScore.estimate(context, mode, trip);
			}

			// Try to estimate aborted plans
			if (qsim.getEndTime().isDefined() && tt.getTime().seconds() > qsim.getEndTime().seconds()) {
				estimate += context.scoring.abortedPlanScore;

				// Estimate the first activity, because the overnight estimation will not be performed
				estimate += actEstimator.estimate(context, 0, planModel.getTrip(0).getOriginActivity());
				break;
			}

			// Estimate overnight scoring if applicable
			if (modes.length > 1 && i == modes.length - 1) {
				estimate += actEstimator.estimateLastAndFirstOfDay(context, tt.getTime().seconds(), trip.getDestinationActivity(), planModel.getTrip(0).getOriginActivity());
			} else
				estimate += actEstimator.estimate(context, tt.getTime().seconds(), trip.getDestinationActivity());

			tt.addActivity(trip.getDestinationActivity());
		}

		// Add the fixed costs estimate if a mode has been used
		for (ModeEstimate mode : singleOptions.values()) {
			FixedCostsEstimator f = fixedCosts.get(mode.getMode());
			TripEstimator t = tripEstimator.get(mode.getMode());

			if (usedModes.contains(mode.getMode())) {

				if (f != null)
					estimate += f.usageUtility(context, mode.getMode(), mode.getOption());

				// This estimator is used on the whole trip
				if (t != null && t.providesMinEstimate(context, mode.getMode(), mode.getOption()))
					estimate += t.estimatePlan(context, mode.getMode(), modes, planModel, mode.getOption());

			}

			if (f != null)
				estimate += f.fixedUtility(context, mode.getMode(), mode.getOption());
		}

		return estimate;
	}

	@SuppressWarnings("unchecked")
	public List<PlanModelService.ConstraintHolder<?>> buildConstraints(EstimatorContext context, PlanModel planModel) {

		List<PlanModelService.ConstraintHolder<?>> constraints = new ArrayList<>();
		for (TripConstraint<?> c : this.constraints) {
			constraints.add(new PlanModelService.ConstraintHolder<>(
				(TripConstraint<Object>) c,
				c.getContext(context, planModel)
			));
		}

		return constraints;
	}

	/**
	 * Calculate the estimates for all options. Note that plan model has to be routed before computing estimates.
	 */
	@SuppressWarnings("rawtypes")
	private void calculateEstimates(EstimatorContext context, PlanModel planModel) {

		for (Map.Entry<String, List<ModeEstimate>> e : planModel.getEstimates().entrySet()) {

			for (ModeEstimate c : e.getValue()) {

				if (!c.isUsable())
					continue;

				// All estimates are stored within the objects and modified directly here
				double[] legEstimates = c.getLegEstimates();
				double[] tValues = c.getTripEstimates();
				double[] actEst = c.getActEst();
				boolean[] noUsage = c.getNoRealUsage();

				// Collect all estimates
				for (int i = 0; i < planModel.trips(); i++) {

					List<Leg> legs = planModel.getLegs(c.getMode(), i);

					// This mode could not be applied
					if (legs == null || legs == EstimateRouter.UN_ROUTED) {
						legEstimates[i] = Double.NEGATIVE_INFINITY;
						continue;
					}

					TripEstimator tripEst = tripEstimator.get(c.getMode());

					// some options may produce equivalent results, but are re-estimated
					// however, the more expensive computation is routing and only done once
					boolean realUsage = planModel.hasModeForTrip(c.getMode(), i);
					noUsage[i] = !realUsage;

					double estimate = 0;
					if (tripEst != null && realUsage) {
						MinMaxEstimate minMax = tripEst.estimate(context, c.getMode(), planModel, legs, c.getOption());
						double tripEstimate = c.isMin() ? minMax.getMin() : minMax.getMax();

						// Only store if required
						if (tValues != null)
							tValues[i] = tripEstimate;
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

					double arrivalTime = planModel.getStartTimes()[i] + tt;

					// early or late arrival can also have an effect on the activity scores which is potentially considered here
					actEst[i] = actEstimator.estimate(context, arrivalTime, trip.getDestinationActivity());

					for (TripScoreEstimator tripScore : tripScores) {
						estimate += tripScore.estimate(context, c.getMode(), trip);
					}

					legEstimates[i] = estimate;
				}
			}
		}
	}
}
