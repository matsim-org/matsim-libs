/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.droeder.ptSubModes.replanning;


import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.PtConstants;
import org.matsim.testcases.MatsimTestCase;


/**
 * @author droeder
 *
 */
public class FixedPtSubModePtInteractionRemoverTest extends MatsimTestCase{
	

	@Test
	public final void testOneLeg() {
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		PopulationFactory factory = sc.getPopulation().getFactory();
		
		Plan plan = factory.createPlan();
		Activity home, work;
		home = factory.createActivityFromCoord("home", new Coord((double) 0, (double) 0));
		work = factory.createActivityFromCoord("work", new Coord((double) 0, (double) 0));
		Leg leg = factory.createLeg("bus");
		
		plan.addActivity(home);
		plan.addLeg(leg);
		plan.addActivity(work);
		
		PtSubModePtInteractionRemover strategy = new PtSubModePtInteractionRemover();
		
		strategy.run(plan);
		Assert.assertEquals("3 planElements expected", 3.0, plan.getPlanElements().size(), MatsimTestCase.EPSILON);
		Assert.assertEquals("expecting activity home", "home", ((Activity)plan.getPlanElements().get(0)).getType());
		Assert.assertEquals("expecting legmode 'bus'", "bus", ((Leg)plan.getPlanElements().get(1)).getMode());
		Assert.assertEquals("expecting activity work", "work", ((Activity)plan.getPlanElements().get(2)).getType());
		
		
	}
	
	@Test
	public final void testMultipleLeg() {
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		PopulationFactory factory = sc.getPopulation().getFactory();
		
		Plan plan = factory.createPlan();
		Activity one, two, three, four, five, six, seven;
		one = factory.createActivityFromCoord("home", new Coord((double) 0, (double) 0));
		two = factory.createActivityFromCoord(PtConstants.TRANSIT_ACTIVITY_TYPE, new Coord((double) 0, (double) 0));
		three = factory.createActivityFromCoord(PtConstants.TRANSIT_ACTIVITY_TYPE, new Coord((double) 0, (double) 0));
		four = factory.createActivityFromCoord("work", new Coord((double) 0, (double) 0));
		five = factory.createActivityFromCoord(PtConstants.TRANSIT_ACTIVITY_TYPE, new Coord((double) 0, (double) 0));
		six = factory.createActivityFromCoord(PtConstants.TRANSIT_ACTIVITY_TYPE, new Coord((double) 0, (double) 0));
		seven = factory.createActivityFromCoord("home", new Coord((double) 0, (double) 0));
		Leg bus = factory.createLeg("bus");
		Leg walk = factory.createLeg(TransportMode.transit_walk); 
		
		plan.addActivity(one);
		plan.addLeg(walk);
		plan.addActivity(two);
		plan.addLeg(bus);
		plan.addActivity(three);
		plan.addLeg(walk);
		plan.addActivity(four);
		plan.addLeg(walk);
		plan.addActivity(five);
		plan.addLeg(bus);
		plan.addActivity(six);
		plan.addLeg(walk);
		plan.addActivity(seven);
		
		PtSubModePtInteractionRemover strategy = new PtSubModePtInteractionRemover();
		
		strategy.run(plan);

		Assert.assertEquals("5 planElements expected", 5.0, plan.getPlanElements().size(), MatsimTestCase.EPSILON);
		Assert.assertEquals("expecting activity home", "home", ((Activity)plan.getPlanElements().get(0)).getType());
		Assert.assertEquals("expecting legmode 'bus'", "bus", ((Leg)plan.getPlanElements().get(1)).getMode());
		Assert.assertEquals("expecting activity work", "work", ((Activity)plan.getPlanElements().get(2)).getType());
		Assert.assertEquals("expecting legmode 'bus'", "bus", ((Leg)plan.getPlanElements().get(3)).getMode());
		Assert.assertEquals("expecting activity home", "home", ((Activity)plan.getPlanElements().get(4)).getType());
	}
	
