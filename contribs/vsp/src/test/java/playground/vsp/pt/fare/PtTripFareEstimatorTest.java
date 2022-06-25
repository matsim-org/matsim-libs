package playground.vsp.pt.fare;

import com.google.inject.Inject;
import com.google.inject.Injector;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.assertj.core.data.Offset;
import org.junit.Before;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.application.MATSimApplication;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;
import org.matsim.modechoice.*;
import org.matsim.modechoice.estimators.MinMaxEstimate;
import org.matsim.modechoice.estimators.TripEstimator;
import playground.vsp.TestScenario;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class PtTripFareEstimatorTest {

	protected InformedModeChoiceConfigGroup group;
	protected Controler controler;

	protected Injector injector;

	@Inject
	private EstimateRouter router;

	@Inject
	private ScoringParametersForPerson params;

	@Inject
	private Map<String, TripEstimator<?>> tripEstimator;


	private PtTripFareEstimator estimator;

	@Before
	public void setUp() throws Exception {

		Config config = TestScenario.loadConfig();

		Map<String, PlanCalcScoreConfigGroup.ModeParams> modes = config.planCalcScore().getScoringParameters("person").getModes();

		PlanCalcScoreConfigGroup.ModeParams pt = modes.get(TransportMode.pt);
		PlanCalcScoreConfigGroup.ModeParams walk = modes.get(TransportMode.walk);

		group = ConfigUtils.addOrGetModule(config, InformedModeChoiceConfigGroup.class);

		PtFareConfigGroup fare = ConfigUtils.addOrGetModule(config, PtFareConfigGroup.class);
		DistanceBasedPtFareParams distanceFare = ConfigUtils.addOrGetModule(config, DistanceBasedPtFareParams.class);

		fare.setApplyUpperBound(true);
		fare.setUpperBoundFactor(1.5);

		distanceFare.setMinFare(0.1);
		distanceFare.setNormalTripIntercept(0.5);
		distanceFare.setNormalTripSlope(0.1);

		distanceFare.setLongDistanceTripThreshold(20000);
		distanceFare.setLongDistanceTripIntercept(1);
		distanceFare.setLongDistanceTripSlope(0.01);

		controler = MATSimApplication.prepare(TestScenario.class, config);
		injector = controler.getInjector();

		injector.injectMembers(this);

		estimator = (PtTripFareEstimator) tripEstimator.get(TransportMode.pt);
	}

	private List<MinMaxEstimate> estimateAgent(Id<Person> personId) {

		Person person = controler.getScenario().getPopulation().getPersons().get(personId);
		Plan plan = person.getSelectedPlan();

		EstimatorContext context = new EstimatorContext(plan.getPerson(), params.getScoringParameters(plan.getPerson()));

		PlanModel model = router.routeModes(plan, Set.of(TransportMode.pt, TransportMode.walk, TransportMode.bike, TransportMode.car));

		List<MinMaxEstimate> ests = new ArrayList<>();

		for (int i = 0; i < model.trips(); i++) {

			List<Leg> trip = model.getLegs(TransportMode.pt, i);

			if (trip == null)
				continue;

			MinMaxEstimate est = estimator.estimate(context, TransportMode.pt, model, trip, ModeAvailability.YES);

			ests.add(est);
		}

		return ests;
	}

	@Test
	public void fare() {

		List<MinMaxEstimate> est = estimateAgent(TestScenario.Agents.get(1));
		System.out.println(est);

		assertThat(est)
				.allMatch(e -> e.getMin() < e.getMax(), "Min smaller max")
				.first().extracting(MinMaxEstimate::getMin, InstanceOfAssertFactories.DOUBLE)
						.isCloseTo(-379.4, Offset.offset(0.1));

	}

	@Test
	public void all() {

		for (Id<Person> agent : TestScenario.Agents) {
			List<MinMaxEstimate> est = estimateAgent(agent);

			assertThat(est)
					.allMatch(e -> e.getMin() <= e.getMax(), "Min smaller max");

		}
	}
}