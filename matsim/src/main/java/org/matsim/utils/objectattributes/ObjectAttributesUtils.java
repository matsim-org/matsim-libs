/* *********************************************************************** *
 * project: org.matsim.*
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

package org.matsim.utils.objectattributes;

import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * @author mrieser / Senozon AG
 */
public final class ObjectAttributesUtils {

	private ObjectAttributesUtils() {
		// abstract helper class
	}
	
	public static void copyAllAttributes(ObjectAttributes source, ObjectAttributes destination, String objectId) {
		Map<String, Object> sAttrs = source.attributes.get(objectId);
		if (sAttrs != null) {
			Map<String, Object> dAttrs = destination.attributes.get(objectId);
			if (dAttrs == null) {
				dAttrs = new IdentityHashMap<String, Object>();
				destination.attributes.put(objectId, dAttrs);
			}
			dAttrs.putAll(sAttrs);
		}
	}
	
	public static Collection<String> getAllAttributeNames(ObjectAttributes attributes, final String objectId) {
		Map<String, Object> map = attributes.attributes.get(objectId);
		if (map == null) {
			return Collections.emptyList();
		}
		return Collections.unmodifiableCollection(map.keySet());
	}

}
