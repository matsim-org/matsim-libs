package cba.toynet2;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;

import cba.resampling.ChoiceSetFactory;
import cba.resampling.MyGumbelDistribution;
import floetteroed.utilities.tabularfileparser.AbstractTabularFileHandlerWithHeaderLine;
import floetteroed.utilities.tabularfileparser.TabularFileParser;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class ChoiceSetFileIO extends AbstractTabularFileHandlerWithHeaderLine implements ChoiceSetFactory<PlanForResampling> {

	// -------------------- CONSTANTS --------------------

	private static final String personIdLabel = "personId";
	private static final String planForResamplingLabel = "planForResampling";
	private static final String activityModeUtilityLabel = "activityModeUtility";
	private static final String sampersTravelTimeUtilityLabel = "sampersTravelTimeUtility";
	private static final String sampersChoiceProbaLabel = "sampersChoiceProba";

	// -------------------- MEMBERS --------------------

	private Map<Id<Person>, Set<PlanForResampling>> personId2choiceSet = new LinkedHashMap<>();

	// -------------------- CONSTRUCTION --------------------

	ChoiceSetFileIO() {
	}

	// -------------------- INTERNALS --------------------

	// -------------------- WRITING --------------------

	void clear() {
		this.personId2choiceSet.clear();
	}

	// void add(final Id<Person> personId, final Set<PlanForResampling>
	// planForResampling) {
	// this.personId2choiceSet.put(personId, planForResampling);
	// }

	void writeToFile(final String fileName, final ChoiceSetFactory<PlanForResampling> choiceSetProvider,
			final Scenario scenario) {

		for (Person person : scenario.getPopulation().getPersons().values()) {
			this.personId2choiceSet.put(person.getId(), choiceSetProvider.newChoiceSet(person));
		}

		try {
			final PrintWriter writer = new PrintWriter(fileName);
			writer.println(personIdLabel + "\t" + planForResamplingLabel + "\t" + activityModeUtilityLabel + "\t"
					+ sampersTravelTimeUtilityLabel + "\t" + sampersChoiceProbaLabel);
			for (Map.Entry<Id<Person>, Set<PlanForResampling>> entry : personId2choiceSet.entrySet()) {
				for (PlanForResampling plan : entry.getValue()) {
					writer.println(
							entry.getKey() + "\t" + plan.getTourSequence().type + "\t" + plan.getSampersOnlyScore()
									+ "\t" + plan.getSampersTimeScore() + "\t" + plan.getSampersChoiceProbability());
				}
			}
			writer.flush();
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	// -------------------- READING --------------------

	private Scenario scenario = null;

	private Double sampersLogitScale = null;

	Map<Id<Person>, Set<PlanForResampling>> readFromFile(final String fileName, final Scenario scenario,
			final Double sampersLogitScale) {
		this.scenario = scenario;
		this.sampersLogitScale = sampersLogitScale;
		final TabularFileParser parser = new TabularFileParser();
		parser.setDelimiterTags(new String[] { "\t" });
		try {
			parser.parse(fileName, this);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return this.personId2choiceSet;
	}

	@Override
	public void startDocument() {
		this.label2index.clear();
		this.label2index.put(personIdLabel, 0);
		this.label2index.put(planForResamplingLabel, 1);
		this.label2index.put(activityModeUtilityLabel, 2);
		this.label2index.put(sampersTravelTimeUtilityLabel, 3);
		this.label2index.put(sampersChoiceProbaLabel, 4);
	}

	@Override
	public void startCurrentDataRow() {
		final Id<Person> personId = Id.createPersonId(this.getStringValue(personIdLabel));
		final TourSequence.Type type = TourSequence.Type.valueOf(this.getStringValue(planForResamplingLabel));
		final Double activityModeUtility = this.getDoubleValue(activityModeUtilityLabel);
		final Double sampersTravelTimeUtility = this.getDoubleValue(sampersTravelTimeUtilityLabel);
		final Double sampersChoiceProba = this.getDoubleValue(sampersChoiceProbaLabel);

		final TourSequence tourSequence = new TourSequence(type);
		final PlanForResampling planForResampling = new PlanForResampling(tourSequence,
				tourSequence.asPlan(this.scenario, this.scenario.getPopulation().getPersons().get(personId)),
				activityModeUtility, sampersTravelTimeUtility, sampersChoiceProba,
				new MyGumbelDistribution(sampersLogitScale));

		Set<PlanForResampling> choiceSet = this.personId2choiceSet.get(personId);
		if (choiceSet == null) {
			choiceSet = new LinkedHashSet<>();
			this.personId2choiceSet.put(personId, choiceSet);
		}
		choiceSet.add(planForResampling);
	}

	// --------------- IMPLEMENTATION OF ChoiceSetProvider ---------------

//	public void takeOverChoiceSets(final ChoiceSetFactory<PlanForResampling> other, final Scenario scenario) {
//		this.personId2choiceSet.clear();
//		for (Person person : scenario.getPopulation().getPersons().values()) {
//			this.personId2choiceSet.put(person.getId(), other.newChoiceSet(person));
//		}
//	}

	@Override
	public Set<PlanForResampling> newChoiceSet(Person person) {
		return this.personId2choiceSet.get(person.getId());
	}
}
