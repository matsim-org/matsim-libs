/* *********************************************************************** *
 * project: org.matsim.*
 * BikeSharingFacilitiesReader.java
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

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.core.utils.misc.Counter;
import org.xml.sax.Attributes;

import java.util.Stack;

/**
 * Fills a {@link BikeSharingFacilities} container, and puts it in a Scenario,
 * under the {@link BikeSharingFacilities#ELEMENT_NAME} name.
 * The container can be retrieved with
 * <tt>
 * BikeSharingFacilities fs = (BikeSharingFacilities) scenario.getScenarioElement( BikeSharingFacilities.ELEMENT_NAME );
 * </tt>
 * @author thibautd
 */
public class BikeSharingFacilitiesReader extends MatsimXmlParser {
	private final Scenario scenario;

	private BikeSharingFacilities facilities;
	private Counter counter;

	public BikeSharingFacilitiesReader(final Scenario scenario) {
		this.scenario = scenario;
	}

	@Override
	public void startTag(
			final String name,
			final Attributes atts,
			final Stack<String> context) {
		if ( name.equals( "bikeSharingFacilities" ) ) {
			facilities = new BikeSharingFacilities();
			counter = new Counter( "reading bike sharing facility # " );
		}
		if ( name.equals( "bikeSharingFacility" ) ) {
			counter.incCounter();
			final String idString = atts.getValue( "id" );
			final String x = atts.getValue( "x" );
			final String y = atts.getValue( "y" );
			final String linkId = atts.getValue( "linkId" );
			final String capacity = atts.getValue( "capacity" );
			final String initialNumberOfBikes = atts.getValue( "initialNumberOfBikes" );

			facilities.addFacility(
					facilities.getFactory().createBikeSharingFacility(
						Id.create( idString, BikeSharingFacility.class ),
							new Coord(Double.parseDouble(x), Double.parseDouble(y)),
						Id.create( linkId, Link.class ),
						Integer.parseInt( capacity ),
						Integer.parseInt( initialNumberOfBikes ) ) );
		}
		if ( name.equals( "attribute" ) ) {
			facilities.addMetadata( atts.getValue( "name" ) , atts.getValue( "value" ) );
		}
	}

	@Override
	public void endTag(
			final String name,
			final String content,
			final Stack<String> context) {
		if ( name.equals( "bikeSharingFacilities" ) ) {
			counter.printCounter();
			scenario.addScenarioElement( BikeSharingFacilities.ELEMENT_NAME, facilities );
		}
	}

}

