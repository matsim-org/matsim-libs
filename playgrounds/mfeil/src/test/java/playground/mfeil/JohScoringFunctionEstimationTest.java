/* *********************************************************************** *
 * project: org.matsim.*
 * JohScoringFunctionEstimationTest.java
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

package playground.mfeil;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.testcases.MatsimTestCase;



public class JohScoringFunctionEstimationTest extends MatsimTestCase{

	private static final Logger log = Logger.getLogger(JohScoringFunctionEstimationTest.class);
	private JohScoringFunctionEstimation testee;
	private PlanImpl plan;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.init();
//		this.testee = new JohScoringFunctionEstimation (this.plan, new NetworkLayer());
		// commenting out the above line since it does not work (presumably, plan==null).  kai, jul'10
	}

	private void init(){
	/*	PersonImpl person = new PersonImpl(new IdImpl("1"));
		person.setAge(20);
		person.setSex("f");
		person.setCarAvail("always");
		person.setEmployed("yes");
		person.setLicence("yes");
		person.addTravelcard("halbtax");

		this.plan = person.createAndAddPlan(true);

		ActivityImpl act0 = new ActivityImpl("home", new CoordImpl(0,0));
		act0.setEndTime(30000);
		LegImpl leg1 = new LegImpl (TransportMode.car);
		leg1.setArrivalTime(31100);
	//	leg1.setRoute(route)
		ActivityImpl act2 = new ActivityImpl("work", new CoordImpl(0,0));
		act2.setEndTime(34600);
		LegImpl leg3 = new LegImpl (TransportMode.pt);
		leg3.setArrivalTime(40000);
		ActivityImpl act4 = new ActivityImpl("education", new CoordImpl(0,0));
		act4.setEndTime(50000);
		LegImpl leg5 = new LegImpl (TransportMode.walk);
		leg5.setArrivalTime(60000);
		ActivityImpl act6 = new ActivityImpl("shop", new CoordImpl(0,0));
		act6.setEndTime(63600);
		LegImpl leg7 = new LegImpl (TransportMode.car);
		leg7.setArrivalTime(63001);
		ActivityImpl act8 = new ActivityImpl("home", new CoordImpl(0,0));
		act8.setEndTime(86400);

		this.plan.addActivity(act0);
		this.plan.addLeg(leg1);
		this.plan.addActivity(act2);
		this.plan.addLeg(leg3);
		this.plan.addActivity(act4);
		this.plan.addLeg(leg5);
		this.plan.addActivity(act6);
		this.plan.addLeg(leg7);
		this.plan.addActivity(act8);*/
	}


	public void testRun (){
	/*	log.info("Running JohScoringFunctionEstimation test...");
		double testeeScore = this.testee.getScore();
		double scoreAct0 = (1+0.169) * (0 + (4.94-0)/(java.lang.Math.pow(1+1*java.lang.Math.exp(0.360*(8.31-(30000/3600))),1/1)));
		double scoreAct2 = (1+0.169) * (0 + (2.68-0)/(java.lang.Math.pow(1+1*java.lang.Math.exp(0.660*(6.20-(3500/3600))),1/1)));
		double scoreAct4 = (1+0.169) * (0 + (1.29-0)/(java.lang.Math.pow(1+1*java.lang.Math.exp(2.60*(2.07-(10000/3600))),1/1)));
		double scoreAct6 = (1+0.169) * (0 + (0.681-0)/(java.lang.Math.pow(1+1*java.lang.Math.exp(5.00*(0.264-(3600/3600))),1/1)));
		double scoreAct8 = (1+0.169) * (0 + (4.94-0)/(java.lang.Math.pow(1+1*java.lang.Math.exp(0.360*(8.31-(23399/3600))),1/1)));
	//	double scoreLeg1 = (1+0.158) * -4.08 * 1100/3600 + 0.0569 * 0.15 * dist/1000;


		log.info("... done.");*/
	}
}
