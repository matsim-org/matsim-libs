/* *********************************************************************** *
 * project: org.matsim.*
 * FeatureNetworkFilter
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
package playground.dgrether.analysis;

import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.network.filter.NetworkLinkFilter;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import com.vividsolutions.jts.geom.Geometry;


/**
 * @author dgrether
 *
 */
public class FeatureNetworkLinkStartOrEndCoordFilter implements NetworkLinkFilter {

	private MathTransform transform;
	private SimpleFeature feature;
	
	public FeatureNetworkLinkStartOrEndCoordFilter(CoordinateReferenceSystem networkSrs,
			SimpleFeature feature, CoordinateReferenceSystem featureSrs) {
		this.feature = feature;
		try {
			this.transform = CRS.findMathTransform(networkSrs, featureSrs, true);
		} catch (FactoryException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	@Override
	public boolean judgeLink(Link l) {
		Coord linkStartCoord = l.getFromNode().getCoord();
		Coord linkEndCoord = l.getToNode().getCoord();
		Geometry linkStartPoint, linkEndPoint = null;
		try {
			linkStartPoint = JTS.transform(MGC.coord2Point(linkStartCoord), this.transform);
			linkEndPoint = JTS.transform(MGC.coord2Point(linkEndCoord), this.transform);
			if (((Geometry) this.feature.getDefaultGeometry()).contains(linkStartPoint) || ((Geometry) this.feature.getDefaultGeometry()).contains(linkEndPoint)) {
				return true;
			}
		} catch (TransformException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		return false;
	}

}
