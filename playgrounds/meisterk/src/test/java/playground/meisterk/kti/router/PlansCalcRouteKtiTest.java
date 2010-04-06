/* *********************************************************************** *
 * project: org.matsim.*
 * PlansCalcRouteKtiTest.java
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

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.misc.Time;
import org.matsim.testcases.MatsimTestCase;

import playground.meisterk.kti.config.KtiConfigGroup;

public class PlansCalcRouteKtiTest extends MatsimTestCase {

	private Config config = null;
	private NetworkLayer network = null;
	private PlansCalcRouteKtiInfo plansCalcRouteKtiInfo = null;
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();

		config = super.loadConfig(null);
		
		KtiConfigGroup ktiConfigGroup = new KtiConfigGroup();
		ktiConfigGroup.setUsePlansCalcRouteKti(true);
		ktiConfigGroup.setPtHaltestellenFilename(this.getClassInputDirectory() + "haltestellen.txt");
		ktiConfigGroup.setPtTraveltimeMatrixFilename(this.getClassInputDirectory() + "pt_Matrix.mtx");
		ktiConfigGroup.setWorldInputFilename(this.getClassInputDirectory() + "world.xml");
		config.addModule(KtiConfigGroup.GROUP_NAME, ktiConfigGroup);

		network = new NetworkLayer();
		
		network.createAndAddNode(new IdImpl(1), new CoordImpl(1000.0, 1000.0));
		network.createAndAddNode(new IdImpl(2), new CoordImpl(1100.0, 1100.0));
		network.createAndAddNode(new IdImpl(3), new CoordImpl(1200.0, 1200.0));

		network.createAndAddLink(new IdImpl(1), network.getNodes().get(new IdImpl("1")), network.getNodes().get(new IdImpl("2")), 1.0, 1.0, 1.0, 1.0);
		network.createAndAddLink(new IdImpl(2), network.getNodes().get(new IdImpl("2")), network.getNodes().get(new IdImpl("3")), 1.0, 1.0, 1.0, 1.0);


		plansCalcRouteKtiInfo = new PlansCalcRouteKtiInfo(ktiConfigGroup);
		plansCalcRouteKtiInfo.prepare(network);

		KtiPtRouteFactory ktiPtRouteFactory = new KtiPtRouteFactory(plansCalcRouteKtiInfo);
		network.getFactory().setRouteFactory(TransportMode.pt, ktiPtRouteFactory);
		
	}
	

	@Override
	protected void tearDown() throws Exception {

		super.tearDown();
		plansCalcRouteKtiInfo = null;
		network = null;
		config = null;
		
	}

	public void testHandleLeg() {
		
		double departureTime = Time.parseTime("06:00:00");
		
		PlansCalcRouteKti testee = new PlansCalcRouteKti(
				config.plansCalcRoute(), 
				network, 
				null, 
				null, 
				new DijkstraFactory(), 
				plansCalcRouteKtiInfo);
		
		
		for (TransportMode mode : TransportMode.values()) {
			Person person = new PersonImpl(new IdImpl("1"));
			LegImpl leg = new LegImpl(mode);
			ActivityImpl fromAct = new ActivityImpl("home", new IdImpl("1"));
			fromAct.setCoord(new CoordImpl(1050.0, 1050.0));
			ActivityImpl toAct = new ActivityImpl("work", new IdImpl("1"));
			toAct.setCoord(new CoordImpl(1052.0, 1052.0));
			
			try {
				double travelTime = testee.handleLeg(person, leg, fromAct, toAct, departureTime);
				assertEquals("Wrong returned travel time for mode \"" + mode.toString() + "\".", 0.0, travelTime);
				assertEquals("Wrong leg departure time for mode \"" + mode.toString() + "\".", departureTime, leg.getDepartureTime());
				assertEquals("Wrong leg travel time for mode \"" + mode.toString() + "\".", 0.0, leg.getTravelTime());
				assertEquals("Wrong leg arrival time for mode \"" + mode.toString() + "\".", departureTime, leg.getArrivalTime());
				assertEquals("Wrong distance for mode \"" + mode.toString() + "\".", 0.0, leg.getRoute().getDistance());
			} catch (RuntimeException e) {
				System.out.println(e.toString());
			}
			
		}
		
		
		
	}
	
	public void testHandleSwissPtLeg() {
		
		PersonImpl person = new PersonImpl(new IdImpl("123"));
		PlanImpl plan = new PlanImpl();
		person.addPlan(plan);
		
		ActivityImpl home = new ActivityImpl("home", new IdImpl("1"));
		home.setCoord(new CoordImpl(1050.0, 1050.0));
		ActivityImpl work = new ActivityImpl("work", new IdImpl("2"));
		work.setCoord(new CoordImpl(1150.0, 1150.0));

		LegImpl leg = new LegImpl(TransportMode.pt);

		plan.addActivity(home);
		plan.addLeg(leg);
		plan.addActivity(work);

		PlansCalcRouteKti testee = new PlansCalcRouteKti(
				config.plansCalcRoute(), 
				network, 
				null, 
				null, 
				new DijkstraFactory(), 
				plansCalcRouteKtiInfo);
		
		testee.handleLeg(person, leg, home, work, Time.parseTime("12:34:56"));
		
		String actualRouteDescription = ((KtiPtRoute) leg.getRoute()).getRouteDescription();
		String expectedRouteDescription = "kti=8503006=26101=300.0=26102=8503015";
		assertEquals(expectedRouteDescription, actualRouteDescription);
		
	}

	public void testGetTimeInVehicle() {
		
		double expectedTimeInVehicle = 300.0;

		KtiPtRoute route = new KtiPtRoute(null, null, this.plansCalcRouteKtiInfo);
		route.setRouteDescription(null, "kti=8503006=26101=300.0=26102=8503015", null);
		
//		assertEquals(expectedTimeInVehicle, route.getPtMatrixInVehicleTime());
		assertNull(route.getInVehicleTime());
	}
	
}
