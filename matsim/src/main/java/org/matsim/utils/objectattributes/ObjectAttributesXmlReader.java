/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package org.matsim.utils.objectattributes;

import static org.matsim.utils.objectattributes.ObjectAttributesXmlWriter.ATTR_ATTRIBUTECLASS;
import static org.matsim.utils.objectattributes.ObjectAttributesXmlWriter.ATTR_ATTRIBUTENAME;
import static org.matsim.utils.objectattributes.ObjectAttributesXmlWriter.ATTR_OBJECTID;
import static org.matsim.utils.objectattributes.ObjectAttributesXmlWriter.TAG_ATTRIBUTE;
import static org.matsim.utils.objectattributes.ObjectAttributesXmlWriter.TAG_OBJECT;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import com.google.inject.Inject;
import org.apache.log4j.Logger;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.utils.objectattributes.attributeconverters.BooleanConverter;
import org.matsim.utils.objectattributes.attributeconverters.DoubleConverter;
import org.matsim.utils.objectattributes.attributeconverters.FloatConverter;
import org.matsim.utils.objectattributes.attributeconverters.IntegerConverter;
import org.matsim.utils.objectattributes.attributeconverters.LongConverter;
import org.matsim.utils.objectattributes.attributeconverters.StringConverter;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * Reads object attributes from a file. The reader supports attributes of type {@link String},
 * {@link Integer}, {@link Double} and {@link Boolean} out of the box. Other types must be manually
 * supported by implementing {@link AttributeConverter} and adding it to the reader with
 * {@link #putAttributeConverter(Class, AttributeConverter)}.
 *
 * @author mrieser
 */
public class ObjectAttributesXmlReader extends MatsimXmlParser {
	private final static Logger log = Logger.getLogger(ObjectAttributesXmlReader.class);
	private final Map<String, AttributeConverter<?>> converters = new HashMap<String, AttributeConverter<?>>();
	private final ObjectAttributes attributes;
	private boolean readCharacters = false;
	private String currentObject = null;
	private String currentAttribute = null;
	private String currentAttributeClass = null;
	private long count = 0;

	private static final StringConverter STRING_Converter = new StringConverter();
	private static final IntegerConverter INTEGER_Converter = new IntegerConverter();
	private static final FloatConverter FLOAT_Converter = new FloatConverter();
	private static final DoubleConverter DOUBLE_Converter = new DoubleConverter();
	private static final BooleanConverter BOOLEAN_Converter = new BooleanConverter();
	private static final LongConverter LONG_Converter = new LongConverter();

	private final Set<String> missingConverters = new HashSet<String>();

	public ObjectAttributesXmlReader(final ObjectAttributes attributes) {
		this.attributes = attributes;
		super.setValidating(false);
		this.converters.put(String.class.getCanonicalName(), STRING_Converter);
		this.converters.put(Integer.class.getCanonicalName(), INTEGER_Converter);
		this.converters.put(Float.class.getCanonicalName(), FLOAT_Converter);
		this.converters.put(Double.class.getCanonicalName(), DOUBLE_Converter);
		this.converters.put(Boolean.class.getCanonicalName(), BOOLEAN_Converter);
		this.converters.put(Long.class.getCanonicalName(), LONG_Converter);
	}

	@Override
	public void startTag(String name, Attributes atts, Stack<String> context) {
		if (TAG_ATTRIBUTE.equals(name)) {
			this.currentAttribute = atts.getValue(ATTR_ATTRIBUTENAME);
			this.currentAttributeClass = atts.getValue(ATTR_ATTRIBUTECLASS);
			this.readCharacters = true;
		} else if (TAG_OBJECT.equals(name)) {
			this.currentObject = atts.getValue(ATTR_OBJECTID);
		}
	}

	@Override
	public void endTag(String name, String content, Stack<String> context) {
		if (TAG_ATTRIBUTE.equals(name)) {
			this.readCharacters = false;
			AttributeConverter<?> c = this.converters.get(this.currentAttributeClass);
			if (c == null) {
				if (missingConverters.add(this.currentAttributeClass)) {
					log.warn("No AttributeConverter found for class " + this.currentAttributeClass + ". Not all attribute values can be read.");
				}
			} else {
				Object o = this.converters.get(this.currentAttributeClass).convert(content);
				this.attributes.putAttribute(this.currentObject, this.currentAttribute, o);
			}
		} else if (TAG_OBJECT.equals(name)) {
			if (this.count % 100000 == 0) {
				log.info("reading object #" + this.count);
			}
			this.count++;
			this.currentObject = null;
		}
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		if (this.readCharacters) {
			super.characters(ch, start, length);
		}
		// ignore characters to prevent OutOfMemoryExceptions
		/* non-validating files contain empty tags with attributes,
		 * but without the dtd or schema, all whitespace between tags is handled
		 * by characters and added up by super.characters, consuming huge
		 * amount of memory when large files are read in.
		 */
	}

	/**
	 * Sets the converter for reading attributes of the specified class.
	 *
	 * @param clazz
	 * @param converter
	 * @return the previously registered converter for this class, or <code>null</code> if none was set before.
	 */
	public AttributeConverter<?> putAttributeConverter(final Class<?> clazz, final AttributeConverter<?> converter) {
		return this.converters.put(clazz.getCanonicalName(), converter);
	}

	@Inject
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
		return this.converters.remove(clazz.getCanonicalName());
	}

}
