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

import org.matsim.utils.objectattributes.AttributeConverter;

/**
 * @author mrieser / Senozon AG
 */
public class FloatConverter implements AttributeConverter<Float> {
	@Override
	public Float convert(String value) {
		return Float.valueOf(value);
	}

	@Override
	public String convertToString(Object o) {
		return o.toString();
	}
}