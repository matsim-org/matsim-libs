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

import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.core.api.internal.MatsimExtensionPoint;

/**
 * A simple helper class to store arbitrary attributes (identified by Strings) for
 * arbitrary objects (identified by String-Ids). Note that this implementation uses
 * large amounts of memory for storing many attributes for many objects, it is not
 * heavily optimized.
 * <p></p>
 * <em>This class is not thread-safe.</em>
 * <p></p>
 * More information can be found in the package's Javadoc.
 * <p></p>
 * Example(s):<ul>
 * <li> {@link RunObjectAttributesExample}
 * </ul>
 * @author mrieser
 */
public class ObjectAttributes implements MatsimExtensionPoint {

	/*package*/ Map<String, Map<String, Object>> attributes = new LinkedHashMap<String, Map<String, Object>>(1000);
	
	@Override
	public String toString() {
		StringBuilder stb = new StringBuilder() ;
		for ( Entry<String, Map<String,Object>> entry : attributes.entrySet() ) {
			String key = entry.getKey() ;
			stb.append("key=").append(key);
			Map<String,Object> map = entry.getValue() ;
			for ( Entry<String,Object> ee : map.entrySet() ) {
				String subkey = ee.getKey();
				stb.append("; subkey=").append(subkey);
				stb.append("; object=").append(ee.getValue().toString());
			}
			stb.append("\n") ;
		}
		return stb.toString() ;
	}

	public Object putAttribute(final String objectId, final String attribute, final Object value) {
		Map<String, Object> attMap = this.attributes.get(objectId);
		if (attMap == null) {
			attMap = new IdentityHashMap<String, Object>(5);
			this.attributes.put(objectId, attMap);
		}
		return attMap.put(attribute.intern(), value);
	}

	public Object getAttribute(final String objectId, final String attribute) {
		Map<String, Object> attMap = this.attributes.get(objectId);
		if (attMap == null) {
			return null;
		}
		return attMap.get(attribute.intern());
	}

	public Object removeAttribute(final String objectId, final String attribute) {
		Map<String, Object> attMap = this.attributes.get(objectId);
		if (attMap == null) {
			return null;
		}
		return attMap.remove(attribute.intern());
	}

	public void removeAllAttributes(final String objectId) {
		this.attributes.remove(objectId);
	}

	/**
	 * Deletes all attributes of all objects, and all objects-ids.
	 */
	public void clear() {
		this.attributes.clear();
	}

}
