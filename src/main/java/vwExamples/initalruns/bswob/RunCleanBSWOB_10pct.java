
/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package vwExamples.initalruns.bswob;

import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.cadyts.car.CadytsCarModule;
import org.matsim.contrib.cadyts.car.CadytsContext;
import org.matsim.contrib.cadyts.general.CadytsScoring;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.filter.NetworkFilterManager;
import org.matsim.core.network.filter.NetworkLinkFilter;
import org.matsim.core.population.algorithms.XY2Links;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.*;
import parking.ParkingRouterConfigGroup;
import parking.ParkingRouterModule;

import javax.inject.Inject;

//import org.matsim.contrib.drt.optimizer.rebalancing.DemandBasedRebalancingStrategy;

/**
 * @author axer
 */

public class RunCleanBSWOB_10pct {

    public static void main(String[] args) {

        String inbase = "D:\\Matsim\\Axer\\BSWOB2.0\\";
        final Config config = ConfigUtils.loadConfig(inbase + "input\\config_10pct.xml");

        // Start with the config
        // Overwrite existing configuration parameters
        String runId = "vw219_10pct_withPark";

        config.plans().setInputFile(inbase + "input\\plans\\vw219_10pct.xml.gz");
        config.network().setInputFile(inbase + "input\\network\\vw219_SpeedCalWithPark_0.10Cap.xml.gz");
        config.strategy().setFractionOfIterationsToDisableInnovation(0.7);
        config.controler().setRunId(runId);
        config.controler().setOutputDirectory(inbase + "output\\" + runId);
        config.controler().setWritePlansInterval(1);
        config.controler().setWriteEventsInterval(1);
        config.controler().setLastIteration(5); // Number of simulation iterations

        // Add parking module
        config.addModule(new ParkingRouterConfigGroup());
        ParkingRouterConfigGroup prc = ParkingRouterConfigGroup.get(config);
        String shapeFile = "shp\\parking-zones.shp";
        prc.setShapeFile(shapeFile);
        prc.setCapacityCalculationMethod("useFromNetwork");
        prc.setShape_key("NO");

        // LOAD the scenario (i.e., load all files!)
        Scenario scenario = ScenarioUtils.loadScenario(config);

        if (true) {
            scenario.getPopulation().getPersons().values().stream().flatMap(p -> p.getPlans().stream())
                    .flatMap(pl -> pl.getPlanElements().stream()).filter(Leg.class::isInstance)
                    .forEach(pe -> ((Leg) pe).setRoute(null));
        }


        //////
        adjustPtNetworkCapacity(scenario.getNetwork(), config.qsim().getFlowCapFactor());
        setXY2Links(scenario, 80 / 3.6);
        //////

        // Use the scenario to create a controler.

        Controler controler = new Controler(scenario);

        controler.addOverridingModule(new CadytsCarModule());
        controler.addOverridingModule(new ParkingRouterModule());
        controler.addOverridingModule(new SwissRailRaptorModule());

        // include cadyts into the plan scoring (this will add the cadyts corrections to
        // the scores):
        controler.setScoringFunctionFactory(new ScoringFunctionFactory() {
            private final ScoringParametersForPerson parameters = new SubpopulationScoringParameters(scenario);
            @Inject
            CadytsContext cContext;

            @Override
            public ScoringFunction createNewScoringFunction(Person person) {

                final ScoringParameters params = parameters.getScoringParameters(person);

                SumScoringFunction scoringFunctionAccumulator = new SumScoringFunction();
                scoringFunctionAccumulator
                        .addScoringFunction(new CharyparNagelLegScoring(params, controler.getScenario().getNetwork(),scenario.getConfig().transit().getTransitModes()));
                scoringFunctionAccumulator.addScoringFunction(new CharyparNagelActivityScoring(params));
                scoringFunctionAccumulator.addScoringFunction(new CharyparNagelAgentStuckScoring(params));

                final CadytsScoring<Link> scoringFunction = new CadytsScoring<>(person.getSelectedPlan(), config,
                        cContext);
                final double cadytsScoringWeight = 20. * config.planCalcScore().getBrainExpBeta();
                scoringFunction.setWeightOfCadytsCorrection(cadytsScoringWeight);
                scoringFunctionAccumulator.addScoringFunction(scoringFunction);

                return scoringFunctionAccumulator;
            }
        });

        controler.run();

    }

    public static void adjustPtNetworkCapacity(Network network, double flowCapacityFactor) {
        if (flowCapacityFactor < 1.0) {
            for (Link l : network.getLinks().values()) {
                if (l.getAllowedModes().contains(TransportMode.pt)) {
                    l.setCapacity(l.getCapacity() / flowCapacityFactor);
                }
            }
        }
    }

    public static void setXY2Links(Scenario scenario, double maxspeed) {
        Network network = NetworkUtils.createNetwork();
        NetworkFilterManager networkFilterManager = new NetworkFilterManager(scenario.getNetwork());
        networkFilterManager.addLinkFilter(new NetworkLinkFilter() {
            @Override
            public boolean judgeLink(Link l) {
                if (l.getFreespeed() > maxspeed && l.getNumberOfLanes() > 1) {
                    return false;
                } else return true;
            }
        });
        network = networkFilterManager.applyFilters();
        XY2Links xy2Links = new XY2Links(network, null);
        for (Person p : scenario.getPopulation().getPersons().values()) {
            xy2Links.run(p);
        }


    }
}
