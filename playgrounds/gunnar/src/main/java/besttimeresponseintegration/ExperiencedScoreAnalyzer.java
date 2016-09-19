package besttimeresponseintegration;

import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.replanning.PlanStrategy;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
@Singleton
public class ExperiencedScoreAnalyzer implements IterationEndsListener {

	private final Population population;

	// private final BestTimeResponseStrategyProvider
	// bestTimeResponseStrategyProvider;

	private final Map<String, Provider<PlanStrategy>> name2planStratProvider;

	private final Map<Id<Person>, Double> person2ExpectedScore = new LinkedHashMap<>();

	@Inject
	ExperiencedScoreAnalyzer(final Population population,
			final Map<String, Provider<PlanStrategy>> name2planStratProvider) {
		this.population = population;
		this.name2planStratProvider = name2planStratProvider;

		// >>> TODO TESTING >>>
		for (Map.Entry<?, ?> entry : this.name2planStratProvider.entrySet()) {
			System.out.println(entry);
		}
		System.exit(0);
		// <<< TODO TESTING <<<

	}

	public void setExpectedScore(final Id<Person> personId, final double score) {
		this.person2ExpectedScore.put(personId, score);
	}

	@Override
	public void notifyIterationEnds(final IterationEndsEvent event) {

		/*
		 * There may be agents in the just ended iteration that have replanned
		 * based on the BestTimeResponseStategyModule. These agents and the
		 * expected scores of the correspondingly created plans are now in
		 * person2expectedScore. The experienced scores of exactly these agents
		 * are subsequently of interest.
		 */

		final Map<Id<Person>, Double> person2ExperiencedScore = new LinkedHashMap<>();
		for (Id<Person> personId : this.person2ExpectedScore.keySet()) {
			final BestTimeResponseStrategyModule bestTimeResponse = null;

			// (BestTimeResponseStrategyModule)
			// this.bestTimeResponseStrategyProvider
			// .get();

			// person2ExperiencedScore.put(personId,
			// this.population.getPersons().get(personId).getSelectedPlan().getScore());
			person2ExperiencedScore.put(personId,
					bestTimeResponse.evaluatePlan(this.population.getPersons().get(personId).getSelectedPlan()));
		}

		/*
		 * TODO The following should be replaced by a more systematic analysis.
		 * For now only for testing. All analysis/bookkeeping should be
		 * performed here so that person2ExpectedScore can be cleared
		 * afterwards.
		 */

		try {
			final PrintWriter writer = new PrintWriter("./testdata/cba/scores" + event.getIteration() + ".txt");
			writer.println("person\texpected\texperienced");
			for (Id<Person> personId : this.person2ExpectedScore.keySet()) {
				writer.print(personId);
				writer.print("\t");
				writer.print(this.person2ExpectedScore.get(personId));
				writer.print("\t");
				writer.println(person2ExperiencedScore.get(personId));
			}
			writer.flush();
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		this.person2ExpectedScore.clear();
	}

}
