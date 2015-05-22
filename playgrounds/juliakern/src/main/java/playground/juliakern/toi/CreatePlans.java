/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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


package playground.juliakern.toi;


import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;



public class CreatePlans {
	
	static String csvFile = "input/oslo/Stefan_trondheim.csv"; 
	static String networkFile = "input/oslo/trondheim_network.xml";
	static String cvsSplitBy = ",";
	static String outputDir = "output/oslo/";
	static String plansFile = "input/oslo/plans_from_csv.xml";

		private static final Logger logger = Logger.getLogger(CreatePlans.class);


		/**
		 * @param args
		 */
		public static void main(String[] args) {

			Config config = ConfigUtils.createConfig();
			
			config.controler().setOutputDirectory(outputDir);
			config.addCoreModules();
			
			ActivityParams home = new ActivityParams("home");
			home.setTypicalDuration(16*3600);
			ActivityParams work = new ActivityParams("work");
			work.setTypicalDuration(8*3600);
			config.planCalcScore().addActivityParams(home);
			config.planCalcScore().addActivityParams(work);
			
			
			Scenario scenario = ScenarioUtils.createScenario(config);
			new MatsimNetworkReader(scenario).readFile(networkFile);
			Population pop = fillScenario(scenario);
			
			Controler controler = new Controler(scenario);
			controler.getConfig().controler().setOverwriteFileSetting(
					true ?
							OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
							OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );

			new PopulationWriter(pop, scenario.getNetwork()).write(plansFile);
		}
		
		
		
