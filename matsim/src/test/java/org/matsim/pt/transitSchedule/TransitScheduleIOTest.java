/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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

package org.matsim.pt.transitSchedule;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitStopArea;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.utils.objectattributes.attributable.Attributes;
import org.matsim.utils.objectattributes.attributable.AttributesUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * @author mrieser / SBB
 */
public class TransitScheduleIOTest {

	@Test
	public void testWriteRead_V2() {
		TransitScheduleFactory f = new TransitScheduleFactoryImpl();
		TransitSchedule schedule = new TransitScheduleImpl(f);
		{ // prepare data

			schedule.getAttributes().putAttribute("source", "myImagination");

			TransitStopFacility stop1 = f.createTransitStopFacility(Id.create(1, TransitStopFacility.class), new Coord(123, 234), true);
			TransitStopFacility stop2 = f.createTransitStopFacility(Id.create(2, TransitStopFacility.class), new Coord(987, 876, 98765), false);
			stop2.getAttributes().putAttribute("air", "thin");
			stop2.setStopAreaId(Id.create("GZ", TransitStopArea.class));

			schedule.addStopFacility(stop1);
			schedule.addStopFacility(stop2);

			schedule.getMinimalTransferTimes().set(stop1.getId(), stop2.getId(), 300.0);
			schedule.getMinimalTransferTimes().set(stop2.getId(), stop1.getId(), 360.0);

			TransitLine line1 = f.createTransitLine(Id.create("blue", TransitLine.class));
			line1.getAttributes().putAttribute("color", "like the sky");
			line1.getAttributes().putAttribute("operator", "higher being");

			NetworkRoute netRoute = RouteUtils.createLinkNetworkRouteImpl(
					Id.create("group", Link.class),
					new Id[]{Id.create("aboveGround", Link.class), Id.create("belowSky", Link.class)},
					Id.create("sky", Link.class));
			List<TransitRouteStop> stops = new ArrayList<>();
			TransitRouteStop rStop1 = f.createTransitRouteStop(stop1, Time.UNDEFINED_TIME, 0.0);
			TransitRouteStop rStop2 = f.createTransitRouteStop(stop1, 9999.9, Time.UNDEFINED_TIME);
			stops.add(rStop1);
			stops.add(rStop2);
			TransitRoute route1a = f.createTransitRoute(Id.create("upwards", TransitRoute.class), netRoute, stops, "elevator");

			route1a.getAttributes().putAttribute("bidirectional", false);

			Departure dep1 = f.createDeparture(Id.create("first", Departure.class), 100);
			Departure dep2 = f.createDeparture(Id.create("last", Departure.class), 86300);
			route1a.addDeparture(dep1);
			route1a.addDeparture(dep2);

			dep1.getAttributes().putAttribute("early", "yes");

			line1.addRoute(route1a);
			schedule.addTransitLine(line1);
		}

		// write data
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		new TransitScheduleWriterV2(schedule).write(outputStream);

		// to see the actual XML written:
//		String dataWritten = new String(outputStream.toByteArray());
//		System.out.println(dataWritten);

		// read data
		ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new TransitScheduleReader(scenario).readStream(inputStream);

		TransitSchedule schedule2 = scenario.getTransitSchedule();
		// assert schedule
		Assert.assertEquals("myImagination", schedule2.getAttributes().getAttribute("source"));

		// assert stop facilities
		TransitStopFacility stop1 = schedule2.getFacilities().get(Id.create(1, TransitStopFacility.class));
		Assert.assertTrue(AttributesUtils.isEmpty(stop1.getAttributes()));
		TransitStopFacility stop2 = schedule2.getFacilities().get(Id.create(2, TransitStopFacility.class));
		Assert.assertFalse(AttributesUtils.isEmpty(stop2.getAttributes()));
		Assert.assertEquals("thin", stop2.getAttributes().getAttribute("air"));
		Assert.assertTrue(stop2.getCoord().hasZ());
		Assert.assertEquals(98765.0, stop2.getCoord().getZ(), 0.0);
		Assert.assertEquals("GZ", stop2.getStopAreaId().toString());

		// assert minmal transfer times
		Assert.assertEquals(300, schedule2.getMinimalTransferTimes().get(stop1.getId(), stop2.getId()), 0.0);
		Assert.assertEquals(360, schedule2.getMinimalTransferTimes().get(stop2.getId(), stop1.getId()), 0.0);
		Assert.assertEquals(Double.NaN, schedule2.getMinimalTransferTimes().get(stop1.getId(), stop1.getId()), 0.0);

		// assert transit lines
		TransitLine line1 = schedule2.getTransitLines().get(Id.create("blue", TransitLine.class));
		Assert.assertNotNull(line1);
		Assert.assertFalse(AttributesUtils.isEmpty(line1.getAttributes()));
		Assert.assertEquals("like the sky", line1.getAttributes().getAttribute("color"));
		Assert.assertEquals("higher being", line1.getAttributes().getAttribute("operator"));

		// assert transit routes
		TransitRoute route1 = line1.getRoutes().get(Id.create("upwards", TransitRoute.class));
		Assert.assertNotNull(route1);
		Assert.assertFalse(AttributesUtils.isEmpty(route1.getAttributes()));
		Assert.assertTrue(route1.getAttributes().getAttribute("bidirectional") instanceof Boolean);
		Assert.assertFalse((Boolean) route1.getAttributes().getAttribute("bidirectional"));

		// assert departures
		Departure dep1 = route1.getDepartures().get(Id.create("first", Departure.class));
		Assert.assertNotNull(dep1);
		Assert.assertFalse(AttributesUtils.isEmpty(dep1.getAttributes()));
		Assert.assertEquals("yes", dep1.getAttributes().getAttribute("early"));
		Departure dep2 = route1.getDepartures().get(Id.create("last", Departure.class));
		Assert.assertNotNull(dep2);
		Assert.assertTrue(AttributesUtils.isEmpty(dep2.getAttributes()));
	}

