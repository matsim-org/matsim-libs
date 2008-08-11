/* *********************************************************************** *
 * project: org.matsim.*
 * PolygonFeatureGenerator.java
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

package playground.gregor.matsim2GIS;

import org.geotools.feature.Feature;
import org.matsim.network.Link;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class PolygonFeatureGenerator implements FeatureGenerator{

	private final WidthCalculator widthCalculator;
	private final CoordinateReferenceSystem crs;


	public PolygonFeatureGenerator(final WidthCalculator widthCalculator, final CoordinateReferenceSystem crs) {
		this.widthCalculator = widthCalculator;
		this.crs = crs;
	}


	public Feature getFeature(final Link link) {
		double width = this.widthCalculator.getWidth(link);
		width += 0;
		return null;
	}

}
