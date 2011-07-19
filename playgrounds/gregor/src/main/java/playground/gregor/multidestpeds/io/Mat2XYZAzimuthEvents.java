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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.events.EventsFactory;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkFactoryImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.NetworkLegRouter;
import org.matsim.core.router.costcalculators.TravelCostCalculatorFactoryImpl;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.PersonalizableTravelCost;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTimeCalculator;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.misc.ConfigUtils;

import playground.gregor.sim2d_v2.events.XYZAzimuthEvent;
import playground.gregor.sim2d_v2.events.XYZAzimuthEventImpl;
import playground.gregor.sim2d_v2.events.XYZEventsManager;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

/**
 * @author laemmel
 * 
 */
//TODO rewrite
@Deprecated
public class Mat2XYZAzimuthEvents {


	// needed to generated "finish lines"
	private static final double COS_LEFT = Math.cos(Math.PI / 2);
	// needed to generated "finish lines"
	private static final double SIN_LEFT = Math.sin(Math.PI / 2);
	// needed to generated "finish lines"
	private static final double COS_RIGHT = Math.cos(-Math.PI / 2);
	// needed to generated "finish lines"
	private static final double SIN_RIGHT = Math.sin(-Math.PI / 2);
	private static HashMap<Id, LineString> finishLines;

	private static final GeometryFactory geofac = new GeometryFactory();

	public static void main(String[] args) {
		Importer imp = new Importer();
		try {
			imp.read();
		} catch (IOException e) {
			e.printStackTrace();
		}
		Config conf = ConfigUtils.loadConfig("/Users/laemmel/devel/dfg/config2d.xml");
		Scenario sc = ScenarioUtils.loadScenario(conf);
		createFinishLines(sc);


		EventWriterXML writer = new EventWriterXML("/Users/laemmel/devel/dfg/events.xml");
		EventsManager manager = EventsUtils.createEventsManager();
		manager.addHandler(writer);
		EventsFactory fac = manager.getFactory();

		List<Ped> peds = imp.getPeds();
		List<Double> timeSteps = imp.getTimeSteps();

		Network network = sc.getNetwork();
		FreeSpeedTravelTimeCalculator fs = new FreeSpeedTravelTimeCalculator();
		PersonalizableTravelCost cost = new TravelCostCalculatorFactoryImpl().createTravelCostCalculator(fs,sc.getConfig().planCalcScore() );
		LeastCostPathCalculator dijkstra = new Dijkstra(network, cost, fs);



		for (int i = 0; i < timeSteps.size(); i++) {
			double time = timeSteps.get(i);
			for (Ped ped : peds) {
				if (time < ped.depart || time > ped.arrived) {
					continue;
				}
				if (time == ped.depart) {

					calculateRoute(time,ped,sc,dijkstra);


					ped.lastPos = ped.coords.get(time);
					manager.processEvent(fac.createAgentDepartureEvent(time, ped.id, ped.path.links.get(ped.currLink).getId(), "walk2d"));
				}
				Id id = ped.id;
				Coordinate c = ped.coords.get(time);
				Coordinate v = ped.velocities.get(time);


				XYZAzimuthEvent ev = new XYZAzimuthEventImpl(id, c, v.x, v.y, time);
				manager.processEvent(ev);

				if (checkForNextLink(ped,c)) {
					manager.processEvent(fac.createLinkLeaveEvent(time, id, ped.path.links.get(ped.currLink).getId()));
					ped.currLink++;
					manager.processEvent(fac.createLinkEnterEvent(time, id, ped.path.links.get(ped.currLink).getId()));
				}

				ped.lastPos = c;
				if (time == ped.arrived) {
					Id linkId = getLinkId(ped.coords.get(time),ped.velocities.get(time),sc);
					manager.processEvent(fac.createAgentArrivalEvent(time, ped.id, linkId, "walk2d"));
				}
			}

		}
		writer.closeFile();

	}

	private static boolean checkForNextLink(Ped ped, Coordinate c) {
		Id currLinkId = ped.path.links.get(ped.currLink).getId();
		LineString finishLine = finishLines.get(currLinkId);
		LineString trajectory = geofac.createLineString(new Coordinate[] { ped.lastPos, c });
		if (trajectory.crosses(finishLine)) {
			return true;
		}
		return false;
	}

	private static void calculateRoute(double time, Ped ped, Scenario sc, LeastCostPathCalculator dijkstra) {
		Id startLink = getLinkId(ped.coords.get(time),ped.velocities.get(time),sc);
		Id endLink = getLinkId(ped.coords.get(ped.arrived),ped.velocities.get(ped.arrived),sc);
		Node from = sc.getNetwork().getLinks().get(startLink).getFromNode();
		Node to = sc.getNetwork().getLinks().get(endLink).getToNode();
		Path path = dijkstra.calcLeastCostPath(from, to , 0);
		boolean routeAgain = false;
		if (!path.links.get(0).getId().equals(startLink)) {

			Link oldStart = sc.getNetwork().getLinks().get(startLink);
			Node oldTo = oldStart.getToNode();
			for (Link l : from.getInLinks().values()) {
				if (l.getFromNode().equals(oldTo)) {
					startLink = l.getId();
					break;
				}
			}
			from = sc.getNetwork().getLinks().get(startLink).getFromNode();
			routeAgain = true;
		}

		if (!path.links.get(path.links.size()-1).getId().equals(endLink)) {
			Link oldEnd = sc.getNetwork().getLinks().get(endLink);
			Node oldFrom = oldEnd.getFromNode();
			for (Link l : to.getOutLinks().values()) {
				if (l.getToNode().equals(oldFrom)) {
					endLink = l.getId();
					break;
				}
			}
			to = sc.getNetwork().getLinks().get(endLink).getToNode();
			routeAgain = true;


		}

		if (routeAgain) {
			path = dijkstra.calcLeastCostPath(from, to , 0);
		}

		ped.path = path;
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


	private static void createFinishLines(Scenario sc) {

		GeometryFactory geofac = new GeometryFactory();

		finishLines = new HashMap<Id, LineString>();
		for (Link link : sc.getNetwork().getLinks().values()) {
			Coordinate to = MGC.coord2Coordinate(link.getToNode().getCoord());
			Coordinate from = MGC.coord2Coordinate(link.getFromNode().getCoord());
			Coordinate c = new Coordinate(from.x - to.x, from.y - to.y);
			// length of finish line is 30 m// TODO does this make sense?
			double scale = 30 / Math.sqrt(Math.pow(c.x, 2) + Math.pow(c.y, 2));
			c.x *= scale;
			c.y *= scale;
			Coordinate c1 = new Coordinate(COS_LEFT * c.x + SIN_LEFT * c.y, -SIN_LEFT * c.x + COS_LEFT * c.y);
			c1.x += to.x;
			c1.y += to.y;
			Coordinate c2 = new Coordinate(COS_RIGHT * c.x + SIN_RIGHT * c.y, -SIN_RIGHT * c.x + COS_RIGHT * c.y);
			c2.x += to.x;
			c2.y += to.y;
			LineString ls = geofac.createLineString(new Coordinate[] { c1, c2 });

			finishLines.put(link.getId(), ls);
		}
	}
}
