/* *********************************************************************** *
 * project: org.matsim.*
 * ChainChopperTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.southafrica.population.freight;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.misc.Time;


/**
 * Test to check that (mainly commercial vehicle) {@link Plan}s are 'chopped'
 * into 24-hour segments.
 * 
 * @author jwjoubert
 */
public class ChainChopperTest {

	@Test
	public void testChop1() {
		Plan plan = buildPlan1();
		List<Plan> list = ChainChopper.chop(plan);
		Assert.assertEquals("Wrong number of segments", 2, list.size());
		
		/* Segment 1. */
		Plan s1 = list.get(0);
		Activity s1End = ((Activity)s1.getPlanElements().get(s1.getPlanElements().size()-1));
		Assert.assertTrue("Wrong activity type at end.",s1End.getType().equalsIgnoreCase("chopEnd"));
		Assert.assertEquals("Wrong coordinate.", 1.0, s1End.getCoord().getY(), 0.1);
		Assert.assertEquals("Wrong number of plan elements.", 3, s1.getPlanElements().size());
		
		/* Segment 2. */
		Plan s2 = list.get(1);
		Activity s2Start = ((Activity)s2.getPlanElements().get(0));
		Assert.assertTrue("Wrong activity type at start.",s2Start.getType().equalsIgnoreCase("chopStart"));
		Assert.assertEquals("Wrong chain start time.", 0.0, s2Start.getEndTime(), 0.1);
		Assert.assertEquals("Wrong coordinate.", 1.0, s2Start.getCoord().getY(), 0.1);
		Assert.assertEquals("Wrong number of plan elements.", 3, s2.getPlanElements().size());
	}
	
	@Test
	public void testChop2(){
		Plan plan = buildPlan2();
		List<Plan> list = ChainChopper.chop(plan);
		Assert.assertEquals("Wrong number of segments.", 2, list.size());
		
		/* Segment 1. */
		Plan s1 = list.get(0);
		Activity s1End = ((Activity)s1.getPlanElements().get(s1.getPlanElements().size()-1));
		Assert.assertTrue("Wrong activity type at end.",s1End.getType().equalsIgnoreCase("b"));
		Assert.assertEquals("Wrong duration.", Time.UNDEFINED_TIME, s1End.getMaximumDuration(), 0.1);
		Assert.assertEquals("Wrong coordinate.", 1.0, s1End.getCoord().getY(), 0.1);
		Assert.assertEquals("Wrong number of plan elements.", 3, s1.getPlanElements().size());
		
		/* Segment 2. */
		Plan s2 = list.get(1);
		Activity s2Start = ((Activity)s2.getPlanElements().get(0));
		Assert.assertTrue("Wrong activity type at start.",s2Start.getType().equalsIgnoreCase("b"));
		Assert.assertEquals("Wrong chain start time.", Time.parseTime("01:00:00"), s2Start.getEndTime(), 0.1);
		Assert.assertEquals("Wrong coordinate.", 1.0, s2Start.getCoord().getY(), 0.1);
		Assert.assertEquals("Wrong number of plan elements.", 3, s2.getPlanElements().size());
	}
	
