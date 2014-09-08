/* *********************************************************************** *
 * project: org.matsim.*
 * NonFlatConfigReader.java
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
package playground.thibautd.config;

import static playground.thibautd.config.NonFlatConfigXmlNames.MODULE;
import static playground.thibautd.config.NonFlatConfigXmlNames.NAME;
import static playground.thibautd.config.NonFlatConfigXmlNames.PARAMETER;
import static playground.thibautd.config.NonFlatConfigXmlNames.PARAMETER_SET;
import static playground.thibautd.config.NonFlatConfigXmlNames.TYPE;
import static playground.thibautd.config.NonFlatConfigXmlNames.VALUE;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Stack;

import org.matsim.core.config.Config;
import org.matsim.core.config.Module;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;

/**
 * @author thibautd
 */
public class NonFlatConfigReader extends MatsimXmlParser {
	private final Config config;

	private final Deque<Module> moduleStack = new ArrayDeque<Module>();

	public NonFlatConfigReader(
			final Config config) {
		this.config = config;
	}

	@Override
	public void startTag(
			final String name,
			final Attributes atts,
			final Stack<String> context) {
		if ( name.equals( MODULE ) ) {
			startModule(atts);
		}
		else if ( name.equals( PARAMETER_SET ) ) {
			startParameterSet(atts);
		}
		else if ( name.equals( PARAMETER ) ) {
			startParameter(atts);
		}
		else if ( !name.equals( "config" ) ) {
			// this is the job of the dtd validation,
			// but better too much safety than too little...
			throw new IllegalArgumentException( "unkown tag "+name );
		}
	}

	private void startParameter(final Attributes atts) {
		// TODO check if necessary for some default modules
		// (it is the job of the Module to decide on the meaning of the
		// "null" string, not the reader...
		if ( !atts.getValue( VALUE ).equalsIgnoreCase( "null" ) ) {
			moduleStack.getFirst().addParam(
					atts.getValue( NAME ),
					atts.getValue( VALUE ) );
		}
	}

	private void startParameterSet(final Attributes atts) {
		final Module m = moduleStack.getFirst().createAndAddParameterSet( atts.getValue( TYPE ) );
		moduleStack.addFirst( m );
	}

	private void startModule(final Attributes atts) {
		final Module m = config.getModule( atts.getValue( NAME ) );
		moduleStack.addFirst(
				m == null ?
				config.createModule( atts.getValue( NAME ) ) :
				m );
	}

	@Override
	public void endTag(
			final String name,
			final String content,
			final Stack<String> context) {
		if ( name.equals( MODULE ) || name.equals( PARAMETER_SET ) ) {
			moduleStack.removeFirst();
		}
	}
}

