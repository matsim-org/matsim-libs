/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.vsp.cadyts.marginals;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.cadyts.general.CadytsConfigGroup;
import org.matsim.contrib.cadyts.general.CadytsScoring;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.*;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.*;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.VehicleType;

import jakarta.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.stream.Collectors;

public class ModalDistanceCadytsSingleDistanceIT {

    @RegisterExtension
	public MatsimTestUtils utils = new MatsimTestUtils();

	/**
	* This test runs a population of 1000 agents which have the same home and work place. All agents start with two plans.
	* One with mode car and one with mode bike. The selected plan is the car plan. Now, the desired distance distribution
	* is set to have an equal share of car and bike users. The accepted error in the test is 5%, due to stochastic fuzziness
	*/
	@Test
	void test() {

        Config config = createConfig();
        CadytsConfigGroup cadytsConfigGroup = new CadytsConfigGroup();
        cadytsConfigGroup.setWriteAnalysisFile(true);
        config.addModule(cadytsConfigGroup);

        Scenario scenario = cretaeScenario(config);

        Controler controler = new Controler(scenario);

        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                bind(DistanceDistribution.class).toInstance(createDistanceDistribution());
            }
        });
        controler.addOverridingModule(new ModalDistanceCadytsModule());

        // we need to also set the scoring function to see an effect
        controler.setScoringFunctionFactory(new ScoringFunctionFactory() {

            @Inject
            private ScoringParametersForPerson parameters;

            @Inject
            private ModalDistanceCadytsContext modalDistanceCadytsContext;

            @Override
            public ScoringFunction createNewScoringFunction(Person person) {
                SumScoringFunction sumScoringFunction = new SumScoringFunction();

                final ScoringParameters params = parameters.getScoringParameters(person);
                sumScoringFunction.addScoringFunction(new CharyparNagelLegScoring(params,
                        controler.getScenario().getNetwork()));
                sumScoringFunction.addScoringFunction(new CharyparNagelActivityScoring(params));
                sumScoringFunction.addScoringFunction(new CharyparNagelAgentStuckScoring(params));

                final CadytsScoring<Id<DistanceDistribution.DistanceBin>> scoringFunctionMarginals = new CadytsScoring<>(person.getSelectedPlan(),
                        config,
                        modalDistanceCadytsContext);

                // we want a rigid correction
                scoringFunctionMarginals.setWeightOfCadytsCorrection(10000);
                sumScoringFunction.addScoringFunction(scoringFunctionMarginals);

                return sumScoringFunction;
            }
        });

        controler.run();

        Map<String, Long> result = scenario.getPopulation().getPersons().values().stream()
                .map(HasPlansAndId::getSelectedPlan)
                .flatMap(plan -> plan.getPlanElements().stream())
                .filter(element -> element instanceof Activity)
                .map(element -> (Activity) element)
                .filter(activity -> activity.getType().endsWith(" interaction"))
                .map(activity -> activity.getType().substring(0, activity.getType().length() - " interaction".length()))
                .collect(Collectors.groupingBy(type -> type, Collectors.counting()));

        final int totalLegs = result.values().stream().mapToInt(Long::intValue).sum();
        final long numberOfCarLegs = result.get(TransportMode.car);
        final long numberOfOtherCarLegs = result.get(TransportMode.bike);

        assertEquals(0.4, (double) numberOfCarLegs / totalLegs, 0.05);
        assertEquals(0.6, (double) numberOfOtherCarLegs / totalLegs, 0.05);
    }

    private Config createConfig() {

        Config config = ConfigUtils.createConfig();
        String[] modes = new String[]{TransportMode.car, TransportMode.bike};

        config.controller().setOutputDirectory(this.utils.getOutputDirectory());
        config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
        config.controller().setLastIteration(20);

        config.counts().setWriteCountsInterval(1);
        config.counts().setAverageCountsOverIterations(1);

        ScoringConfigGroup.ActivityParams home = new ScoringConfigGroup.ActivityParams("home");
        home.setMinimalDuration(6 * 3600);
        home.setTypicalDuration(6 * 3600);
        home.setEarliestEndTime(6 * 3600);
        config.scoring().addActivityParams(home);

        ScoringConfigGroup.ActivityParams work = new ScoringConfigGroup.ActivityParams("work");
        work.setMinimalDuration(8 * 3600);
        work.setTypicalDuration(8 * 3600);
        work.setEarliestEndTime(14 * 3600);
        work.setOpeningTime(6 * 3600);
        work.setClosingTime(18 * 3600);
        config.scoring().addActivityParams(work);

        // have random selection of plans to generate heterogenity in the beginning, so that cadyts can calibrate its correction
        ReplanningConfigGroup.StrategySettings selectRandom = new ReplanningConfigGroup.StrategySettings();
        selectRandom.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.SelectRandom);
        selectRandom.setDisableAfter(17);
        selectRandom.setWeight(0.5);
        config.replanning().addStrategySettings(selectRandom);

        // have change exp beta, so that mode distribution converges at the end of the simulation
        ReplanningConfigGroup.StrategySettings changeExpBeta = new ReplanningConfigGroup.StrategySettings();
        changeExpBeta.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta);
        changeExpBeta.setWeight(0.5);
        config.replanning().addStrategySettings(changeExpBeta);

        // remove teleported bike
        config.routing().removeModeRoutingParams(TransportMode.bike);
        config.routing().setNetworkModes(Arrays.asList(modes));
        config.routing().setAccessEgressType(RoutingConfigGroup.AccessEgressType.accessEgressModeToLink);

        config.qsim().setMainModes(Arrays.asList(modes));
        config.qsim().setLinkDynamics(QSimConfigGroup.LinkDynamics.PassingQ);
        config.qsim().setTrafficDynamics(QSimConfigGroup.TrafficDynamics.kinematicWaves);

        config.qsim().setVehiclesSource(QSimConfigGroup.VehiclesSource.modeVehicleTypesFromVehiclesData);

        config.travelTimeCalculator().setAnalyzedModes(new HashSet<>((Arrays.asList(modes))));
        config.travelTimeCalculator().setSeparateModes(true);
        config.travelTimeCalculator().setFilterModes(true);
        config.changeMode().setModes(modes);
        config.changeMode().setBehavior(ChangeModeConfigGroup.Behavior.fromSpecifiedModesToSpecifiedModes);
        config.replanning().setFractionOfIterationsToDisableInnovation(0.8);

        return config;
    }

    private static Scenario cretaeScenario(Config config) {

        Scenario scenario = ScenarioUtils.createScenario(config);
        createNetwork(scenario.getNetwork());
        createPopulation(scenario.getPopulation(), scenario.getNetwork());

        VehicleType car = scenario.getVehicles().getFactory().createVehicleType(Id.create(TransportMode.car, VehicleType.class));
        car.setMaximumVelocity(100 / 3.6);
        car.setLength(7.5);

        // make bike and car equally fast for now
        VehicleType bike = scenario.getVehicles().getFactory().createVehicleType(Id.create(TransportMode.bike, VehicleType.class));
        bike.setMaximumVelocity(25 / 3.6);
        bike.setLength(7.5);
        scenario.getVehicles().addVehicleType(car);
        scenario.getVehicles().addVehicleType(bike);
        return scenario;
    }

    private static void createNetwork(Network network) {

        Node node1 = network.getFactory().createNode(Id.createNodeId("node-1"), new Coord(0, 0));
        Node node2 = network.getFactory().createNode(Id.createNodeId("node-2"), new Coord(50, 0));
        Node node3 = network.getFactory().createNode(Id.createNodeId("node-3"), new Coord(2050, 0));
        Node node4 = network.getFactory().createNode(Id.createNodeId("node-4"), new Coord(2100, 0));

        network.addNode(node1);
        network.addNode(node2);
        network.addNode(node3);
        network.addNode(node4);

        Link link1 = createLink(node1, node2, "start-link", 50, network.getFactory());
        Link link3 = createLink(node2, node3, "short-link", 2000, network.getFactory());
        Link link4 = createLink(node3, node4, "end-link", 50, network.getFactory());

        network.addLink(link1);
        network.addLink(link3);
        network.addLink(link4);
    }

    private static Link createLink(Node from, Node to, String id, double length, NetworkFactory factory) {
        Link link = factory.createLink(Id.createLinkId(id), from, to);
        link.setLength(length);
        link.setCapacity(10000);
        link.setAllowedModes(new HashSet<>(Arrays.asList(TransportMode.car, TransportMode.bike)));
        link.setFreespeed(100 / 3.6);
        return link;
    }

    private static void createPopulation(Population population, Network network) {

        for (int i = 0; i < 1000; i++) {
            Id<Person> personId = Id.createPersonId(population.getPersons().size() + 1);
            Person person = population.getFactory().createPerson(personId);

            person.addPlan(createPlan(TransportMode.bike, network, population.getFactory()));
            Plan carPlan = createPlan(TransportMode.car, network, population.getFactory());
            person.addPlan(carPlan);
            person.setSelectedPlan(carPlan);
            population.addPerson(person);
        }
    }

    private static Plan createPlan(String mode, Network network, PopulationFactory factory) {

        Plan plan = factory.createPlan();

        //home
        Activity h = factory
                .createActivityFromCoord("home",
                        network.getLinks().get(Id.createLinkId("start-link")).getFromNode().getCoord());
        h.setEndTime(6. * 3600. /*+ MatsimRandom.getRandom().nextInt(3600)*/);
        plan.addActivity(h);

        plan.addLeg(factory.createLeg(mode));

        //work
        Activity w = factory
                .createActivityFromLinkId("work",
                        Id.createLinkId("end-link"));
        w.setEndTime(16. * 3600. + MatsimRandom.getRandom().nextInt(3600));
        plan.addActivity(w);

        return plan;
    }

    private static DistanceDistribution createDistanceDistribution() {

        DistanceDistribution result = new DistanceDistribution();
        result.add(TransportMode.car, 1900, 2150, 10, 400);
        result.add(TransportMode.bike, 1900, 2150, 10, 600);
        return result;
    }
}
