/* *********************************************************************** *
 * project: org.matsim.*
 * CompareEuclideanAndLinkLengths.java
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
package playground.thibautd.scripts;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.io.IOUtils;
import playground.ivt.utils.ArgParser;
import playground.ivt.utils.ArgParser.Args;

import java.io.BufferedWriter;
import java.io.IOException;

/**
 * @author thibautd
 */
public class CompareEuclideanAndLinkLengths {
	public static void main(final String[] args) throws IOException {
		final ArgParser parser = new ArgParser();

		parser.setDefaultValue( "-n" , null );
		parser.setDefaultValue( "-o" , null );

		main( parser.parseArgs( args ) );
	}

	public static void main(final Args args) throws IOException {
		final String networkFile = args.getValue( "-n" );
		final String outFile = args.getValue( "-o" );

		final Scenario sc = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		new MatsimNetworkReader(sc.getNetwork()).readFile( networkFile );
		
		final BufferedWriter writer = IOUtils.getBufferedWriter( outFile );
		writer.write( "linkId\tlinkLength\teuclLength" );
		for ( Link l : sc.getNetwork().getLinks().values() ) {
			final double eucl = CoordUtils.calcEuclideanDistance( l.getFromNode().getCoord() , l.getToNode().getCoord() );
			writer.newLine();
			writer.write( l.getId() +"\t"+ l.getLength() +"\t"+ eucl );
		}
		writer.close();
	}
}

