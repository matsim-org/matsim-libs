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
package playground.agarwalamit.analysis.legMode.distributions;

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
public class LegModeRouteDistanceDistributionFromPlansHandler extends AbstractAnalyisModule{
	private final Logger log = Logger.getLogger(LegModeRouteDistanceDistributionFromPlansHandler.class);

	private Scenario scenario;
	private final List<Integer> distanceClasses;
	private final SortedSet<String> usedModes;

	private SortedMap<String, SortedMap<Integer, Integer>> mode2DistanceClass2LegCount ;
	private SortedMap<String, Map<Id<Person>, List<Double>>> mode2PersonId2dist;

	public LegModeRouteDistanceDistributionFromPlansHandler(){
		super(LegModeRouteDistanceDistributionFromPlansHandler.class.getSimpleName());
		this.log.info("enabled");

		this.distanceClasses = new ArrayList<Integer>();
		this.usedModes = new TreeSet<String>();
		this.mode2PersonId2dist = new TreeMap<String, Map<Id<Person>,List<Double>>>();
		this.mode2DistanceClass2LegCount = new TreeMap<String, SortedMap<Integer,Integer>>();
	}

	public void init(Scenario sc){
		this.scenario = sc;
		initializeDistanceClasses(this.scenario.getPopulation());
		initializeUsedModes(this.scenario.getPopulation());

		for(String mode:this.usedModes){
			this.mode2PersonId2dist.put(mode, new HashMap<Id<Person>, List<Double>>());
			SortedMap<Integer, Integer> distClass2Legs = new TreeMap<Integer, Integer>();
			for(int i: this.distanceClasses){
				distClass2Legs.put(i, 0);
			}
			this.mode2DistanceClass2LegCount.put(mode, distClass2Legs);
		}
	}

	@Override
	public List<EventHandler> getEventHandler() {
		return new LinkedList<EventHandler>();
	}

	@Override
	public void preProcessData() {

		this.log.info("Checking if the plans file that will be analyzed is based on a run with simulated public transport.");
		this.log.info("Transit activities and belonging transit walk legs will be removed from the plan.");

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
		calculateMode2DistanceClass2LegCount();
		calculateMode2PersonId2Distances();
	}

	@Override
	public void writeResults(String outputFolder) {

		String outFile = outputFolder + "legModeRouteDistanceDistribution.txt";
		try{
			BufferedWriter writer1 = IOUtils.getBufferedWriter(outFile);
			writer1.write("class");
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
			writer1.close();
			this.log.info("Finished writing output to " + outFile);
		}catch (Exception e){
			this.log.error("Data is not written. Reason " + e.getMessage());
		}
	}

	private void calculateMode2DistanceClass2LegCount() {
		Population pop = this.scenario.getPopulation();
		for(Person person : pop.getPersons().values()){
			PlanImpl plan = (PlanImpl) person.getSelectedPlan();
			List<PlanElement> planElements = plan.getPlanElements();
			for(PlanElement pe : planElements){
				if(pe instanceof Leg){
					Leg leg = (Leg)pe;
					Route route = ((Route)((Leg)pe).getRoute());
					double distance = route.getDistance();
					for(int i=0;i<this.distanceClasses.size()-1;i++){
						if(distance > this.distanceClasses.get(i) && distance <= this.distanceClasses.get(i + 1)){
							SortedMap<Integer, Integer> distanceClass2NoOfLegs = this.mode2DistanceClass2LegCount.get(leg.getMode());	
							int oldLeg = distanceClass2NoOfLegs.get(this.distanceClasses.get(i+1));
							int newLeg = oldLeg+1;
							distanceClass2NoOfLegs.put(this.distanceClasses.get(i+1), newLeg);
						} 
					}
				}
			}
		}
	}

	private void calculateMode2PersonId2Distances() {
		Population pop = this.scenario.getPopulation();
		for(Person person : pop.getPersons().values()){
			PlanImpl plan = (PlanImpl) person.getSelectedPlan();
			List<PlanElement> planElements = plan.getPlanElements();
			for(PlanElement pe : planElements){
				if(pe instanceof Leg){
					Leg leg = (Leg)pe;
					Route route = ((Route)((Leg)pe).getRoute());
					double distance = route.getDistance();
					Map<Id<Person>, List<Double>> personId2dist = this.mode2PersonId2dist.get(leg.getMode());
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
		this.log.info("The longest distance is found to be: " + longestDistance);
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
		this.log.info("The following distance classes were defined: " + this.distanceClasses);
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
		this.log.info("The following transport modes are considered: " + this.usedModes);
	}

	public SortedMap<String, SortedMap<Integer, Integer>> getMode2DistanceClass2LegCount() {
		return this.mode2DistanceClass2LegCount;
	}

	public SortedMap<String, Map<Id<Person>, List<Double>>> getMode2PersonId2RouteDistances(){
		return this.mode2PersonId2dist;
	}
	public SortedMap<String, Map<Id<Person>, Double>> getMode2PersonId2TotalRouteDistance(){
		SortedMap<String, Map<Id<Person>, Double>> mode2PersonId2TotalRouteDist = new TreeMap<String, Map<Id<Person>,Double>>();
		for(String str:this.mode2PersonId2dist.keySet()){
			Map<Id<Person>, Double> personIdeRouteDist = new HashMap<Id<Person>, Double>();
			for(Id<Person> id:this.mode2PersonId2dist.get(str).keySet()){
				double sum=0;
				for(double d:this.mode2PersonId2dist.get(str).get(id)){
					sum +=d;
				}
				personIdeRouteDist.put(id, sum);
			}
			mode2PersonId2TotalRouteDist.put(str, personIdeRouteDist);
		}
		return mode2PersonId2TotalRouteDist;
	}
}