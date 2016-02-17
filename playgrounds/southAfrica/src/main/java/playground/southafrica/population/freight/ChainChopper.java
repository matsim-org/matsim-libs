/* *********************************************************************** *
 * project: org.matsim.*
 * ChainChopper.java
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.misc.Time;

import playground.southafrica.utilities.Header;

/**
 * Class to <i>chop</i> a {@link Plan} into segments that each fit into a 
 * 24:00:00 period. This is mainly used for commercial vehicle activity chains
 * that tend to exceed 24:00:00. The agent, or {@link Person}, is replaced 
 * with any <i><b>one</b></i> of the resulting segments of the plan.
 * 
 * @author jwjoubert
 */
public class ChainChopper {
	final private static Logger LOG = Logger.getLogger(ChainChopper.class);
	final public static Double AVERAGE_SPEED = 60.0/3.6;
	final public static Double CROWFLY_FACTOR = 1.3;
	
	private static Map<Integer, Integer> segmentCountMap = new TreeMap<Integer, Integer>();

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(ChainChopper.class.toString(), args);
		String inputPopulation = args[0];
		String outputPopulation = args[1];
		
		Scenario scInput = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Scenario scOutput = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimPopulationReader(scInput).parse(inputPopulation);;
		for(Person person : scInput.getPopulation().getPersons().values()){
			Plan plan = person.getSelectedPlan();
			List<Plan> list = ChainChopper.chop(plan); 
			Plan segment = ChainChopper.selectRandomSegment(list);
			person.removePlan(plan);
			person.addPlan(segment);
			person.setSelectedPlan(segment);
			scOutput.getPopulation().addPerson(person);
		}
		
		ChainChopper.reportSegmentCounts();
		
		new PopulationWriter(scOutput.getPopulation()).write(outputPopulation);

