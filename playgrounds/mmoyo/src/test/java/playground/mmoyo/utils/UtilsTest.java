/* *********************************************************************** *
 * project: org.matsim.*
 * CadytsIntegrationTest.java
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

package playground.mmoyo.utils;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.testcases.MatsimTestUtils;

import playground.mmoyo.utils.PlansMerger;

public class UtilsTest extends MatsimTestCase {

	@Rule public MatsimTestUtils utils = new MatsimTestUtils();
	@Test
	public final void testUtils() {
		
		final String inputDir = this.getInputDirectory();
		final String outputDir = this.getOutputDirectory();
		//final String inputDir = "../playgrounds/mmoyo/test/input/playground/mmoyo/CadytsIntegrationTest/testCalibration/";
		//final String outputDir = "../playgrounds/mmoyo/test/output/playground/mmoyo/CadytsIntegrationTest/testCalibration/";
		
		System.out.println(" Input Dir " + inputDir );
		System.out.println(" Output Dir " + outputDir );
		
		/**test Plans Merger*/
		ScenarioImpl scn1 = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		ScenarioImpl scn2 = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		ScenarioImpl scn3 = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		
		Population pop1 = scn1.getPopulation();
		Population pop2 = scn2.getPopulation();
		Population pop3 = scn3.getPopulation();

		Id id1 = new IdImpl("person1");
		Person person1 = new PersonImpl(id1);
		Plan plan1 = new PlanImpl();
		Coord coord1 = new CoordImpl(1.0, 1.0);
		Activity act1 = new ActivityImpl("home", coord1);
		Activity act2 = new ActivityImpl("work", coord1);
		plan1.addActivity(act1);
		plan1.addLeg(new LegImpl(TransportMode.walk));
		plan1.addActivity(act2);
		person1.addPlan(plan1);
		pop1.addPerson(person1);
		
		Person person2 = new PersonImpl(id1);
		Plan plan2 = new PlanImpl();
		Coord coord2 = new CoordImpl(2.0, 2.0);
		Activity act1b = new ActivityImpl("home", coord2);
		Activity act2b = new ActivityImpl("work", coord2);
		plan2.addActivity(act1b);
		plan2.addLeg(new LegImpl(TransportMode.bike));
		plan2.addActivity(act2b);
		person2.addPlan(plan2);
		pop2.addPerson(person2);
	
		Id id3 = new IdImpl("person3");
		Person person3 = new PersonImpl(id3);
		Plan plan3 = new PlanImpl();
		Coord coord3 = new CoordImpl(3.0, 3.0);
		Activity act1c = new ActivityImpl("home", coord3);
		Activity act2c = new ActivityImpl("work", coord3);
		plan3.addActivity(act1c);
		plan3.addLeg(new LegImpl(TransportMode.pt));
		plan3.addActivity(act2c);
		person3.addPlan(plan3);
		pop3.addPerson(person3);

		PlansMerger planMerger = new PlansMerger();
		Population[] popArray = {pop1,pop2,pop3};
		Population mergedPop = planMerger.plansAggregator(popArray);

		Person p1 = mergedPop.getPersons().get(id1);
		Person p3 = mergedPop.getPersons().get(id3);
		
		Assert.assertEquals("Diferent number persons.", mergedPop.getPersons().size() , 2);
		Assert.assertEquals("Diferent number of plans in person 1", p1.getPlans().size() , 2);
		Assert.assertEquals("Diferent number of plans in person 3", p3.getPlans().size() , 1);
	
		Assert.assertEquals("wrong coordinate in selected plan in person 1", ((Activity)p1.getSelectedPlan().getPlanElements().get(0)).getCoord().getX() , 1.0, MatsimTestUtils.EPSILON);
		Assert.assertEquals("wrong coordinate in selected plan in person 1", ((Activity)p1.getSelectedPlan().getPlanElements().get(0)).getCoord().getY() , 1.0, MatsimTestUtils.EPSILON);
		Assert.assertEquals("wrong mode in selected plan in person 1", ((Leg)p1.getSelectedPlan().getPlanElements().get(1)).getMode() , TransportMode.walk);
		
		Assert.assertEquals("wrong coordinate in unselected plan in person 1", ((Activity)p1.getPlans().get(1).getPlanElements().get(0)).getCoord().getX() , 2.0, MatsimTestUtils.EPSILON);
		Assert.assertEquals("wrong coordinate in unselected plan in person 1", ((Activity)p1.getPlans().get(1).getPlanElements().get(0)).getCoord().getY() , 2.0, MatsimTestUtils.EPSILON);
		Assert.assertEquals("wrong mode in unselected plan in person 1", ((Leg)p1.getPlans().get(1).getPlanElements().get(1)).getMode() , TransportMode.bike);		
		
		Assert.assertEquals("wrong coordinate in selected plan in person 3", ((Activity)p3.getSelectedPlan().getPlanElements().get(0)).getCoord().getX() , 3.0, MatsimTestUtils.EPSILON);
		Assert.assertEquals("wrong coordinate in selected plan in person 3", ((Activity)p3.getSelectedPlan().getPlanElements().get(0)).getCoord().getY() , 3.0, MatsimTestUtils.EPSILON);
		Assert.assertEquals("wrong mode in selected plan in person 3", ((Leg)p3.getSelectedPlan().getPlanElements().get(1)).getMode() , TransportMode.pt);		

		
		
	}

		
	
}
