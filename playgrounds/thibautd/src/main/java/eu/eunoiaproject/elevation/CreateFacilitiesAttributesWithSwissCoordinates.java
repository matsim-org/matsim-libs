/* *********************************************************************** *
 * project: org.matsim.*
 * CreateFacilitiesAttributesWithSwissCoordinates.java
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
package eu.eunoiaproject.elevation;

import java.io.BufferedWriter;
import java.io.IOException;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.api.experimental.facilities.Facility;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.transformations.CH1903LV03toWGS84;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.facilities.MatsimFacilitiesReader;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

import playground.ivt.utils.ArgParser;
import playground.ivt.utils.ArgParser.Args;

/**
 * @author thibautd
 */
public class CreateFacilitiesAttributesWithSwissCoordinates {
	public static void main(final String[] args) {
		final ArgParser parser = new ArgParser();
		parser.setDefaultValue( "-a" , "--altitude-file" , null );
		parser.setDefaultValue( "-f" , "--facilities-file" , null );
		parser.setDefaultValue( "-o" , "--output-attributes" , null );
		parser.setDefaultValue( "-xy" , "--output-xy" , null );
		main( parser.parseArgs( args ) );
	}

	private static void main(final Args args) {
		final String altitudeFile = args.getValue( "-a" );
		final String facilitiesFile = args.getValue( "-f" );
		final String outputFile = args.getValue( "-o" );
		final String outputXy = args.getValue( "-xy" );

		final GridElevationProvider elevation = ArcInfoASCIIElevationReader.read( altitudeFile );
		elevation.setFacilityToGridCoordTransformation( new CH1903LV03toWGS84() );

		final Scenario sc = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		new MatsimFacilitiesReader( sc ).readFile( facilitiesFile );

		final ActivityFacilities facilities = sc.getActivityFacilities();
		for ( Facility f : facilities.getFacilities().values() ) {
			facilities.getFacilityAttributes().putAttribute(
					f.getId().toString(),
					"elevation",
					elevation.getAltitude( f.getCoord() ) );
		}

		new ObjectAttributesXmlWriter( facilities.getFacilityAttributes() ).writeFile( outputFile );
		try {
			if ( outputXy != null ) writeXy( facilities , outputXy );
		}
		catch (IOException e) {
			throw new UncheckedIOException( e );
		}
	}

	private static void writeXy(
			final ActivityFacilities facilities,
			final String outputXy) throws IOException {
		final BufferedWriter writer = IOUtils.getBufferedWriter( outputXy );

		writer.write( "facilityId\tx\ty\talt" );

		for ( Facility f : facilities.getFacilities().values() ) {
			writer.newLine();
			writer.write( f.getId().toString() );
			writer.write( "\t" );
			writer.write( f.getCoord().getX()+"\t"+f.getCoord().getY() );
			writer.write( "\t"+
				facilities.getFacilityAttributes().getAttribute(
						f.getId().toString(),
						"elevation" ) );
		}

		writer.close();
	}
}

