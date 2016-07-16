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
package playground.kai.run;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.run.NetworkCleaner;

/**
 * @author nagel
 *
 */
public class KNNetworkToNetwork {
	private final static Logger log = Logger.getLogger( KNNetworkToNetwork.class );

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Config config = ConfigUtils.createConfig();
		
		config.network().setInputFile("/Users/nagel/kairuns/tampa/inputs/tbrpmcarbus.xml.gz");
		
		Network network1 = ScenarioUtils.loadScenario(config).getNetwork() ;
		
		Collection<Link> toRemove = new ArrayList<Link>() ;
		
		for ( Link link : network1.getLinks().values() ) {
			if ( link.getCapacity() == 11000 ) {
				log.warn( " presumably found connector" ) ;
				toRemove.add(link) ;
			}
			if ( link.getFreespeed() < 20. ) {
				link.setFreespeed( link.getFreespeed()/2. );
			}
		}
		
		for ( Link link : toRemove ) {
			network1.removeLink( link.getId() ) ;
		}
		
		new org.matsim.core.network.algorithms.NetworkCleaner().run(network1);
		
		new NetworkWriter(network1).write("/Users/nagel/tmp/net.xml.gz");
		
	}

}
