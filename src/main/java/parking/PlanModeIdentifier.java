/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package parking;/*
 * created by jbischoff, 23.10.2018
 */

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.MainModeIdentifierImpl;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.TripStructureUtils;

import java.util.List;

import static org.matsim.core.config.ConfigUtils.createConfig;
import static org.matsim.core.scenario.ScenarioUtils.createScenario;

public class PlanModeIdentifier {

    public static void main(String[] args) {
        Config config = createConfig();

        Scenario scenario = createScenario(config);
        StageActivityTypes blackList = new ParkingRouterNetworkRoutingModule.ParkingAccessEgressStageActivityTypes();
        MainModeIdentifier mainModeIdentifier = new MainModeIdentifierImpl();

        new PopulationReader(scenario).readFile("D:/runs-svn/vw_rufbus/vw220park10T/vw220park10T.output_plans.xml.gz");
        for (Person person : scenario.getPopulation().getPersons().values()) {
            System.out.println(person.getId().toString() + ": ");
            Plan plan = person.getSelectedPlan();
            final List<PlanElement> planElements = plan.getPlanElements();

            final List<TripStructureUtils.Trip> trips = TripStructureUtils.getTrips(plan, blackList);

            for (TripStructureUtils.Trip trip : trips) {
                final List<PlanElement> fullTrip =
                        planElements.subList(
                                planElements.indexOf(trip.getOriginActivity()) + 1,
                                planElements.indexOf(trip.getDestinationActivity()));
                final String mode = mainModeIdentifier.identifyMainMode(fullTrip);
                System.out.println(mode);

            }

        }

    }
}
