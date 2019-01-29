/* *********************************************************************** *
 * project: org.matsim.*
 * AttributesBuilder.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package org.matsim.testcases.utils;

import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.AttributesImpl;

/**
 * A simple builder class to create SAX-{@link Attributes} for manually testing
 * SAX Parsers, like all parsers inheriting from {@link MatsimXmlParser}.
 *
 * <h3>Advised Usage:</h3>
 * <pre>
 * MyParser parser = new MyParser();
 * Attributes atts = new AttributesBuilder().add("key1", "value1").add("k2", "v2").get();
 * parser.startTag("myTag", atts, context);
 * // test that the tag and its attributes were correctly handled
 * // ...
 * </pre>
 *
 * @author mrieser
 */
public class AttributesBuilder {
	private final static Attributes emptyAtts = new AttributesImpl();
	private final AttributesImpl atts = new AttributesImpl();

	public AttributesBuilder add(final String key, final String value) {
		this.atts.addAttribute(null, null, key, null, value);
		return this;
	}

	public Attributes get() {
		return this.atts;
	}

	public static Attributes getEmpty() {
		return emptyAtts;
	}
}
