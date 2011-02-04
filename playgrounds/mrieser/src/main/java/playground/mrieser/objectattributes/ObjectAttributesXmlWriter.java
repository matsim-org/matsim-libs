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

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.MatsimXmlWriter;

import playground.mrieser.objectattributes.attributeconverters.BooleanConverter;
import playground.mrieser.objectattributes.attributeconverters.DoubleConverter;
import playground.mrieser.objectattributes.attributeconverters.IntegerConverter;
import playground.mrieser.objectattributes.attributeconverters.StringConverter;

/**
 * Writes object attributes to a file.
 *
 * @author mrieser
 */
public class ObjectAttributesXmlWriter extends MatsimXmlWriter {

	private final static Logger log = Logger.getLogger(ObjectAttributesXmlWriter.class);

	/*package*/ final static String TAG_OBJECT_ATTRIBUTES = "objectAttributes";
	/*package*/ final static String TAG_OBJECT = "object";
	/*package*/ final static String TAG_ATTRIBUTE = "attribute";
	/*package*/ final static String ATTR_OBJECTID = "id";
	/*package*/ final static String ATTR_ATTRIBUTENAME = "name";
	/*package*/ final static String ATTR_ATTRIBUTECLASS = "class";

	private static final StringConverter STRING_Converter = new StringConverter();
	private static final IntegerConverter INTEGER_Converter = new IntegerConverter();
	private static final DoubleConverter DOUBLE_Converter = new DoubleConverter();
	private static final BooleanConverter BOOLEAN_Converter = new BooleanConverter();

	private final ObjectAttributes attributes;
	private final Map<String, AttributeConverter<? extends Object>> converters = new HashMap<String, AttributeConverter<? extends Object>>();
	private final Set<Class<?>> missingConverters = new HashSet<Class<?>>();

	public ObjectAttributesXmlWriter(final ObjectAttributes attributes) {
		this.attributes = attributes;
		this.converters.put(String.class.getCanonicalName(), STRING_Converter);
		this.converters.put(Integer.class.getCanonicalName(), INTEGER_Converter);
		this.converters.put(Double.class.getCanonicalName(), DOUBLE_Converter);
		this.converters.put(Boolean.class.getCanonicalName(), BOOLEAN_Converter);
	}

	public void writeFile(final String filename) throws IOException {
		openFile(filename);
		writeXmlHead();
		writeStartTag(TAG_OBJECT_ATTRIBUTES, null);
		List<Tuple<String, String>> xmlAttributes = new LinkedList<Tuple<String, String>>();
		for (Map.Entry<String, Map<String, Object>> entry : this.attributes.attributes.entrySet()) {
			xmlAttributes.add(super.createTuple(ATTR_OBJECTID, entry.getKey()));
			writeStartTag(TAG_OBJECT, xmlAttributes);
			xmlAttributes.clear();
			// sort attributes by name
			Map<String,Object> objAttributes = new TreeMap<String, Object>();
			for (Map.Entry<String, Object> objAttribute : entry.getValue().entrySet()) {
				objAttributes.put(objAttribute.getKey(), objAttribute.getValue());
			}
			// write attributes
			for (Map.Entry<String, Object> objAttribute : objAttributes.entrySet()) {
				Class<?> clazz = objAttribute.getValue().getClass();
				AttributeConverter<?> conv = this.converters.get(clazz.getCanonicalName());
				if (conv != null) {
					xmlAttributes.add(super.createTuple(ATTR_ATTRIBUTENAME, objAttribute.getKey()));
					xmlAttributes.add(super.createTuple(ATTR_ATTRIBUTECLASS, clazz.getCanonicalName()));
					writeStartTag(TAG_ATTRIBUTE, xmlAttributes);
					xmlAttributes.clear();
					writeContent(objAttribute.getValue().toString(), false);
					writeEndTag(TAG_ATTRIBUTE);
				} else {
					if (!missingConverters.contains(clazz)) {
						missingConverters.add(clazz);
						log.info("No AttributeConverter found for class " + clazz.getCanonicalName() + ". Not all attribute values will be written.");
					}
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

}
