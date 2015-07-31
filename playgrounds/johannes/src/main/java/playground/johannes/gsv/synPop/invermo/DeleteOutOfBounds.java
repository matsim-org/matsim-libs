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

package playground.johannes.gsv.synPop.invermo;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import playground.johannes.gsv.synPop.CommonKeys;
import playground.johannes.gsv.synPop.ProxyPlanTask;
import playground.johannes.sna.gis.CRSUtils;
import playground.johannes.synpop.data.Element;
import playground.johannes.synpop.data.Episode;

/**
 * @author johannes
 *
 */
public class DeleteOutOfBounds implements ProxyPlanTask {

	private final Geometry bounds;
	
	private MathTransform transform;
	
	private final GeometryFactory factory = JTSFactoryFinder.getGeometryFactory(null);
	
	public DeleteOutOfBounds(Geometry bounds) {
		this.bounds = bounds;
		
		try {
			transform = CRS.findMathTransform(DefaultGeographicCRS.WGS84, CRSUtils.getCRS(31467));
		} catch (FactoryException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void apply(Episode plan) {
		for(Element act : plan.getActivities()) {
			String coordStr = act.getAttribute(InvermoKeys.COORD);
			if(coordStr != null) {
				Point p = string2Point(coordStr);
				if(!bounds.contains(p)) {
					plan.setAttribute(CommonKeys.DELETE, "true");
					break;
				}
			} else {
				plan.setAttribute(CommonKeys.DELETE, "true");
			}
		}

	}

	private Point string2Point(String str) {
		String tokens[] = str.split(",");
		double x = Double.parseDouble(tokens[0]);
		double y = Double.parseDouble(tokens[1]);

		double[] points = new double[] { x, y };
		try {
			transform.transform(points, 0, points, 0, 1);
		} catch (TransformException e) {
			e.printStackTrace();
		}

		return factory.createPoint(new Coordinate(points[0], points[1]));
	}
}