		private static Population fillScenario(Scenario scenario) {
			Population population = scenario.getPopulation();
			
			PopulationFactory populationFactory = population.getFactory();
			
			BufferedReader br = null;
			String line = "";
			Map<Id<Person>, ArrayList<String>> person2lines = new HashMap<>();
		 
			try {
		 
				br = new BufferedReader(new FileReader(csvFile));
				br.readLine(); //skip first line
				while ((line = br.readLine()) != null) {
		 
				    // use comma as separator
					String[] trip = new String[line.split(cvsSplitBy).length];
					trip = line.split(cvsSplitBy);
					Id<Person> personId = Id.create(trip[0], Person.class);
					if(!person2lines.containsKey(personId)){
						person2lines.put(personId, new ArrayList<String>());
					}
					person2lines.get(personId).add(line);
				}
		 
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				if (br != null) {
					try {
						br.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		 
			System.out.println("Done reading csv file.");
			
//			CoordinateTransformation ct = //TransformationFactory.getCoordinateTransformation("PROJCS[\"ETRS89_UTM_Zone_33\",GEOGCS[\"GCS_ETRS89\",DATUM[\"D_ETRS89\",SPHEROID[\"GRS_1980\",6378137.0,298.257222101]],PRIMEM[\"Greenwich\",0.0],UNIT[\"Degree\",0.0174532925199433]],PROJECTION[\"Transverse_Mercator\"],PARAMETER[\"False_Easting\",3500000.0],PARAMETER[\"False_Northing\",0.0],PARAMETER[\"Central_Meridian\",15.0],PARAMETER[\"Scale_Factor\",0.9996],PARAMETER[\"Latitude_Of_Origin\",0.0],UNIT[\"Meter\",1.0]]", "EPSG:3395");
//					  TransformationFactory.getCoordinateTransformation(  TransformationFactory.WGS84, TransformationFactory.WGS84  );
			//TransformationFactory.getCoordinateTransformation(  TransformationFactory.WGS84, TransformationFactory.CH1903_LV03  );
			
			
			for(Id personId : person2lines.keySet()){
				System.out.println(personId);
				Person person = populationFactory.createPerson(personId);
				population.addPerson(person);
				Plan plan = populationFactory.createPlan();
				person.addPlan(plan);
				//TODO sort!
				//TODO first activity
				// TODO last activity without endtime
				String[] last; String[] trip = null;
				for(String currline: person2lines.get(personId)){
						trip = currline.split(cvsSplitBy);
						Double startx = Double.parseDouble(trip[4]);
						Double starty = Double.parseDouble(trip[5]);
						
						Coord startCoordinates = scenario.createCoord(startx,starty);
						//create activity
						String actType = "work";
						if(trip[2].equals("1"))actType="home";
						// TODO diff trip types
						Activity act = populationFactory.createActivityFromCoord(actType, startCoordinates);
						act.setEndTime(getEndTimeInSeconds(trip[11]));	
						plan.addActivity(act);
						//create leg
						plan.addLeg(populationFactory.createLeg("car"));
						last= trip;
//						logger.info("x="+startx+" y="+ starty);
				}
				Double endx = Double.parseDouble(trip[7]);
				Double endy = Double.parseDouble(trip[8]);
				Coord endCoordinates = scenario.createCoord(endx, endy);
				Activity lastAct = populationFactory.createActivityFromCoord("home", endCoordinates);
				plan.addActivity(lastAct);
//				logger.info("x=" + endx + " y=" + endy);
			}
			/*
			//+++++++++++++++
			Id personId = new IdImpl(trip[0]);
			Person person; Plan plan;
			if(!population.getPersons().containsKey(personId)){
				//create new person
				person = populationFactory.createPerson(personId);
				plan = populationFactory.createPlan();
				person.addPlan(plan);
			}else{
				person = population.getPersons().get(personId);
				plan = person.getSelectedPlan(); //TODO ist der selected??
			}

			//add plan element					
			CoordinateTransformation ct = //TransformationFactory.getCoordinateTransformation("PROJCS[\"ETRS89_UTM_Zone_33\",GEOGCS[\"GCS_ETRS89\",DATUM[\"D_ETRS89\",SPHEROID[\"GRS_1980\",6378137.0,298.257222101]],PRIMEM[\"Greenwich\",0.0],UNIT[\"Degree\",0.0174532925199433]],PROJECTION[\"Transverse_Mercator\"],PARAMETER[\"False_Easting\",3500000.0],PARAMETER[\"False_Northing\",0.0],PARAMETER[\"Central_Meridian\",15.0],PARAMETER[\"Scale_Factor\",0.9996],PARAMETER[\"Latitude_Of_Origin\",0.0],UNIT[\"Meter\",1.0]]", "EPSG:3395");
					  TransformationFactory.getCoordinateTransformation(  TransformationFactory.WGS84, TransformationFactory.CH1903_LV03  );
			 
			 Coord startCoordinates = scenario.createCoord(Double.parseDouble(trip[5])/10,Double.parseDouble(trip[4])*10);
			 Coord endCoordinates = scenario.createCoord(Double.parseDouble(trip[8])/10, Double.parseDouble(trip[7])*10);
			 
			 String actType = "home"; //TODO erste activity des tages, typen unterscheiden
			 Activity activity = populationFactory.createActivityFromCoord(actType, ct.transform(endCoordinates));
			 //TODO
			//-----------------------
			Person person = populationFactory .createPerson(scenario.createId("1"));
			population.addPerson(person);
			
			 Plan plan = populationFactory.createPlan();
			 person.addPlan(plan);
			
			 CoordinateTransformation ct = //TransformationFactory.getCoordinateTransformation("PROJCS[\"ETRS89_UTM_Zone_33\",GEOGCS[\"GCS_ETRS89\",DATUM[\"D_ETRS89\",SPHEROID[\"GRS_1980\",6378137.0,298.257222101]],PRIMEM[\"Greenwich\",0.0],UNIT[\"Degree\",0.0174532925199433]],PROJECTION[\"Transverse_Mercator\"],PARAMETER[\"False_Easting\",3500000.0],PARAMETER[\"False_Northing\",0.0],PARAMETER[\"Central_Meridian\",15.0],PARAMETER[\"Scale_Factor\",0.9996],PARAMETER[\"Latitude_Of_Origin\",0.0],UNIT[\"Meter\",1.0]]", "EPSG:3395");
					  TransformationFactory.getCoordinateTransformation(  TransformationFactory.WGS84, TransformationFactory.CH1903_LV03  );
			 
			 Coord homeCoordinates = scenario.createCoord(756503.,2063809. );
			 Activity activity1 = populationFactory.createActivityFromCoord("home",ct.transform( homeCoordinates));
			 activity1.setEndTime(6*60*60);
			 plan.addActivity(activity1);
			 plan.addLeg(populationFactory.createLeg("car"));
			 
			 Coord workCoordinates = scenario.createCoord(7800000., 2050000.);
			 Activity activity2 = populationFactory.createActivityFromCoord("work", ct.transform(workCoordinates));
			 activity2.setEndTime(15*60*60);
			 plan.addActivity(activity2);
			 plan.addLeg(populationFactory.createLeg("car"));
			 
			 Activity activity3 = populationFactory.createActivityFromCoord("home", ct.transform(homeCoordinates));
			 plan.addActivity(activity3);
			 
			 //-----
			  * */
			
			return population;
		}



		private static double getEndTimeInSeconds(String string) {
			String[]time= string.split(" ");
			Double pm=0.;
			if(time[1].equals("PM")){
				pm=43200.; //12*60*60;
			}
			time= time[0].split(":");
			Double timeInSeconds = 3600*Double.parseDouble(time[0])+60*Double.parseDouble(time[1])+Double.parseDouble(time[2])+pm;
			return timeInSeconds;
		}
}
