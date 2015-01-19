/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * Main.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */

package tutorial.programming.example20customTravelTime;

import org.matsim.analysis.VolumesAnalyzerModule;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.corelisteners.LegHistogramModule;
import org.matsim.core.controler.corelisteners.LinkStatsModule;
import org.matsim.core.replanning.StrategyManagerModule;
import org.matsim.core.router.TripRouterModule;
import org.matsim.core.router.costcalculators.TravelDisutilityModule;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;

/**
 * How to tell the Controler *not* to use the Events-collecting
 * TravelTimeCalculator to provide the TravelTime for the next iteration,
 * but to use the constant FreeSpeedTravelTime instead.
 *
 * @author michaz
 *
 */
public class Main {

    public static void main(String[] args) {
        String configFilename = "examples/equil/config.xml";
        Controler controler = new Controler(configFilename);
        controler.setModules(new AbstractModule() {
            @Override
            public void install() {
                // Include some things from ControlerDefaultsModule.java,
                // but leave out TravelTimeCalculator.
                // You can just comment out these lines if you don't want them,
                // these modules are optional.
                include(new TripRouterModule());
                include(new StrategyManagerModule());
                include(new LinkStatsModule());
                include(new VolumesAnalyzerModule());
                include(new LegHistogramModule());
                include(new TravelDisutilityModule());

                // Because TravelTimeCalculatorModule is left out,
                // we have to provide a TravelTime.
                // This line says: Use this thing here as the TravelTime implementation.
                // Try removing this line: You will get an error because there is no
                // TravelTime and someone needs it.
                bindToInstance(TravelTime.class, new FreeSpeedTravelTime());
            }
        });
        controler.run();

    }
}
