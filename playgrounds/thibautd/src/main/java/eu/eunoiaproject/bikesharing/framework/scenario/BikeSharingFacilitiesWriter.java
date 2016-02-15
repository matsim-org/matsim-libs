/* *********************************************************************** *
 * project: org.matsim.*
 * BikeSharingFacilitiesWriter.java
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
package eu.eunoiaproject.bikesharing.framework.scenario;

import org.apache.log4j.Logger;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.MatsimXmlWriter;
import org.matsim.core.utils.misc.Counter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Writes a {@link BikeSharingFacilities} container to a file.
 * @author thibautd
 */
public class BikeSharingFacilitiesWriter extends MatsimXmlWriter {
	private static final Logger log =
		Logger.getLogger(BikeSharingFacilitiesWriter.class);

	private final BikeSharingFacilities facilities;

	public BikeSharingFacilitiesWriter( final BikeSharingFacilities facilities ) {
		this.facilities = facilities;
	}

	public void write(final String fileName) {
		log.info( "writing bike sharing facilities in file "+fileName );
		openFile( fileName );
		writeXmlHead();
		writeDoctype( "bikeSharingFacilities" , "bikesharingfacilities_v1.dtd" );
		writeStartTag( "bikeSharingFacilities" , Collections.<Tuple<String, String>>emptyList() );

		if ( !facilities.getMetadata().isEmpty() ) {
			writeStartTag( "metadata" , Collections.<Tuple<String, String>>emptyList() );
			for ( Map.Entry<String, String> meta : facilities.getMetadata().entrySet() ) {
				writeStartTag(
						"attribute",
						Arrays.asList(
							createTuple( "name" , meta.getKey() ),
							createTuple( "value" , meta.getValue() ) ),
						true);
			}
			writeEndTag( "metadata" );
		}

		// this has the side effect of jumping a line, making the output more readable
		writeContent( "" , true );

		final Counter counter = new Counter( "writing bike sharing facility # " );
		for ( BikeSharingFacility f : facilities.getFacilities().values() ) {
			counter.incCounter();
			writeFacility( f );
		}
		counter.printCounter();

		writeEndTag( "bikeSharingFacilities" );
		close();
	}

	private void writeFacility(final BikeSharingFacility f) {
		final List< Tuple<String, String> > atts = new ArrayList< Tuple<String, String> >();

		atts.add( createTuple( "id" , f.getId().toString() ) );
		if ( f.getLinkId() != null) atts.add( createTuple( "linkId" , f.getLinkId().toString() ) );
		atts.add( createTuple( "x" , f.getCoord().getX() ) );
		atts.add( createTuple( "y" , f.getCoord().getY() ) );
		atts.add( createTuple( "capacity" , f.getCapacity() ) );
		atts.add( createTuple( "initialNumberOfBikes" , f.getInitialNumberOfBikes() ) );

		writeStartTag(
				"bikeSharingFacility",
				atts,
				true );
	}
}

