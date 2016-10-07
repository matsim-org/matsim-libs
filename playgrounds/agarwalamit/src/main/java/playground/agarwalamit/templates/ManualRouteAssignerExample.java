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

package playground.agarwalamit.templates;

import static org.eclipse.jetty.http.HttpParser.LOG;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.population.algorithms.PersonPrepareForSim;
import org.matsim.core.population.algorithms.PlanAlgorithm;
import org.matsim.core.router.PlanRouter;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripRouterFactoryBuilderWithDefaults;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutilityFactory;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import playground.agarwalamit.mixedTraffic.patnaIndia.router.FreeSpeedTravelTimeForBike;
import playground.agarwalamit.mixedTraffic.patnaIndia.router.FreeSpeedTravelTimeForTruck;
import playground.agarwalamit.utils.LoadMyScenarios;

/**
 * Created by amit on 28/09/16.
 */


public class ManualRouteAssignerExample {

    private  Scenario scenario;
    private  String initialNetwork;
    private  Set<String> modes;


    private void reassignRoutes() {
        LOG.info("Routes of all bike plans will be re-assinged.");

        // following is required to get the routes (or links) from base network and not from the running scenario network.
        Scenario scNetwork = LoadMyScenarios.loadScenarioFromNetwork(this.initialNetwork);

        // create routers for all modes, since this step is manual assignment of routes
        Map<String, PersonPrepareForSim> mode2routers = new HashMap<>();
        for (String mode : scenario.getConfig().qsim().getMainModes()) {
            TravelTime tt;
            if (mode.equals("bike")) tt = new FreeSpeedTravelTimeForBike();
            else if (mode.equals("truck")) tt = new FreeSpeedTravelTimeForTruck();
            else tt = new FreeSpeedTravelTime();

            TripRouterFactoryBuilderWithDefaults routerFactory = new TripRouterFactoryBuilderWithDefaults();
            routerFactory.setTravelTime(tt);
            routerFactory.setTravelDisutility(new RandomizingTimeDistanceTravelDisutilityFactory(mode, scenario.getConfig().planCalcScore()).createTravelDisutility(tt));

            final TripRouter tripRouter = routerFactory.build(scNetwork).get();
            PlanAlgorithm router = new PlanRouter(tripRouter);

            PersonPrepareForSim pp4s = new PersonPrepareForSim(router, (MutableScenario) scNetwork);
            mode2routers.put(mode, pp4s);
        }

        // first remove routes from legs of bike mode
        for (Person p : scenario.getPopulation().getPersons().values()) {
            for (Plan plan : p.getPlans()) {
                List<PlanElement> pes = plan.getPlanElements();
                for (PlanElement pe : pes) {
                    if (pe instanceof Activity) {
                        Activity act = ((Activity) pe);
                        Id<Link> linkId = act.getLinkId();
                        Coord cord = act.getCoord();

                        if (linkId == null) { // activity should have at least one of link id or coord
                            if (cord == null)
                                throw new RuntimeException("Activity " + act.toString() + " do not have either of link id or coord. Aborting...");
                            else {/*nothing to do; cord is assigned*/ }
                        } else if (cord == null) { // if cord is null, get it from
                            if (scNetwork.getNetwork().getLinks().containsKey(linkId)) {
                                cord = scNetwork.getNetwork().getLinks().get(linkId).getCoord();
                                act.setLinkId(null);
                                act.setCoord(cord);
                            } else
                                throw new RuntimeException("Activity " + act.toString() + " do not have cord and link id is not present in network. Aborting...");
                        }
                    } else if (pe instanceof Leg) {
                        Leg leg = (Leg) pe;
                        if (modes.contains(leg.getMode())) leg.setRoute(null);
                    }
                }
            }
        }

        for (Person p : scenario.getPopulation().getPersons().values()) {
            // since all trips have same mode, following is safe.
            String mode = ((Leg) p.getSelectedPlan().getPlanElements().get(1)).getMode();
            mode2routers.get(mode).run(p);
        }
    }
}
