/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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
package playground.gregor.gis.worldmercatorvis;

import org.geotools.geometry.jts.JTS;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.utils.geometry.CoordImpl;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import com.vividsolutions.jts.geom.Coordinate;

public class NetworkConverter {
	
	private Scenario sc;
	private MathTransform transform;

	public NetworkConverter(Scenario sc, MathTransform transform) {
		this.sc = sc;
		this.transform = transform;
		
	}
	
	public void run() {
		for (Node n : this.sc.getNetwork().getNodes().values()) {
			CoordImpl c = (CoordImpl) n.getCoord();
			Coordinate cc = new Coordinate(c.getX(),c.getY());
			try {
				JTS.transform(cc, cc, transform);
			} catch (TransformException e) {
				e.printStackTrace();
			}
			c.setXY(cc.x, cc.y);
		}
	}

}
