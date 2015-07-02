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
import org.matsim.core.api.experimental.events.EventsManager;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import com.vividsolutions.jts.geom.Coordinate;

import playground.gregor.sim2d_v4.events.XYVxVyEventImpl;
import playground.gregor.sim2d_v4.events.XYVxVyEventsHandler;

public class CoordinateConverter implements XYVxVyEventsHandler{

	private EventsManager em;
	private MathTransform transform;

	public CoordinateConverter(EventsManager em,MathTransform transform) {
		this.em = em;
		this.transform = transform;
	}


	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub

	}

	@Override
	public void handleEvent(XYVxVyEventImpl event){
		Coordinate c = new Coordinate(event.getX(),event.getY());
		try {
			JTS.transform(c, c, this.transform);
		} catch (TransformException e) {
			e.printStackTrace();
		}
		XYVxVyEventImpl e2 = new XYVxVyEventImpl(event.getPersonId(), c.x, c.y, event.getVX(), event.getVY(), event.getTime());
		this.em.processEvent(e2);

	}

}
