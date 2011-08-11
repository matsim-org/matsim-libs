/* *********************************************************************** *
 * project: org.matsim.*
 * ChangeActivityTimesEasyTest.java
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

package playground.jbischoff.matsimha2;

import static org.junit.Assert.fail;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.Config;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.misc.ConfigUtils;

/**
 * @author jbischoff
 *
 */

public class ChangeActivityTimesEasyTest {
	private ChangeActivityTimesEasy test1;
	private Plan testplan;
	@Before
	public void setUp() throws Exception {

		Config config = ConfigUtils.createConfig();
		Scenario sc = ScenarioUtils.createScenario(config);


		Network network = sc.getNetwork();
		Population population = sc.getPopulation();   
		PopulationFactory populationFactory = population.getFactory();

		Person person = populationFactory.createPerson(sc.createId("1"));
		population.addPerson(person);


		testplan = populationFactory.createPlan();
		person.addPlan(testplan);


		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.WGS84_UTM33N);

		Coord homeCoordinates = sc.createCoord(13.077, 52.357);
		Activity activity1 = populationFactory.createActivityFromCoord("home", ct.transform(homeCoordinates));
		activity1.setEndTime(8*3600);
		testplan.addActivity(activity1);


		testplan.addLeg(populationFactory.createLeg("car"));


		Activity activity2 = populationFactory.createActivityFromCoord("work", ct.transform(sc.createCoord(13.0500, 52.441)));
		activity2.setEndTime(16*3600);
		testplan.addActivity(activity2);


		testplan.addLeg(populationFactory.createLeg("car"));


		Activity activity3 = populationFactory.createActivityFromCoord("home", ct.transform(homeCoordinates));
		testplan.addActivity(activity3);
		test1 = new ChangeActivityTimesEasy();
	
	}

	@Test
	public void testChangeActivityTimesEasy() {
		test1 = new ChangeActivityTimesEasy();
		test1.setVARIATION(3600.0);
		
		Assert.assertEquals(3600.0, test1.getVARIATION(),0.0);
		
		
	}

	@Test
	public void testHandlePlan() {
		try {
			this.setUp();
			
			
			test1.handlePlan(testplan);
			Activity act1 = (Activity)testplan.getPlanElements().get(0);
//			System.out.println("ET:" + act1.getEndTime());
			Assert.assertEquals(8.0*3600,act1.getEndTime(),3600.0);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail("Exception");
		}
		
		
		
		}

}

