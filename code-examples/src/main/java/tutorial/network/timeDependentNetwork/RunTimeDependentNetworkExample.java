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
package tutorial.network.timeDependentNetwork;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkChangeEvent.ChangeType;
import org.matsim.core.network.NetworkChangeEvent.ChangeValue;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author nagel
 *
 */
public class RunTimeDependentNetworkExample {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Config config = ConfigUtils.loadConfig( args[0] ) ;
		config.network().setTimeVariantNetwork(true);
		
		// ---
		
		Scenario scenario = ScenarioUtils.loadScenario(config) ;
		
		// ---

		for ( Link link : scenario.getNetwork().getLinks().values() ) {
			double speed = link.getFreespeed() ;
			final double threshold = 5./3.6;
			if ( speed > threshold ) {
				{
					NetworkChangeEvent event = new NetworkChangeEvent(7.*3600.) ;
					event.setFreespeedChange(new ChangeValue( ChangeType.ABSOLUTE_IN_SI_UNITS,  threshold/10 ));
					event.addLink(link);
					final NetworkChangeEvent event1 = event;
					NetworkUtils.addNetworkChangeEvent(((Network)scenario.getNetwork()),event1);
				}
				{
					NetworkChangeEvent event = new NetworkChangeEvent(11.5*3600.) ;
					event.setFreespeedChange(new ChangeValue( ChangeType.ABSOLUTE_IN_SI_UNITS,  speed ));
					event.addLink(link);
					final NetworkChangeEvent event1 = event;
					NetworkUtils.addNetworkChangeEvent(((Network)scenario.getNetwork()),event1);
				}
			}
		}
		
		// ---
		
		Controler controler = new Controler( scenario ) ;
		
		controler.run() ;
	
	}

}