	@Test
	public final void testOnlyTransitWalk() {
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		PopulationFactory factory = sc.getPopulation().getFactory();
		
		Plan plan = factory.createPlan();
		Activity home, work;
		home = factory.createActivityFromCoord("home", new Coord((double) 0, (double) 0));
		work = factory.createActivityFromCoord("work", new Coord((double) 0, (double) 0));
		Leg leg = factory.createLeg(TransportMode.transit_walk);
		
		plan.addActivity(home);
		plan.addLeg(leg);
		plan.addActivity(work);
		
		PtSubModePtInteractionRemover strategy = new PtSubModePtInteractionRemover();
		
		strategy.run(plan);
		
		Assert.assertEquals("3 planElements expected", 3.0, plan.getPlanElements().size(), MatsimTestCase.EPSILON);
		Assert.assertEquals("expecting activity home", "home", ((Activity)plan.getPlanElements().get(0)).getType());
		Assert.assertEquals("expecting legmode '" + TransportMode.pt + "'", TransportMode.pt, ((Leg)plan.getPlanElements().get(1)).getMode());
		Assert.assertEquals("expecting activity work", "work", ((Activity)plan.getPlanElements().get(2)).getType());
	}
	
	@Test
	public final void testLineSwitch() {
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		PopulationFactory factory = sc.getPopulation().getFactory();
		
		Plan plan = factory.createPlan();
		Activity one, two, three;
		one = factory.createActivityFromCoord("home", new Coord((double) 0, (double) 0));
		two = factory.createActivityFromCoord(PtConstants.TRANSIT_ACTIVITY_TYPE, new Coord((double) 0, (double) 0));
		three = factory.createActivityFromCoord("work", new Coord((double) 0, (double) 0));
		Leg bus = factory.createLeg("bus");
		
		plan.addActivity(one);
		plan.addLeg(bus);
		plan.addActivity(two);
		plan.addLeg(bus);
		plan.addActivity(three);

		PtSubModePtInteractionRemover strategy = new PtSubModePtInteractionRemover();
		
		strategy.run(plan);
		
		Assert.assertEquals("3 planElements expected", 3.0, plan.getPlanElements().size(), MatsimTestCase.EPSILON);
		Assert.assertEquals("expecting activity home", "home", ((Activity)plan.getPlanElements().get(0)).getType());
		Assert.assertEquals("expecting legmode 'bus'", "bus", ((Leg)plan.getPlanElements().get(1)).getMode());
		Assert.assertEquals("expecting activity work", "work", ((Activity)plan.getPlanElements().get(2)).getType());
	}
	
	@Test
	public final void testLineSwitch2Modes() {
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		PopulationFactory factory = sc.getPopulation().getFactory();
		
		Plan plan = factory.createPlan();
		Activity one, two, three;
		one = factory.createActivityFromCoord("home", new Coord((double) 0, (double) 0));
		two = factory.createActivityFromCoord(PtConstants.TRANSIT_ACTIVITY_TYPE, new Coord((double) 0, (double) 0));
		three = factory.createActivityFromCoord("work", new Coord((double) 0, (double) 0));
		Leg bus = factory.createLeg("bus");
		Leg train = factory.createLeg("train");
		
		plan.addActivity(one);
		plan.addLeg(bus);
		plan.addActivity(two);
		plan.addLeg(train);
		plan.addActivity(three);

		PtSubModePtInteractionRemover strategy = new PtSubModePtInteractionRemover();
		
		strategy.run(plan);
		
		Assert.assertEquals("3 planElements expected", 3.0, plan.getPlanElements().size(), MatsimTestCase.EPSILON);
		Assert.assertEquals("expecting activity home", "home", ((Activity)plan.getPlanElements().get(0)).getType());
		Assert.assertEquals("expecting legmode 'pt'", TransportMode.pt, ((Leg)plan.getPlanElements().get(1)).getMode());
		Assert.assertEquals("expecting activity work", "work", ((Activity)plan.getPlanElements().get(2)).getType());
	}

}
