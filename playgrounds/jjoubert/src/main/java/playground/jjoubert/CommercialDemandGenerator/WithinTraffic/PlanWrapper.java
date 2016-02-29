/* *********************************************************************** *
 * project: org.matsim.*
 * PlanWrapper.java
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

package playground.jjoubert.CommercialDemandGenerator.WithinTraffic;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.misc.Time;

/**
 * A class to wrap a plan around a given time window. This is achieved by 
 * <i>chopping</i> the chain at midnight. If midnight occurs on a {@link Leg} 
 * between {@link Activity} <i>A</i> and <i>B</i>, a dummy {@link Activity} 
 * with type <i>chop</i> is added at midnight, and the chain segment is 
 * ended. The location of the dummy activity is on the line connecting 
 * <i>AB</i>, and the distance from <i>A</i> is proportional to the {@link 
 * Leg} time. That is, 
 * <br><br><i> (midnight  - A's end time) / (B's start time - A's end time) </i> <br><br>
 * 
 * The {@link Leg} mode is the same as between <i>A</i> and <i>B</i>. The next
 * segment starts with the dummy activity, followed by a {@link Leg} with the
 * same mode as on <i>AB</i>, then {@link Activity} <i>B</i> followed by the 
 * remainder of the chain.
 *
 * @author jwjoubert
 */
public class PlanWrapper {
	private final static Logger LOG = Logger.getLogger(PlanWrapper.class);
	
	/**
	 * The method receives an activity plan, which may or may not span a given time window, and
	 * <i>wrap</i> it into separate plans, each fitting within the time window.
	 *
	 * @param plan of type {@link Plan}
	 * @return an {@code ArrayList} of {@code BasicPlan}s
	 */
	public static List<Plan> wrapPlan(Plan plan, String timeWindow) {
		List<Plan> list = new ArrayList<Plan>();
		PlanImpl tmpPlan = new PlanImpl();
		tmpPlan.copyFrom(plan);
		
		int dayCount = 1;
		
		PlanImpl segment = new PlanImpl();
		int index = 0;
		while(index < tmpPlan.getPlanElements().size()){
			PlanElement pe = tmpPlan.getPlanElements().get(index);
			if(pe instanceof Leg){
				/* Just add the leg. */
				segment.addLeg((Leg) pe);
				index++;
			} else {
				/* Handle the facility. */
				Activity act = (Activity) pe;
				double startTime = index == 0 ? 0 : act.getStartTime();
				double endTime = index == plan.getPlanElements().size()-1 ? Math.max(Time.MIDNIGHT, act.getStartTime()) : act.getEndTime();
				
				if(startTime < Time.MIDNIGHT*dayCount){
					if(endTime < Time.MIDNIGHT*dayCount){
						/* The activity can simply be added as it is entire wholly
						 * or partially within the current day. */
						act.setStartTime(act.getStartTime() - Time.MIDNIGHT*(dayCount-1));
						act.setEndTime(act.getEndTime() - Time.MIDNIGHT*(dayCount-1));
						segment.addActivity(act);
						index++;						
					} else{
						/* The activity itself runs over midnight. Check the
						 * activity type, though. */
						if(act.getType().contains("minor")){
							/* Split it up proportionally. */
							Activity end = new ActivityImpl(act);
							end.setStartTime(end.getStartTime() - Time.MIDNIGHT*(dayCount-1));
							end.setEndTime(Time.MIDNIGHT);
							segment.addActivity(end);
							
							PlanImpl p = new PlanImpl();
							p.copyFrom(segment);
							list.add(p);
												
							/* Start a new segment. */
							Activity start = new ActivityImpl(act);
							start.setStartTime(Time.parseTime("00:00:00"));
							start.setEndTime(start.getEndTime() - Time.MIDNIGHT*(dayCount));
							segment = new PlanImpl();
							segment.addActivity(start);
							dayCount++;
							index++;		
						} else{
							/* It is most probably the end of the chain. */
							if(index != tmpPlan.getPlanElements().size()){
								LOG.error("Non-minor activity in the middle of the chain: " 
										+ act.getType() + ". Behaviour not guaranteed!!");
							}
							act.setStartTime(act.getStartTime() - Time.MIDNIGHT*(dayCount-1));
							act.setEndTime(act.getEndTime() - Time.MIDNIGHT*(dayCount-1));
							segment.addActivity(act);
							PlanImpl p = new PlanImpl();
							p.copyFrom(segment);
							list.add(p);	
							
							/* Start a new segment. */
							Activity start = new ActivityImpl(act);
							start.setStartTime(Time.parseTime("00:00:00"));
							start.setEndTime(act.getEndTime() - Time.MIDNIGHT*(dayCount));
							segment = new PlanImpl();
							segment.addActivity(start);
							dayCount++;
							index++;
						}
					}
				} else {
					/* The activity starts in a new day. Add a dummy activity
					 * at midnight, the location being proportionally between 
					 * the two activities. */
					Activity previousActivity = tmpPlan.getPreviousActivity(tmpPlan.getPreviousLeg(act));
					double chopFraction = (Time.MIDNIGHT*dayCount - previousActivity.getEndTime()) / (act.getStartTime() - previousActivity.getEndTime());
					double distance = CoordUtils.calcEuclideanDistance(act.getCoord(), previousActivity.getCoord()) * chopFraction;
					double dy = act.getCoord().getY() - previousActivity.getCoord().getY();
					double dx = act.getCoord().getX() - previousActivity.getCoord().getX();
					double angle = Math.atan(dy / dx);
					Coord coord = new Coord(distance * Math.cos(angle), distance * Math.sin(angle));
					
					Activity chopEnd = new ActivityImpl("chopEnd", coord);
					chopEnd.setEndTime(Time.MIDNIGHT);
					Activity chopStart = new ActivityImpl("chopStart", coord);
					chopStart.setEndTime(Time.parseTime("00:00:00"));
					
					/* Finish off the current segment. */
					segment.addActivity(chopEnd);
					PlanImpl p = new PlanImpl();
					p.copyFrom(segment);
					list.add(p);
										
					/* Start a new segment. */
					segment = new PlanImpl();
					segment.addActivity(chopStart);
					segment.addLeg(tmpPlan.getPreviousLeg(act));
					Activity firstRealActivity = new ActivityImpl(act);
					firstRealActivity.setStartTime(act.getStartTime() - Time.MIDNIGHT*dayCount);
					firstRealActivity.setEndTime(act.getEndTime() - Time.MIDNIGHT*dayCount);
					segment.addActivity(act);
					
					dayCount++;
					index++;					
				}
			}
		}

		/* Run through the list of plans, and update all activity times 
		 * of those in subsequent days. */
		/*TODO I don't think this is necessary anymore. */
//		for(Plan subplan : list){
//			int daysToSubtract = (int) Math.floor(((PlanImpl)subplan).getFirstActivity().getStartTime() / Time.MIDNIGHT);
//			for(int i = 0; i < daysToSubtract; i++){
//				for(PlanElement pe : subplan.getPlanElements()){
//					if(pe instanceof Activity){
//						Activity act = (Activity)pe;
//						act.setStartTime(act.getStartTime() - daysToSubtract*Time.MIDNIGHT);
//						act.setEndTime(act.getEndTime() - daysToSubtract*Time.MIDNIGHT);
//					}
//				}
//			}
//		}
		return list;
	}	
	
}