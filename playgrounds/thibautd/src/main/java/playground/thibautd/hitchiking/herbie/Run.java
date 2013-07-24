/* *********************************************************************** *
 * project: org.matsim.*
 * Run.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.thibautd.hitchiking.herbie;

import herbie.running.config.HerbieConfigGroup;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;

import playground.thibautd.analysis.listeners.ModeAnalysis;
import playground.thibautd.hitchiking.HitchHikingUtils;
import playground.thibautd.hitchiking.spotweights.FrequentationSpotWeighter;

/**
 * @author thibautd
 */
public class Run {
	public static void main(final String[] args) {
		String configFile = args[ 0 ];

		Config config = ConfigUtils.createConfig();
		config.addModule( new HerbieConfigGroup() );
		HitchHikingUtils.loadConfig( config , configFile );
		Scenario sc = HitchHikingUtils.loadScenario( config );

		FrequentationSpotWeighter weighter = new FrequentationSpotWeighter( config );
		Controler controler = new HHHerbieControler( sc , weighter );
		controler.addControlerListener( weighter );
		controler.addControlerListener(new ModeAnalysis( true ));
		controler.run();
	}
}

