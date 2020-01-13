/* *********************************************************************** *
 * project: org.matsim.*
 * FixedGroupsIdentifierFileParser.java
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
package org.matsim.contrib.socnetsim.framework.replanning.grouping;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author thibautd
 */
public class FixedGroupsIdentifierFileParser {
	public static FixedGroupsIdentifier readCliquesFile(final String fileName) {
		return new FixedGroupsIdentifierFileParser().readFile( fileName );
	}

	private static XMLStreamReader getStreamReader(final String fileName) {
		final XMLInputFactory xmlif = XMLInputFactory.newInstance();
		try {
			final InputStream stream = IOUtils.getInputStream(fileName);
			return xmlif.createXMLStreamReader( stream );
		}
		catch (Exception e) {
			throw new ParsingException( e );
		}
	}

	protected  FixedGroupsIdentifier parse(
			final XMLStreamReader streamReader) throws XMLStreamException {
		final Counter counter = new Counter( "parsing group # " );
		final List<Collection<Id<Person>>> groups = new ArrayList<>();

		List<Id<Person>> currentGroup = new ArrayList<>();
		groups.add( currentGroup );
		while ( streamReader.hasNext() ) {
			if ( streamReader.next() != XMLStreamConstants.START_ELEMENT ) continue;

			if ( streamReader.getLocalName().equals( "clique" ) && 
					currentGroup.size() > 0 ) {
				counter.incCounter();
				currentGroup = new ArrayList<>();
				groups.add( currentGroup );
			}

			if ( streamReader.getLocalName().equals( "person" ) ) {
				currentGroup.add( parseId( streamReader , Person.class ) );
			}
		}
		counter.printCounter();

		return new FixedGroupsIdentifier( groups );
	}

	private static <T> Id<T> parseId(final XMLStreamReader streamReader, final Class<T> idType ) {
		if ( streamReader.getAttributeCount() != 1 ) {
			throw new ParsingException( "unexpected attribute count "+streamReader.getAttributeCount() );
		}

		if ( !streamReader.getAttributeLocalName( 0 ).equals( "id" ) ) {
			throw new ParsingException( "unexpected attribute name "+streamReader.getAttributeLocalName( 0 ) );
		}

		return Id.create( streamReader.getAttributeValue( 0 ).intern() , idType);
	}

	protected FixedGroupsIdentifier readFile(final String fileName) {
		final XMLStreamReader streamReader = getStreamReader( fileName );
		try {
			return parse( streamReader );
		}
		catch (XMLStreamException e) {
			throw new ParsingException( e );
		}
		finally {
			try {
				streamReader.close();
			} catch (XMLStreamException e) {
				throw new ParsingException( e );
			}
		}
	}

	public static final class ParsingException extends RuntimeException {
		private static final long serialVersionUID = 1L;

		public ParsingException(final String m) {
			super( m );
		}

		public ParsingException(final Throwable e) {
			super( e );
		}


		public ParsingException(final String m, final Throwable e) {
			super( m , e );
		}
	}
}

