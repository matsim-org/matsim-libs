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

package playground.mrieser.objectattributes;

import static playground.mrieser.objectattributes.ObjectAttributesXmlWriter.ATTR_ATTRIBUTECLASS;
import static playground.mrieser.objectattributes.ObjectAttributesXmlWriter.ATTR_ATTRIBUTENAME;
import static playground.mrieser.objectattributes.ObjectAttributesXmlWriter.ATTR_OBJECTID;
import static playground.mrieser.objectattributes.ObjectAttributesXmlWriter.TAG_ATTRIBUTE;
import static playground.mrieser.objectattributes.ObjectAttributesXmlWriter.TAG_OBJECT;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.MatsimXmlParser;
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
	private final Map<String, AttributeConverter> converters = new HashMap<String, AttributeConverter>();
	private final ObjectAttributes attributes;
	private boolean readCharacters = false;
	private String currentObject = null;
	private String currentAttribute = null;
	private String currentAttributeClass = null;
	private long count = 0;

	public ObjectAttributesXmlReader(final ObjectAttributes attributes) {
		this.attributes = attributes;
		super.setValidating(false);
		this.converters.put(String.class.getCanonicalName(), new StringConverter());
		this.converters.put(Integer.class.getCanonicalName(), new IntegerConverter());
		this.converters.put(Double.class.getCanonicalName(), new DoubleConverter());
		this.converters.put(Boolean.class.getCanonicalName(), new BooleanConverter());
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
			Object o = this.converters.get(this.currentAttributeClass).convert(content);
			this.attributes.putAttribute(this.currentObject, this.currentAttribute, o);
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
	public AttributeConverter putAttributeConverter(final Class<?> clazz, final AttributeConverter converter) {
		return this.converters.put(clazz.getCanonicalName(), converter);
	}

	/**
	 * Removes the converter for reading attributes of the specified class.
	 *
	 * @param clazz
	 * @return the previously registered converter for this class, of <code>null</code> if none was set.
	 */
	public AttributeConverter removeAttributeConverter(final Class<?> clazz) {
		return this.converters.remove(clazz.getCanonicalName());
	}

	/**
	 * Converts a read attribute, given as a string, into a specific object type.
	 *
	 * @author mrieser
	 */
	public static interface AttributeConverter {
		public Object convert(final String value);
	}

	private static class IntegerConverter implements AttributeConverter {
		@Override
		public Integer convert(String value) {
			return Integer.valueOf(value);
		}
	}
	private static class DoubleConverter implements AttributeConverter {
		@Override
		public Double convert(String value) {
			return Double.valueOf(value);
		}
	}
	private static class StringConverter implements AttributeConverter {
		private final Map<String, String> stringCache = new HashMap<String,  String>(1000);
		@Override
		public String convert(String value) {
			String s = this.stringCache.get(value);
			if (s == null) {
				s = new String(value); // copy, in case 'value' was generated as substring from a larger string
				this.stringCache.put(s, s);
			}
			return s;
		}
	}
	private static class BooleanConverter implements AttributeConverter {
		@Override
		public Boolean convert(String value) {
			return Boolean.valueOf(value);
		}
	}

}
