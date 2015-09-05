/* *********************************************************************** *
 * project: org.matsim.*
 * DesiresConverter.java
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.population.Desires;
import org.matsim.utils.objectattributes.AttributeConverter;

/**
 * Allows to dump desires to ObjectAttributes
 * @author thibautd
 */
public class DesiresXmlLikeConverter implements AttributeConverter<Desires> {

	@Override
	public Desires convert(final String value) {
		try {
			final Document doc = new SAXBuilder().build( new ByteArrayInputStream( value.getBytes() ) );
			final Desires desires = new Desires(
					doc.getRootElement().getAttribute( "desc" ) == null ? null :
					doc.getRootElement().getAttribute( "desc" ).getValue() );

			for ( Object option : doc.getRootElement().getChildren( "actDur" ) ) {
				final Element optionElem = (Element) option;
				desires.putActivityDuration(
						optionElem.getAttribute( "type" ).getValue(),
						optionElem.getAttribute( "dur" ).getDoubleValue() );
			}

			return desires;
		} catch (JDOMException e) {
			throw new RuntimeException( e );
		} catch (IOException e) {
			throw new UncheckedIOException( e );
		}
	}

	@Override
	public String convertToString(final Object o) {
		final Desires desires = (Desires) o;
		final Document document = new Document();
		document.setRootElement( new Element( "desires" ) );
		if ( desires.getDesc() != null ) {
			document.getRootElement().setAttribute( "desc" , desires.getDesc() );
		}

		for ( Map.Entry<String, Double> entry : desires.getActivityDurations().entrySet() ) {
			final Element element = new Element( "actDur" );
			document.getRootElement().addContent( element );

			element.setAttribute( "type" , entry.getKey() );
			element.setAttribute( "dur" , entry.getValue().toString() );
		}

		return new XMLOutputter().outputString( document );
	}
}

