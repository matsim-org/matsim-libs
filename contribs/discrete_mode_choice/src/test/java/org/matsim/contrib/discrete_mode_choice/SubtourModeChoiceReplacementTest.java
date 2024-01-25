package org.matsim.contrib.discrete_mode_choice;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.discrete_mode_choice.test_utils.PlanBuilder;
import org.matsim.contrib.discrete_mode_choice.test_utils.PlanTester;
import org.matsim.contribs.discrete_mode_choice.components.constraints.SubtourModeConstraint;
import org.matsim.contribs.discrete_mode_choice.components.constraints.VehicleTourConstraint;
import org.matsim.contribs.discrete_mode_choice.components.estimators.UniformTourEstimator;
import org.matsim.contribs.discrete_mode_choice.components.tour_finder.PlanTourFinder;
import org.matsim.contribs.discrete_mode_choice.components.tour_finder.TourFinder;
import org.matsim.contribs.discrete_mode_choice.components.utils.home_finder.FirstActivityHomeFinder;
import org.matsim.contribs.discrete_mode_choice.components.utils.home_finder.HomeFinder;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceModel;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceModel.FallbackBehaviour;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceModel.NoFeasibleChoiceException;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.constraints.CompositeTourConstraintFactory;
import org.matsim.contribs.discrete_mode_choice.model.filters.CompositeTourFilter;
import org.matsim.contribs.discrete_mode_choice.model.mode_availability.CarModeAvailability;
import org.matsim.contribs.discrete_mode_choice.model.mode_availability.DefaultModeAvailability;
import org.matsim.contribs.discrete_mode_choice.model.mode_availability.ModeAvailability;
import org.matsim.contribs.discrete_mode_choice.model.mode_chain.DefaultModeChainGenerator;
import org.matsim.contribs.discrete_mode_choice.model.mode_chain.ModeChainGeneratorFactory;
import org.matsim.contribs.discrete_mode_choice.model.tour_based.TourBasedModel;
import org.matsim.contribs.discrete_mode_choice.model.tour_based.TourConstraintFactory;
import org.matsim.contribs.discrete_mode_choice.model.tour_based.TourEstimator;
import org.matsim.contribs.discrete_mode_choice.model.tour_based.TourFilter;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.candidates.TripCandidate;
import org.matsim.contribs.discrete_mode_choice.model.utilities.RandomSelector;
import org.matsim.contribs.discrete_mode_choice.model.utilities.UtilitySelectorFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlansConfigGroup.ActivityDurationInterpretation;
import org.matsim.core.config.groups.PlansConfigGroup.TripDurationHandling;
import org.matsim.core.population.algorithms.ChooseRandomLegModeForSubtour;
import org.matsim.core.population.algorithms.PermissibleModesCalculator;
import org.matsim.core.population.algorithms.PermissibleModesCalculatorImpl;
import org.matsim.core.replanning.modules.SubtourModeChoice;
import org.matsim.core.router.MainModeIdentifierImpl;
import org.matsim.core.utils.timing.TimeInterpretation;

