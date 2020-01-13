
/* *********************************************************************** *
 * project: org.matsim.*
 * AttributesXmlReaderDelegate.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

 package org.matsim.utils.objectattributes.attributable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.apache.log4j.Logger;
import org.matsim.core.gbl.Gbl;
import org.matsim.utils.objectattributes.AttributeConverter;
import org.matsim.utils.objectattributes.ObjectAttributesConverter;
import org.matsim.utils.objectattributes.attributeconverters.BooleanConverter;
import org.matsim.utils.objectattributes.attributeconverters.DoubleConverter;
import org.matsim.utils.objectattributes.attributeconverters.FloatConverter;
import org.matsim.utils.objectattributes.attributeconverters.IntegerConverter;
import org.matsim.utils.objectattributes.attributeconverters.LongConverter;
import org.matsim.utils.objectattributes.attributeconverters.StringConverter;

/**
 * This class is meant to be used as a delegate by any reader that reads an {@link Attributable} object
 * @author thibautd
 */
public class AttributesXmlReaderDelegate {
	private final static Logger log = Logger.getLogger(AttributesXmlReaderDelegate.class);
	private final ObjectAttributesConverter converter = new ObjectAttributesConverter();

	private Attributes currentAttributes = null;
	private String currentAttribute = null;
	private String currentAttributeClass = null;

	/*package*/ final static String TAG_ATTRIBUTES = "attributes";
	/*package*/ final static String TAG_ATTRIBUTE = "attribute";
	/*package*/ final static String ATTR_ATTRIBUTENAME = "name";
	/*package*/ final static String ATTR_ATTRIBUTECLASS = "class";

	public void startTag(String name,
						 org.xml.sax.Attributes atts,
						 Stack<String> context,
						 Attributes currentAttributes ) {
		if (TAG_ATTRIBUTE.equals(name)) {
			this.currentAttribute = atts.getValue(ATTR_ATTRIBUTENAME);
			this.currentAttributeClass = atts.getValue(ATTR_ATTRIBUTECLASS);
		} else if (TAG_ATTRIBUTES.equals(name)) {
			this.currentAttributes = currentAttributes;
		}
	}

	public void endTag(String name, String content, Stack<String> context) {
		if (TAG_ATTRIBUTE.equals(name)) {
			Object o = converter.convert(this.currentAttributeClass, content);
			if (o == null) return;
			Gbl.assertNotNull( this.currentAttributes );
			this.currentAttributes.putAttribute( this.currentAttribute, o);
		}
	}

	public Attributes getCurrentAttributes() {
		return currentAttributes;
	}

	/**
	 * Sets the converter for reading attributes of the specified class.
	 *
	 * @param clazz
	 * @param converter
	 * @return the previously registered converter for this class, or <code>null</code> if none was set before.
	 */
	public AttributeConverter<?> putAttributeConverter(final Class<?> clazz, final AttributeConverter<?> converter) {
		return this.converter.putAttributeConverter(clazz, converter);
	}

	public void putAttributeConverters( final Map<Class<?>, AttributeConverter<?>> converters ) {
		for ( Map.Entry<Class<?>, AttributeConverter<?>> e : converters.entrySet() ) {
			putAttributeConverter( e.getKey() , e.getValue() );
		}
	}

	/**
	 * Removes the converter for reading attributes of the specified class.
	 *
	 * @param clazz
	 * @return the previously registered converter for this class, of <code>null</code> if none was set.
	 */
	public AttributeConverter<?> removeAttributeConverter(final Class<?> clazz) {
		return this.converter.removeAttributeConverter(clazz);
	}
}
