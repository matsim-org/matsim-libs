/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.mrieser.routedistance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import junit.framework.Assert;

import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

/**
 * @author mrieser
 */
public class ExperimentalTransitRouteDistanceCalculatorTest {

	@Test
	public void testCalcDistance() {
		Fixture f = new Fixture();
		ExperimentalTransitRoute r = new ExperimentalTransitRoute(
				f.schedule.getFacilities().get(f.ids[1]), f.line, f.route, f.schedule.getFacilities().get(f.ids[4]));
		Assert.assertEquals(300.0+400.0+500.0, f.distCalc.calcDistance(r), 1e-7);

		r = new ExperimentalTransitRoute(
				f.schedule.getFacilities().get(f.ids[2]), f.line, f.route, f.schedule.getFacilities().get(f.ids[3]));
		Assert.assertEquals(400.0, f.distCalc.calcDistance(r), 1e-7);
	}

	@Test
	public void testCalcDistance_fromFirstStop() {
		Fixture f = new Fixture();
		ExperimentalTransitRoute r = new ExperimentalTransitRoute(
				f.schedule.getFacilities().get(f.ids[0]), f.line, f.route, f.schedule.getFacilities().get(f.ids[4]));
		Assert.assertEquals(200.0+300.0+400.0+500.0, f.distCalc.calcDistance(r), 1e-7);

		r = new ExperimentalTransitRoute(
				f.schedule.getFacilities().get(f.ids[0]), f.line, f.route, f.schedule.getFacilities().get(f.ids[1]));
		Assert.assertEquals(200.0, f.distCalc.calcDistance(r), 1e-7);
	}

	@Test
	public void testCalcDistance_toLastStop() {
		Fixture f = new Fixture();
		ExperimentalTransitRoute r = new ExperimentalTransitRoute(
				f.schedule.getFacilities().get(f.ids[1]), f.line, f.route, f.schedule.getFacilities().get(f.ids[5]));
		Assert.assertEquals(300.0+400.0+500.0+600.0, f.distCalc.calcDistance(r), 1e-7);

		r = new ExperimentalTransitRoute(
				f.schedule.getFacilities().get(f.ids[3]), f.line, f.route, f.schedule.getFacilities().get(f.ids[5]));
		Assert.assertEquals(500.0+600.0, f.distCalc.calcDistance(r), 1e-7);
	}

	@Test
	public void testCalcDistance_fromFirstToLastStop() {
		Fixture f = new Fixture();
		ExperimentalTransitRoute r = new ExperimentalTransitRoute(
				f.schedule.getFacilities().get(f.ids[0]), f.line, f.route, f.schedule.getFacilities().get(f.ids[5]));
		Assert.assertEquals(200.0+300.0+400.0+500.0+600.0, f.distCalc.calcDistance(r), 1e-7);
	}

	@Test
	public void testCalcDistance_immediateExit() {
		Fixture f = new Fixture();
		ExperimentalTransitRoute r = new ExperimentalTransitRoute(
				f.schedule.getFacilities().get(f.ids[2]), f.line, f.route, f.schedule.getFacilities().get(f.ids[2]));
		Assert.assertEquals(0.0, f.distCalc.calcDistance(r), 1e-7);
	}

	private static class Fixture {
		protected final Id[] ids;
		protected final Scenario scenario;
		protected final Network network;
		protected final TransitSchedule schedule;
		protected final TransitLine line;
		protected final TransitRoute route;

		protected final ExperimentalTransitRouteDistanceCalculator distCalc;

