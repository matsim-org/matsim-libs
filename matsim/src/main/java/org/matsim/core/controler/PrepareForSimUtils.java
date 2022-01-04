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

package org.matsim.core.controler;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.events.EventsManagerModule;
import org.matsim.core.router.TripRouterModule;
import org.matsim.core.router.costcalculators.TravelDisutilityModule;
import org.matsim.core.scenario.ScenarioByInstanceModule;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorModule;
import org.matsim.core.utils.timing.TimeInterpretationModule;

/**
 * Created by amit on 16.05.17.
 */


public class PrepareForSimUtils {

    public static PrepareForSim createDefaultPrepareForSim(final Scenario scenario) {
        com.google.inject.Injector injector = org.matsim.core.controler.Injector.createInjector(scenario.getConfig(),
                new AbstractModule() {
                    @Override
                    public void install() {
                        install(new ScenarioByInstanceModule(scenario));
                        install(new EventsManagerModule());
                        install(new TripRouterModule());
                        install(new TravelDisutilityModule());
                        install(new TravelTimeCalculatorModule());
                        install(new DefaultPrepareForSimModule());
                        install(new TimeInterpretationModule());
                    }
                });
        return injector.getInstance(PrepareForSim.class);
    }
}
