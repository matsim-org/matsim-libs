/* *********************************************************************** *
 * project: org.matsim.*
 * KtiPtRouteTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.meisterk.kti.router;

import org.matsim.api.core.v01.BasicLocation;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.testcases.fakes.FakeLink;

import playground.balmermi.world.Layer;
import playground.balmermi.world.World;
import playground.balmermi.world.Zone;
import playground.balmermi.world.ZoneLayer;
import playground.meisterk.kti.config.KtiConfigGroup;

public class KtiPtRouteTest extends MatsimTestCase {

	private PlansCalcRouteKtiInfo plansCalcRouteKtiInfo = null;
	private KtiConfigGroup ktiConfigGroup = null;

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		Config config = super.loadConfig(null);
		ktiConfigGroup = new KtiConfigGroup();
		ktiConfigGroup.setUsePlansCalcRouteKti(true);
		ktiConfigGroup.setPtHaltestellenFilename(this.getClassInputDirectory() + "haltestellen.txt");
		ktiConfigGroup.setPtTraveltimeMatrixFilename(this.getClassInputDirectory() + "pt_Matrix.mtx");
		ktiConfigGroup.setWorldInputFilename(this.getClassInputDirectory() + "world.xml");
		ktiConfigGroup.setIntrazonalPtSpeed(10.0);
		config.addModule(ktiConfigGroup);

		NetworkImpl dummyNetwork = NetworkImpl.createNetwork();
		dummyNetwork.createAndAddNode(Id.create("1000", Node.class), new CoordImpl(900.0, 900.0));
		dummyNetwork.createAndAddNode(Id.create("1001", Node.class), new CoordImpl(3200.0, 3200.0));

		this.plansCalcRouteKtiInfo = new PlansCalcRouteKtiInfo(ktiConfigGroup);
		this.plansCalcRouteKtiInfo.prepare(dummyNetwork);

	}

	@Override
	protected void tearDown() throws Exception {
		this.plansCalcRouteKtiInfo = null;
		super.tearDown();
	}

	public void testInitializationLinks() {

		Link link1 = new FakeLink(Id.create(1, Link.class));
		Link link2 = new FakeLink(Id.create(2, Link.class));
		KtiPtRoute testee = new KtiPtRoute(link1.getId(), link2.getId(), this.plansCalcRouteKtiInfo);
		assertEquals(link1.getId(), testee.getStartLinkId());
		assertEquals(link2.getId(), testee.getEndLinkId());
		assertEquals(null, testee.getFromStop());
		assertEquals(null, testee.getFromMunicipality());
		assertEquals(null, testee.getInVehicleTime());
		assertEquals(null, testee.getToMunicipality());
		assertEquals(null, testee.getToStop());

	}

	public void testFullInitialization() {

		Link link1 = new FakeLink(Id.create(1, Link.class));
		Link link2 = new FakeLink(Id.create(2, Link.class));
		SwissHaltestelle fromStop = new SwissHaltestelle("123", new CoordImpl(1000.0, 1000.0));
		SwissHaltestelle toStop = new SwissHaltestelle("456", new CoordImpl(2000.0, 2000.0));

		World world = new World();
		ZoneLayer municipalities = (ZoneLayer) world.createLayer(Id.create("municipality", Layer.class));

		Coord dummyMuniCoord = new CoordImpl(3003.0, 3003.0);
		BasicLocation fromMunicipality = new Zone(Id.create("30000", Zone.class), dummyMuniCoord, dummyMuniCoord, dummyMuniCoord);
		dummyMuniCoord.setXY(4004.0, 4004.0);
		BasicLocation toMunicipality = new Zone(Id.create("30001", Zone.class), dummyMuniCoord, dummyMuniCoord, dummyMuniCoord);

		KtiPtRoute testee = new KtiPtRoute(link1.getId(), link2.getId(), this.plansCalcRouteKtiInfo, fromStop, fromMunicipality, toMunicipality, toStop);
		assertEquals(link1.getId(), testee.getStartLinkId());
		assertEquals(link2.getId(), testee.getEndLinkId());
		assertEquals(fromStop, testee.getFromStop());
		assertEquals(toStop, testee.getToStop());
		assertEquals(fromMunicipality, testee.getFromMunicipality());
		assertEquals(toMunicipality, testee.getToMunicipality());
		assertEquals(330.0, testee.getInVehicleTime().doubleValue());

		assertEquals("kti=123=30000=330.0=30001=456", testee.getRouteDescription());

	}

	public void testRouteDescription_KtiPtRoute() {

		String expectedRouteDescription = "kti=321=40000=456.78=40001=654";
		KtiPtRoute testee = new KtiPtRoute(null, null, this.plansCalcRouteKtiInfo);
		testee.setRouteDescription(null, expectedRouteDescription, null);
		assertEquals("321", testee.getFromStop().getId());
		assertEquals("654", testee.getToStop().getId());
		assertEquals(Id.create("40000", Zone.class), testee.getFromMunicipality().getId());
		assertEquals(Id.create("40001", Zone.class), testee.getToMunicipality().getId());
		assertEquals(456.78, testee.getInVehicleTime().doubleValue(), MatsimTestCase.EPSILON);
//		assertNull(testee.getFromStop());
//		assertNull(testee.getToStop());
//		assertNull(testee.getFromMunicipality());
//		assertNull(testee.getToMunicipality());
//		assertNull(testee.getInVehicleTime());

	}

	public void testRouteDescription_NoKtiPtRoute() {

		String expectedRouteDescription = System.getProperty("line.separator") + "\t\t\t\t" + System.getProperty("line.separator");
		KtiPtRoute testee = new KtiPtRoute(null, null, this.plansCalcRouteKtiInfo);
		testee.setRouteDescription(null, expectedRouteDescription, null);
		assertNull(testee.getFromStop());
		assertNull(testee.getToStop());
		assertNull(testee.getFromMunicipality());
		assertNull(testee.getToMunicipality());
		assertNull(testee.getInVehicleTime());

	}

	public void testCalcInVehicleTimeIntrazonal() {

		Link link1 = new FakeLink(Id.create(1, Link.class));
		Link link2 = new FakeLink(Id.create(2, Link.class));

		KtiPtRoute testee = new KtiPtRoute(
				link1.getId(),
				link2.getId(),
				this.plansCalcRouteKtiInfo,
				this.plansCalcRouteKtiInfo.getHaltestellen().getHaltestelle("1001"),
				this.plansCalcRouteKtiInfo.getLocalWorld().getLayer("municipality").getLocation(Id.create(30000, Zone.class)),
				this.plansCalcRouteKtiInfo.getLocalWorld().getLayer("municipality").getLocation(Id.create(30000, Zone.class)),
				this.plansCalcRouteKtiInfo.getHaltestellen().getHaltestelle("1002"));

		double expectedInVehicleTime = testee.calcInVehicleDistance() / ktiConfigGroup.getIntrazonalPtSpeed();
		double actualInVehicleTime = testee.getInVehicleTime();

		assertEquals(expectedInVehicleTime, actualInVehicleTime);
	}

}
