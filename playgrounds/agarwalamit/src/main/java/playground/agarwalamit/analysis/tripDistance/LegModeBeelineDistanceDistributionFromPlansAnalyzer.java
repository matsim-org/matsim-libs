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
package playground.agarwalamit.analysis.tripDistance;

import java.io.BufferedWriter;
import java.util.*;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.pt.PtConstants;
import playground.vsp.analysis.modules.AbstractAnalysisModule;

/**
 * Provides leg mode distance distribution, distances are calculated from routes of selected plans
 * unlike playground.vsp.analysis.modules.legModeDistanceDistribution where beeline distance is calculated
 *
 * Also returns mode2PersonId2RouteDistances.
 * @author amit
 */
public class LegModeBeelineDistanceDistributionFromPlansAnalyzer extends AbstractAnalysisModule{
	private final static Logger LOG = Logger.getLogger(LegModeBeelineDistanceDistributionFromPlansAnalyzer.class);

	private Scenario scenario;
	private final List<Double> distanceClasses;
	private final SortedSet<String> usedModes;

	private final SortedMap<String, SortedMap<Double, Integer>> mode2DistanceClass2LegCount ;
	private final SortedMap<String, Map<Id<Person>, List<Double>>> mode2PersonId2dist;

	public LegModeBeelineDistanceDistributionFromPlansAnalyzer(){
		this (new ArrayList<>());
	}

	public LegModeBeelineDistanceDistributionFromPlansAnalyzer(final List<Double> distClasses ){
		super(LegModeBeelineDistanceDistributionFromPlansAnalyzer.class.getSimpleName());
		LOG.info("enabled");

		this.distanceClasses = distClasses;
		this.usedModes = new TreeSet<>();
		this.mode2PersonId2dist = new TreeMap<>();
		this.mode2DistanceClass2LegCount = new TreeMap<>();
	}

	public void init(final Scenario sc){
		this.scenario = sc;

		if (this.distanceClasses.isEmpty() ) { // default distance classes
			initializeDistanceClasses(this.scenario.getPopulation());
		}

		initializeUsedModes(this.scenario.getPopulation());

		for(String mode:this.usedModes){
			this.mode2PersonId2dist.put(mode, new HashMap<>());
			SortedMap<Double, Integer> distClass2Legs = new TreeMap<>();
			for(Double i: this.distanceClasses){
				distClass2Legs.put(i, 0);
			}
			this.mode2DistanceClass2LegCount.put(mode, distClass2Legs);
		}
	}

	@Override
	public List<EventHandler> getEventHandler() {
		return new LinkedList<>();
	}

