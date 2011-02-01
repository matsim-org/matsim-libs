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

package playground.mrieser.objectattributes.attributeconverters;

import playground.mrieser.objectattributes.AttributeConverter;

/**
 * @author mrieser
 */
public class BooleanConverter implements AttributeConverter<Boolean> {
	@Override
	public Boolean convert(String value) {
		return Boolean.valueOf(value);
	}

	@Override
	public String convertToObject(Boolean b) {
		return b.toString();
	}
}