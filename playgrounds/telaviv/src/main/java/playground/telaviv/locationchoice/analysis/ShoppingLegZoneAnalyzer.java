/* *********************************************************************** *
 * project: org.matsim.*
 * ShoppingLegZoneAnalyzer.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.telaviv.locationchoice.analysis;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import playground.telaviv.locationchoice.LocationChoicePlanModule;
import playground.telaviv.locationchoice.LocationChoiceProbabilityCreator;
import playground.telaviv.zones.ZoneMapping;

public class ShoppingLegZoneAnalyzer {

	private static final Logger log = Logger.getLogger(ShoppingLegZoneAnalyzer.class);
	
	private static String basePath = "../../matsim/mysimulations/telaviv/";
	
	private static String networkFile = basePath + "input/network.xml";
//	private static String populationFile = basePath + "output/ITERS/it.80/80.plans.xml.gz";
	private static String populationFile = basePath + "input/plans_10.xml.gz";
	
//	private String outFileCarProbabilities = basePath + "output/ITERS/it.80/80.shoppingLegsCarProbabilities.txt";
	private String outFileCarProbabilities = basePath + "input/initial.shoppingLegsCarProbabilities.txt";

	private String delimiter = "\t";
	private Charset charset = Charset.forName("UTF-8");
	
	private Scenario scenario;
	
	private ZoneMapping zoneMapping;
	private LocationChoiceProbabilityCreator locationChoiceProbabilityCreator;
	
	private Map<Id, List<Integer>> shoppingActivities;	// <PersonId, List<Index of Shopping Activity>
	private List<Double> probabilities;
	
	public static void main(String[] args)
	{
		Scenario scenario = new ScenarioImpl();
		
		// load network
		new MatsimNetworkReader(scenario).readFile(networkFile);
			
		// load population
		new MatsimPopulationReader(scenario).readFile(populationFile);

		new ShoppingLegZoneAnalyzer(scenario);
	}
	
	public ShoppingLegZoneAnalyzer(Scenario scenario)
	{
		this.scenario = scenario;
		
		log.info("Identifying shopping activities...");
		LocationChoicePlanModule lcpm = new LocationChoicePlanModule(scenario);
		shoppingActivities = lcpm.getShoppingActivities();
		log.info("done. Found " + shoppingActivities.size());
		
		log.info("Creating ZoneMapping...");
		zoneMapping = new ZoneMapping(scenario, TransformationFactory.getCoordinateTransformation("EPSG:2039", "WGS84"));
		log.info("done.");
		
		log.info("Creating LocationChoiceProbabilityCreator...");
		locationChoiceProbabilityCreator = new LocationChoiceProbabilityCreator(scenario);
		log.info("done.");
		
		log.info("Get probabilities of selected shopping zones...");
		getProbabilities();
		log.info("done.");
		
		log.info("Writing probabilities to file...");
		writeFile(outFileCarProbabilities);
		log.info("done.");
	}
	
	private void getProbabilities()
	{
		probabilities = new ArrayList<Double>();
		
		for (Person person : scenario.getPopulation().getPersons().values())
		{
			List<Integer> shoppingActivitiesList = shoppingActivities.get(person.getId());
			
			if (shoppingActivitiesList == null) continue;
			
			/*
			 * The first Activity is always being at home.
			 */
			Activity homeActivity = (Activity) person.getSelectedPlan().getPlanElements().get(0);
			
			for (int index : shoppingActivitiesList)
			{	
				Activity shoppingActivity = (Activity) person.getSelectedPlan().getPlanElements().get(index);
				double probability = getProbability(homeActivity, shoppingActivity);
				
				probabilities.add(probability);
			}
		}
	}
	
	private double getProbability(Activity homeActivity, Activity shoppingActivity)
	{
		Id homeLinkId = homeActivity.getLinkId();
		Id shoppingLinkId = shoppingActivity.getLinkId();
		
		int homeTAZ = zoneMapping.getLinkTAZ(homeLinkId);
		int shoppingTAZ = zoneMapping.getLinkTAZ(shoppingLinkId);
		
		Map<Integer, Double> probabilities = locationChoiceProbabilityCreator.getFromZoneProbabilities(homeTAZ);
		
		return probabilities.get(shoppingTAZ);
	}
	
	private void writeFile(String outFile)
	{
		FileOutputStream fos = null; 
		OutputStreamWriter osw = null; 
	    BufferedWriter bw = null;
		
	    try 
	    {
			fos = new FileOutputStream(outFile);
			osw = new OutputStreamWriter(fos, charset);
			bw = new BufferedWriter(osw);
			
			// write Header
			bw.write("probability" + "\n");
			
			// write Values
			for (Double probability : probabilities)
			{
				bw.write(String.valueOf(probability));
				bw.write("\n");
			}
			
			bw.close();
			osw.close();
			fos.close();
		}
		catch (IOException e) 
		{
			e.printStackTrace();
		}	
	}
}