	@Override
	public void preProcessData() {

		LOG.info("Checking if the plans file that will be analyzed is based on a run with simulated public transport.");
		LOG.info("Transit activities and belonging transit walk legs will be removed from the plan.");

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
							final int index = i;
							PopulationUtils.removeActivity(plan, index); // also removes the following leg
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
			// making it 0+, 2km+, 10km+ etc. Amit June'17
			for(int i = 0; i < this.distanceClasses.size() ; i++){
				writer1.write(this.distanceClasses.get(i) + "\t");
				Integer totalLegsInDistanceClass = 0;
				for(String mode : this.usedModes){
					Integer modeLegs = null;
					modeLegs = this.mode2DistanceClass2LegCount.get(mode).get(this.distanceClasses.get(i ));
					totalLegsInDistanceClass = totalLegsInDistanceClass + modeLegs;
					writer1.write(modeLegs.toString() + "\t");
				}
				writer1.write(totalLegsInDistanceClass.toString());
				writer1.write("\n");
			}
			writer1.close();
			LOG.info("Finished writing output to " + outFile);
		}catch (Exception e){
			LOG.error("Data is not written. Reason " + e.getMessage());
		}
	}

	private void calculateMode2DistanceClass2LegCount() {
		Population pop = this.scenario.getPopulation();
		for(Person person : pop.getPersons().values()){
			Plan plan = person.getSelectedPlan();
			List<PlanElement> planElements = plan.getPlanElements();
			for(PlanElement pe : planElements){
				if(pe instanceof Leg){
					Leg leg = (Leg)pe;

					String legMode = leg.getMode();
					final Leg leg2 = leg;
					Coord from = PopulationUtils.getPreviousActivity(plan, leg2).getCoord();
					final Leg leg1 = leg;
					Coord to = PopulationUtils.getNextActivity(plan, leg1).getCoord();
					Double legBeelineDist = CoordUtils.calcEuclideanDistance(from, to);

					// making it 0+, 2km+, 10km+ etc. Amit June'17
					for(int i=0;i<this.distanceClasses.size();i++){
						SortedMap<Double, Integer> distanceClass2NoOfLegs = this.mode2DistanceClass2LegCount.get(leg.getMode());
						if ( (i== this.distanceClasses.size()-1) ||
								( legBeelineDist >= this.distanceClasses.get(i) && legBeelineDist < this.distanceClasses.get(i + 1)) ){

							int oldLeg = distanceClass2NoOfLegs.get(this.distanceClasses.get(i));
							distanceClass2NoOfLegs.put(this.distanceClasses.get(i), oldLeg+1);
							this.mode2DistanceClass2LegCount.put(leg.getMode(), distanceClass2NoOfLegs);
						}

					}
				}
			}
		}
	}

	private void calculateMode2PersonId2Distances() {
		Population pop = this.scenario.getPopulation();
		for(Person person : pop.getPersons().values()){
			Plan plan = person.getSelectedPlan();
			List<PlanElement> planElements = plan.getPlanElements();
			for(PlanElement pe : planElements){
				if(pe instanceof Leg){
					Leg leg = (Leg)pe;
					Route route = ((Route)((Leg)pe).getRoute());
					double distance = route != null ? route.getDistance() : 0.;
					Map<Id<Person>, List<Double>> personId2dist = this.mode2PersonId2dist.get(leg.getMode());
					if(personId2dist.containsKey(person.getId())){
						List<Double> dists = personId2dist.get(person.getId());
						dists.add(distance);
						personId2dist.put(person.getId(), dists);
					} else {
						List<Double> dists = new ArrayList<>();
						dists.add(distance);
						personId2dist.put(person.getId(), dists);
					}
				}
			}
		}
	}

	private double getLongestDistance(final Population pop){
		double longestDistance = 0.0;
		for(Person person : pop.getPersons().values()){
			Plan plan = person.getSelectedPlan();
			List<PlanElement> planElements = plan.getPlanElements();
			for(PlanElement pe : planElements){
				if(pe instanceof Leg ){
					Route route = ((Route)((Leg)pe).getRoute());
					double distance = route!=null ? route.getDistance() : 0.;
					if(distance > longestDistance){
						longestDistance = distance;
					}
				}
			}
		}
		LOG.info("The longest distance is found to be: " + longestDistance);
		return longestDistance;
	}

	private void initializeDistanceClasses(final Population pop) {
		double longestDistance = getLongestDistance(pop);
		Double endOfDistanceClass = 0.;
		int classCounter = 0;
		this.distanceClasses.add(endOfDistanceClass);

		while(endOfDistanceClass <= longestDistance){
			endOfDistanceClass = 100 * Math.pow(2, classCounter);
			classCounter++;
			this.distanceClasses.add(endOfDistanceClass);
		}
		LOG.info("The following distance classes were defined: " + this.distanceClasses);
	}

	private void initializeUsedModes(final Population pop) {
		for(Person person : pop.getPersons().values()){
			for(PlanElement pe : person.getSelectedPlan().getPlanElements()){
				if(pe instanceof Leg){
					Leg leg = (Leg) pe;
					this.usedModes.add(leg.getMode());
				}
			}
		}
		LOG.info("The following transport modes are considered: " + this.usedModes);
	}

	public SortedMap<String, SortedMap<Double, Integer>> getMode2DistanceClass2LegCount() {
		return this.mode2DistanceClass2LegCount;
	}

	public SortedMap<String, Map<Id<Person>, List<Double>>> getMode2PersonId2RouteDistances(){
		return this.mode2PersonId2dist;
	}
	public SortedMap<String, Map<Id<Person>, Double>> getMode2PersonId2TotalRouteDistance(){
		SortedMap<String, Map<Id<Person>, Double>> mode2PersonId2TotalRouteDist = new TreeMap<>();
		for(String str:this.mode2PersonId2dist.keySet()){
			Map<Id<Person>, Double> personIdeRouteDist = new HashMap<>();
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