/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.gregor.multidestpeds.helper;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;

import playground.gregor.sim2d_v3.events.XYVxVyEvent;
import playground.gregor.sim2d_v3.events.XYVxVyEventsFileReader;
import playground.gregor.sim2d_v3.events.XYVxVyEventsHandler;
import playground.gregor.sim2d_v3.helper.gisdebug.GisDebugger;

import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;


public class EventsToShape implements XYVxVyEventsHandler{


	private final GeometryFactory geofac = new GeometryFactory();


	public static void main (String [] args) {
		String events = "/Users/laemmel/devel/dfg/events.xml";
		EventsManager manager = EventsUtils.createEventsManager();
		EventsToShape handler = new EventsToShape();
		manager.addHandler(handler);
		XYVxVyEventsFileReader reader = new XYVxVyEventsFileReader(manager);
		reader.parse(events);
		GisDebugger.dump("/Users/laemmel/tmp/points.shp");
	}

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub

	}

	@Override
	public void handleEvent(XYVxVyEvent event) {
		Point p = this.geofac.createPoint(event.getCoordinate());
		GisDebugger.addGeometry(p);

	}
}
