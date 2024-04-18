/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package org.matsim.utils.objectattributes.attributeconverters;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.matsim.utils.objectattributes.AttributeConverter;


/**
 * @author mrieser
 */
public class StringConverter implements AttributeConverter<String> {
	private final Map<String, String> stringCache = new ConcurrentHashMap<>(1000);

	@Override
	public String convert(String value) {
		return stringCache.computeIfAbsent(value, k -> k);
	}

	@Override
	public String convertToString(Object o) {
		return (String) o;
	}
}
