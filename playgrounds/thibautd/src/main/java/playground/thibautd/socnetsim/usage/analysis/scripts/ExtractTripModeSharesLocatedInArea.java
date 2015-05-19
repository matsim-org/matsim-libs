/* *********************************************************************** *
 * project: org.matsim.*
 * ExtractTripModeSharesLocatedInArea.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.thibautd.socnetsim.usage.analysis.scripts;

import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;

import playground.ivt.utils.AcceptAllFilter;
import playground.ivt.utils.ArgParser;
import playground.ivt.utils.ArgParser.Args;
import playground.ivt.utils.Filter;
import playground.ivt.utils.SubpopulationFilter;

import playground.thibautd.socnetsim.usage.analysis.LocatedTripsWriter;

/**
 * @author thibautd
 */
public class ExtractTripModeSharesLocatedInArea {
	public static void main(final String[] args) {
		final ArgParser parser = new ArgParser();

		parser.setDefaultValue( "-p" , null );
		parser.setDefaultValue( "-o" , null );
		// optionnal
		parser.setDefaultValue( "-a" , null );
		parser.setDefaultValue( "-s" , null );
		parser.setDefaultValue( "--att-name" , new PlansConfigGroup().getSubpopulationAttributeName() );

		main( parser.parseArgs( args ) );
	}

	public static void main(final Args args) {
		final String inPopFile = args.getValue( "-p" );
		final String outRawData = args.getValue( "-o" );
		final String personAttributes = args.getValue( "-a" );
		final String subpopulation = args.getValue( "-s" );
		final String attName = args.getValue( "--att-name" );

		final Filter<Person> filter =
			personAttributes != null ?
				new SubpopulationFilter(
						readAttributes( personAttributes ),
						attName,
						subpopulation ).getPersonVersion() :
				new AcceptAllFilter<Person>();

		LocatedTripsWriter.write(
				filter,
				inPopFile,
				outRawData );
	}

	private static ObjectAttributes readAttributes(final String personAttributes) {
		final ObjectAttributes atts = new ObjectAttributes();
		new ObjectAttributesXmlReader( atts ).parse( personAttributes );
		return atts;
	}
}

