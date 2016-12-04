package cba.toynet;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.Provider;

import cba.resampling.LogitEpsilonDistribution;
import floetteroed.utilities.math.MultinomialLogit;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class ChoiceModel {

	// -------------------- MEMBERS --------------------

	private final int sampleCnt;

	private final Random rnd;

	private final Scenario scenario;

	private final Provider<TripRouter> tripRouterProvider;

	private final Map<String, TravelTime> mode2travelTime;

	private final int maxTrials;

	private final int maxFailures;

	private final List<TourSequence> tourSeqAlts = new ArrayList<>(TourSequence.Type.values().length);

	// -------------------- CONSTRUCTION --------------------

	ChoiceModel(final int sampleCnt, final Random rnd, final Scenario scenario,
			final Provider<TripRouter> tripRouterProvider, final Map<String, TravelTime> mode2travelTime,
			final int maxTrials, final int maxFailures) {
		this.sampleCnt = sampleCnt;
		this.rnd = rnd;
		this.scenario = scenario;
		this.tripRouterProvider = tripRouterProvider;
		this.mode2travelTime = mode2travelTime;
		this.maxTrials = maxTrials;
		this.maxFailures = maxFailures;

		for (TourSequence.Type type : TourSequence.Type.values()) {
			final TourSequence tourSeq = new TourSequence(type);
			this.tourSeqAlts.add(tourSeq);
		}
	}

	// -------------------- IMPLEMENTATION --------------------

	Set<PlanForResampling> choosePlans(final Person person) {

		final List<Plan> planAlts = new ArrayList<>(this.tourSeqAlts.size());
		for (TourSequence tourSeq : this.tourSeqAlts) {
			planAlts.add(tourSeq.asPlan(this.scenario, person));
		}

		final MultinomialLogit teleportationBasedMNL = new MultinomialLogit(TourSequence.Type.values().length, 1);
		teleportationBasedMNL.setUtilityScale(1.0);
		teleportationBasedMNL.setCoefficient(0, 1.0);

		final MultinomialLogit congestionBasedMNL = new MultinomialLogit(TourSequence.Type.values().length, 1);
		congestionBasedMNL.setUtilityScale(1.0);
		congestionBasedMNL.setCoefficient(0, 1.0);

		final List<Double> activityModeOnlyUtilities = new ArrayList<>(TourSequence.Type.values().length);
		final List<Double> teleportationTravelTimeUtilities = new ArrayList<>(TourSequence.Type.values().length);
		final List<Double> congestedTravelTimeUtilities = new ArrayList<>(TourSequence.Type.values().length);

		final UtilityFunction utilityFunction = new UtilityFunction(this.scenario, this.tripRouterProvider,
				this.mode2travelTime, this.maxTrials, this.maxFailures);
		for (int i = 0; i < this.tourSeqAlts.size(); i++) {
			utilityFunction.evaluate(planAlts.get(i), this.tourSeqAlts.get(i));
			activityModeOnlyUtilities.add(utilityFunction.getActivityModeOnlyUtility());
			teleportationTravelTimeUtilities.add(utilityFunction.getTeleportationTravelTimeUtility());
			congestedTravelTimeUtilities.add(utilityFunction.getCongestedTravelTimeUtility());
			teleportationBasedMNL.setAttribute(i, 0,
					utilityFunction.getActivityModeOnlyUtility() + utilityFunction.getTeleportationTravelTimeUtility());
			congestionBasedMNL.setAttribute(i, 0,
					utilityFunction.getActivityModeOnlyUtility() + utilityFunction.getCongestedTravelTimeUtility());
		}
		teleportationBasedMNL.enforcedUpdate();
		congestionBasedMNL.enforcedUpdate();

		final Map<Integer, PlanForResampling> plansForResampling = new LinkedHashMap<>(this.sampleCnt);
		for (int i = 0; i < this.sampleCnt; i++) {
			final int planIndex = teleportationBasedMNL.draw(this.rnd);
			if (!plansForResampling.containsKey(planIndex)) {
				final PlanForResampling planForResampling = new PlanForResampling(planAlts.get(planIndex),
						activityModeOnlyUtilities.get(planIndex), teleportationTravelTimeUtilities.get(planIndex),
						congestedTravelTimeUtilities.get(planIndex), teleportationBasedMNL.getProbs().get(planIndex),
						new LogitEpsilonDistribution(1.0));
				planForResampling.setMATSimChoiceProba(congestionBasedMNL.getProbs().get(planIndex));
				planForResampling.setTourSequence(this.tourSeqAlts.get(planIndex));
				plansForResampling.put(planIndex, planForResampling);
			}
		}
		return new LinkedHashSet<>(plansForResampling.values());
	}
}
