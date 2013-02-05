/* *********************************************************************** *
 * project: org.matsim.*
 * AbstractParsePullXmlReader.java
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
package playground.thibautd.utils;

import java.io.InputStream;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.matsim.core.utils.io.IOUtils;

/**
 * @author thibautd
 */
public abstract class AbstractParsePullXmlReader<T> {
	protected abstract T parse( XMLStreamReader streamReader ) throws XMLStreamException;

	protected T readFile(final String fileName) {
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

	private static XMLStreamReader getStreamReader(final String fileName) {
		final XMLInputFactory xmlif = XMLInputFactory.newInstance();
		try {
			final InputStream stream = IOUtils.getInputStream( fileName );
			return xmlif.createXMLStreamReader( stream );
		}
		catch (Exception e) {
			throw new ParsingException( e );
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

