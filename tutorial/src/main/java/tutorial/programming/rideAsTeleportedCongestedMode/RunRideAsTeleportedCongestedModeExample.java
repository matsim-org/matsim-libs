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
package tutorial.programming.rideAsTeleportedCongestedMode;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.router.NetworkRouting;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * Uses the congested car router also for the routing of the ride mode.  By <i>not</i> making ride a mobsim main mode, 
 * ride will now be teleported in the mobsim, i.e. it will use the travel time and travel distance written into the route by the router.
 * 
 * @author nagel
 */
public class RunRideAsTeleportedCongestedModeExample {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Config config = ConfigUtils.loadConfig( args[0] ) ;
		
		List<String> modes = new ArrayList<>() ;
		modes.add( TransportMode.car ) ;
		config.qsim().setMainModes( modes ) ;
		
		Scenario scenario = ScenarioUtils.loadScenario( config ) ;
		
		Controler controler = new Controler( scenario ) ;
		
		// tell the system to use the congested car router for the ride mode:
		controler.addOverridingModule(new AbstractModule(){
			@Override public void install() {
				this.addRoutingModuleBinding( TransportMode.ride ).toProvider(new NetworkRouting( TransportMode.car ))  ;
			}
		});
		
		controler.run();
	}

}