	@Test
	public void testWriteRead_V2_specialAttributes() {
		TransitScheduleFactory f = new TransitScheduleFactoryImpl();
		TransitSchedule schedule = f.createTransitSchedule();

		addSpecialAttributes(schedule.getAttributes());
		TransitStopFacility f1 = f.createTransitStopFacility(Id.create("1", TransitStopFacility.class), new Coord(123, 456), false);
		schedule.addStopFacility(f1);
		addSpecialAttributes(f1.getAttributes());

		// write data
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		new TransitScheduleWriterV2(schedule).write(outputStream);

		// to see the actual XML written:
		String dataWritten = new String(outputStream.toByteArray());
		System.out.println(dataWritten);

		// read data
		ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new TransitScheduleReader(scenario).readStream(inputStream);

		TransitSchedule schedule2 = scenario.getTransitSchedule();
		assertSpecialAttributes(schedule2.getAttributes());
		assertSpecialAttributes(schedule2.getFacilities().get(f1.getId()).getAttributes());
	}

	private void addSpecialAttributes(Attributes attributes) {
		attributes.putAttribute("normal", "one");
		attributes.putAttribute("special", "two, three & four % five");
		attributes.putAttribute("special&2", "six # seven");
		attributes.putAttribute("special<3>\"'", "eight > nine < ten\"'");
	}

	private void assertSpecialAttributes(Attributes attributes) {
		Assert.assertEquals("one", attributes.getAttribute("normal"));
		Assert.assertEquals("two, three & four % five", attributes.getAttribute("special"));
		Assert.assertEquals("six # seven", attributes.getAttribute("special&2"));
		Assert.assertEquals("eight > nine < ten\"'", attributes.getAttribute("special<3>\"'"));
	}

}
