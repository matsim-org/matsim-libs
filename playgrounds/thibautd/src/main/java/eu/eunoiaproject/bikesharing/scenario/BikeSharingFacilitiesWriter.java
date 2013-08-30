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
package eu.eunoiaproject.bikesharing.scenario;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.MatsimXmlWriter;

/**
 * @author thibautd
 */
public class BikeSharingFacilitiesWriter extends MatsimXmlWriter {
	private final BikeSharingFacilities facilities;

	public BikeSharingFacilitiesWriter( final BikeSharingFacilities facilities ) {
		this.facilities = facilities;
	}

	public void write(final String fileName) {
		openFile( fileName );
		writeXmlHead();
		writeStartTag( "bikeSharingFacilities" , Collections.<Tuple<String, String>>emptyList() );
		for ( BikeSharingFacility f : facilities.getFacilities().values() ) {
			writeFacility( f );
		}
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

