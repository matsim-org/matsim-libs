package org.matsim.codeexamples.scoring.pseudoRandomErrors;

import java.util.Arrays;
import java.util.Collections;
import java.util.Random;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.codeexamples.scoring.pseudoRandomErrors.replanning.RandomModeModule;
import org.matsim.codeexamples.scoring.pseudoRandomErrors.scoring.EpsilonModeScoring;
import org.matsim.codeexamples.scoring.pseudoRandomErrors.scoring.EpsilonProvider;
import org.matsim.codeexamples.scoring.pseudoRandomErrors.scoring.GumbelEpsilonProvider;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ScoringParameterSet;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup.ModeRoutingParams;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.CharyparNagelScoringFunctionFactory;

import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Singleton;

public class RunPseudoRandomExperiment {
	static public void main(String[] args) throws ConfigurationException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.allowOptions("selection-strategy", "innovation-rate", "innovation-strategy") //
				.allowOptions("car-score", "pt-score") //
				.allowOptions("use-epsilons", "population-size") //
				.build();

		// CONFIG PART
		Config config = ConfigUtils.createConfig();

		// Some initial setup
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setLastIteration(300);
		config.controler().setOutputDirectory("simulation_output");

		// Setting up the activity scoring parameters ...
		ScoringParameterSet scoringParameters = config.planCalcScore().getOrCreateScoringParameters(null);

		// ... for generic activity
		ActivityParams homeParams = scoringParameters.getOrCreateActivityParams("generic");
		homeParams.setTypicalDuration(1.0);
		homeParams.setScoringThisActivityAtAll(false);

		// ... for car
		ModeParams carParams = scoringParameters.getOrCreateModeParams("car");
		carParams.setConstant(cmd.getOption("car-score").map(Double::parseDouble).orElse(-0.1));
		carParams.setMarginalUtilityOfTraveling(0.0);
		carParams.setMonetaryDistanceRate(0.0);

		// ... for pt
		ModeParams ptParams = scoringParameters.getOrCreateModeParams("pt");
		ptParams.setConstant(cmd.getOption("pt-score").map(Double::parseDouble).orElse(-0.2));
		ptParams.setMarginalUtilityOfTraveling(0.0);
		ptParams.setMonetaryDistanceRate(0.0);

		// Setting up replanning ...
		StrategyConfigGroup strategyConfig = config.strategy();
		strategyConfig.clearStrategySettings();
		strategyConfig.setMaxAgentPlanMemorySize(3);

		double innovationRate = cmd.getOption("innovation-rate").map(Double::parseDouble).orElse(0.1);

		// ... for selection
		StrategySettings selectionStrategy = new StrategySettings();
		selectionStrategy.setStrategyName(cmd.getOption("selection-strategy").orElse("ChangeExpBeta"));
		selectionStrategy.setWeight(1.0 - innovationRate);
		strategyConfig.addStrategySettings(selectionStrategy);

		// ... for innovation
		StrategySettings innovationStrategy = new StrategySettings();
		innovationStrategy.setStrategyName(cmd.getOption("innovation-strategy").orElse("RandomMode"));
		innovationStrategy.setWeight(innovationRate);
		strategyConfig.addStrategySettings(innovationStrategy);

		config.changeMode().setModes(new String[] { "car", "pt" });

		// Setting up routing ...
		config.plansCalcRoute().setNetworkModes(Collections.emptySet());
		config.qsim().setMainModes(Collections.emptySet());
		config.travelTimeCalculator().setAnalyzedModes(Collections.emptySet());
		config.linkStats().setWriteLinkStatsInterval(0);

		// ... for car ...
		ModeRoutingParams carRoutingParams = config.plansCalcRoute().getOrCreateModeRoutingParams("car");
		carRoutingParams.setTeleportedModeFreespeedFactor(1.0);

		// ... and for public transport
		ModeRoutingParams ptRoutingParams = config.plansCalcRoute().getOrCreateModeRoutingParams("pt");
		ptRoutingParams.setTeleportedModeFreespeedFactor(1.0);

		// Set configuration options
		cmd.applyConfiguration(config);

		Scenario scenario = ScenarioUtils.createScenario(config);

		// NETWORK PART
		Network network = scenario.getNetwork();
		NetworkFactory networkFactory = network.getFactory();

		/*-
		 *  N1 ------ N2 ------ N3
		 *       L1        L2 
		 */

		Node node1 = networkFactory.createNode(Id.createNodeId("N1"), new Coord(0.0, 1000.0));
		Node node2 = networkFactory.createNode(Id.createNodeId("N2"), new Coord(0.0, 2000.0));
		Node node3 = networkFactory.createNode(Id.createNodeId("N3"), new Coord(0.0, 3000.0));

		Link link1 = networkFactory.createLink(Id.createLinkId("L1"), node1, node2);
		Link link2 = networkFactory.createLink(Id.createLinkId("L2"), node2, node3);

		network.addNode(node1);
		network.addNode(node2);
		network.addNode(node3);

		network.addLink(link1);
		network.addLink(link2);

		for (Link link : Arrays.asList(link1, link2)) {
			link.setFreespeed(10.0);
		}

		// POPULATION PART
		Population population = scenario.getPopulation();
		PopulationFactory factory = population.getFactory();

		Random random = new Random(config.global().getRandomSeed());

		int populationSize = cmd.getOption("population-size").map(Integer::parseInt).orElse(10000);
		for (int k = 0; k < populationSize; k++) {
			Person person = factory.createPerson(Id.createPersonId(k));
			population.addPerson(person);

			Plan plan = factory.createPlan();
			person.addPlan(plan);

			Activity startActivity = factory.createActivityFromLinkId("generic", Id.createLinkId("L1"));
			startActivity.setEndTime(0.0);
			startActivity.setCoord(node1.getCoord());
			plan.addActivity(startActivity);

			Leg leg = factory.createLeg(random.nextBoolean() ? "car" : "pt");
			plan.addLeg(leg);

			Activity endActivity = factory.createActivityFromLinkId("generic", Id.createLinkId("L2"));
			endActivity.setCoord(node3.getCoord());
			plan.addActivity(endActivity);
		}

		// CONTROLLER PART
		Controler controller = new Controler(scenario);
		controller.addOverridingModule(new RandomModeModule());

		if (cmd.getOption("use-epsilons").map(Boolean::parseBoolean).orElse(false)) {
			controller.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					bind(CharyparNagelScoringFunctionFactory.class);
					bind(EpsilonProvider.class).to(GumbelEpsilonProvider.class);
				}

				@Provides
				@Singleton
				ScoringFunctionFactory provideScoringFunctionFactory(CharyparNagelScoringFunctionFactory delegate,
						Provider<EpsilonProvider> epsilonProvider) {
					return new ScoringFunctionFactory() {
						@Override
						public ScoringFunction createNewScoringFunction(Person person) {
							SumScoringFunction scoringFunction = (SumScoringFunction) delegate
									.createNewScoringFunction(person);
							scoringFunction
									.addScoringFunction(new EpsilonModeScoring(person.getId(), epsilonProvider.get()));
							return scoringFunction;
						}
					};
				}

				@Provides
				public GumbelEpsilonProvider provideGumbelEpsilonProvider(GlobalConfigGroup config) {
					return new GumbelEpsilonProvider(config.getRandomSeed(), 1.0);
				}
			});

		}

		controller.run();
	}
}
