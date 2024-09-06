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
package org.matsim.core.config;

import static org.matsim.core.config.ConfigV2XmlNames.MODULE;
import static org.matsim.core.config.ConfigV2XmlNames.NAME;
import static org.matsim.core.config.ConfigV2XmlNames.PARAMETER;
import static org.matsim.core.config.ConfigV2XmlNames.PARAMETER_SET;
import static org.matsim.core.config.ConfigV2XmlNames.TYPE;
import static org.matsim.core.config.ConfigV2XmlNames.VALUE;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Stack;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;

/**
 * @author thibautd
 * @author mrieser (alias-functionality)
 */
class ConfigReaderMatsimV2 extends MatsimXmlParser {
	private final static Logger LOG = LogManager.getLogger(ConfigReaderMatsimV2.class);

	private final Config config;

	private final ConfigAliases aliases;
	private final Deque<ConfigGroup> moduleStack = new ArrayDeque<>();
	private final Deque<String> pathStack = new ArrayDeque<>();

	ConfigReaderMatsimV2(final Config config) {
		super(ValidationType.DTD_ONLY);
		this.config = config;
		this.aliases = new ConfigAliases();
	}

	ConfigReaderMatsimV2(final Config config, final ConfigAliases aliases) {
		super(ValidationType.DTD_ONLY);
		this.config = config;
		this.aliases = aliases;
	}

	public ConfigAliases getConfigAliases() {
		return this.aliases;
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
			throw new IllegalArgumentException( "unknown tag "+name );
		}
	}

	private void startParameter(final Attributes atts) {
		String name = this.aliases.resolveAlias(atts.getValue(NAME), this.pathStack);
		this.moduleStack.getFirst().addParam(
				name,
				atts.getValue( VALUE ) );
	}

	private void startParameterSet(final Attributes atts) {
		String type = this.aliases.resolveAlias(atts.getValue(TYPE), this.pathStack);
		final ConfigGroup m = this.moduleStack.getFirst().createParameterSet( type );
		this.moduleStack.addFirst(m);
		this.pathStack.addFirst(m.getName());
	}

	private void startModule(final Attributes atts) {
		String name = this.aliases.resolveAlias(atts.getValue(NAME), this.pathStack);
		ConfigGroup m = this.config.getModule(name);
		if (m == null) {
			m = this.config.createModule(name);
		}
		this.moduleStack.addFirst(m);
		this.pathStack.addFirst(m.getName());
	}

	@Override
	public void endTag(
			final String name,
			final String content,
			final Stack<String> context) {
		if ( name.equals( MODULE ) || name.equals( PARAMETER_SET ) ) {
			final ConfigGroup head = this.moduleStack.removeFirst();
			this.pathStack.removeFirst();

			if ( !this.moduleStack.isEmpty() ) this.moduleStack.getFirst().addParameterSet( head );
		}
	}

}