public class SubtourModeChoiceReplacementTest {
	@Test
	void testChoiceSet() throws NoFeasibleChoiceException {
		List<String> modes = Arrays.asList("walk", "pt");
		List<String> constrainedModes = Arrays.asList();
		boolean considerCarAvailability = true;
		int samples = 1000;

		Set<List<String>> dmcChains;
		Set<List<String>> smcChains;
		PlanBuilder planBuilder;

		// Test I) Simple plan with one tour

		planBuilder = new PlanBuilder() //
				.addActivityWithLinkId("home", "A") //
				.addLeg() //
				.addActivityWithLinkId("home", "B") //
				.addLeg() //
				.addActivityWithLinkId("home", "A");

		// Don't allow single legs
		dmcChains = computeDMC(planBuilder, modes, constrainedModes, considerCarAvailability, false, samples);
		smcChains = computeSMC(planBuilder, modes, constrainedModes, considerCarAvailability, false, samples);

		assertEquals(dmcChains, smcChains);

		// Allow single legs
		dmcChains = computeDMC(planBuilder, modes, constrainedModes, considerCarAvailability, false, samples);
		smcChains = computeSMC(planBuilder, modes, constrainedModes, considerCarAvailability, false, samples);

		assertEquals(dmcChains, smcChains);

		// Test II) Two tours

		planBuilder = new PlanBuilder() //
				.addActivityWithLinkId("home", "A") //
				.addLeg() //
				.addActivityWithLinkId("home", "B") //
				.addLeg() //
				.addActivityWithLinkId("home", "A") //
				.addLeg() //
				.addActivityWithLinkId("home", "B") //
				.addLeg() //
				.addActivityWithLinkId("home", "A");

		// Don't allow single legs
		dmcChains = computeDMC(planBuilder, modes, constrainedModes, considerCarAvailability, false, samples);
		smcChains = computeSMC(planBuilder, modes, constrainedModes, considerCarAvailability, false, samples);

		assertEquals(dmcChains, smcChains);

		// Allow single legs
		dmcChains = computeDMC(planBuilder, modes, constrainedModes, considerCarAvailability, true, samples);
		smcChains = computeSMC(planBuilder, modes, constrainedModes, considerCarAvailability, true, samples);

		assertEquals(dmcChains, smcChains);

		// Test II) Three tours
		planBuilder = new PlanBuilder() //
				.addActivityWithLinkId("home", "A") //
				.addLeg() //
				.addActivityWithLinkId("home", "B") //
				.addLeg() //
				.addActivityWithLinkId("home", "A") //
				.addLeg() //
				.addActivityWithLinkId("home", "C") //
				.addLeg() //
				.addActivityWithLinkId("home", "A") //
				.addLeg() //
				.addActivityWithLinkId("home", "B") //
				.addLeg() //
				.addActivityWithLinkId("home", "C") //
				.addLeg() //
				.addActivityWithLinkId("home", "A");

		// Don't allow single legs
		dmcChains = computeDMC(planBuilder, modes, constrainedModes, considerCarAvailability, false, samples);
		smcChains = computeSMC(planBuilder, modes, constrainedModes, considerCarAvailability, false, samples);

		assertEquals(dmcChains, smcChains);

		// Allow single legs
		samples = 5000;
		dmcChains = computeDMC(planBuilder, modes, constrainedModes, considerCarAvailability, true, samples);
		smcChains = computeSMC(planBuilder, modes, constrainedModes, considerCarAvailability, true, samples);

		assertEquals(dmcChains, smcChains);
	}

	@Test
	void testConstrainedChoiceSet() throws NoFeasibleChoiceException {
		List<String> modes = Arrays.asList("walk", "car");
		List<String> constrainedModes = Arrays.asList("car");
		boolean considerCarAvailability = true;
		int samples = 1000;

		Set<List<String>> dmcChains;
		Set<List<String>> smcChains;
		PlanBuilder planBuilder;

		// Test I) Simple plan with one tour

		planBuilder = new PlanBuilder() //
				.addActivityWithLinkId("home", "A") //
				.addLeg() //
				.addActivityWithLinkId("home", "B") //
				.addLeg() //
				.addActivityWithLinkId("home", "A");

		// Don't allow single legs
		dmcChains = computeDMC(planBuilder, modes, constrainedModes, considerCarAvailability, false, samples);
		smcChains = computeSMC(planBuilder, modes, constrainedModes, considerCarAvailability, false, samples);

		assertEquals(dmcChains, smcChains);

		// Allow single legs
		dmcChains = computeDMC(planBuilder, modes, constrainedModes, considerCarAvailability, false, samples);
		smcChains = computeSMC(planBuilder, modes, constrainedModes, considerCarAvailability, false, samples);

		assertEquals(dmcChains, smcChains);

		// Test II) Two tours

		planBuilder = new PlanBuilder() //
				.addActivityWithLinkId("home", "A") //
				.addLeg() //
				.addActivityWithLinkId("home", "B") //
				.addLeg() //
				.addActivityWithLinkId("home", "A") //
				.addLeg() //
				.addActivityWithLinkId("home", "B") //
				.addLeg() //
				.addActivityWithLinkId("home", "A");

		// Don't allow single legs
		dmcChains = computeDMC(planBuilder, modes, constrainedModes, considerCarAvailability, false, samples);
		smcChains = computeSMC(planBuilder, modes, constrainedModes, considerCarAvailability, false, samples);

		assertEquals(dmcChains, smcChains);

		// Allow single legs
		dmcChains = computeDMC(planBuilder, modes, constrainedModes, considerCarAvailability, true, samples);
		smcChains = computeSMC(planBuilder, modes, constrainedModes, considerCarAvailability, true, samples);

		assertEquals(dmcChains, smcChains);

		// Test II) Three tours
		planBuilder = new PlanBuilder() //
				.addActivityWithLinkId("home", "A") //
				.addLeg() //
				.addActivityWithLinkId("home", "B") //
				.addLeg() //
				.addActivityWithLinkId("home", "A") //
				.addLeg() //
				.addActivityWithLinkId("home", "C") //
				.addLeg() //
				.addActivityWithLinkId("home", "A") //
				.addLeg() //
				.addActivityWithLinkId("home", "B") //
				.addLeg() //
				.addActivityWithLinkId("home", "C") //
				.addLeg() //
				.addActivityWithLinkId("home", "A");

		// Don't allow single legs
		dmcChains = computeDMC(planBuilder, modes, constrainedModes, considerCarAvailability, false, samples);
		smcChains = computeSMC(planBuilder, modes, constrainedModes, considerCarAvailability, false, samples);

		assertEquals(dmcChains, smcChains);

		// Allow single legs
		samples = 5000;
		dmcChains = computeDMC(planBuilder, modes, constrainedModes, considerCarAvailability, true, samples);
		smcChains = computeSMC(planBuilder, modes, constrainedModes, considerCarAvailability, true, samples);

		assertEquals(dmcChains, smcChains);
	}

