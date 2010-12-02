/* *********************************************************************** *
 * project: org.matsim.*
 * Mat2XYZAzimuthEvents.java
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
package playground.gregor.pedvis;

import java.io.IOException;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.EventsFactory;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.algorithms.EventWriterXML;

import playground.gregor.sim2d_v2.events.XYZAzimuthEvent;
import playground.gregor.sim2d_v2.events.XYZAzimuthEventImpl;
import playground.gregor.sim2d_v2.events.XYZEventsManager;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * @author laemmel
 * 
 */
public class Mat2XYZAzimuthEvents {

	private static final double PI_HALF = Math.PI / 2;
	private static final double TWO_PI = 2 * Math.PI;

	public static void main(String[] args) {
		Importer imp = new Importer();
		try {
			imp.read();
		} catch (IOException e) {
			e.printStackTrace();
		}

		EventWriterXML writer = new EventWriterXML("/home/laemmel/devel/dfg/events.xml");
		EventsManager manager = new EventsManagerImpl();
		manager.addHandler(writer);
		EventsFactory fac = manager.getFactory();

		List<Ped> peds = imp.getPeds();
		List<Double> timeSteps = imp.getTimeSteps();

		for (int i = 0; i < timeSteps.size(); i++) {
			double time = timeSteps.get(i);
			for (Ped ped : peds) {
				if (time < ped.depart || time > ped.arrived) {
					continue;
				}
				if (time == ped.depart) {
					if (ped.color.equals("red")) {
						manager.processEvent(fac.createAgentDepartureEvent(time, ped.id, new IdImpl(1), "walk2d"));
					} else {
						manager.processEvent(fac.createAgentDepartureEvent(time, ped.id, new IdImpl(2), "walk2d"));
					}
				}
				Id id = ped.id;
				Coordinate c = ped.coords.get(time);
				float a = 0;
				if (i > 0) {
					double preTime = timeSteps.get(i - 1);
					Coordinate preC = ped.coords.get(preTime);
					if (preC != null) {
						a = (float) getPhaseAngle(c.x - preC.x, c.y - preC.y);
					}
				}

				XYZAzimuthEvent ev = new XYZAzimuthEventImpl(id, c, a, time);
				manager.processEvent(ev);

				if (time == ped.arrived) {
					manager.processEvent(fac.createAgentArrivalEvent(time, ped.id, new IdImpl(-2), "walk2d"));
				}
			}

		}
		writer.closeFile();

	}

	private static double getPhaseAngle(double dX, double dY) {
		double alpha = 0.0;
		if (dX > 0) {
			alpha = Math.atan(dY / dX);
		} else if (dX < 0) {
			alpha = Math.PI + Math.atan(dY / dX);
		} else { // i.e. DX==0
			if (dY > 0) {
				alpha = PI_HALF;
			} else {
				alpha = -PI_HALF;
			}
		}
		if (alpha < 0.0)
			alpha += TWO_PI;
		return alpha;
	}
}
