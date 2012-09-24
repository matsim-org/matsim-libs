/* *********************************************************************** *
 * project: org.matsim.*
 * JointTripPossibilitiesXMLWriter.java
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
package playground.thibautd.cliquessim.population.jointtrippossibilities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.MatsimXmlWriter;

import playground.thibautd.cliquessim.population.jointtrippossibilities.JointTripPossibilities.Possibility;
import static playground.thibautd.cliquessim.population.jointtrippossibilities.JointTripPossibilitiesXMLNames.*;

/**
 * @author thibautd
 */
public class JointTripPossibilitiesXMLWriter extends MatsimXmlWriter {
	private final JointTripPossibilities possibilities;

	public JointTripPossibilitiesXMLWriter(final JointTripPossibilities possibilities) {
		this.possibilities = possibilities;
	}

	public void write(final String file) {
		openFile( file );
		writeXmlHead();
		//doctype?
		writeStartTag(
				ROOT_TAG,
				Arrays.asList( createTuple( DESC_ATT , possibilities.getName() ) ) );
		for (Possibility p : possibilities.getAll()) {
			writeStartTag( POSS_TAG , getAtts( p ) , true );
		}
		writeEndTag( ROOT_TAG );
		close();
	}

	private List<Tuple<String, String>> getAtts(final Possibility p) {
		List<Tuple<String, String>> atts = new ArrayList<Tuple<String, String>>();

		atts.add( createTuple( DRIVER_ID_ATT , p.getDriver() ) );
		atts.add( createTuple( DRIVER_OR_ATT , p.getDriverOd().getOriginLinkId() ) );
		atts.add( createTuple( DRIVER_DEST_ATT , p.getDriverOd().getDestinationLinkId() ) );

		atts.add( createTuple( PASSENGER_ID_ATT , p.getPassenger() ) );
		atts.add( createTuple( PASSENGER_OR_ATT , p.getPassengerOd().getOriginLinkId() ) );
		atts.add( createTuple( PASSENGER_DEST_ATT , p.getPassengerOd().getDestinationLinkId() ) );

		return atts;
	}

	private Tuple<String, String> createTuple(final String name, final Id id) {
		return createTuple( name , id.toString() );
	}
}

