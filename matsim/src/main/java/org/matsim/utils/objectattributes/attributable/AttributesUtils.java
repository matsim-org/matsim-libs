
/* *********************************************************************** *
 * project: org.matsim.*
 * AttributesUtils.java
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

/**
 * @author thibautd
 */
public class AttributesUtils {
	public static final String ATTRIBUTES = "attributes";
	public static final String ATTRIBUTE = "attribute";

	/**
	 * Adds the mappings from "from" to "to". Nothing is done to copy the Object themselves,
	 * which should be fine for 99.9% of the usecases of Attributes (value objects)
	 */
	public static void copyTo( Attributes from , Attributes to ) {
		for (var entry : from.getAsMap().entrySet()) {
			to.putAttribute(entry.getKey(), entry.getValue());
		}
	}

	/**
	 * Adds the mappings from "from" to "to". Nothing is done to copy the Object
	 * themselves, which should be fine for 99.9% of the usecases of Attributes
	 * (value objects).
	 * 
	 * @param from
	 * @param to
	 * @param exceptAttribute does not copy attribute with that name
	 */
	public static void copyToExcept(Attributes from, Attributes to, String exceptAttribute) {
		for (var entry : from.getAsMap().entrySet()) {
			String key = entry.getKey();
			if (!key.equals(exceptAttribute)) {
				to.putAttribute(key, entry.getValue());
			}
		}
	}

	/**
	 * Adds the mappings from "from" to "to". Nothing is done to copy the Object themselves,
	 * which should be fine for 99.9% of the usecases of Attributes (value objects)
	 */
	public static <T extends Attributable> void copyAttributesFromTo( T from , T to ) {
		copyTo( from.getAttributes() , to.getAttributes() );
	}

	/**
	 * Adds the mappings from "from" to "to". Nothing is done to copy the Object
	 * themselves, which should be fine for 99.9% of the usecases of Attributes
	 * (value objects).
	 * 
	 * @param from
	 * @param to
	 * @param exceptAttribute does not copy attribute with that name
	 */
	public static <T extends Attributable> void copyAttributesFromToExcept(T from, T to, String exceptAttribute) {
		copyToExcept(from.getAttributes(), to.getAttributes(), exceptAttribute);
	}

	/**
	 * @param attributes collection of attributes
	 * @return <code>true</code> if the attributes collection does not contain any attribute
	 * @deprecated use {@link Attributes#isEmpty()} instead
	 */
	@Deprecated
	public static boolean isEmpty(Attributes attributes) {
		return attributes.size() == 0;
	}
}
