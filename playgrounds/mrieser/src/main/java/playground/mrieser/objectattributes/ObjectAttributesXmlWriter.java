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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.MatsimXmlWriter;

/**
 * Writes object attributes to a file.
 *
 * @author mrieser
 */
public class ObjectAttributesXmlWriter extends MatsimXmlWriter {

	/*package*/ final static String TAG_OBJECT_ATTRIBUTES = "objectAttributes";
	/*package*/ final static String TAG_OBJECT = "object";
	/*package*/ final static String TAG_ATTRIBUTE = "attribute";
	/*package*/ final static String ATTR_OBJECTID = "id";
	/*package*/ final static String ATTR_ATTRIBUTENAME = "name";
	/*package*/ final static String ATTR_ATTRIBUTECLASS = "class";

	private final ObjectAttributes attributes;

	public ObjectAttributesXmlWriter(final ObjectAttributes attributes) {
		this.attributes = attributes;
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
				objAttributes.put(objAttribute.getKey(),objAttribute.getValue());
			}
			// write attributes
			for (Map.Entry<String, Object> objAttribute : objAttributes.entrySet()) {
				xmlAttributes.add(super.createTuple(ATTR_ATTRIBUTENAME, objAttribute.getKey()));
				xmlAttributes.add(super.createTuple(ATTR_ATTRIBUTECLASS, objAttribute.getValue().getClass().getCanonicalName()));
				writeStartTag(TAG_ATTRIBUTE, xmlAttributes);
				xmlAttributes.clear();
				writeContent(objAttribute.getValue().toString(), false);
				writeEndTag(TAG_ATTRIBUTE);
			}
			writeEndTag(TAG_OBJECT);
		}
		writeEndTag(TAG_OBJECT_ATTRIBUTES);
		close();
	}

}
