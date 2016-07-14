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

import java.io.BufferedWriter;
import java.io.IOException;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;

public class KNNetwork4Gnuplot {

	public static void main(String[] args) {
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(args[0]);

		Network network ;
		{
			Scenario scenario1 = ScenarioUtils.loadScenario( config ) ;
			network = scenario1.getNetwork() ;
		}
		
		BufferedWriter writer = IOUtils.getBufferedWriter("net.txt") ;
		for ( Link link : network.getLinks().values() ) {
			if ( link.getCapacity() > 3000. ) {
				try {
					writer.write( link.getFromNode().getCoord().getX() + "\t" + link.getFromNode().getCoord().getY() + "\n");
					writer.write( link.getToNode().getCoord().getX() + "\t" + link.getToNode().getCoord().getY() + "\n\n\n" );
				} catch (IOException e) {
					e.printStackTrace();
					throw new RuntimeException("io fail") ;
				} 
			}
		}
		try {
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("io fail") ;
		}
		
	}

}
