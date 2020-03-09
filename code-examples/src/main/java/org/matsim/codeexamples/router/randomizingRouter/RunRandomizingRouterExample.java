/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package org.matsim.codeexamples.router.randomizingRouter;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutilityFactory;
import org.matsim.core.scenario.ScenarioUtils;


/**
 * Short script-in-java explaining how to use {@link org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutilityFactory}.  This is now
 * (mar'20) the default anyways, see {@link org.matsim.core.router.costcalculators.TravelDisutilityModule}. So for this particular case the script is not
 * doing anything that is truly necessary.  Leaving it here for reference anyways.
 * <br/>
 * Note that this randomizes the prefactor of the marginal cost of distance.  I (kn) find this quite plausible, since it reflects different utilities of
 * money, e.g. between rich people and poor, or commercial vs private travel.
 *
 * @author nagel
 */
public class RunRandomizingRouterExample {

	public static void main(String[] args) {

		// this is an example script that was never tested!!

		Config config = ConfigUtils.createConfig() ;
		config.controler().setOverwriteFileSetting( OverwriteFileSetting.deleteDirectoryIfExists );
		config.controler().setLastIteration(1);

		config.plansCalcRoute().setRoutingRandomness( 3. );
		// (This is currently the default anyways. kai, mar'20)

		Scenario scenario = ScenarioUtils.createScenario(config) ;

		Controler controler = new Controler( scenario ) ;

		controler.addOverridingModule(new AbstractModule(){
			@Override public void install() {
				addTravelDisutilityFactoryBinding( TransportMode.car ).toInstance(
						new RandomizingTimeDistanceTravelDisutilityFactory( TransportMode.car, config ) );
				// (This is currently the default anyways. kai, mar'20)
			}
		});

		// this sets the routing randomness (currently between time and money only, so be careful
		// that you have a monetary term in the standard disutility, e.g. a distance cost)


		controler.run();

	}

}
