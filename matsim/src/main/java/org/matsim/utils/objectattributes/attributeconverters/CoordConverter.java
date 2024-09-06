
/* *********************************************************************** *
 * project: org.matsim.*
 * CoordConverter.java
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

 package org.matsim.utils.objectattributes.attributeconverters;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.utils.objectattributes.AttributeConverter;

public class CoordConverter implements AttributeConverter<Coord> {
	private final Logger log = LogManager.getLogger(CoordConverter.class);

	@Override
	public Coord convert(String value) {
		String s = value.replace("(", "");
		s = s.replace(")", "");
		String[] sa = s.split(";");
		return new Coord(Double.parseDouble(sa[0]), Double.parseDouble(sa[1]));
	}

	@Override
	public String convertToString(Object o) {
		if(!(o instanceof Coord)){
			log.error("Object is not of type Coord: " + o.getClass().toString());
			return null;
		}
		Coord c = (Coord)o;

		return String.format("(%s;%s)", Double.toString(c.getX()), Double.toString(c.getY()));
	}

}
