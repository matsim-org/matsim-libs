/* *********************************************************************** *
 * project: org.matsim.*
 * GenerateFacilitiesOnAllLinks.java
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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOption;
import org.matsim.facilities.FacilitiesWriter;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Arrays;

/**
 * @author thibautd
 */
public class GenerateFacilitiesOnAllLinks {
	public static void main(final String[] args) throws IOException {
		final String netfile = args[ 0 ];
		final String outfacilities = args[ 1 ];
		final String outf2l = args[ 2 ];
		final String[] acttypes = Arrays.copyOfRange( args , 3 , args.length );

		final Scenario scenario = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		new MatsimNetworkReader(scenario.getNetwork()).readFile( netfile );
		final BufferedWriter f2l = IOUtils.getBufferedWriter( outf2l );
		f2l.write( "fid\tlid" );

		for ( Link l : scenario.getNetwork().getLinks().values() ) {
			final ActivityFacility fac =
				scenario.getActivityFacilities().getFactory().createActivityFacility(
						Id.create(l.getId().toString(), ActivityFacility.class),
						l.getCoord(), l.getId() );
			f2l.newLine();
			f2l.write( l.getId()+"\t"+l.getId() );

			scenario.getActivityFacilities().addActivityFacility( fac );

			for ( String type : acttypes ) {
				final ActivityOption option =
					scenario.getActivityFacilities().getFactory().createActivityOption( type );
				// no open times
				fac.addActivityOption( option );
			}
		}

		f2l.close();

		new FacilitiesWriter( scenario.getActivityFacilities() ).write( outfacilities );
	}
}

