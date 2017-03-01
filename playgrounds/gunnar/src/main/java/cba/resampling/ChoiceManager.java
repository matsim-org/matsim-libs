package cba.resampling;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class ChoiceManager {

	// -------------------- MEMBERS --------------------

	private final Random rnd;

	private final int sampleCnt;

	private Map<Id<Person>, Set<? extends Alternative>> personId2choiceSet = new LinkedHashMap<>();

	private Map<Id<Person>, Alternative> personId2choice = new LinkedHashMap<>();

	private Set<Id<Person>> personIdsHavingReplanned = new LinkedHashSet<>();
	
	// -------------------- CONSTRUCTION --------------------

	public ChoiceManager(final Random rnd, final int sampleCnt) {
		this.rnd = rnd;
		this.sampleCnt = sampleCnt;
	}

	// -------------------- IMPLEMENTATION --------------------

	public void createChoiceSets(final Population population, final ChoiceSetProvider choiceSetProvider,
			final int numberOfDraws) {
		for (Map.Entry<Id<Person>, ? extends Person> id2personEntry : population.getPersons().entrySet()) {
			this.personId2choiceSet.put(id2personEntry.getKey(),
					choiceSetProvider.newChoiceSet(id2personEntry.getValue(), numberOfDraws));
		}
	}

	public void simulateChoices(final double explorationProba) {
		for (Map.Entry<Id<Person>, Set<? extends Alternative>> personId2choiceSetEntry : this.personId2choiceSet
				.entrySet()) {
			if (this.rnd.nextDouble() < explorationProba) {
				final List<Alternative> altList = new ArrayList<>(personId2choiceSetEntry.getValue());
				this.personId2choice.put(personId2choiceSetEntry.getKey(),
						altList.get(this.rnd.nextInt(altList.size())));
				this.personIdsHavingReplanned.remove(personId2choiceSetEntry.getKey());
			} else {
				final Sampers2MATSimResampler resampler = new Sampers2MATSimResampler(this.rnd,
						personId2choiceSetEntry.getValue(), this.sampleCnt);
				this.personId2choice.put(personId2choiceSetEntry.getKey(), resampler.next());
				this.personIdsHavingReplanned.add(personId2choiceSetEntry.getKey());
			}
		}
	}

	public void updateMATSimTimeScores(final Population population, final double innoWeight) {
		for (Map.Entry<Id<Person>, ? extends Person> id2person : population.getPersons().entrySet()) {
			final Alternative alt = this.personId2choice.get(id2person.getKey());
			alt.setMATSimTimeScore(innoWeight * id2person.getValue().getSelectedPlan().getScore()
					+ (1.0 - innoWeight) * alt.getMATSimTimeScore());
		}
	}
}
