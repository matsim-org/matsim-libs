/* *********************************************************************** *
 * project: org.matsim.*
 * Household.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.balmermi.teleatlas;

import com.vividsolutions.jts.geom.Coordinate;


public class JcElement {

	public Long id;

	public static enum JcFeatureType {
		JUNCTION, RAILWAY
	};
	public JcFeatureType featType;

	public static enum JunctionType {
		JUNCTION, BIFURCATION, RAILWAY_CROSSING, COUNTRY_BORDER_CROSSING,
		TRAIN_FERRY_CROSSING, INTERNAL_DATASET_BORDER_CROSSING
	};
	public JunctionType juntype;

	public Coordinate c;

	//////////////////////////////////////////////////////////////////////
	// print method
	//////////////////////////////////////////////////////////////////////

	@Override
	public final String toString() {
		StringBuffer str = new StringBuffer();
		str.append(this.getClass().getSimpleName()); str.append(':');
		str.append("id="); str.append(id); str.append(';');
		str.append("featType="); str.append(featType); str.append(';');
		str.append("juntype="); str.append(juntype); str.append(';');
		str.append("c="); str.append(c); str.append(';');
		return str.toString();
	}
}
