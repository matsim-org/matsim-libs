/* *********************************************************************** *
 * project: org.matsim.*
 * GeotoolsTransformation.java
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

package org.matsim.utils.geometry.transformations;

import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.matsim.interfaces.basic.v01.Coord;
import org.matsim.utils.geometry.CoordinateTransformation;
import org.matsim.utils.geometry.geotools.MGC;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.spatialschema.geometry.MismatchedDimensionException;

import com.vividsolutions.jts.geom.Point;

/**
 * A transformation factory for various coordinate systems using the GeoTools.
 *
 * @author laemmel
 */
public class GeotoolsTransformation implements CoordinateTransformation {

	private MathTransform transform;




	/**
	 * Creates a new coordinate transformation that makes use of GeoTools.
	 * The coordinate systems to translate from and to can either be specified as
	 * shortened names, as defined in {@link TransformationFactory}, or as
	 * Well-Known-Text (WKT) as supported by the GeoTools.
	 *
	 * @param from Specifies the origin coordinate reference system
	 * @param to Specifies the destination coordinate reference system
	 *
	 * @see <a href="http://geoapi.sourceforge.net/snapshot/javadoc/org/opengis/referencing/doc-files/WKT.html">WKT specifications</a>
	 */
	public GeotoolsTransformation(final String from, final String to) {
		CoordinateReferenceSystem sourceCRS = MGC.getCRS(from);
		CoordinateReferenceSystem targetCRS = MGC.getCRS(to);

		try {
			this.transform = CRS.findMathTransform(sourceCRS, targetCRS,true);
		} catch (FactoryException e) {
			throw new RuntimeException(e);
		}
	}

	public Coord transform(final Coord coord) {
		Point p = null;
		try {
			p = (Point) JTS.transform(MGC.coord2Point(coord), this.transform);
		} catch (MismatchedDimensionException e) {
			throw new RuntimeException(e);
		} catch (TransformException e) {
			throw new RuntimeException(e);
		}
		return MGC.point2Coord(p);
	}



}
