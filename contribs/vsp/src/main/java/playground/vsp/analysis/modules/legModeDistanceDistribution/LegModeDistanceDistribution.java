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
package playground.vsp.analysis.modules.legModeDistanceDistribution;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.pt.PtConstants;

import playground.vsp.analysis.modules.AbstractAnalysisModule;

/**
 * This tool calculates modal split over predefined distance classes.
 * 
 * WARNING: The tool depends on plans only, no events are processed!
 * Trip distances are beeline distances between activity locations.
 * The actual (driven, teleported, ...) distances may differ significantly. 
 * 
 * @author aneumann, benjamin, ihab
 *
 */
public class LegModeDistanceDistribution extends AbstractAnalysisModule{
	private final static Logger log = Logger.getLogger(LegModeDistanceDistribution.class);

	private Scenario scenario;
	private final List<Integer> distanceClasses;
	private final SortedSet<String> usedModes;

	private SortedMap<String, Map<Integer, Integer>> mode2DistanceClass2LegCount;
	private SortedMap<String, Integer> mode2LegCount;
	private SortedMap<String, Double> mode2Share;

	public LegModeDistanceDistribution(){
		// why are the following two lines necessary? bk oct'12
		super(LegModeDistanceDistribution.class.getSimpleName());
		log.info("enabled");

		this.distanceClasses = new ArrayList<Integer>();
		this.usedModes = new TreeSet<String>();
	}

	public void init(Scenario sc){
		this.scenario = sc;
		initializeDistanceClasses(this.scenario.getPopulation());
		initializeUsedModes(this.scenario.getPopulation());
	}

	@Override
	public List<EventHandler> getEventHandler() {
		// nothing to return
		return new LinkedList<EventHandler>();
	}

