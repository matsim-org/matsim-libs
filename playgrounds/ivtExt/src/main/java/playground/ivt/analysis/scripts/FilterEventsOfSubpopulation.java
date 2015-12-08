/* *********************************************************************** *
 * project: org.matsim.*
 * FilterEventsOfDefaultSubpopulation.java
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
package playground.ivt.analysis.scripts;

import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;
import playground.ivt.utils.ArgParser;
import playground.ivt.utils.ArgParser.Args;
import playground.ivt.utils.SubpopulationFilteringEventsManager;

/**
 * @author thibautd
 */
public class FilterEventsOfSubpopulation {
	public static void main(final String[] args) {
		final ArgParser parser = new ArgParser();
		parser.setDefaultValue( "-a" , "--person-attributes" , null );
		parser.setDefaultValue( "-e" , "--input-events" , null );
		parser.setDefaultValue( "-o" , "--output-events" , null );

		// optionnal: if none, default subpopulation
		parser.setDefaultValue( "-s" , "--subpopulation-name" , null );
		main( parser.parseArgs( args ) );
	}

	public static void main(final Args args) {
		final String personAttributesFile = args.getValue( "-a" );
		final String inputEventsFile = args.getValue( "-e" );
		final String outputEventsFile = args.getValue( "-o" );
		final String subpopulationName = args.getValue( "-s" );

		final ObjectAttributes atts = new ObjectAttributes();
		new ObjectAttributesXmlReader( atts ).parse( personAttributesFile );

		final EventWriterXML writer = new EventWriterXML( outputEventsFile );
		final SubpopulationFilteringEventsManager events =
			new SubpopulationFilteringEventsManager( atts , subpopulationName );
		events.addHandler( writer );

		new EventsReaderXMLv1( events ).parse( inputEventsFile );

		writer.closeFile();
	}
}

