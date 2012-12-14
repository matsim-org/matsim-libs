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
package playground.thibautd.socnetsim.replanning.grouping;

import java.io.File;
import java.io.FileInputStream;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.misc.Counter;

/**
 * @author thibautd
 */
public class FixedGroupsIdentifierFileParser {
	public static FixedGroupsIdentifier readCliquesFile(final String fileName) {
		final XMLStreamReader streamReader = getStreamReader( fileName );
		try {
			return parseGroups( streamReader );
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

	private static FixedGroupsIdentifier parseGroups(
			final XMLStreamReader streamReader) throws XMLStreamException {
		final Counter counter = new Counter( "parsing group # " );
		final List<Collection<Id>> groups = new ArrayList<Collection<Id>>();

		List<Id> currentGroup = new ArrayList<Id>();
		groups.add( currentGroup );
		while ( streamReader.hasNext() ) {
			if ( streamReader.next() != XMLStreamConstants.START_ELEMENT ) continue;

			if ( streamReader.getLocalName().equals( "clique" ) && 
					currentGroup.size() > 0 ) {
				counter.incCounter();
				currentGroup = new ArrayList<Id>();
				groups.add( currentGroup );
			}

			if ( streamReader.getLocalName().equals( "person" ) ) {
				currentGroup.add( parseId( streamReader ) );
			}
		}
		counter.printCounter();

		return new FixedGroupsIdentifier( groups );
	}

	private static Id parseId(final XMLStreamReader streamReader) throws XMLStreamException {
		while ( streamReader.hasNext() ) {
			if ( streamReader.next() != XMLStreamConstants.ATTRIBUTE ) {
				throw new ParsingException( "unexpected event type "+streamReader.getEventType() );
			}

			if ( streamReader.getAttributeCount() != 1 ) {
				throw new ParsingException( "unexpected attribute count "+streamReader.getAttributeCount() );
			}

			if ( !streamReader.getAttributeLocalName( 0 ).equals( "id" ) ) {
				throw new ParsingException( "unexpected attribute name "+streamReader.getAttributeLocalName( 0 ) );
			}

			return new IdImpl( streamReader.getAttributeValue( 0 ).intern() );
		}
		throw new ParsingException( "could not find an id" );
	}

	private static XMLStreamReader getStreamReader(final String fileName) {
		final XMLInputFactory xmlif = XMLInputFactory.newInstance();
		final File file = new File( fileName );
		try {
			final FileInputStream stream = new FileInputStream( file );
			return xmlif.createXMLStreamReader( stream );
		}
		catch (Exception e) {
			throw new ParsingException( e );
		}
	}

	public static class ParsingException extends RuntimeException {
		private ParsingException(final String m) {
			super( m );
		}

		private ParsingException(final Exception e) {
			super( e );
		}
	}
}

