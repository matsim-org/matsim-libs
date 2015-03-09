/* *********************************************************************** *
 * project: org.matsim.*
 * WriteZurichScenarioF2L.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.thibautd.scripts.scenariohandling;

import java.io.BufferedWriter;
import java.io.IOException;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.facilities.ActivityFacility;

import playground.ivt.matsim2030.Matsim2030Utils;

/**
 * @author thibautd
 */
public class WriteZurichScenarioF2L {
	public static void main( final String[] args ) throws UncheckedIOException, IOException {
		final String configFile = args[ 0 ];
		final String outf2l = args[ 1 ];

		final Config config = Matsim2030Utils.loadConfig( configFile );
		final Scenario scenario = Matsim2030Utils.loadScenario( config );

		try ( final BufferedWriter writer = IOUtils.getBufferedWriter( outf2l ) ) {
			writer.write( "fid\tlid" );

			for ( ActivityFacility f : scenario.getActivityFacilities().getFacilities().values() ) {
				writer.newLine();
				writer.write( f.getId()+"\t"+f.getLinkId() );
			}
		}
	}
}

