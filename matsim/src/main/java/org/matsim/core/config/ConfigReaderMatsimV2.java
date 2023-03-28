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
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.stream.Collectors;

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

	private final Map<String, List<ConfigAlias>> aliases = new HashMap<>();
	private final Deque<ConfigGroup> moduleStack = new ArrayDeque<>();

	ConfigReaderMatsimV2(final Config config) {
		this.config = config;
		this.addDefaultAliases();
	}

	public void addDefaultAliases() {
		// when renaming config modules and parameter names, add them here as aliases:
//		this.addAlias("controler", "controller");
//		this.addAlias("planCalcScore", "scoring");
//		this.addAlias("planscalcroute", "routing");
	}

	public void addAlias(String oldName, String newName, String... path) {
		this.aliases.computeIfAbsent(oldName, k -> new ArrayList<>(2)).add(new ConfigAlias(oldName, newName, path));
	}

	public void clearAliases() {
		this.aliases.clear();
	}

	private String resolveAlias(String oldName) {
		List<ConfigAlias> definedAliases = this.aliases.get(oldName);
		if (definedAliases == null || definedAliases.isEmpty()) {
			return oldName;
		}
		for (ConfigAlias alias: definedAliases) {
			boolean matches = true;

			if (alias.path.length > this.moduleStack.size()) {
				matches = false;
			} else {
				Iterator<ConfigGroup> iter = this.moduleStack.iterator();
				for (int i = alias.path.length - 1; i >= 0; i--) {
					if (iter.hasNext()) {
						String name = iter.next().getName();
						if (!name.equals(alias.path[i])) {
							matches = false;
							break;
						}
					} else {
						matches = false;
						break;
					}
				}
			}

			if (matches) {
				String stack = this.moduleStack.stream().map(c -> c.getName()).collect(Collectors.joining(" < ", oldName + " < ", " /"));
				LOG.warn("Config name '{}' is deprecated, please use '{}' instead. Config path: {}", oldName, alias.newName, stack);
				return alias.newName;
			}
		}
		return oldName;
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
		String name = resolveAlias(atts.getValue(NAME));
		moduleStack.getFirst().addParam(
				name,
				atts.getValue( VALUE ) );
	}

	private void startParameterSet(final Attributes atts) {
		String type = resolveAlias(atts.getValue(TYPE));
		final ConfigGroup m = moduleStack.getFirst().createParameterSet( type );
		moduleStack.addFirst( m );
	}

	private void startModule(final Attributes atts) {
		String name = resolveAlias(atts.getValue(NAME));
		final ConfigGroup m = config.getModule(name);
		moduleStack.addFirst(
				m == null ?
				config.createModule(name) :
				m );
	}

	@Override
	public void endTag(
			final String name,
			final String content,
			final Stack<String> context) {
		if ( name.equals( MODULE ) || name.equals( PARAMETER_SET ) ) {
			final ConfigGroup head = moduleStack.removeFirst();

			if ( !moduleStack.isEmpty() ) moduleStack.getFirst().addParameterSet( head );
		}
	}

	public record ConfigAlias(String oldName, String newName, String[] path) {
	}

}

