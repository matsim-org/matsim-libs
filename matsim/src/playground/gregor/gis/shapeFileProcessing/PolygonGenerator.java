/* *********************************************************************** *
 * project: org.matsim.*
 * PolygonGenerator.java
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

package playground.gregor.gis.shapeFileProcessing;

import java.util.Collection;

import org.geotools.data.FeatureSource;
import org.geotools.feature.Feature;

public class PolygonGenerator {

	private Collection<Feature> graph;
	private FeatureSource featuresSource;

	public PolygonGenerator(Collection<Feature> graph, FeatureSource featureSource) {
		this.graph = graph;
		this.featuresSource = featureSource;
		
	}

}
