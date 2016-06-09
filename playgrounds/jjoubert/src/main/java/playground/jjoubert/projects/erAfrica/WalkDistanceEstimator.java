/* *********************************************************************** *
 * project: org.matsim.*
 * WalkDistanceEstimator.java                                                                        *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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
package playground.jjoubert.projects.erAfrica;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.households.HouseholdsReaderV10;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;

import playground.southafrica.population.census2011.attributeConverters.CoordConverter;
import playground.southafrica.utilities.Header;

/**
 * Extracting the walking distance from the Cape Town travel diary.
 *  
 * @author jwjoubert
 */
public class WalkDistanceEstimator {
	final private static Logger LOG = Logger.getLogger(WalkDistanceEstimator.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(WalkDistanceEstimator.class.toString(), args);
		String householdFile = args[0];
		String householdAttributesFile = args[1];
		String populationFile = args[2];
		String populationAttributesFile = args[3];
		
		String output = args[4];
		
		/* Parse the entire population. */
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new HouseholdsReaderV10(sc.getHouseholds()).parse(householdFile);
		new MatsimPopulationReader(sc).parse(populationFile);
		
		/* Parse the attributes. */
		ObjectAttributesXmlReader hhaReader = new ObjectAttributesXmlReader(sc.getHouseholds().getHouseholdAttributes());
		hhaReader.putAttributeConverter(Coord.class, new CoordConverter());
		hhaReader.parse(householdAttributesFile);
		
		new ObjectAttributesXmlReader(sc.getPopulation().getPersonAttributes()).parse(populationAttributesFile);
		
		WalkDistanceEstimator.extractWalkDistances(sc, output);
		
		Header.printFooter();
	}
	
	
	private static void extractWalkDistances(Scenario sc, String filename){
		LOG.info("Extracting walk legs...");
		List<String> list = new ArrayList<>();
		for(Id<Person> pid : sc.getPopulation().getPersons().keySet()){
			Plan plan = sc.getPopulation().getPersons().get(pid).getSelectedPlan();
			
			/* There are 'null' plans: ignore them. */
			if(plan != null){
				for(int i = 1; i < plan.getPlanElements().size()-1; i+=2 ){
					Leg leg = (Leg)plan.getPlanElements().get(i);
					if(leg.getMode().equalsIgnoreCase("walk")){
						Coord a = ((Activity)plan.getPlanElements().get(i-1)).getCoord();
						Coord b = ((Activity)plan.getPlanElements().get(i+1)).getCoord();
						double dist = CoordUtils.calcEuclideanDistance(a, b);
						
						/* Add all the wanted attributes. */
						list.add(String.format("%.0f", dist));
					}
				}
			}
		}
		LOG.info("Done extracting walk legs.");
		
		LOG.info("Writing legs to file...");
		Counter counter = new Counter("  legs # ");
		BufferedWriter bw = IOUtils.getBufferedWriter(filename);
		try{
			for(String s : list){
				bw.write(s);
				bw.newLine();
				counter.incCounter();
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot write to " + filename);
		} finally{
			try {
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close " + filename);
			}
		}
		counter.printCounter();
		LOG.info("Done writing legs to file.");
	}
	

}
