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
package playground.thibautd.parknride;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;

import playground.thibautd.router.controler.MultiLegRoutingControler;

/**
 * Executable to run a simulation with park and ride
 * @author thibautd
 */
public class Run {
	public static void main(final String[] args) {
		String configFile = args[0];

		Config config = ConfigUtils.createConfig();
		ParkAndRideUtils.setConfigGroup( config );
		ConfigUtils.loadConfig( config , configFile );

		Scenario scenario = ParkAndRideUtils.loadScenario( config );

		MultiLegRoutingControler controler = new MultiLegRoutingControler( scenario );
		controler.run();

		String outputFacilities =
			controler.getControlerIO().getOutputFilename( "output_pnrFacilities.xml.gz" );
		(new ParkAndRideFacilitiesXmlWriter( ParkAndRideUtils.getParkAndRideFacilities( scenario ) )).write( outputFacilities );
	}
}

