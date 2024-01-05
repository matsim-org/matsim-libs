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

import java.io.UncheckedIOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.google.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.MatsimXmlWriter;

/**
 * Writes object attributes to a file.
 *
 * @author mrieser
 */
public class ObjectAttributesXmlWriter extends MatsimXmlWriter {

	private final static Logger log = LogManager.getLogger(ObjectAttributesXmlWriter.class);

	/*package*/ final static String TAG_OBJECT_ATTRIBUTES = "objectAttributes";
	/*package*/ final static String TAG_OBJECT = "object";
	/*package*/ final static String TAG_ATTRIBUTE = "attribute";
	/*package*/ final static String ATTR_OBJECTID = "id";
	/*package*/ final static String ATTR_ATTRIBUTENAME = "name";
	/*package*/ final static String ATTR_ATTRIBUTECLASS = "class";



	private final ObjectAttributes attributes;
	private final ObjectAttributesConverter converter = new ObjectAttributesConverter();

	public ObjectAttributesXmlWriter(final ObjectAttributes attributes) {
		this.attributes = attributes;
	}

	public void writeFile(final String filename) throws UncheckedIOException {
		openFile(filename);
		writeXmlHead();
		writeDoctype(TAG_OBJECT_ATTRIBUTES, "http://matsim.org/files/dtd/objectattributes_v1.dtd");
		writeStartTag(TAG_OBJECT_ATTRIBUTES, null);
		List<Tuple<String, String>> xmlAttributes = new LinkedList<Tuple<String, String>>();
		for (Map.Entry<String, Map<String, Object>> entry : this.attributes.attributes.entrySet()) {
			xmlAttributes.add(super.createTuple(ATTR_OBJECTID, entry.getKey()));
			writeStartTag(TAG_OBJECT, xmlAttributes);
			xmlAttributes.clear();
			// sort attributes by name
			Map<String, Object> objAttributes = new TreeMap<String, Object>();
			objAttributes.putAll(entry.getValue());
			// write attributes
			for (Map.Entry<String, Object> objAttribute : objAttributes.entrySet()) {
				Class<?> clazz = objAttribute.getValue().getClass();
				String value = converter.convertToString(objAttribute.getValue());
				if (value != null) {
					xmlAttributes.add(super.createTuple(ATTR_ATTRIBUTENAME, objAttribute.getKey()));
					xmlAttributes.add(super.createTuple(ATTR_ATTRIBUTECLASS, clazz.getCanonicalName()));
					writeStartTag(TAG_ATTRIBUTE, xmlAttributes);
					xmlAttributes.clear();
					writeContent(value, false);
					writeEndTag(TAG_ATTRIBUTE);
				}
			}
			writeEndTag(TAG_OBJECT);
		}
		writeEndTag(TAG_OBJECT_ATTRIBUTES);
		close();
	}

	/**
	 * Sets the converter for reading attributes of the specified class.
	 *
	 * @param clazz
	 * @param converter
	 * @return the previously registered converter for this class, or <code>null</code> if none was set before.
	 */
	public AttributeConverter putAttributeConverter(final Class<?> clazz, final AttributeConverter converter) {
		return this.converter.putAttributeConverter(clazz, converter);
	}

	@Inject
	public void putAttributeConverters( final Map<Class<?>, AttributeConverter<?>> converters ) {
		this.converter.putAttributeConverters(converters);
	}

	/**
	 * Removes the converter for reading attributes of the specified class.
	 *
	 * @param clazz
	 * @return the previously registered converter for this class, of <code>null</code> if none was set.
	 */
	public AttributeConverter removeAttributeConverter(final Class<?> clazz) {
		return this.converter.removeAttributeConverter(clazz);
	}

}
