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

import org.geotools.feature.Feature;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.network.filter.NetworkLinkFilter;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.spatialschema.geometry.MismatchedDimensionException;

import com.vividsolutions.jts.geom.Geometry;


/**
 * @author dgrether
 *
 */
public class FeatureNetworkLinkFilter implements NetworkLinkFilter {

	private MathTransform transform;
	private Feature feature;
	
	public FeatureNetworkLinkFilter(CoordinateReferenceSystem networkSrs,
			Feature feature, CoordinateReferenceSystem featureSrs) {
		
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
		Coord linkCoord = l.getCoord();
		Geometry linkPoint = null;
		try {
			linkPoint = JTS.transform(MGC.coord2Point(linkCoord), this.transform);
			if (this.feature.getDefaultGeometry().contains(linkPoint)) {
				return true;
			}
		} catch (MismatchedDimensionException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		} catch (TransformException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		return false;
	}

}
