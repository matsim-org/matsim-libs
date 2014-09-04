/* *********************************************************************** *
 * project: org.matsim.*
 * AnalyseNmbmSamplePopulation.java
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

package playground.southafrica.population.analysis;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PopulationReaderMatsimV5;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.IOUtils;

import playground.southafrica.population.utilities.PopulationUtils;
import playground.southafrica.utilities.Header;

/**
 * Class to
 *
 * @author jwjoubert
 */
public class AnalyseNmbmSamplePopulation {
	private final static Logger LOG = Logger.getLogger(AnalyseNmbmSamplePopulation.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(AnalyseNmbmSamplePopulation.class.toString(), args);
		
		String outputFolder = args[0];
		/* Read in the population. */
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		PopulationReaderMatsimV5 pr = new PopulationReaderMatsimV5(sc);
		pr.parse(args[1]);
		
		/* Determine statistics for activity types. */
		extractActivityCoordinates(outputFolder, sc);
		extractActivityDurations(outputFolder, sc);
		
		
		Header.printFooter();
	}

	/**
	 * This has been replaced by {@link PopulationUtils#extractActivityDurations(String, String)}
	 * 
	 * @param outputFolder
	 * @param sc
	 */
	@Deprecated 
	private static void extractActivityDurations(String outputFolder, Scenario sc) {
		List<Tuple<String, Double>> durations = new ArrayList<Tuple<String,Double>>();
		for(Person person : sc.getPopulation().getPersons().values()){
			Plan selectedPlan = person.getSelectedPlan();
			for(int i = 0; i < selectedPlan.getPlanElements().size(); i++){
				PlanElement pe = selectedPlan.getPlanElements().get(i);
				if(pe instanceof Activity){
					double duration = 0.0;
					ActivityImpl act = (ActivityImpl) pe;
					if(i == 0){
						/* It is the first (home) activity. */
						if(!act.getType().equalsIgnoreCase("h")){
							LOG.warn("Chain starting with activity other than `home': " 
									+ act.getType() + " (" + person.getId() + ")");
						}
						duration = act.getEndTime();
						durations.add(new Tuple<String, Double>("h1", duration));
						if(duration < 0){
							LOG.warn("First!! Negative duration: " + duration);
						}
					} else if(i < selectedPlan.getPlanElements().size() - 1){
						/* It can be any activity. */
						if(act.getType().equalsIgnoreCase("h")){
							durations.add(new Tuple<String, Double>("h3", act.getEndTime() - act.getStartTime()));
						} else {
							duration = act.getEndTime() - act.getStartTime();
							durations.add(new Tuple<String, Double>(act.getType(), duration));
						}
						if(duration < 0){
							LOG.warn("Mid!! Negative duration: " + duration);
						}
					} else {
						/* It is the final (home) activity. */
						if(!act.getType().equalsIgnoreCase("h")){
							LOG.warn("Chain ending with activity other than `home': " 
									+ act.getType() + " (" + person.getId() + ")");
						}
						duration = Math.max(24*60*60, act.getStartTime()) - act.getStartTime();
						durations.add(new Tuple<String, Double>("h2", duration));
						if(duration < 0){
							LOG.warn("LAST!! Negative duration: " + duration);
						}
					}
				}
			}
		}
		
		/* Write the output. */
		String filename = outputFolder + "activityDurations.csv";
		BufferedWriter bw = IOUtils.getBufferedWriter(filename);
		try {
			bw.write("Type,Duration");
			bw.newLine();
			for(Tuple<String, Double> tuple : durations){
				bw.write(tuple.getFirst());
				/* In minutes. */
				bw.write(String.format(",%.0f\n", tuple.getSecond() / 60)); 
			}
		} catch (IOException e) {
			throw new RuntimeException("Could not read from BufferedWriter "
					+ filename);
		} finally {
			try {
				bw.close();
			} catch (IOException e) {
				throw new RuntimeException("Could not close BufferedWriter "
						+ filename);
			}
		}
	}
	
	
	
	
	private static void extractActivityCoordinates(String outputFolder, Scenario sc) {
		Map<String, List<Coord>> typeCoords = new HashMap<String, List<Coord>>();
		for(Id person : sc.getPopulation().getPersons().keySet()){
			Plan plan = sc.getPopulation().getPersons().get(person).getSelectedPlan();
			for(PlanElement pe : plan.getPlanElements()){
				if(pe instanceof Activity){
					Activity act = (Activity) pe;
					if(!typeCoords.containsKey(act.getType())){
						typeCoords.put(act.getType(), new ArrayList<Coord>());
					}
					typeCoords.get(act.getType()).add(act.getCoord());
				}
			}
		}
		LOG.info("----------------------------------------------------------------------");
		LOG.info("Activity types:");
		for(String type : typeCoords.keySet()){
			BufferedWriter bw = IOUtils.getBufferedWriter(outputFolder + "coords_" + type + ".txt"); 
			LOG.info("   " + type + ": " + typeCoords.get(type).size());
			try{
				bw.write("Long,Lat");
				bw.newLine();
				for(Coord c : typeCoords.get(type)){
					bw.write(String.format("%.0f,%.0f\n", c.getX(), c.getY() ) ) ;
				}				
			} catch (IOException e) {
				throw new RuntimeException("Could not write to BufferedWriter.");
			} finally{
				try {
					bw.close();
				} catch (IOException e) {
					throw new RuntimeException("Could not close BufferedWriter.");
				}
			}
		}
		LOG.info("----------------------------------------------------------------------");
	}

}