		Header.printFooter();
	}

	
	/**
	 * Constructor hidden from the outside.
	 */
	private ChainChopper() {
	}

	
	/**
	 * Chops a given plan into segments. Each segment is a plan on its own and 
	 * fitting within a 24-hour period.
	 * @param plan
	 * @return
	 */
	public static List<Plan> chop(Plan plan){
		List<Plan> segments = new ArrayList<>();

		double cumTime = 0.0;
		Coord lastLocation = null;
		PlanImpl currentPlan = new PlanImpl();
		for(int i=0; i<plan.getPlanElements().size(); i+=2){
			Activity act = (Activity)plan.getPlanElements().get(i);
			double startTime;
			double endTime;
			double distance;
			double travelTime = 0.0;
			if(i == 0){
				startTime = 0.0;
				endTime = act.getEndTime();
			} else{
				distance = CoordUtils.calcEuclideanDistance(lastLocation, act.getCoord()) * CROWFLY_FACTOR;
				travelTime = distance / AVERAGE_SPEED;
				startTime = cumTime + travelTime;
				if(i == plan.getPlanElements().size()-1){
					endTime = Time.UNDEFINED_TIME;
				} else{
					endTime = startTime + act.getMaximumDuration();
				}
			}

			if(startTime > Time.MIDNIGHT){
				/* Midnight was crossed while travelling. */
				if(i == 0){
					LOG.error("First activity should start on the first day.");
				} else{
					/* Calculate time fraction in current day. */
					double tFraction = (Time.MIDNIGHT - cumTime) / travelTime;
					Coord c1 = lastLocation;
					Coord c2 = act.getCoord();

					/* Calculate the estimate cut-off position at midnight. */
					double xCut = c1.getX() + (c2.getX() - c1.getX())*tFraction;
					double yCut = c1.getY() + (c2.getY() - c1.getY())*tFraction;
					Coord cCut = CoordUtils.createCoord(xCut, yCut);

					/* Add the proportional section to the end of the 
					 * current plan. */
					Leg leg = (Leg)plan.getPlanElements().get(i-1);
					currentPlan.addLeg(leg);
					Activity cutActivityEnd = new ActivityImpl("chopEnd", cCut);
					currentPlan.addActivity(cutActivityEnd);
					PlanImpl segment = new PlanImpl();
					segment.copyFrom(currentPlan);
					segments.add(segment);

					/* Add the remaining portion to the start of the new
					 * plan. */
					currentPlan = new PlanImpl();
					Activity cutActivityStart = new ActivityImpl("chopStart", cCut);
					cutActivityStart.setEndTime(Time.parseTime("00:01:00"));
					currentPlan.addActivity(cutActivityStart);
					lastLocation = cCut;
					cumTime = 0.0;
				}

				/* If it is the last activity, just add the current plan to the 
				 * list of segment, provided it has at least 3 plan elements. */
				if(i == plan.getPlanElements().size()-1){
					Leg leg = (Leg)plan.getPlanElements().get(i-1);
					currentPlan.addLeg(leg);
					currentPlan.addActivity(act);
					
					/* Add the final segment to the list. */
					PlanImpl segment = new PlanImpl();
					segment.copyFrom(currentPlan);
					segments.add(segment);
					
					currentPlan = new PlanImpl();
				}
			} else{
				if(endTime > Time.MIDNIGHT){
					/* This activity ran over midnight. First add the 
					 * previous leg. */
					Leg leg = (Leg)plan.getPlanElements().get(i-1);
					currentPlan.addLeg(leg);

					/* Split the activity. Add one portion to the end of 
					 * the current plan. */
					Activity endPortion = new ActivityImpl(act.getType(), act.getCoord());
					endPortion.setStartTime(startTime);
					currentPlan.addActivity(endPortion);
					endPortion.setMaximumDuration(Time.UNDEFINED_TIME);
					PlanImpl segment = new PlanImpl();
					segment.copyFrom(currentPlan);
					segments.add(segment);

					/* Add the remainder to the start of the new plan. */
					currentPlan = new PlanImpl();
					Activity startPortion = new ActivityImpl(act.getType(), act.getCoord());
					startPortion.setEndTime(endTime - Time.MIDNIGHT);
					currentPlan.addActivity(startPortion);

					lastLocation = act.getCoord();
					cumTime = startPortion.getEndTime();
				} else{
					/* Just add the activity, with the previous leg, to 
					 * the current plan. */
					if(i == 0){
						cumTime = act.getEndTime(); 
					} else{
						Leg leg = (Leg)plan.getPlanElements().get(i-1);
						currentPlan.addLeg(leg);
						cumTime = endTime;
					}
					currentPlan.addActivity(act);
					lastLocation = act.getCoord();
				}
			}
			
			/* Add the remainder of the current plan to the list of segments. */
			if(i == plan.getPlanElements().size()-1){
				if(currentPlan.getPlanElements().size() >= 3){
					PlanImpl segment = new PlanImpl();
					segment.copyFrom(currentPlan);
					segments.add(segment);
				}
			}
		}
		
		/* Update the segment map. */
		if(segmentCountMap.containsKey(segments.size())){
			int oldCount = segmentCountMap.get(segments.size());
			segmentCountMap.put(segments.size(), oldCount+1);
		} else{
			segmentCountMap.put(segments.size(), 1);
		}

		return segments;
	}

	
	/**
	 * Randomly selects one plan from a given list of the plans.
	 * 
	 * @param list
	 * @return
	 */
	public static Plan selectRandomSegment(List<Plan> list){
		return list.get(MatsimRandom.getLocalInstance().nextInt(list.size()));
	}
	
	
	/**
	 * Reports the observed occurrences of number of segments.  
	 */
	public static void reportSegmentCounts(){
		LOG.info("Observed segment counts:");
		for(int i : segmentCountMap.keySet()){
			LOG.info(String.format("%4s: %d", String.valueOf(i), segmentCountMap.get(i)));
		}
	}
	
}