	@Override
	public void preProcessData() {
		
		log.info("Checking if the plans file that will be analyzed is based on a run with simulated public transport.");
		log.info("Transit activities and belonging transit walk legs will be removed from the plan.");
		log.info("However, there might be some transit walk legs remaining without previous transit activity.");
		
		for (Person person : this.scenario.getPopulation().getPersons().values()){
			Plan selectedPlan = person.getSelectedPlan();
			List<PlanElement> planElements = selectedPlan.getPlanElements();
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
						PopulationUtils.removeActivity(((Plan) selectedPlan), index); // also removes the following leg
						n -= 2;
						i--;
					}
				}
			}
		}
		int transitWalkInterpretation = 0;
		for (Person person : this.scenario.getPopulation().getPersons().values()){
			Plan selectedPlan = person.getSelectedPlan();
			List<PlanElement> planElements = selectedPlan.getPlanElements();
			for (int i = 0, n = planElements.size(); i < n; i++) {
				PlanElement pe = planElements.get(i);
				if (pe instanceof Leg) {
					String legMode = ((Leg) pe).getMode();
					if(legMode.equals(TransportMode.transit_walk)){
						((Leg) pe).setMode(TransportMode.walk);
						if(transitWalkInterpretation  < 1){
							log.info("Transit walk leg without previous transit activity detected. It will be interpreted as walk leg.");
							log.info(Gbl.ONLYONCE);
						}
						transitWalkInterpretation ++ ;
					}
				}
			}
		}
		log.info(transitWalkInterpretation + " transit walk legs without previous transit activity were interpreted as walk legs.");
	}

	@Override
	public void postProcessData() {
		this.mode2DistanceClass2LegCount = calculateMode2DistanceClass2LegCount(this.scenario.getPopulation());
		this.mode2LegCount = calculateMode2LegCount(this.scenario.getPopulation());
		this.mode2Share = calculateModeShare(this.mode2LegCount);
	}

	@Override
	public void writeResults(String outputFolder) {
		
//		File outputFolderFile = new File(outputFolder);
//		outputFolderFile.mkdirs();
		
		String outFile = outputFolder + "legModeDistanceDistribution.txt";
		try{
			FileWriter fstream = new FileWriter(outFile);			
			BufferedWriter writer1 = new BufferedWriter(fstream);
			writer1.write("#");
			for(String mode : this.usedModes){
				writer1.write("\t" + mode);
			}
			writer1.write("\t" + "sum");
			writer1.write("\n");
			for(int i = 0; i < this.distanceClasses.size() - 1 ; i++){
				//	Integer middleOfDistanceClass = ((this.distanceClasses.get(i) + this.distanceClasses.get(i + 1)) / 2);
				//	out.write(middleOfDistanceClass + "\t");
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

			BufferedWriter writer2 = IOUtils.getBufferedWriter(outputFolder + "legModeShare.txt");
			writer2.write("# mode\tshare"); writer2.newLine();
			for (Entry<String, Double> modeShareEntry : this.mode2Share.entrySet()) {
				writer2.write(modeShareEntry.getKey() + "\t" + modeShareEntry.getValue()); writer2.newLine();
			}
			writer2.flush();
			writer2.close(); //Close the output stream
			log.info("Finished writing output to " + outFile);
			
		}catch (Exception e){
			log.error("Error: " + e.getMessage());
		}
		
//		log.info("Dumping population for control in case legs were cut out (simulated public transport)...");
//		PopulationWriter popWriter = new PopulationWriter(this.scenario.getPopulation(), null);
//		popWriter.write(outputFolder + "plans_modified.xml.gz");
	}

	private SortedMap<String, Double> calculateModeShare(SortedMap<String, Integer> mode2NoOfLegs) {
		SortedMap<String, Double> mode2Pct = new TreeMap<String, Double>();
		int totalNoOfLegs = 0;
		for(String mode : mode2NoOfLegs.keySet()){
			int modeLegs = mode2NoOfLegs.get(mode);
			totalNoOfLegs += modeLegs;
		}
		for(String mode : mode2NoOfLegs.keySet()){
			double share = 100. * (double) mode2NoOfLegs.get(mode) / totalNoOfLegs;
			mode2Pct.put(mode, share);
		}
		return mode2Pct;
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

	private SortedMap<String, Map<Integer, Integer>> calculateMode2DistanceClass2LegCount(Population pop) {
		SortedMap<String, Map<Integer, Integer>> mode2DistanceClassNoOfLegs = new TreeMap<String, Map<Integer, Integer>>();

		for(String mode : this.usedModes){
			SortedMap<Integer, Integer> distanceClass2NoOfLegs = new TreeMap<Integer, Integer>();
			for(int i = 0; i < this.distanceClasses.size() - 1 ; i++){
				Integer noOfLegs = 0;
				for(Person person : pop.getPersons().values()){
					Plan plan = (Plan) person.getSelectedPlan();
					List<PlanElement> planElements = plan.getPlanElements();
					for(PlanElement pe : planElements){
						if(pe instanceof Leg){
							Leg leg = (Leg) pe;
							String legMode = leg.getMode();
							final Leg leg2 = leg;
							Coord from = PopulationUtils.getPreviousActivity(plan, leg2).getCoord();
							final Leg leg1 = leg;
							Coord to = PopulationUtils.getNextActivity(plan, leg1).getCoord();
							Double legBeelineDist = CoordUtils.calcEuclideanDistance(from, to);

							if(legMode.equals(mode)){
								if(legBeelineDist > this.distanceClasses.get(i) && legBeelineDist <= this.distanceClasses.get(i + 1)){
									noOfLegs++;
								} else {
									// TODO: counter for legs that are longer than the highest distance class
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

	private void initializeDistanceClasses(Population pop) {
		double longestBeelineDistance = getLongestBeelineDistance(pop);
		int endOfDistanceClass = 0;
		int classCounter = 0;
		this.distanceClasses.add(endOfDistanceClass);
		
		while(endOfDistanceClass <= longestBeelineDistance){
//			endOfDistanceClass = endOfDistanceClass + 1000;
			endOfDistanceClass = 100 * (int) Math.pow(2, classCounter);
			classCounter++;
			this.distanceClasses.add(endOfDistanceClass);
		}
		log.info("The following distance classes were defined: " + this.distanceClasses);
	}

	private double getLongestBeelineDistance(Population pop){
		double longestBeelineDistance = 0.0;
		for(Person person : pop.getPersons().values()){
			Plan plan = (Plan) person.getSelectedPlan();
			List<PlanElement> planElements = plan.getPlanElements();
			for(PlanElement pe : planElements){
				if(pe instanceof Leg){
					Leg leg = (Leg) pe;
					final Leg leg2 = leg;
					Coord from = PopulationUtils.getPreviousActivity(plan, leg2).getCoord();
					final Leg leg1 = leg;
					Coord to = PopulationUtils.getNextActivity(plan, leg1).getCoord();
					Double legBeelineDist = CoordUtils.calcEuclideanDistance(from, to);
					
					if(legBeelineDist > longestBeelineDistance){
						longestBeelineDistance = legBeelineDist;
					}
				}
			}
		}
		log.info("The longest beeline distance between two activity locations is found to be: " + longestBeelineDistance);
		return longestBeelineDistance;
	}
	
	private void initializeUsedModes(Population pop) {
		int transitWalkCounter = 0;
		for(Person person : pop.getPersons().values()){
			for(PlanElement pe : person.getSelectedPlan().getPlanElements()){
				if(pe instanceof Leg){
					Leg leg = (Leg) pe;
					String legMode = leg.getMode();
					if(!this.usedModes.contains(legMode))
						if(legMode.equals(TransportMode.transit_walk)){
							if(transitWalkCounter < 1){
								log.info("Transit walk leg detected."
										+ " If it belongs to a pt leg, it will be removed."
										+ " If it does not belong to a pt leg, it will be interpreted as walk leg.");
								log.info(Gbl.ONLYONCE);
							}
							transitWalkCounter ++;
						} else {
							this.usedModes.add(legMode);
						}
				}
			}
		}
		log.info("The following transport modes are considered: " + this.usedModes);
	}

	public SortedMap<String, Map<Integer, Integer>> getMode2DistanceClass2LegCount() {
		return this.mode2DistanceClass2LegCount;
	}

	public void setMode2DistanceClass2LegCount(
			SortedMap<String, Map<Integer, Integer>> mode2DistanceClass2LegCount) {
		this.mode2DistanceClass2LegCount = mode2DistanceClass2LegCount;
	}

	public SortedMap<String, Double> getMode2Share() {
		return this.mode2Share;
	}

	public void setMode2Share(SortedMap<String, Double> mode2Share) {
		this.mode2Share = mode2Share;
	}
}