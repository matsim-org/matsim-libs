package org.matsim.application.analysis.population;

import com.google.inject.Inject;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.router.AnalysisMainModeIdentifier;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.utils.objectattributes.attributable.Attributable;

import static org.matsim.application.analysis.population.VttsAttributes.*;
import static org.matsim.core.router.TripStructureUtils.StageActivityHandling.ExcludeStageActivities;

/** Single-run activity values and person summaries, separated from file export. */
public final class VttsAnalysis {

	private final MuseComputation muse;
	private final VttsParameters parameters;
	private final AnalysisMainModeIdentifier modeIdentifier;

	@Inject
	public VttsAnalysis(MuseComputation muse, VttsParameters parameters, AnalysisMainModeIdentifier modeIdentifier) {
		this.muse = muse;
		this.parameters = parameters;
		this.modeIdentifier = modeIdentifier;
	}

	/**
	 * Computes MUSE, VSE and VTTS for the supplied experienced plan and stores means on its original
	 * person. VTTS assumes one second less traveling in the trip's realized analysis main mode and
	 * a destination start one second earlier, with other components held fixed. PT values concern
	 * in-vehicle travel, not waiting/access time. The conversion assumes linear travel and money terms.
	 */
	public void analyze(Person person, Plan plan) {
		// Reset first so repeated analyses cannot leave a previous plan's values on the person.
		// Missing results use NaN; zero remains a valid economic result.
		resetSummary(person, "missing_plan");
		if (plan == null) return;
		resetSummary(plan, "not_mobile");
		// Remove only our previous results. Other activity attributes remain inputs to scoring.
		for (Activity activity : TripStructureUtils.getActivities(plan, ExcludeStageActivities)) {
			for (String key : new String[]{MUSE, VSE, VTTS, MONEY_UTILITY, TRAVEL_UTILITY, MODE, STATUS})
				activity.getAttributes().removeAttribute(key);
		}
		var trips = TripStructureUtils.getTrips(plan);
		if (trips.isEmpty()) {
			person.getAttributes().putAttribute(STATUS, "not_mobile");
			return;
		}
		// Experienced plans may be truncated. Report those explicitly instead of handing
		// malformed timing sequences to the unchanged KN calculation.
		if (!isComplete(plan)) {
			person.getAttributes().putAttribute(STATUS, "incomplete_plan");
			plan.getAttributes().putAttribute(STATUS, "incomplete_plan");
			return;
		}

		// Pass the original person even when the experienced plan has no person backpointer.
		muse.computeMuseForAllActs(person, plan);
		// Money utility is score/money unit. It must be positive and finite for this conversion;
		// its actual value comes from the run's person parameters, not a new income calculation.
		double moneyUtility = parameters.marginalUtilityOfMoney(person);
		boolean validMoney = Double.isFinite(moneyUtility) && moneyUtility > 0;
		double sumMuse = 0;
		double sumVtts = 0;
		// Keep separate denominators: MUSE/VSE can be available when VTTS lacks a mode coefficient.
		int count = 0;
		int vttsCount = 0;
		for (var trip : trips) {
			Activity destination = trip.getDestinationActivity();
			Double value = getMuse(destination);
			destination.getAttributes().putAttribute(VTTS, Double.NaN);
			destination.getAttributes().putAttribute(VSE, Double.NaN);
			destination.getAttributes().putAttribute(TRAVEL_UTILITY, Double.NaN);
			destination.getAttributes().putAttribute(MONEY_UTILITY, moneyUtility);
			if (value == null || !Double.isFinite(value)) {
				destination.getAttributes().putAttribute(STATUS, "non_finite_muse");
				continue;
			}
			// Retain finite zero and negative values; neither is an automatic coverage failure.
			sumMuse += value;
			count++;
			String mode = savingMode(trip);
			destination.getAttributes().putAttribute(MODE, mode == null ? "" : mode);
			if (!validMoney) {
				destination.getAttributes().putAttribute(STATUS, "invalid_money_utility");
				continue;
			}
			// VSE monetizes the activity effect alone. A missing travel coefficient must not
			// discard this valid result, so compute it before checking the mode parameters.
			destination.getAttributes().putAttribute(VSE, value / moneyUtility);
			double travelUtility = mode == null ? Double.NaN : parameters.marginalUtilityOfTraveling(person, mode);
			if (!Double.isFinite(travelUtility)) {
				destination.getAttributes().putAttribute(STATUS, "unsupported_mode");
				continue;
			}
			// Saving travel time removes its utility contribution. With beta_travel = -6,
			// subtracting it adds 6 score/hour to MUSE before converting into money/hour.
			double vtts = (value - travelUtility) / moneyUtility;
			if (!Double.isFinite(vtts)) {
				destination.getAttributes().putAttribute(STATUS, "non_finite_vtts");
				continue;
			}
			destination.getAttributes().putAttribute(TRAVEL_UTILITY, travelUtility);
			destination.getAttributes().putAttribute(VTTS, vtts);
			destination.getAttributes().putAttribute(STATUS, "ok");
			sumVtts += vtts;
			vttsCount++;
		}
		String status = count == 0 ? "non_finite_muse" : !validMoney ? "invalid_money_utility"
			: vttsCount == trips.size() ? "ok" : "partial_vtts";
		// Each valid destination has equal weight within this person's mean. Store that same
		// summary on the experienced plan and original person for XML and person-level CSV export.
		for (Attributable object : new Attributable[]{person, plan}) {
			object.getAttributes().putAttribute(MUSE, count == 0 ? Double.NaN : sumMuse / count);
			object.getAttributes().putAttribute(MONEY_UTILITY, moneyUtility);
			object.getAttributes().putAttribute(VSE, count == 0 || !validMoney ? Double.NaN : sumMuse / count / moneyUtility);
			object.getAttributes().putAttribute(VTTS, vttsCount == 0 ? Double.NaN : sumVtts / vttsCount);
			object.getAttributes().putAttribute(DESTINATIONS, count);
			object.getAttributes().putAttribute(VTTS_DESTINATIONS, vttsCount);
			object.getAttributes().putAttribute(STATUS, status);
		}
	}

	/** Validate dashboard inputs without changing the original KN calculation's input handling. */
	private static boolean isComplete(Plan plan) {
		var activities = TripStructureUtils.getActivities(plan, ExcludeStageActivities);
		if (activities.size() < 2) return false;
		for (int i = 0; i < activities.size(); i++) {
			Activity activity = activities.get(i);
			// Undefined morning start and evening end are expected MATSim boundary markers.
			// All other starts/ends must be defined, and defined times must be finite.
			if (i == 0 ? activity.getStartTime().isDefined() : activity.getStartTime().isUndefined()) return false;
			if (i == activities.size() - 1 ? activity.getEndTime().isDefined() : activity.getEndTime().isUndefined()) return false;
			if (activity.getStartTime().isDefined() && !Double.isFinite(activity.getStartTime().seconds())) return false;
			if (activity.getEndTime().isDefined() && !Double.isFinite(activity.getEndTime().seconds())) return false;
		}
		return true;
	}

	private String savingMode(TripStructureUtils.Trip trip) {
		if (trip.getLegsOnly().isEmpty()) return null;
		// A requested PT route can, for example, be realized as walking when no service is available.
		// Use the run's analysis identifier to classify the actual trip rather than its routing choice.
		return modeIdentifier.identifyMainMode(trip.getTripElements());
	}
}