		protected Fixture() {
			this.scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
			this.scenario.getConfig().scenario().setUseTransit(true);
			this.network = this.scenario.getNetwork();
			this.schedule = ((ScenarioImpl) this.scenario).getTransitSchedule();
			NetworkFactory nf = this.network.getFactory();

			this.ids = new Id[7];
			for (int i = 0; i < this.ids.length; i++) {
				this.ids[i] = this.scenario.createId(Integer.toString(i));
			}

			Node n0 = nf.createNode(this.ids[0], this.scenario.createCoord(0, 0));
			Node n1 = nf.createNode(this.ids[1], this.scenario.createCoord(100, 0));
			Node n2 = nf.createNode(this.ids[2], this.scenario.createCoord(200, 0));
			Node n3 = nf.createNode(this.ids[3], this.scenario.createCoord(300, 0));
			Node n4 = nf.createNode(this.ids[4], this.scenario.createCoord(400, 0));
			Node n5 = nf.createNode(this.ids[5], this.scenario.createCoord(500, 0));
			Node n6 = nf.createNode(this.ids[6], this.scenario.createCoord(600, 0));
			this.network.addNode(n0);
			this.network.addNode(n1);
			this.network.addNode(n2);
			this.network.addNode(n3);
			this.network.addNode(n4);
			this.network.addNode(n5);
			this.network.addNode(n6);
			this.network.addLink(nf.createLink(this.ids[0], n0, n1));
			this.network.addLink(nf.createLink(this.ids[1], n1, n2));
			this.network.addLink(nf.createLink(this.ids[2], n2, n3));
			this.network.addLink(nf.createLink(this.ids[3], n3, n4));
			this.network.addLink(nf.createLink(this.ids[4], n4, n5));
			this.network.addLink(nf.createLink(this.ids[5], n5, n6));

			this.network.getLinks().get(this.ids[0]).setLength(100.0);
			this.network.getLinks().get(this.ids[1]).setLength(200.0);
			this.network.getLinks().get(this.ids[2]).setLength(300.0);
			this.network.getLinks().get(this.ids[3]).setLength(400.0);
			this.network.getLinks().get(this.ids[4]).setLength(500.0);
			this.network.getLinks().get(this.ids[5]).setLength(600.0);

			TransitScheduleFactory sf = this.schedule.getFactory();

			TransitStopFacility fac0 = sf.createTransitStopFacility(this.ids[0], this.scenario.createCoord(99, 0), false);
			TransitStopFacility fac1 = sf.createTransitStopFacility(this.ids[1], this.scenario.createCoord(199, 0), false);
			TransitStopFacility fac2 = sf.createTransitStopFacility(this.ids[2], this.scenario.createCoord(299, 0), false);
			TransitStopFacility fac3 = sf.createTransitStopFacility(this.ids[3], this.scenario.createCoord(399, 0), false);
			TransitStopFacility fac4 = sf.createTransitStopFacility(this.ids[4], this.scenario.createCoord(499, 0), false);
			TransitStopFacility fac5 = sf.createTransitStopFacility(this.ids[5], this.scenario.createCoord(599, 0), false);
			fac0.setLinkId(this.ids[0]);
			fac1.setLinkId(this.ids[1]);
			fac2.setLinkId(this.ids[2]);
			fac3.setLinkId(this.ids[3]);
			fac4.setLinkId(this.ids[4]);
			fac5.setLinkId(this.ids[5]);

			this.schedule.addStopFacility(fac0);
			this.schedule.addStopFacility(fac1);
			this.schedule.addStopFacility(fac2);
			this.schedule.addStopFacility(fac3);
			this.schedule.addStopFacility(fac4);
			this.schedule.addStopFacility(fac5);

			List<TransitRouteStop> stops = new ArrayList<TransitRouteStop>();
			stops.add(sf.createTransitRouteStop(fac0, 0, 0));
			stops.add(sf.createTransitRouteStop(fac1, 60, 60));
			stops.add(sf.createTransitRouteStop(fac2, 120, 120));
			stops.add(sf.createTransitRouteStop(fac3, 180, 180));
			stops.add(sf.createTransitRouteStop(fac4, 240, 240));
			stops.add(sf.createTransitRouteStop(fac5, 300, 300));
			this.line = sf.createTransitLine(this.ids[0]);
			NetworkRoute netRoute = new LinkNetworkRouteImpl(this.ids[0], this.ids[5]);
			List<Id> linkIds = new ArrayList<Id>();
			Collections.addAll(linkIds, this.ids[1], this.ids[2], this.ids[3], this.ids[4]);
			netRoute.setLinkIds(this.ids[0], linkIds, this.ids[5]);
			this.route = sf.createTransitRoute(this.ids[1], netRoute, stops, TransportMode.car);

			this.line.addRoute(this.route);
			this.schedule.addTransitLine(this.line);

			this.distCalc = new ExperimentalTransitRouteDistanceCalculator(this.schedule, this.network);
		}
	}
}
