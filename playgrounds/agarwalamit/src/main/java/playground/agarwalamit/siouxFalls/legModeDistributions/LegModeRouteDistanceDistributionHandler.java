/* *********************************************************************** *
 * project: org.matsim.*
 * LegModeDistanceDistribution.java
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
package playground.agarwalamit.siouxFalls.legModeDistributions;

import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.pt.PtConstants;

import playground.vsp.analysis.modules.AbstractAnalyisModule;

/**
 * Provides leg mode distance distribution, distances are calculated from routes of selected plans 
 * unlike playground.vsp.analysis.modules.legModeDistanceDistribution where beeline distance is calculated
 *
 * Also returns mode2PersonId2RouteDistances.
 * @author amit
 */
public class LegModeRouteDistanceDistributionHandler extends AbstractAnalyisModule{
	private final static Logger log = Logger.getLogger(LegModeRouteDistanceDistributionHandler.class);

	private Scenario scenario;
	private final List<Integer> distanceClasses;
	private final SortedSet<String> usedModes;

	private SortedMap<String, Map<Integer, Integer>> mode2DistanceClass2LegCount;
	private SortedMap<String, Map<Id, List<Double>>> mode2PersonId2dist;

	public LegModeRouteDistanceDistributionHandler(){
		super(LegModeRouteDistanceDistributionHandler.class.getSimpleName());
		log.info("enabled");

		this.distanceClasses = new ArrayList<Integer>();
		this.usedModes = new TreeSet<String>();
		this.mode2PersonId2dist = new TreeMap<String, Map<Id,List<Double>>>();
	}

	public void init(Scenario sc){
		this.scenario = sc;
		initializeDistanceClasses(this.scenario.getPopulation());
		initializeUsedModes(this.scenario.getPopulation());

		for(String mode:this.usedModes){
			mode2PersonId2dist.put(mode, new HashMap<Id, List<Double>>());
		}
	}

	@Override
	public List<EventHandler> getEventHandler() {
		return new LinkedList<EventHandler>();
	}

	@Override
	public void preProcessData() {

		log.info("Checking if the plans file that will be analyzed is based on a run with simulated public transport.");
		log.info("Transit activities and belonging transit walk legs will be removed from the plan.");

		for (Person person : this.scenario.getPopulation().getPersons().values()){
			for (Plan plan : person.getPlans()){
				List<PlanElement> planElements = plan.getPlanElements();
				for (int i = 0, n = planElements.size(); i < n; i++) {
					PlanElement pe = planElements.get(i);
					if (pe instanceof Activity) {
						Activity act = (Activity) pe;
						if (PtConstants.TRANSIT_ACTIVITY_TYPE.equals(act.getType())) {
							PlanElement previousPe = planElements.get(i-1);
							if (previousPe instanceof Leg) {
								Leg previousLeg = (Leg) previousPe;
								previousLeg.setMode(TransportMode.pt);
								previousLeg.setRoute(null);
							} else {
								throw new RuntimeException("A transit activity should follow a leg! Aborting...");
							}
							((PlanImpl) plan).removeActivity(i); // also removes the following leg
							n -= 2;
							i--;
						}
					}
				}
			}
		}
	}

	@Override
	public void postProcessData() {
		this.mode2DistanceClass2LegCount = calculateMode2DistanceClass2LegCount(this.scenario.getPopulation());
	}

	@Override
	public void writeResults(String outputFolder) {
		
		String outFile = outputFolder + "legModeRouteDistanceDistribution.txt";
		try{
			BufferedWriter writer1 = IOUtils.getBufferedWriter(outFile);
			writer1.write("#");
			for(String mode : this.usedModes){
				writer1.write("\t" + mode);
			}
			writer1.write("\t" + "sum");
			writer1.write("\n");
			for(int i = 0; i < this.distanceClasses.size() - 1 ; i++){
				writer1.write(this.distanceClasses.get(i+1) + "\t");
				Integer totalLegsInDistanceClass = 0;
				for(String mode : this.usedModes){
					Integer modeLegs = null;
					modeLegs = this.mode2DistanceClass2LegCount.get(mode).get(this.distanceClasses.get(i + 1));
					totalLegsInDistanceClass = totalLegsInDistanceClass + modeLegs;
					writer1.write(modeLegs.toString() + "\t");
				}
				writer1.write(totalLegsInDistanceClass.toString());
				writer1.write("\n");
			}
			writer1.close(); //Close the output stream

			log.info("Finished writing output to " + outFile);

		}catch (Exception e){
			log.error("Data is not written. Reason " + e.getMessage());
		}
	}

