/* *********************************************************************** *
 * project: org.matsim.*
 * ParkAndRideFacilitiesXmlReader.java
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
package playground.thibautd.parknride;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.core.utils.misc.Counter;
import org.matsim.facilities.ActivityFacility;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.xml.sax.Attributes;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import static playground.thibautd.parknride.ParkAndRideFacilitiesXmlSchemaNames.FACILITY_TAG;
import static playground.thibautd.parknride.ParkAndRideFacilitiesXmlSchemaNames.ID_ATT;
import static playground.thibautd.parknride.ParkAndRideFacilitiesXmlSchemaNames.LINK_ID_ATT;
import static playground.thibautd.parknride.ParkAndRideFacilitiesXmlSchemaNames.NAME_ATT;
import static playground.thibautd.parknride.ParkAndRideFacilitiesXmlSchemaNames.ROOT_TAG;
import static playground.thibautd.parknride.ParkAndRideFacilitiesXmlSchemaNames.STOP_TAG;
import static playground.thibautd.parknride.ParkAndRideFacilitiesXmlSchemaNames.X_COORD_ATT;
import static playground.thibautd.parknride.ParkAndRideFacilitiesXmlSchemaNames.Y_COORD_ATT;

/**
 * reads an xml file containing PnR facilities.
 * @author thibautd
 */
public class ParkAndRideFacilitiesXmlReader extends MatsimXmlParser {
	private ParkAndRideFacilities facilities = null;
	private FacilityBuilder currentBuilder = null;
	private Counter counter = null;

	public ParkAndRideFacilitiesXmlReader() {
		// do not validate
		super( false );
	}

	@Override
	public void startTag(
			final String name,
			final Attributes atts,
			final Stack<String> context) {
		if (name.equals( ROOT_TAG )) {
			facilities = new ParkAndRideFacilities( atts.getValue( NAME_ATT ) );
			counter = new Counter( "reading park and ride facility # " );
		}
		else if (name.equals( FACILITY_TAG )) {
			counter.incCounter();
			currentBuilder = new FacilityBuilder();
			currentBuilder.coord = getCoord( atts );
			currentBuilder.id = getId( atts , ID_ATT , ActivityFacility.class );
			currentBuilder.linkId = getId( atts , LINK_ID_ATT , Link.class );
		}
		else if (name.equals( STOP_TAG )) {
			currentBuilder.stops.add( getId( atts , ID_ATT , TransitStopFacility.class ) );
		}
	}

	private static final Coord getCoord( final Attributes atts ) {
		final String x = atts.getValue( X_COORD_ATT );
		final String y = atts.getValue( Y_COORD_ATT );

		return new Coord(Double.parseDouble(x), Double.parseDouble(y));
	}

	private static final <T> Id<T> getId( final Attributes atts , final String qName , final Class<T> idType ) {
		return Id.create( atts.getValue( qName ) , idType );
	}

	@Override
	public void endTag(
			final String name,
			final String content,
			final Stack<String> context) {
		if (name.equals( FACILITY_TAG )) {
			facilities.addFacility( currentBuilder.create() );
			currentBuilder = null;
		}
		if (name.equals( ROOT_TAG )) {
			counter.printCounter();
			counter = null;
		}
	}

	public ParkAndRideFacilities getFacilities() {
		return facilities;
	}
}

class FacilityBuilder {
	public Coord coord = null;
	public Id<ActivityFacility> id = null;
	public Id<Link> linkId = null;
	public final List<Id<TransitStopFacility>> stops = new ArrayList<Id<TransitStopFacility>>();

	public ParkAndRideFacility create() {
		return new ParkAndRideFacility( id , coord , linkId , stops);
	}
}
