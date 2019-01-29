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
package org.matsim.roadpricing.run;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.roadpricing.RoadPricingConfigGroup;
import org.matsim.roadpricing.RoadPricingModule;
import org.matsim.roadpricing.RoadPricingSchemeUsingTollFactor;
import org.matsim.roadpricing.TollFactor;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

/**
 * Illustrative example how to first read a "base" toll file, and then make it dependent on the vehicle type.
 * <br/><br/>
 * Not tested.
 * 
 * @author nagel
 *
 */
public class RunRoadPricingUsingTollFactorExample {

	public static void main(String[] args) {

		// load the config from file:
		Config config = ConfigUtils.loadConfig(args[0]) ;
		
		// "materialize" the road pricing config group:
		RoadPricingConfigGroup rpConfig = ConfigUtils.addOrGetModule(config, RoadPricingConfigGroup.class) ;
		
		// load the scenario from file.  This will _not_ load the road pricing file, since it is not part of the main distribution.
		final Scenario scenario = ScenarioUtils.loadScenario(config) ;
		
		// define the toll factor as an anonymous class.  If more flexibility is needed, convert to "full" class.
		TollFactor tollFactor = new TollFactor(){
			@Override public double getTollFactor(Id<Person> personId, Id<Vehicle> vehicleId, Id<Link> linkId, double time) {
				VehicleType someVehicleType = null ; // --> replace by something meaningful <-- 
				if ( scenario.getVehicles().getVehicles().get( vehicleId ).getType().equals( someVehicleType ) ) {
					return 2 ;
				} else {
					return 1 ; 
				}
			}
		} ;
		
		// instantiate the road pricing scheme, with the toll factor inserted:
		RoadPricingSchemeUsingTollFactor scheme = new RoadPricingSchemeUsingTollFactor(rpConfig.getTollLinksFile(), tollFactor) ;
		
		// instantiate the control(l)er:
		Controler controler = new Controler( scenario ) ;
		
		// add the road pricing module, with our scheme from above inserted:
		controler.addOverridingModule( new RoadPricingModule( scheme ) ) ;
		
		// run everything:
		controler.run();
		
	}

}
