/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.johannes.gsv.zones;

import com.vividsolutions.jts.geom.Geometry;
import playground.johannes.synpop.gis.Zone;

/**
 * @author johannes
 *
 */
public class NUTSZone extends Zone {
	
	public static final String NUTS_CODE_KEY = "nutsCode";
	
	public static final String NAME_KEY = "name";
	
	public NUTSZone(Geometry geometry) {
		super(geometry);
	}

	public String getCode() {
		return getAttribute(NUTS_CODE_KEY);
	}
	
	public String getName() {
		return getAttribute(NAME_KEY);
	}
}
