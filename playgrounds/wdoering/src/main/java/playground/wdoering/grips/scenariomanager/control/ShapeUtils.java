/* *********************************************************************** *
 * project: org.matsim.*
 * MyMapViewer.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.wdoering.grips.scenariomanager.control;

import java.awt.geom.Point2D;

import org.geotools.referencing.CRS;
import org.matsim.contrib.evacuation.control.algorithms.PolygonalCircleApproximation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.NoninvertibleTransformException;

import playground.wdoering.grips.scenariomanager.model.shape.CircleShape;
import playground.wdoering.grips.scenariomanager.model.shape.PolygonShape;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Polygon;

/**
 * geometric functions for shapes 
 * 
 * @author wdoering
 *
 */
public class ShapeUtils
{
	private Controller controller;
	public ShapeUtils(Controller controller)
	{
		this.controller = controller;
	}

	public PolygonShape getPolygonFromCircle(CircleShape circle)
	{
		// copy circle data to polygon data
		PolygonShape polygon = new PolygonShape(circle.getLayerID(), null);
		polygon.setDescription(circle.getDescription());
		polygon.setId(circle.getId());
		polygon.setMetaData(circle.getAllMetaData());
		
		polygon.setStyle(circle.getStyle());

		CoordinateReferenceSystem sourceCRS = MGC.getCRS(controller.getSourceCoordinateSystem());
		CoordinateReferenceSystem targetCRS = MGC.getCRS(controller.getConfigCoordinateSystem());

		MathTransform transform = null;
		try
		{
			transform = CRS.findMathTransform(sourceCRS, targetCRS, true);
		}
		catch (FactoryException e)
		{
			throw new RuntimeException(e);
		}
		
		Point2D c0 = circle.getOrigin();
		Point2D c1 = circle.getDestination();
		
		Coordinate coord0 = new Coordinate(c0.getY(), c0.getX());
		Coordinate coord1 = new Coordinate(c1.getY(), c1.getX());
		PolygonalCircleApproximation.transform(coord0, transform);
		PolygonalCircleApproximation.transform(coord1, transform);

		Polygon poly = PolygonalCircleApproximation.getPolygonFromGeoCoords(coord0, coord1);

		try
		{
			poly = (Polygon) PolygonalCircleApproximation.transform(poly, transform.inverse());
		}
		catch (NoninvertibleTransformException e)
		{
			e.printStackTrace();
		}

		polygon.setPolygon(poly);

		return polygon;
	}

}
