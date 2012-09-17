/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.michalm.vrp.run.online;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.router.*;
import org.matsim.core.router.old.NetworkLegRouter;
import org.matsim.core.router.old.PlansCalcRoute;
import org.matsim.core.router.util.*;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.population.algorithms.*;

import playground.michalm.vrp.taxi.TaxiModeDepartureHandler;


public class PlanInitializationUtils
{
    /**
     * This was used when empty plans (without routes) were read from file. Now it is useless.
     */
    public static void initEmptyRoutes(final Scenario scenario, TravelTime ttimeCalc,
            TravelDisutility tcostCalc)
    {
        DijkstraFactory leastCostPathCalculatorFactory = new DijkstraFactory();

        ModeRouteFactory routeFactory = ((PopulationFactoryImpl)scenario.getPopulation()
                .getFactory()).getModeRouteFactory();

        final PlansCalcRoute routingAlgorithm = new PlansCalcRoute(scenario.getConfig()
                .plansCalcRoute(), scenario.getNetwork(), tcostCalc, ttimeCalc,
                leastCostPathCalculatorFactory, routeFactory);

        routingAlgorithm
                .addLegHandler(
                        TaxiModeDepartureHandler.TAXI_MODE,
                        new NetworkLegRouter(scenario.getNetwork(), routingAlgorithm
                                .getLeastCostPathCalculator(), routeFactory));

        ParallelPersonAlgorithmRunner.run(scenario.getPopulation(), 1,
                new ParallelPersonAlgorithmRunner.PersonAlgorithmProvider() {
                    @Override
                    public AbstractPersonAlgorithm getPersonAlgorithm()
                    {
                        return new PersonPrepareForSim(routingAlgorithm, (ScenarioImpl)scenario);
                    }
                });
    }
}
