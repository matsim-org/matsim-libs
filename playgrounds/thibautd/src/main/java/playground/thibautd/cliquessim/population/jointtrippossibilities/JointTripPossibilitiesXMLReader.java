/* *********************************************************************** *
 * project: org.matsim.*
 * JointTripPossibilitiesXMLReader.java
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

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;

import playground.thibautd.cliquessim.population.jointtrippossibilities.JointTripPossibilities.Od;
import playground.thibautd.cliquessim.population.jointtrippossibilities.JointTripPossibilities.Possibility;
import static playground.thibautd.cliquessim.population.jointtrippossibilities.JointTripPossibilitiesXMLNames.*;

/**
 * @author thibautd
 */
public class JointTripPossibilitiesXMLReader extends MatsimXmlParser {
	private final IdFactory ids = new IdFactory();
	private JointTripPossibilities possibilities;

	public JointTripPossibilitiesXMLReader() {
		super( false );
	}

	@Override
	public void startTag(
			final String name,
			final Attributes atts,
			final Stack<String> context) {
		if ( name.equals( ROOT_TAG ) ) {
			possibilities = new JointTripPossibilities( atts.getValue( DESC_ATT ) );
		}
		else if (name.equals( POSS_TAG ) ) {
			Id d = ids.create( atts.getValue( DRIVER_ID_ATT ).trim() );
			Id dor = ids.create( atts.getValue( DRIVER_OR_ATT ).trim() );
			Id ddest = ids.create( atts.getValue( DRIVER_DEST_ATT ).trim() );
			Id p = ids.create( atts.getValue( PASSENGER_ID_ATT ).trim() );
			Id por = ids.create( atts.getValue( PASSENGER_OR_ATT ).trim() );
			Id pdest = ids.create( atts.getValue( PASSENGER_DEST_ATT ).trim() );

			possibilities.add(
					new Possibility(
						d,
						Od.create( dor , ddest ),
						p,
						Od.create( por , pdest )) );
		}
	}

	@Override
	public void endTag(
			final String name,
			final String content,
			final Stack<String> context) {
		// nothing to do
	}

	public JointTripPossibilities getJointTripPossibilities() {
		return possibilities;
	}

	private static class IdFactory {
		private final Map<String, Id> ids = new HashMap<String, Id>();

		public Id create( final String s ) {
			Id id = ids.get( s );

			if (id == null) {
				id = new IdImpl( s );
				ids.put( s , id );
			}

			return id;
		}
	}
}

