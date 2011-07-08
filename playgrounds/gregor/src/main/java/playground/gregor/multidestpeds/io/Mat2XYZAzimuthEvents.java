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
package playground.gregor.multidestpeds.io;

import java.io.IOException;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsFactory;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.misc.ConfigUtils;

import playground.gregor.sim2d_v2.events.XYZAzimuthEvent;
import playground.gregor.sim2d_v2.events.XYZAzimuthEventImpl;
import playground.gregor.sim2d_v2.events.XYZEventsManager;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * @author laemmel
 * 
 */
//TODO rewrite
@Deprecated
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
		Config conf = ConfigUtils.loadConfig("/Users/laemmel/devel/dfg/config2d.xml");
		Scenario sc = ScenarioUtils.loadScenario(conf);
		EventWriterXML writer = new EventWriterXML("/Users/laemmel/devel/dfg/events.xml");
		EventsManager manager = EventsUtils.createEventsManager();
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
					Id linkId = getLinkId(ped.coords.get(time),ped.velocities.get(time),sc);
					manager.processEvent(fac.createAgentDepartureEvent(time, ped.id, linkId, "walk2d"));
				}
				Id id = ped.id;
				Coordinate c = ped.coords.get(time);
				Coordinate v = ped.velocities.get(time);
				float a = (float) getPhaseAngle(v.x, v.y);


				XYZAzimuthEvent ev = new XYZAzimuthEventImpl(id, c, a, time);
				manager.processEvent(ev);

				if (time == ped.arrived) {
					Id linkId = getLinkId(ped.coords.get(time),ped.velocities.get(time),sc);
					manager.processEvent(fac.createAgentArrivalEvent(time, ped.id, linkId, "walk2d"));
				}
				break;
			}

		}
		writer.closeFile();

	}

	private static Id getLinkId(Coordinate loc, Coordinate vel,
			Scenario sc) {

		LinkImpl l1 = ((NetworkImpl)sc.getNetwork()).getNearestLink(MGC.coordinate2Coord(loc));
		LinkImpl l2 = null;
		for (Link l : l1.getToNode().getOutLinks().values()) {
			if (l.getToNode() == l1.getFromNode()) {
				l2 = (LinkImpl) l;
			}
		}

		Coordinate next = new Coordinate(loc.x+vel.x,loc.y+vel.y);
		double distNow = MGC.coord2Coordinate(l1.getToNode().getCoord()).distance(loc);
		double distNext = MGC.coord2Coordinate(l1.getToNode().getCoord()).distance(next);
		if (distNow > distNext) {
			return l1.getId();
		} else {
			return l2.getId();
		}

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