	private SortedMap<String, Map<Integer, Integer>> calculateMode2DistanceClass2LegCount(Population pop) {
		SortedMap<String, Map<Integer, Integer>> mode2DistanceClassNoOfLegs = new TreeMap<String, Map<Integer, Integer>>();

		for(String mode : this.usedModes){
			SortedMap<Integer, Integer> distanceClass2NoOfLegs = new TreeMap<Integer, Integer>();
			for(int i = 0; i < this.distanceClasses.size() - 1 ; i++){
				Integer noOfLegs = 0;
				for(Person person : pop.getPersons().values()){
					PlanImpl plan = (PlanImpl) person.getSelectedPlan();
					List<PlanElement> planElements = plan.getPlanElements();
					for(PlanElement pe : planElements){
						if(pe instanceof Leg){
							Leg leg = (Leg)pe;
							Route route = ((Route)((Leg)pe).getRoute());
							double distance = route.getDistance();
							//							if (route instanceof NetworkRoute) {
							//								distance = RouteUtils.calcDistance((NetworkRoute) route, net);
							//							} else distance = route.getDistance();
							if(leg.getMode().equals(mode)){
								if(distance > this.distanceClasses.get(i) && distance <= this.distanceClasses.get(i + 1)){
									noOfLegs++;
								} 
							}	
						}
					}
				}
				distanceClass2NoOfLegs.put(this.distanceClasses.get(i + 1), noOfLegs);
			}
			mode2DistanceClassNoOfLegs.put(mode, distanceClass2NoOfLegs);
		}
		return mode2DistanceClassNoOfLegs;
	}

	private void calculateMode2PersonId2Distances(Population pop) {
		for(Person person : pop.getPersons().values()){
			PlanImpl plan = (PlanImpl) person.getSelectedPlan();
			List<PlanElement> planElements = plan.getPlanElements();
			for(PlanElement pe : planElements){
				if(pe instanceof Leg){
					Leg leg = (Leg)pe;
					Route route = ((Route)((Leg)pe).getRoute());
					double distance = route.getDistance();
					Map<Id, List<Double>> personId2dist = mode2PersonId2dist.get(leg.getMode());
					if(personId2dist.containsKey(person.getId())){
						List<Double> dists = personId2dist.get(person.getId());
						dists.add(distance);
						personId2dist.put(person.getId(), dists);
					} else {
						List<Double> dists = new ArrayList<Double>();
						dists.add(distance);
						personId2dist.put(person.getId(), dists);
					}
				}
			}
		}
	}

	private double getLongestDistance(Population pop){
		double longestDistance = 0.0;
		for(Person person : pop.getPersons().values()){
			PlanImpl plan = (PlanImpl) person.getSelectedPlan();
			List<PlanElement> planElements = plan.getPlanElements();
			for(PlanElement pe : planElements){
				if(pe instanceof Leg ){
					Route route = ((Route)((Leg)pe).getRoute());
					double distance = route.getDistance();
					if(distance > longestDistance){
						longestDistance = distance;
					}
				}
			}
		}
		log.info("The longest distance is found to be: " + longestDistance);
		return longestDistance;
	}

	private void initializeDistanceClasses(Population pop) {
		double longestDistance = getLongestDistance(pop);
		int endOfDistanceClass = 0;
		int classCounter = 0;
		this.distanceClasses.add(endOfDistanceClass);

		while(endOfDistanceClass <= longestDistance){
			endOfDistanceClass = 100 * (int) Math.pow(2, classCounter);
			classCounter++;
			this.distanceClasses.add(endOfDistanceClass);
		}
		log.info("The following distance classes were defined: " + this.distanceClasses);
	}

	private void initializeUsedModes(Population pop) {
		for(Person person : pop.getPersons().values()){
			for(PlanElement pe : person.getSelectedPlan().getPlanElements()){
				if(pe instanceof Leg){
					Leg leg = (Leg) pe;
					this.usedModes.add(leg.getMode());
				}
			}
		}
		log.info("The following transport modes are considered: " + this.usedModes);
	}

	public SortedMap<String, Map<Integer, Integer>> getMode2DistanceClass2LegCount() {
		return this.mode2DistanceClass2LegCount;
	}

	public SortedMap<String, Map<Id, List<Double>>> getMode2PersonId2RouteDistance(){
		calculateMode2PersonId2Distances(scenario.getPopulation());
		return this.mode2PersonId2dist;
	}
}