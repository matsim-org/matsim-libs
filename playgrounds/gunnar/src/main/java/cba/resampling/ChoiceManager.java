package cba.resampling;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class ChoiceManager<A extends Alternative> {

	// -------------------- MEMBERS --------------------

	private final Random rnd;

	private final int numberOfDrawsToGenerateChoiceSet;

	private final int numberOfDrawsToEstimateLogsum;

	private Map<Id<Person>, Set<A>> personId2choiceSet = new LinkedHashMap<>();
	private Map<Id<Person>, A> personId2choice = new LinkedHashMap<>();
	private Map<Id<Person>, LinkedList<A>> personId2unexploredAlternatives = new LinkedHashMap<>();

	private Map<A, Integer> alt2evalCnt = new LinkedHashMap<>();

	private int getEvalCnt(final A alt) {
		final Integer val = this.alt2evalCnt.get(alt);
		return ((val != null) ? val : 0);
	}

	private void incEvalCnt(final A alt) {
		this.alt2evalCnt.put(alt, this.getEvalCnt(alt) + 1);
	}

	// -------------------- CONSTRUCTION --------------------

	public ChoiceManager(final Random rnd, final int numberOfDrawsToGenerateChoiceSet,
			final int numberOfDrawsToEstimateLogsum) {
		this.rnd = rnd;
		this.numberOfDrawsToGenerateChoiceSet = numberOfDrawsToGenerateChoiceSet;
		this.numberOfDrawsToEstimateLogsum = numberOfDrawsToEstimateLogsum;
	}

	// -------------------- IMPLEMENTATION --------------------

	public void createChoiceSets(final Population population, final ChoiceSetFactory<A> choiceSetProvider) {
		for (Map.Entry<Id<Person>, ? extends Person> id2personEntry : population.getPersons().entrySet()) {
			final Set<A> alternativesSet = choiceSetProvider.newChoiceSet(id2personEntry.getValue());
			this.personId2choiceSet.put(id2personEntry.getKey(), alternativesSet);

			final LinkedList<A> alternativesList = new LinkedList<>(alternativesSet);
			Collections.shuffle(alternativesList);
			this.personId2unexploredAlternatives.put(id2personEntry.getKey(), alternativesList);
		}
	}

	public void simulateChoices(final Population population, final double replanProba, final double explorationProba) {

		for (Map.Entry<Id<Person>, Set<A>> personId2choiceSetEntry : this.personId2choiceSet.entrySet()) {

			final Id<Person> personId = personId2choiceSetEntry.getKey();
			final Set<A> choiceSet = personId2choiceSetEntry.getValue();

			// internal choice update

			final A choice;

			if (this.personId2unexploredAlternatives.get(personId).size() > 0) {

				choice = this.personId2unexploredAlternatives.get(personId).removeFirst();
				choice.setSampersEpsilonRealization(0.0);
				this.personId2choice.put(personId, choice);

			} else if (this.rnd.nextDouble() < replanProba) {

				if (this.rnd.nextDouble() < explorationProba) {

					choice = (new ArrayList<>(choiceSet)).get(this.rnd.nextInt(choiceSet.size()));
					choice.setSampersEpsilonRealization(0.0);
					this.personId2choice.put(personId, choice);

				} else {

					final Sampers2MATSimResampler<A> resampler = new Sampers2MATSimResampler<>(this.rnd, choiceSet,
							this.numberOfDrawsToGenerateChoiceSet);
					choice = resampler.next();
					this.personId2choice.put(personId, choice);

				}

			} else {

				choice = this.personId2choice.get(personId);

			}

			this.incEvalCnt(choice);

			// wire choice into MATSim person

			final Person person = population.getPersons().get(personId);
			final Plan plan = choice.getMATSimPlan();
			person.getPlans().clear();
			person.setSelectedPlan(null);
			person.addPlan(plan);
			plan.setPerson(person);

			// TODO NEW
			plan.setScore(null);
			// plan.setScore(choice.getSampersOnlyScore() +
			// choice.getMATSimTimeScore());

			person.setSelectedPlan(plan);

		}
	}

	public Alternative getChoice(final Id<Person> personId) {
		return this.personId2choice.get(personId);
	}

	public void updateMATSimTimeScores(final Population population) {
		for (Map.Entry<Id<Person>, ? extends Person> id2person : population.getPersons().entrySet()) {
			final A alt = this.personId2choice.get(id2person.getKey());

			final int evalCnt = this.getEvalCnt(alt);
			if (evalCnt <= 0) {
				throw new RuntimeException("Evaluation count is " + evalCnt + " but should be at least one.");
			}
			final double innoWeight;
			if (evalCnt <= 2) {
				innoWeight = 1.0;
			} else {
				// do not include the first trial evaluation
				innoWeight = 1.0 / (evalCnt - 1);
			}

			// TODO NEW
			// alt.setMATSimTimeScore(innoWeight *
			// id2person.getValue().getSelectedPlan().getScore()
			// + (1.0 - innoWeight) * alt.getMATSimTimeScore());
			alt.updateMATSimTimeScore(id2person.getValue().getSelectedPlan().getScore(), innoWeight);
		}
	}

	public double getEstimatedLogsum(final Id<Person> personId) {
		double sum = 0;
		for (int r = 0; r < this.numberOfDrawsToEstimateLogsum; r++) {
			final Sampers2MATSimResampler<A> resampler = new Sampers2MATSimResampler<>(this.rnd,
					this.personId2choiceSet.get(personId), this.numberOfDrawsToGenerateChoiceSet);
			final Alternative choice = resampler.next();
			sum += choice.getSampersOnlyScore() + choice.getSampersEpsilonRealization() + choice.getMATSimTimeScore();
		}
		return (sum / this.numberOfDrawsToEstimateLogsum);
	}

	public double getEstimatedMaximumUtility(final Population population) {
		double result = 0;
		for (Id<Person> personId : population.getPersons().keySet()) {
			result += this.getEstimatedLogsum(personId);
		}
		return result;
	}

	public double getRealizedMaximumUtility(final Population population) {
		double result = 0;
		for (Id<Person> personId : population.getPersons().keySet()) {
			final Alternative alt = this.getChoice(personId);
			result += alt.getSampersOnlyScore() + alt.getSampersEpsilonRealization() + alt.getMATSimTimeScore();
		}
		return result;
	}

	public Map<A, Double> newAlternative2choiceProba(final Id<Person> personId) {

		final Map<A, Double> result = new LinkedHashMap<>();
		for (A alternative : this.personId2choiceSet.get(personId)) {
			result.put(alternative, 0.0);
		}

		final Sampers2MATSimResampler<A> resampler = new Sampers2MATSimResampler<>(this.rnd,
				this.personId2choiceSet.get(personId), this.numberOfDrawsToGenerateChoiceSet);
		for (int r = 0; r < this.numberOfDrawsToEstimateLogsum; r++) {
			final A choice = resampler.next();
			result.put(choice, result.get(choice) + 1.0);
		}

		for (A alternative : this.personId2choiceSet.get(personId)) {
			result.put(alternative, result.get(alternative) / this.numberOfDrawsToEstimateLogsum);
		}
		return result;
	}
}