	@Test
	void testLargerCase() throws NoFeasibleChoiceException {
		List<String> modes = Arrays.asList("walk", "car", "pt", "bike");
		List<String> constrainedModes = Arrays.asList("car", "bike");
		boolean considerCarAvailability = true;
		int samples = 1000;

		Set<List<String>> dmcChains;
		Set<List<String>> smcChains;
		PlanBuilder planBuilder;

		// Test I) Simple plan with one tour

		planBuilder = new PlanBuilder() //
				.addActivityWithLinkId("home", "A") //
				.addLeg() //
				.addActivityWithLinkId("home", "B") //
				.addLeg() //
				.addActivityWithLinkId("home", "A");

		// Don't allow single legs
		dmcChains = computeDMC(planBuilder, modes, constrainedModes, considerCarAvailability, false, samples);
		smcChains = computeSMC(planBuilder, modes, constrainedModes, considerCarAvailability, false, samples);

		assertEquals(dmcChains, smcChains);

		// Allow single legs
		dmcChains = computeDMC(planBuilder, modes, constrainedModes, considerCarAvailability, false, samples);
		smcChains = computeSMC(planBuilder, modes, constrainedModes, considerCarAvailability, false, samples);

		assertEquals(dmcChains, smcChains);

		// Test II) Two tours

		planBuilder = new PlanBuilder() //
				.addActivityWithLinkId("home", "A") //
				.addLeg() //
				.addActivityWithLinkId("home", "B") //
				.addLeg() //
				.addActivityWithLinkId("home", "A") //
				.addLeg() //
				.addActivityWithLinkId("home", "B") //
				.addLeg() //
				.addActivityWithLinkId("home", "A");

		// Don't allow single legs
		dmcChains = computeDMC(planBuilder, modes, constrainedModes, considerCarAvailability, false, samples);
		smcChains = computeSMC(planBuilder, modes, constrainedModes, considerCarAvailability, false, samples);

		assertEquals(dmcChains, smcChains);

		// Allow single legs
		dmcChains = computeDMC(planBuilder, modes, constrainedModes, considerCarAvailability, true, samples);
		smcChains = computeSMC(planBuilder, modes, constrainedModes, considerCarAvailability, true, samples);

		assertEquals(dmcChains, smcChains);

		// Test II) Three tours
		planBuilder = new PlanBuilder() //
				.addActivityWithLinkId("home", "A") //
				.addLeg() //
				.addActivityWithLinkId("home", "B") //
				.addLeg() //
				.addActivityWithLinkId("home", "A") //
				.addLeg() //
				.addActivityWithLinkId("home", "C") //
				.addLeg() //
				.addActivityWithLinkId("home", "A") //
				.addLeg() //
				.addActivityWithLinkId("home", "B") //
				.addLeg() //
				.addActivityWithLinkId("home", "C") //
				.addLeg() //
				.addActivityWithLinkId("home", "A");

		// Don't allow single legs
		dmcChains = computeDMC(planBuilder, modes, constrainedModes, considerCarAvailability, false, samples);
		smcChains = computeSMC(planBuilder, modes, constrainedModes, considerCarAvailability, false, samples);

		assertEquals(dmcChains, smcChains);

		// Allow single legs
		samples = 5000;
		dmcChains = computeDMC(planBuilder, modes, constrainedModes, considerCarAvailability, true, samples);

		samples = 20000;
		smcChains = computeSMC(planBuilder, modes, constrainedModes, considerCarAvailability, true, samples);

		assertEquals(dmcChains, smcChains);
	}

