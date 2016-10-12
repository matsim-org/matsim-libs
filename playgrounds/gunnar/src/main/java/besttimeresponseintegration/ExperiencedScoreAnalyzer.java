package besttimeresponseintegration;

import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scoring.ExperiencedPlansService;
import org.matsim.core.scoring.functions.CharyparNagelScoringParametersForPerson;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import matsimintegration.TimeDiscretizationInjection;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
@Singleton
public class ExperiencedScoreAnalyzer implements IterationEndsListener {

	private final Map<Id<Person>, Double> person2ExpectedScore = new LinkedHashMap<>();

	@Inject
	private Network network;

	@Inject
	private CharyparNagelScoringParametersForPerson scoringParams;

	@Inject
	private Map<String, TravelTime> mode2tt;

	@Inject
	private Population population;

	@Inject
	private TimeDiscretizationInjection timeDiscrInj;

	@Inject
	ExperiencedPlansService experiencedPlansService;

	@Inject
	ExperiencedScoreAnalyzer() {
	}

	public void setExpectedScore(final Id<Person> personId, final double score) {
		this.person2ExpectedScore.put(personId, score);
	}

	@Override
	public void notifyIterationEnds(final IterationEndsEvent event) {
//		
//		// if (event.getIteration() == 0) {
//		// return;
//		// }
//
//		/*
//		 * There may be agents in the just ended iteration that have replanned
//		 * based on the BestTimeResponseStategyModule. These agents and the
//		 * expected scores of the correspondingly created plans are now in
//		 * person2expectedScore. The experienced scores of exactly these agents
//		 * are subsequently of interest.
//		 */
//
//		// TODO crossing fingers that this provides the most recently
//		// experienced plans ...
//		final Map<Id<Person>, Plan> personId2experiencedPlan = this.experiencedPlansService.getExperiencedPlans();
//
//		final Map<Id<Person>, Double> person2ExperiencedScore = new LinkedHashMap<>();
//		for (Id<Person> personId : this.person2ExpectedScore.keySet()) {
//			final boolean interpolate = true;
//			final Plan plan = personId2experiencedPlan.get(personId);
//			plan.setPerson(population.getPersons().get(personId)); // otherwise
//																	// null
//			final BestTimeResponseStrategyFunctionality planData = new BestTimeResponseStrategyFunctionality(plan,
//					this.network, this.scoringParams, this.timeDiscrInj.getInstance(), mode2tt.get("car"), interpolate, false);
//			person2ExperiencedScore.put(personId, planData.evaluate());
//		}
//
//		/*
//		 * TODO The following should be replaced by a more systematic analysis.
//		 * For now only for testing. All analysis/bookkeeping should be
//		 * performed here so that person2ExpectedScore can be cleared
//		 * afterwards.
//		 */
//
//		try {
//			final PrintWriter writer = new PrintWriter("./testdata/cba/scores" + event.getIteration() + ".txt");
//			writer.println("person\texpected\texperienced");
//			for (Id<Person> personId : this.person2ExpectedScore.keySet()) {
//				writer.print(personId);
//				writer.print("\t");
//				writer.print(this.person2ExpectedScore.get(personId));
//				writer.print("\t");
//				writer.println(person2ExperiencedScore.get(personId));
//			}
//			writer.flush();
//			writer.close();
//		} catch (Exception e) {
//			throw new RuntimeException(e);
//		}
//
//		this.person2ExpectedScore.clear();
	}

}
