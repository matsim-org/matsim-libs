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

import com.google.inject.Provider;

import cba.resampling.Alternative;
import cba.resampling.ChoiceSetProvider;
import cba.resampling.MyGumbelDistribution;
import floetteroed.utilities.math.MultinomialLogit;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class VanillaChoiceSetProvider implements ChoiceSetProvider {

	// -------------------- MEMBERS --------------------

	final double sampersLogitScale;

	private final Random rnd;

	private final Scenario scenario;

	private final Provider<TripRouter> tripRouterProvider;

	private final double betaTravelSampers_1_h;

	// -------------------- CONSTRUCTION --------------------

	public VanillaChoiceSetProvider(final double sampersLogitScale, final Random rnd, final Scenario scenario,
			final Provider<TripRouter> tripRouterProvider, final double betaTravelSampers_1_h) {
		this.sampersLogitScale = sampersLogitScale;
		this.rnd = new Random(rnd.nextLong());
		this.scenario = scenario;
		this.tripRouterProvider = tripRouterProvider;
		this.betaTravelSampers_1_h = betaTravelSampers_1_h;
	}

	// --------------- IMPLEMENTATION OF ChoiceSetProvider ---------------

	@Override
	public Set<Alternative> newChoiceSet(Person person, int numberOfDraws) {

		final MultinomialLogit sampersMNL = new MultinomialLogit(TourSequence.Type.values().length, 1);
		sampersMNL.setUtilityScale(this.sampersLogitScale);
		sampersMNL.setCoefficient(0, 1.0);

		final UtilityFunction utilityFunction = new UtilityFunction(this.scenario, this.tripRouterProvider, null, 10, 3,
				true, false, this.betaTravelSampers_1_h, null);

		final List<TourSequence> tourSeqAlts = new ArrayList<>(TourSequence.Type.values().length);
		final List<Plan> planAlts = new ArrayList<>(TourSequence.Type.values().length);
		final List<Double> activityModeOnlyUtilities = new ArrayList<>(TourSequence.Type.values().length);
		final List<Double> sampersTravelTimeUtilities = new ArrayList<>(TourSequence.Type.values().length);
		for (int i = 0; i < TourSequence.Type.values().length; i++) {
			final TourSequence tourSeq = new TourSequence(TourSequence.Type.values()[i]);
			tourSeqAlts.add(tourSeq);
			final Plan plan = tourSeq.asPlan(this.scenario, person);
			planAlts.add(plan);
			utilityFunction.evaluate(plan, tourSeq);
			activityModeOnlyUtilities.add(utilityFunction.getActivityModeOnlyUtility());
			sampersTravelTimeUtilities.add(utilityFunction.getTeleportationTravelTimeUtility());
			sampersMNL.setAttribute(i, 0,
					utilityFunction.getActivityModeOnlyUtility() + utilityFunction.getTeleportationTravelTimeUtility());
		}
		sampersMNL.enforcedUpdate();

		final Map<Integer, Alternative> plansForResampling = new LinkedHashMap<>();
		for (int i = 0; i < numberOfDraws; i++) {
			final int planIndex = sampersMNL.draw(this.rnd);
			if (!plansForResampling.containsKey(planIndex)) {
				final PlanForResampling planForResampling = new PlanForResampling(planAlts.get(planIndex),
						activityModeOnlyUtilities.get(planIndex), sampersTravelTimeUtilities.get(planIndex), 0.0,
						sampersMNL.getProbs().get(planIndex), new MyGumbelDistribution(this.sampersLogitScale));
				planForResampling.setTourSequence(tourSeqAlts.get(planIndex));
				plansForResampling.put(planIndex, planForResampling);
			}
		}

		return new LinkedHashSet<>(plansForResampling.values());
	}
}