	private Set<List<String>> computeDMC(PlanBuilder planBuilder, List<String> modes, List<String> constrainedModes,
			boolean considerCarAvailability, boolean allowSingleLegs, int samples) throws NoFeasibleChoiceException {
		TimeInterpretation timeInterpretation = TimeInterpretation.create(
				ActivityDurationInterpretation.tryEndTimeThenDuration, TripDurationHandling.shiftActivityEndTimes);

		TourEstimator estimator = new UniformTourEstimator(timeInterpretation);
		ModeAvailability modeAvailability = considerCarAvailability ? new CarModeAvailability(modes)
				: new DefaultModeAvailability(modes);
		TourFinder tourFinder = new PlanTourFinder();
		UtilitySelectorFactory selectorFactory = new RandomSelector.Factory();
		ModeChainGeneratorFactory modeChainGeneratorFactory = new DefaultModeChainGenerator.Factory();
		FallbackBehaviour fallbackBehaviour = FallbackBehaviour.EXCEPTION;

		HomeFinder homeFinder = new FirstActivityHomeFinder();
		TourConstraintFactory vehicleConstraintFactory = new VehicleTourConstraint.Factory(constrainedModes,
				homeFinder);

		TourConstraintFactory subtourModeChoiceConstraintFactory = new SubtourModeConstraint.Factory(
				allowSingleLegs ? constrainedModes : modes);

		CompositeTourConstraintFactory constraintFactory = new CompositeTourConstraintFactory();
		constraintFactory.addFactory(vehicleConstraintFactory);
		constraintFactory.addFactory(subtourModeChoiceConstraintFactory);

		TourFilter tourFilter = new CompositeTourFilter(Collections.emptySet());

		DiscreteModeChoiceModel model = new TourBasedModel(estimator, modeAvailability, constraintFactory, tourFinder,
				tourFilter, selectorFactory, modeChainGeneratorFactory, fallbackBehaviour, timeInterpretation);

		Plan plan = planBuilder.buildPlan();
		List<DiscreteModeChoiceTrip> trips = planBuilder.buildDiscreteModeChoiceTrips();
		Random random = new Random(0);

		Set<List<String>> chains = new HashSet<>();

		for (int i = 0; i < samples; i++) {
			List<TripCandidate> result = model.chooseModes(plan.getPerson(), trips, random);
			chains.add(PlanTester.getModeChain(result));
		}

		return chains;
	}

	private Set<List<String>> computeSMC(PlanBuilder planBuilder, List<String> modes, List<String> constrainedModes,
			boolean considerCarAvailability, boolean allowSingleLegs, int samples) {
		double singleLegProbability = allowSingleLegs ? 0.5 : 0.0;

		String[] availableModes = modes.toArray(new String[] {});
		String[] chainBasedModes = constrainedModes.toArray(new String[] {});
		Config config = ConfigUtils.createConfig();
		config.subtourModeChoice().setModes(availableModes);
		config.subtourModeChoice().setConsiderCarAvailability(considerCarAvailability);
		PermissibleModesCalculator permissibleModesCalculator = new PermissibleModesCalculatorImpl(config);
		Random rng = new Random(0);
		SubtourModeChoice.Behavior behavior = SubtourModeChoice.Behavior.fromSpecifiedModesToSpecifiedModes;

		ChooseRandomLegModeForSubtour smc = new ChooseRandomLegModeForSubtour(new MainModeIdentifierImpl(),
				permissibleModesCalculator, availableModes, chainBasedModes, rng, behavior, singleLegProbability);

		Set<List<String>> chains = new HashSet<>();
		Plan plan = planBuilder.buildPlan();

		for (int i = 0; i < samples; i++) {
			smc.run(plan);
			chains.add(PlanTester.getModeChain(plan));
		}

		return chains;
	}
}
