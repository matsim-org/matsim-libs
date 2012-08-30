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
package playground.andreas.aas.modules.legModeDistanceDistribution;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.io.IOUtils;

import playground.andreas.aas.modules.AbstractAnalyisModule;

/**
 * Beware! This one depends on plans only. No events are processed. Thus, the actual distance may differ significantly. 
 * 
 * @author aneumann, benjamin
 *
 */
public class LegModeDistanceDistribution extends AbstractAnalyisModule{
	private final static Logger log = Logger.getLogger(LegModeDistanceDistribution.class);

	private ScenarioImpl scenario;
	private final List<Integer> distanceClasses;

	//	private final UserGroup group2analyze = UserGroup.MID;
	private final UserGroup group2analyze = null;

	private boolean considerUserGroupOnly;
	private final SortedSet<String> usedModes;

	private SortedMap<String, Map<Integer, Integer>> mode2DistanceClass2LegCount;

	private SortedMap<String, Integer> mode2LegCount;

	private SortedMap<String, Double> mode2Share;

	public LegModeDistanceDistribution(String ptDriverPrefix){
		super("LegModeDistanceDistribution", ptDriverPrefix);
		log.info("enabled");

		this.distanceClasses = new ArrayList<Integer>();
		this.usedModes = new TreeSet<String>();

		if(group2analyze == null){
			this.considerUserGroupOnly = false;
		} else {
			this.considerUserGroupOnly = true;
		}
	}

	public void init(ScenarioImpl scenario, int noOfDistanceClasses){
		this.scenario = scenario;

		initializeDistanceClasse(noOfDistanceClasses);
		initializeUsedModes(this.scenario.getPopulation());
	}

	@Override
	public EventHandler getEventHandler() {
		return null;
	}

	@Override
	public void preProcessData() {
		// nothing to do here
	}

	@Override
	public void postProcessData() {
		if(this.considerUserGroupOnly){
			log.warn("Values are calculated for " + group2analyze + " ...");
			Population relevantPop = new PersonFilter().getPopulation(this.scenario.getPopulation(), group2analyze);
			this.mode2DistanceClass2LegCount = calculateMode2DistanceClass2LegCount(relevantPop);
			this.mode2LegCount = calculateMode2LegCount(relevantPop);
		} else {
			log.warn("Values are calculated for the whole population ...");
			this.mode2DistanceClass2LegCount = calculateMode2DistanceClass2LegCount(this.scenario.getPopulation());
			this.mode2LegCount = calculateMode2LegCount(this.scenario.getPopulation());
		}

		this.mode2Share = calculateModeShare(this.mode2LegCount);
	}

	@Override
	public void writeResults(String outputFolder) {
		String outFile = outputFolder + "legModeDistanceDistribution.txt";
		try{
			FileWriter fstream = new FileWriter(outFile);			
			BufferedWriter out = new BufferedWriter(fstream);
			out.write("#");
			for(String mode : this.usedModes){
				out.write("\t" + mode);
			}
			out.write("\t" + "sum");
			out.write("\n");
			for(int i = 0; i < this.distanceClasses.size() - 1 ; i++){
				//	Integer middleOfDistanceClass = ((this.distanceClasses.get(i) + this.distanceClasses.get(i + 1)) / 2);
				//	out.write(middleOfDistanceClass + "\t");
				out.write(this.distanceClasses.get(i+1) + "\t");
				Integer totalLegsInDistanceClass = 0;
				for(String mode : this.usedModes){
					Integer modeLegs = null;
					modeLegs = this.mode2DistanceClass2LegCount.get(mode).get(this.distanceClasses.get(i + 1));
					totalLegsInDistanceClass = totalLegsInDistanceClass + modeLegs;
					out.write(modeLegs.toString() + "\t");
				}
				out.write(totalLegsInDistanceClass.toString());
				out.write("\n");
			}
			//Close the output stream
			out.close();

			BufferedWriter writer = IOUtils.getBufferedWriter(outputFolder + "modeShares.txt");
			writer.write("# mode\tshare"); writer.newLine();
			for (Entry<String, Double> modeShareEntry : this.mode2Share.entrySet()) {
				writer.write(modeShareEntry.getKey() + "\t" + modeShareEntry.getValue()); writer.newLine();
			}
			writer.flush();
			writer.close();

			log.info("Finished writing output to " + outFile);
		}catch (Exception e){
			log.error("Error: " + e.getMessage());
		}
	}

	private SortedMap<String, Double> calculateModeShare(SortedMap<String, Integer> mode2NoOfLegs) {
		SortedMap<String, Double> mode2Share = new TreeMap<String, Double>();
		int totalNoOfLegs = 0;
		for(String mode : mode2NoOfLegs.keySet()){
			int modeLegs = mode2NoOfLegs.get(mode);
			totalNoOfLegs += modeLegs;
		}
		for(String mode : mode2NoOfLegs.keySet()){
			double share = 100. * (double) mode2NoOfLegs.get(mode) / totalNoOfLegs;
			mode2Share.put(mode, share);
		}
		return mode2Share;
	}

	private SortedMap<String, Integer> calculateMode2LegCount(Population population) {
		SortedMap<String, Integer> mode2NoOfLegs = new TreeMap<String, Integer>();

		for(Person person : population.getPersons().values()){
			Plan plan = person.getSelectedPlan();
			for (PlanElement pe : plan.getPlanElements()){
				if(pe instanceof Leg){
					String mode = ((Leg) pe).getMode();

					if(mode2NoOfLegs.get(mode) == null){
						mode2NoOfLegs.put(mode, 1);
					} else {
						int legsSoFar = mode2NoOfLegs.get(mode);
						int legsAfter = legsSoFar + 1;
						mode2NoOfLegs.put(mode, legsAfter);
					}
				}
			}
		}
		return mode2NoOfLegs;
	}

	private SortedMap<String, Map<Integer, Integer>> calculateMode2DistanceClass2LegCount(Population population) {
		SortedMap<String, Map<Integer, Integer>> mode2DistanceClassNoOfLegs = new TreeMap<String, Map<Integer, Integer>>();

		for(String mode : this.usedModes){
			SortedMap<Integer, Integer> distanceClass2NoOfLegs = new TreeMap<Integer, Integer>();
			for(int i = 0; i < this.distanceClasses.size() - 1 ; i++){
				Integer noOfLegs = 0;
				for(Person person : population.getPersons().values()){
					PlanImpl plan = (PlanImpl) person.getSelectedPlan();
					List<PlanElement> planElements = plan.getPlanElements();
					for(PlanElement pe : planElements){
						if(pe instanceof Leg){
							Leg leg = (Leg) pe;
							String legMode = leg.getMode();
							Coord from = plan.getPreviousActivity(leg).getCoord();
							Coord to = plan.getNextActivity(leg).getCoord();
							Double legDist = CoordUtils.calcDistance(from, to);

							if(legMode.equals(mode)){
								if(legDist > this.distanceClasses.get(i) && legDist <= this.distanceClasses.get(i + 1)){
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

	private void initializeDistanceClasse(int noOfDistanceClasses) {
		this.distanceClasses.add(0);
		for(int noOfClasses = 0; noOfClasses < noOfDistanceClasses; noOfClasses++){
			int distanceClass = 100 * (int) Math.pow(2, noOfClasses);
			this.distanceClasses.add(distanceClass);
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
	}
}