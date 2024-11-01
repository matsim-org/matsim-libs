
/* *********************************************************************** *
 * project: org.matsim.*
 * OverrideCarTraveltimeTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

 package org.matsim.core.controler;
import org.junit.jupiter.api.Assertions;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.events.ReplanningEvent;
import org.matsim.core.controler.listener.ReplanningListener;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;

import jakarta.inject.Inject;
import java.util.Map;

 public class OverrideCarTraveltimeTest {

    public static void main(String[] args) {
        final Config config = ConfigUtils.createConfig();
        config.controller().setLastIteration(1);
        config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
        Controler controler = new Controler(ScenarioUtils.createScenario(config));
        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                bindCarTravelDisutilityFactory().to(InterestingTravelDisutilityFactory.class);
                bindNetworkTravelTime().to(InterestingTravelTime.class);
                addControlerListenerBinding().to(InterestingControlerListener.class);
            }
        });
        controler.run();
    }

    private static class InterestingTravelTime implements TravelTime {
        @Override
        public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
            return 42.0;
        }
    }

    private static class InterestingTravelDisutilityFactory implements TravelDisutilityFactory {
        @Override
        public TravelDisutility createTravelDisutility(TravelTime timeCalculator) {
            return new TravelDisutility() {
                @Override
                public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle) {
                    return 37.0;
                }

                @Override
                public double getLinkMinimumTravelDisutility(Link link) {
                    return 37.0;
                }
            };
        }
    }

    private static class InterestingControlerListener implements ReplanningListener {

        @Inject
        Config config;

        @Inject
        Map<String, TravelTime> travelTimes;

        @Inject
        Map<String, TravelDisutilityFactory> travelDisutilities;

        @Override
        public void notifyReplanning(ReplanningEvent event) {
            Assertions.assertEquals(42.0, travelTimes.get(TransportMode.car).getLinkTravelTime(null, 0.0, null, null), 0.0);
            Assertions.assertEquals(37.0, travelDisutilities.get(TransportMode.car).createTravelDisutility(travelTimes.get(TransportMode.car)).getLinkTravelDisutility(null, 0.0, null, null), 0.0);
        }
    }
}
