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
import org.matsim.analysis.LegHistogramModule;
import org.matsim.analysis.LinkStatsModule;
import org.matsim.core.mobsim.DefaultMobsimModule;
import org.matsim.core.replanning.StrategyManagerModule;
import org.matsim.core.router.TripRouterModule;
import org.matsim.core.router.costcalculators.TravelDisutilityModule;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scoring.functions.CharyparNagelScoringFunctionModule;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;

/**
 * How to tell the Controler *not* to use the Events-collecting
 * TravelTimeCalculator to provide the TravelTime for the next iteration,
 * but to use the constant FreeSpeedTravelTime instead.
 *
 * @author michaz
 *
 */
public class RunCustomTravelTimeExample {

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
				// For an alternative approach (which uses the defaults and just overrides), see below.
				install(new DefaultMobsimModule());
				install(new CharyparNagelScoringFunctionModule());
				install(new TripRouterModule());
				install(new StrategyManagerModule());
				install(new LinkStatsModule());
				install(new VolumesAnalyzerModule());
				install(new LegHistogramModule());
				install(new TravelDisutilityModule());

				// Because TravelTimeCalculatorModule is left out,
				// we have to provide a TravelTime.
				// This line says: Use this thing here as the TravelTime implementation.
				// Try removing this line: You will get an error because there is no
				// TravelTime and someone needs it.
				bind(TravelTime.class).toInstance(new FreeSpeedTravelTime());
			}
		});
		controler.run();
	}
	
	// alternative variant:
	public static void main2() {
		String configFilename = "examples/equil/config.xml";
		Controler controler = new Controler(configFilename);

		// this uses "addOVERRIDINGModule".  It thus uses the Controler defaults, and overrides them or adds to them.
		controler.addOverridingModule( new AbstractModule(){
			@Override public void install() {
				this.bind( TravelTime.class ).toInstance( new FreeSpeedTravelTime() );
			}
		});
	}
}
