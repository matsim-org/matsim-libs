package cba.toynet2;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;

import cba.resampling.Alternative;
import cba.resampling.ChoiceSetFactory;
import cba.resampling.MyGumbelDistribution;
import floetteroed.utilities.math.MultinomialLogit;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class VanillaChoiceSetFactory implements ChoiceSetFactory {

	// -------------------- MEMBERS --------------------

	private final double sampersLogitScale;

	private final Random rnd;

	private final Scenario scenario;

	private final UtilityFunction sampersUtilityFunction;

	// -------------------- CONSTRUCTION --------------------

	VanillaChoiceSetFactory(final double sampersLogitScale, final double sampersDefaultDestModeUtil,
			final double sampersDefaultTimeUtil, final Random rnd, final Scenario scenario) {
		this.sampersLogitScale = sampersLogitScale;
		this.rnd = rnd;
		this.scenario = scenario;
		this.sampersUtilityFunction = new UtilityFunction(sampersDefaultDestModeUtil, sampersDefaultTimeUtil);
	}

	// --------------- IMPLEMENTATION OF ChoiceSetProvider ---------------

	@Override
	public Set<Alternative> newChoiceSet(final Person person, final int numberOfDraws) {

		final MultinomialLogit sampersMNL = new MultinomialLogit(TourSequence.Type.values().length, 1);
		sampersMNL.setUtilityScale(this.sampersLogitScale);
		sampersMNL.setCoefficient(0, 1.0);

		// define universal choice set

		final List<Double> activityModeUtilities = new ArrayList<>(TourSequence.Type.values().length);
		final List<Double> sampersTravelTimeUtilities = new ArrayList<>(TourSequence.Type.values().length);
		for (int i = 0; i < TourSequence.Type.values().length; i++) {
			final TourSequence tourSeq = new TourSequence(TourSequence.Type.values()[i]);
			final double activityModeUtility = this.sampersUtilityFunction.getActivityModeUtility(tourSeq.type);
			final double sampersTimeUtility = this.sampersUtilityFunction.getSampersTimeUtility(tourSeq.type);
			activityModeUtilities.add(activityModeUtility);
			sampersTravelTimeUtilities.add(sampersTimeUtility);
			sampersMNL.setAttribute(i, 0, activityModeUtility + sampersTimeUtility);
		}
		sampersMNL.enforcedUpdate();

		// sample choice set

		final Map<Integer, Alternative> plansForResampling = new LinkedHashMap<>();
		for (int i = 0; i < numberOfDraws; i++) {
			final int planIndex = sampersMNL.draw(this.rnd);
			if (!plansForResampling.containsKey(planIndex)) {
				final TourSequence tourSequence = new TourSequence(TourSequence.Type.values()[planIndex]);
				final PlanForResampling planForResampling = new PlanForResampling(tourSequence,
						tourSequence.asPlan(this.scenario, person), activityModeUtilities.get(planIndex),
						sampersTravelTimeUtilities.get(planIndex), sampersMNL.getProbs().get(planIndex),
						new MyGumbelDistribution(this.sampersLogitScale));
				plansForResampling.put(planIndex, planForResampling);
			}
		}

		return new LinkedHashSet<>(plansForResampling.values());
	}
}
