package org.matsim.modechoice.search;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.multibindings.Multibinder;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.assertj.core.api.InstanceOfAssertFactory;
import org.assertj.core.data.Offset;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripRouter;
import org.matsim.core.scoring.functions.ScoringParameters;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.facilities.ActivityFacilitiesImpl;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.modechoice.*;
import org.matsim.modechoice.constraints.TripConstraint;
import org.matsim.modechoice.estimators.FixedCostsEstimator;
import org.matsim.modechoice.estimators.LegEstimator;
import org.matsim.modechoice.estimators.MinMaxEstimate;
import org.matsim.modechoice.estimators.TripEstimator;
import org.matsim.testcases.MatsimTestUtils;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TopKMinMaxTest {

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	private TopKChoicesGenerator generator;
	private Injector injector;

	@Mock
	private TripRouter router;

	@Before
	public void setUp() throws Exception {

		Map<String, ModeOptions<?>> options = Map.of(TransportMode.car, new ModeOptions.AlwaysAvailable(), TransportMode.walk, new ModeOptions.AlwaysAvailable());

		InformedModeChoiceConfigGroup config = new InformedModeChoiceConfigGroup();
		config.setModes(List.of(TransportMode.car, TransportMode.walk));

		generator = new TopKChoicesGenerator(config, options);

		injector = Guice.createInjector(new TestModule());

		injector.injectMembers(generator);
	}

	@Test
	public void minmax() {

		Person person = create();

		Collection<PlanCandidate> candidates = generator.generate(person.getSelectedPlan());

		Iterator<PlanCandidate> it = candidates.iterator();

		System.out.println(candidates);

		// car mode will have walk and car leg
		// 2 * -0.5
		assertThat(it.next())
				.isEqualTo(new PlanCandidate(new String[]{"walk", "walk"}, Double.NaN))
				.extracting(PlanCandidate::getUtility).asInstanceOf(InstanceOfAssertFactories.DOUBLE)
				.isCloseTo(-1, Offset.offset(0d));

		// -0.5 -0.5 -1  (-1 const)
		assertThat(it.next())
				.isEqualTo(new PlanCandidate(new String[]{"car", "walk"}, Double.NaN))
				.extracting(PlanCandidate::getUtility).asInstanceOf(InstanceOfAssertFactories.DOUBLE)
				.isCloseTo(-3, Offset.offset(0d));

		// same but different order
		it.next();

		// -0.5 * 2  -1 * 2 -1 (const)
		assertThat(it.next())
				.isEqualTo(new PlanCandidate(new String[]{"car", "car"}, Double.NaN))
				.extracting(PlanCandidate::getUtility).asInstanceOf(InstanceOfAssertFactories.DOUBLE)
				.isCloseTo(-4, Offset.offset(0d));

	}

	private static Person create() {

		PopulationFactory f = PopulationUtils.getFactory();

		Person person = f.createPerson(Id.createPersonId(0));

		Plan plan = f.createPlan();

		Activity act1 = f.createActivityFromCoord("home", new Coord(0, 0));
		act1.setEndTime(0);

		plan.addActivity(act1);

		plan.addLeg(f.createLeg(TransportMode.walk));

		Activity act2 = f.createActivityFromCoord("work", new Coord(1, 1));
		act2.setEndTime(1000);

		plan.addActivity(act2);

		plan.addLeg(f.createLeg(TransportMode.walk));

		plan.addActivity(f.createActivityFromCoord("home", new Coord(0, 0)));

		person.addPlan(plan);
		person.setSelectedPlan(plan);

		return person;
	}

	private class TestModule extends AbstractModule {

		@Override
		protected void configure() {

			PopulationFactory f = PopulationUtils.getFactory();

			when(router.calcRoute(any(), any(), any(), anyDouble(), any(), any())).then(new Answer<List<PlanElement>>() {
				@Override
				public List<PlanElement> answer(InvocationOnMock invocationOnMock) throws Throwable {

					Leg walk = f.createLeg(TransportMode.walk);
					Leg car = f.createLeg(TransportMode.car);

					walk.setTravelTime(1);
					car.setTravelTime(10);

					if (invocationOnMock.getArgument(0).equals(TransportMode.car))
						return List.of(walk, car);
					else if (invocationOnMock.getArgument(0).equals(TransportMode.walk)) {
						walk.setTravelTime(100);
						return List.of(walk);
					} else
						throw new Exception("Unknown main mode:" + invocationOnMock.getArgument(0));
				}
			});

			bind(EstimateRouter.class).toInstance(new EstimateRouter(router,
					FacilitiesUtils.createActivityFacilities(),
					TimeInterpretation.create(PlansConfigGroup.ActivityDurationInterpretation.minOfDurationAndEndTime, PlansConfigGroup.TripDurationHandling.shiftActivityEndTimes)));


			Multibinder<TripConstraint<?>> tcBinder = Multibinder.newSetBinder(binder(), new TypeLiteral<>() {
			});

			MapBinder<String, FixedCostsEstimator<?>> fcBinder = MapBinder.newMapBinder(binder(), new TypeLiteral<>() {
			}, new TypeLiteral<>() {
			});

			fcBinder.addBinding(TransportMode.car).toInstance((context, mode, option) -> -1);

			MapBinder<String, LegEstimator<?>> legBinder = MapBinder.newMapBinder(binder(), new TypeLiteral<>() {
			}, new TypeLiteral<>() {
			});

			legBinder.addBinding(TransportMode.walk).toInstance((context, mode, leg, option) -> -0.5);


			MapBinder<String, TripEstimator<?>> tripBinder = MapBinder.newMapBinder(binder(), new TypeLiteral<>() {
			}, new TypeLiteral<>() {
			});

			tripBinder.addBinding(TransportMode.car).toInstance(new CarTripEstimator());

			Config config = TestScenario.loadConfig(utils);

			ScoringParameters.Builder scoring = new ScoringParameters.Builder(config.planCalcScore(), config.planCalcScore().getScoringParameters("person"), Map.of(), config.scenario());

			bind(ScoringParametersForPerson.class).toInstance(person -> scoring.build());


		}
	}

	// Provides fixed estimates for testing
	private class CarTripEstimator implements TripEstimator<ModeAvailability> {

		@Override
		public MinMaxEstimate estimate(EstimatorContext context, String mode, PlanModel plan, List<Leg> trip, ModeAvailability option) {
			return MinMaxEstimate.of(-2, 0);
		}

		@Override
		public double estimate(EstimatorContext context, String mode, String[] modes, PlanModel plan, ModeAvailability option) {

			double est = 0;
			for (String m : modes) {
				if (m.equals(mode))
					est -= 1;

			}

			return est;
		}

		@Override
		public boolean providesMinEstimate(EstimatorContext context, String mode, ModeAvailability option) {
			return true;
		}
	}

}
