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

import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;

import playground.ivt.utils.SubpopulationFilteringEventsManager;

/**
 * @author thibautd
 */
public class FilterEventsOfDefaultSubpopulation {
	public static void main(final String[] args) {
		final String personAttributesFile = args[ 0 ];
		final String inputEventsFile = args[ 1 ];
		final String outputEventsFile = args[ 2 ];

		final ObjectAttributes atts = new ObjectAttributes();
		new ObjectAttributesXmlReader( atts ).parse( personAttributesFile );

		final EventWriterXML writer = new EventWriterXML( outputEventsFile );
		final SubpopulationFilteringEventsManager events =
			new SubpopulationFilteringEventsManager( atts );
		events.addHandler( writer );

		new EventsReaderXMLv1( events ).parse( inputEventsFile );

		writer.closeFile();
	}
}

