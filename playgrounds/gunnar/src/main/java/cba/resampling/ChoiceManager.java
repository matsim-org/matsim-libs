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
public class ChoiceManager {

	// -------------------- MEMBERS --------------------

	private final Random rnd;

	private final int numberOfDrawsToGenerateChoiceSet;

	private final int numberOfDrawsToEstimateLogsum;

	private Map<Id<Person>, Set<Alternative>> personId2choiceSet = new LinkedHashMap<>();
	private Map<Id<Person>, Alternative> personId2choice = new LinkedHashMap<>();
	private Map<Id<Person>, LinkedList<Alternative>> personId2unexploredAlternatives = new LinkedHashMap<>();

	// -------------------- CONSTRUCTION --------------------

	public ChoiceManager(final Random rnd, final int numberOfDrawsToGenerateChoiceSet,
			final int numberOfDrawsToEstimateLogsum) {
		this.rnd = rnd;
		this.numberOfDrawsToGenerateChoiceSet = numberOfDrawsToGenerateChoiceSet;
		this.numberOfDrawsToEstimateLogsum = numberOfDrawsToEstimateLogsum;
	}

	// -------------------- IMPLEMENTATION --------------------

	public void createChoiceSets(final Population population, final ChoiceSetFactory choiceSetProvider) {
		for (Map.Entry<Id<Person>, ? extends Person> id2personEntry : population.getPersons().entrySet()) {
			final Set<Alternative> alternativesSet = choiceSetProvider.newChoiceSet(id2personEntry.getValue(),
					this.numberOfDrawsToGenerateChoiceSet);
			this.personId2choiceSet.put(id2personEntry.getKey(), alternativesSet);

			final LinkedList<Alternative> alternativesList = new LinkedList<>(alternativesSet);
			Collections.shuffle(alternativesList);
			this.personId2unexploredAlternatives.put(id2personEntry.getKey(), alternativesList);
		}
	}

	public void simulateChoices(final Population population, final double explorationProba) {

		for (Map.Entry<Id<Person>, Set<Alternative>> personId2choiceSetEntry : this.personId2choiceSet.entrySet()) {

			final Id<Person> personId = personId2choiceSetEntry.getKey();
			final Set<Alternative> choiceSet = personId2choiceSetEntry.getValue();

			// internal choice update

			final Alternative choice;

			if (this.personId2unexploredAlternatives.get(personId).size() > 0) {

				choice = this.personId2unexploredAlternatives.get(personId).removeFirst();
				choice.setSampersEpsilonRealization(0.0);
				this.personId2choice.put(personId, choice);

			} else if (this.rnd.nextDouble() < explorationProba) {

				choice = (new ArrayList<>(choiceSet)).get(this.rnd.nextInt(choiceSet.size()));
				choice.setSampersEpsilonRealization(0.0);
				this.personId2choice.put(personId, choice);

			} else {

				final Sampers2MATSimResampler resampler = new Sampers2MATSimResampler(this.rnd, choiceSet,
						this.numberOfDrawsToGenerateChoiceSet);
				choice = resampler.next();
				this.personId2choice.put(personId, choice);

			}

			// wire choice into MATSim person

			final Person person = population.getPersons().get(personId);
			final Plan plan = choice.getMATSimPlan();
			person.getPlans().clear();
			person.setSelectedPlan(null);
			person.addPlan(plan);
			plan.setPerson(person);
			plan.setScore(choice.getSampersOnlyScore() + choice.getMATSimTimeScore());
			person.setSelectedPlan(plan);

		}
	}

	public Alternative getChoice(final Id<Person> personId) {
		return this.personId2choice.get(personId);
	}

	public void updateMATSimTimeScores(final Population population, final double innoWeight) {
		for (Map.Entry<Id<Person>, ? extends Person> id2person : population.getPersons().entrySet()) {
			final Alternative alt = this.personId2choice.get(id2person.getKey());
			alt.setMATSimTimeScore(innoWeight * id2person.getValue().getSelectedPlan().getScore()
					+ (1.0 - innoWeight) * alt.getMATSimTimeScore());
		}
	}

	public double getEstimatedLogsum(final Id<Person> personId) {
		double sum = 0;
		for (int r = 0; r < this.numberOfDrawsToEstimateLogsum; r++) {
			final Sampers2MATSimResampler resampler = new Sampers2MATSimResampler(this.rnd,
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
}
