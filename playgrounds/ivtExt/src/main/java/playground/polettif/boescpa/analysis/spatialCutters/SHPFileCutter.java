/*
 * *********************************************************************** *
 * project: org.matsim.*                                                   *
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
 * *********************************************************************** *
 */

package playground.polettif.boescpa.analysis.spatialCutters;

import com.vividsolutions.jts.geom.Geometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;
import playground.polettif.boescpa.lib.tools.coordUtils.CoordAnalyzer;
import playground.polettif.boescpa.lib.tools.SHPFileUtils;

import java.util.HashSet;
import java.util.Set;

/**
 * Returns for an area specified in an SHP-File if a link is in the area (true) or not (false).
 *
 * @author boescpa
 */
public class SHPFileCutter implements SpatialCutter {

	private final String shpFile;
	private final CoordAnalyzer coordAnalyzer;

	public SHPFileCutter(String shpFile) {
		this.shpFile = shpFile;
		Set<SimpleFeature> features = new HashSet<>();
		SHPFileUtils util = new SHPFileUtils();
		features.addAll(ShapeFileReader.getAllFeatures(shpFile));
		Geometry area = util.mergeGeometries(features);
		coordAnalyzer = new CoordAnalyzer(area);
	}

	public boolean spatiallyConsideringLink(Link link) {
		return coordAnalyzer.isLinkAffected(link);
	}

	public boolean spatiallyConsideringCoord(Coord coord) {
		return coordAnalyzer.isCoordAffected(coord);
	}

	@Override
	public String toString() {
		return "Area: " + shpFile;
	}

}
