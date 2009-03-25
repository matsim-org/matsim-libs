/* *********************************************************************** *
 * project: org.matsim.*
 * ActivityScoringFunctionTest.java
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

package playground.meisterk.org.matsim.scoring.ktiYear3;

import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.basic.v01.IdImpl;
import org.matsim.config.Config;
import org.matsim.config.groups.CharyparNagelScoringConfigGroup;
import org.matsim.facilities.FacilitiesImpl;
import org.matsim.interfaces.basic.v01.Id;
import org.matsim.interfaces.basic.v01.population.BasicLeg;
import org.matsim.interfaces.basic.v01.population.BasicLeg.Mode;
import org.matsim.interfaces.core.v01.Activity;
import org.matsim.interfaces.core.v01.CarRoute;
import org.matsim.interfaces.core.v01.Facilities;
import org.matsim.interfaces.core.v01.Facility;
import org.matsim.interfaces.core.v01.Leg;
import org.matsim.interfaces.core.v01.Link;
import org.matsim.interfaces.core.v01.Node;
import org.matsim.interfaces.core.v01.Person;
import org.matsim.interfaces.core.v01.Plan;
import org.matsim.locationchoice.facilityload.FacilityPenalty;
import org.matsim.network.NetworkLayer;
import org.matsim.population.Desires;
import org.matsim.population.PersonImpl;
import org.matsim.scoring.ScoringFunction;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.utils.geometry.CoordImpl;
import org.matsim.utils.misc.Time;

public class ActivityScoringFunctionTest extends MatsimTestCase {

	private Plan plan;
	private Config config;
	
	/*package*/ static final Logger logger = Logger.getLogger(ActivityScoringFunctionTest.class);

	protected void setUp() throws Exception {
		super.setUp();
		
		// generate config
		this.config = super.loadConfig(null);
		CharyparNagelScoringConfigGroup scoring = this.config.charyparNagelScoring();
		scoring.setBrainExpBeta(2.0);
		scoring.setLateArrival(0.0);
		scoring.setEarlyDeparture(-6.0);
		scoring.setPerforming(+6.0);
		scoring.setTraveling(0.0);
		scoring.setTravelingPt(0.0);
		scoring.setMarginalUtlOfDistanceCar(0.0);
		scoring.setWaiting(0.0);
		
		// generate person
		Person person = new PersonImpl(new IdImpl("123"));

		// generate facilities
		Facilities facilities = new FacilitiesImpl();
		Facility facility1 = facilities.createFacility(new IdImpl("1"), new CoordImpl(0.0, 0.0));
		Facility facility3 = facilities.createFacility(new IdImpl("3"), new CoordImpl(1000.0, 1000.0));
		Facility facility5 = facilities.createFacility(new IdImpl("5"), new CoordImpl(1000.0, 1010.0));
		
		// generate network
		NetworkLayer network = new NetworkLayer();
		Node node1 = network.createNode(new IdImpl("1"), new CoordImpl(    0.0, 0.0));
		Node node2 = network.createNode(new IdImpl("2"), new CoordImpl(  500.0, 0.0));
		Node node3 = network.createNode(new IdImpl("3"), new CoordImpl( 5500.0, 0.0));
		Node node4 = network.createNode(new IdImpl("4"), new CoordImpl( 6000.0, 0.0));
		Node node5 = network.createNode(new IdImpl("5"), new CoordImpl(11000.0, 0.0));
		Node node6 = network.createNode(new IdImpl("6"), new CoordImpl(11500.0, 0.0));
		Link link1 = network.createLink(new IdImpl("1"), node1, node2, 500, 25, 3600, 1);
		network.createLink(new IdImpl("2"), node2, node3, 5000, 50, 3600, 1);
		Link link3 = network.createLink(new IdImpl("3"), node3, node4, 500, 25, 3600, 1);
		network.createLink(new IdImpl("4"), node4, node5, 5000, 50, 3600, 1);
		Link link5 = network.createLink(new IdImpl(5), node5, node6, 500, 25, 3600, 1);

		// generate desires
		Desires desires = person.createDesires("test desires");
		desires.putActivityDuration("home", Time.parseTime("16:00:00"));
		desires.putActivityDuration("work_sector3", Time.parseTime("07:00:00"));
		desires.putActivityDuration("leisure", Time.parseTime("01:00:00"));
		
		// generate plan
		plan = person.createPlan(true);

		Activity act = plan.createAct("home", facility1);
		act.setLink(link1);
		Leg leg = this.plan.createLeg(Mode.car);
		CarRoute route = (CarRoute) network.getFactory().createRoute(BasicLeg.Mode.car, link1, link3);
		leg.setRoute(route);
		route.setDistance(25000.0);
		route.setTravelTime(Time.parseTime("00:30:00"));

		act = plan.createAct("work_sector3", facility3);
		act.setLink(link3);
		leg = this.plan.createLeg(Mode.pt);
		route = (CarRoute) network.getFactory().createRoute(BasicLeg.Mode.car, link3, link5);
		leg.setRoute(route);
		route.setDistance(2000.0);
		route.setTravelTime(Time.parseTime("00:05:00"));

		act = plan.createAct("leisure", facility5);
		act.setLink(link5);
		leg = this.plan.createLeg(Mode.pt);
		route = (CarRoute) network.getFactory().createRoute(BasicLeg.Mode.car, link5, link3);
		leg.setRoute(route);
		route.setDistance(2000.0);
		route.setTravelTime(Time.parseTime("00:05:00"));

		act = plan.createAct("work_sector3", facility3);
		act.setLink(link3);
		leg = this.plan.createLeg(Mode.car);
		route = (CarRoute) network.getFactory().createRoute(BasicLeg.Mode.car, link3, link1);
		leg.setRoute(route);
		route.setDistance(25000.0);
		route.setTravelTime(Time.parseTime("00:30:00"));

		
		act = plan.createAct("home", facility1);
		act.setLink(link1);

	}

	private ScoringFunction getScoringFunction() {
		
		TreeMap<Id, FacilityPenalty> emptyFacilityPenalties = new TreeMap<Id, FacilityPenalty>();
		KTIYear3ScoringFunctionFactory factory = new KTIYear3ScoringFunctionFactory(config.charyparNagelScoring(), emptyFacilityPenalties);
		ScoringFunction testee = factory.getNewScoringFunction(this.plan);

		testee.endActivity(Time.parseTime("08:00:00"));
		testee.startLeg(Time.parseTime("08:00:00"), (Leg) this.plan.getPlanElements().get(1));
		testee.endLeg(Time.parseTime("08:30:00"));
		testee.startActivity(Time.parseTime("08:30:00"), (Activity) this.plan.getPlanElements().get(2));
		testee.endActivity(Time.parseTime("14:30:00"));
		testee.startLeg(Time.parseTime("14:30:00"), (Leg) this.plan.getPlanElements().get(3));
		testee.endLeg(Time.parseTime("14:35:00"));
		testee.startActivity(Time.parseTime("14:35:00"), (Activity) this.plan.getPlanElements().get(4));
		testee.endActivity(Time.parseTime("14:55:00"));
		testee.startLeg(Time.parseTime("14:55:00"), (Leg) this.plan.getPlanElements().get(5));
		testee.endLeg(Time.parseTime("15:00:00"));
		testee.startActivity(Time.parseTime("15:00:00"), (Activity) this.plan.getPlanElements().get(6));
		testee.endActivity(Time.parseTime("17:00:00"));
		testee.startLeg(Time.parseTime("17:00:00"), (Leg) this.plan.getPlanElements().get(7));
		testee.endLeg(Time.parseTime("17:30:00"));
		testee.startActivity(Time.parseTime("17:30:00"), (Activity) this.plan.getPlanElements().get(8));
		testee.finish();
		
		return testee;
	}
	
	public void testDefault() {

		ScoringFunction sf = this.getScoringFunction();
		
		logger.info("Overall score: " + sf.getScore());
		
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		this.plan = null;
		this.config = null;
	}
	
}
