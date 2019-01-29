/* *********************************************************************** *
 * project: org.matsim.*
 * SocialNetworkReader.java
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
package org.matsim.contrib.socnetsim.framework.population;

import java.util.Stack;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;

/**
 * @author thibautd
 */
public class SocialNetworkReader extends MatsimXmlParser {
	private final Scenario scenario;
	private SocialNetwork socialNetwork;

	private boolean isReflective = false;

	private final String elementName;

	public SocialNetworkReader() {
		this( null );
	}

	public SocialNetworkReader(final Scenario scenario) {
		this.scenario = scenario;
		this.elementName = SocialNetwork.ELEMENT_NAME;
	}

	public SocialNetworkReader(
			final String elementName,
			final Scenario scenario) {
		this.scenario = scenario;
		this.elementName = elementName;
	}

	public SocialNetwork read( final String file ) {
		readFile( file );
		return this.socialNetwork;
	}

	@Override
	public void startTag(
			final String name,
			final Attributes atts,
			final Stack<String> context) {
		if ( name.equals( SocialNetworkWriter.ROOT_TAG ) ) {
			this.isReflective = Boolean.parseBoolean( atts.getValue( SocialNetworkWriter.REFLECTIVE_ATT ) );
			this.socialNetwork = new SocialNetworkImpl( this.isReflective );
			if ( this.scenario != null ) {
				// this dates back from pre-Guice times, and could hopefully be removed. Needs tests.
				this.scenario.addScenarioElement( elementName, this.socialNetwork );
			}
		}
		else if ( name.equals( SocialNetworkWriter.EGO_TAG ) ) {
			final Id<Person> ego = Id.create(
						atts.getValue(
							SocialNetworkWriter.EGO_ATT ) , Person.class );
			this.socialNetwork.addEgo( ego );
		}
		else if ( name.equals( SocialNetworkWriter.TIE_TAG ) ) {
			final Id<Person> ego = Id.create(
						atts.getValue(
							SocialNetworkWriter.EGO_ATT ) , Person.class );
			final Id<Person> alter = Id.create(
						atts.getValue(
							SocialNetworkWriter.ALTER_ATT ) , Person.class );
			if ( this.isReflective ) this.socialNetwork.addBidirectionalTie( ego , alter );
			else this.socialNetwork.addMonodirectionalTie( ego , alter );
		}
		else if ( name.equals( SocialNetworkWriter.ATTRIBUTE_TAG ) ) {
			this.socialNetwork.addMetadata(
					atts.getValue( SocialNetworkWriter.NAME_ATT ),
					atts.getValue( SocialNetworkWriter.VALUE_ATT ) );
		}
	}

	@Override
	public void endTag(
			final String name,
			final String content,
			final Stack<String> context) {}

}