	@Test
	public void testChop3(){
		Plan plan = buildPlan3();
		List<Plan> list = ChainChopper.chop(plan);
		Assert.assertEquals("Wrong number of segments.", 1, list.size());
		
		/* Segment 1. */
		Plan s1 = list.get(0);
		Activity s1Start = (Activity)s1.getPlanElements().get(0);
		Assert.assertTrue("Wrong activity type.", s1Start.getType().equalsIgnoreCase("a"));
		Assert.assertEquals("Wrong start time.", Time.UNDEFINED_TIME, s1Start.getStartTime(), 0.1);
		Assert.assertEquals("Wrong end time.", Time.parseTime("16:00:00"), s1Start.getEndTime(), 0.1);
		Assert.assertEquals("Wrong duration.", Time.UNDEFINED_TIME, s1Start.getMaximumDuration(), 0.1);
		Activity s1End = (Activity)s1.getPlanElements().get(2);
		Assert.assertTrue("Wrong activity type.", s1End.getType().equalsIgnoreCase("b"));
		Assert.assertEquals("Wrong start time.", Time.UNDEFINED_TIME, s1End.getStartTime(), 0.1);
		Assert.assertEquals("Wrong end time.", Time.UNDEFINED_TIME, s1End.getEndTime(), 0.1);
		Assert.assertEquals("Wrong duration.", Time.UNDEFINED_TIME, s1End.getMaximumDuration(), 0.1);
	}
	
	
	/**
	 * A test plan for which the 24-hour cut is along the journey.
	 * 
	 * 16:00  |   08:00
	 *   a ---|---> b
	 *        |
	 *      24:00
	 * @return
	 */
	private Plan buildPlan1(){
		PlanImpl plan = new PlanImpl();
		Activity a = new ActivityImpl("a", CoordUtils.createCoord(0.0, 0.0));
		a.setEndTime(Time.parseTime("16:00:00"));
		plan.addActivity(a);
		
		plan.addLeg(new LegImpl("truck"));
		
		/* Create the point far enough away that it will only reach it by 
		 * 08:00 the next morning. */
		double travelTime = Time.parseTime("16:00:00");
		double distance = Math.round((travelTime * ChainChopper.AVERAGE_SPEED) / ChainChopper.CROWFLY_FACTOR);
		
		Activity b = new ActivityImpl("b", CoordUtils.createCoord(distance, 2.0));
		plan.addActivity(b);
		return plan;
	}
	
	/**
	 * A test plan for which the 24-hour cut is over an activity in the middle
	 * of the activity chain.
	 * 
	 *              |
	 * 16:00   23:00-01:00
	 *   a -------> b -------> c
	 *              |
	 *            24:00
	 * @return
	 */
	private Plan buildPlan2(){
		PlanImpl plan = new PlanImpl();
		Activity a = new ActivityImpl("a", CoordUtils.createCoord(0.0, 0.0));
		a.setEndTime(Time.parseTime("16:00:00"));
		plan.addActivity(a);
		
		plan.addLeg(new LegImpl("truck"));
		
		/* Create the point far enough away that it will reach it by 23:00:00 
		 * the evening. */
		double travelTime = Time.parseTime("07:00:00");
		double distance = Math.round((travelTime * ChainChopper.AVERAGE_SPEED) / ChainChopper.CROWFLY_FACTOR);
		
		Activity b = new ActivityImpl("b", CoordUtils.createCoord(distance, 1.0));
		b.setMaximumDuration(Time.parseTime("02:00:00"));
		plan.addActivity(b);
		
		plan.addLeg(new LegImpl("truck"));
		
		Activity c = new ActivityImpl("c", CoordUtils.createCoord(2*distance, 2.0));
		plan.addActivity(c);
		return plan;
	}

	
	/**
	 * A test plan that falls entirely within the 24-hour cut.
	 * 
	 * 16:00     20:00   |
	 *   a ------> b     |
	 *                   |
	 *                 24:00
	 * @return
	 */
	private Plan buildPlan3(){
		PlanImpl plan = new PlanImpl();
		Activity a = new ActivityImpl("a", CoordUtils.createCoord(0.0, 0.0));
		a.setEndTime(Time.parseTime("16:00:00"));
		plan.addActivity(a);
		
		plan.addLeg(new LegImpl("truck"));
		
		/* Create the point far enough away that it will reach it by 20:00 the 
		 * same day. */
		double travelTime = Time.parseTime("06:00:00");
		double distance = Math.round((travelTime * ChainChopper.AVERAGE_SPEED) / ChainChopper.CROWFLY_FACTOR);
		
		Activity b = new ActivityImpl("b", CoordUtils.createCoord(distance, 2.0));
		plan.addActivity(b);
		return plan;
	}
	

}
