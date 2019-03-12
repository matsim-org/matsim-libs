/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package ft.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author saxer
 *
 */
public class CountAgentsWithSchoolLocation {
	private static final Logger LOG = Logger.getLogger(CountAgentsWithSchoolLocation.class);
	
	public static void main(String[] args) throws FileNotFoundException {
		
		getWorkersXY();
	

		}
	public static void getWorkersXY() throws FileNotFoundException {
		
		
		int studentCounter = 0;
		// Create a Scenario
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		// Fill this Scenario with a population.
		new PopulationReader(scenario).readFile("E:\\Thiel\\Programme\\MatSim\\00_HannoverModel_1.0\\Input\\Cemdap\\cemdap_output\\Hannover_big_wchildren\\1\\plans.xml.gz");

		PrintWriter pw = new PrintWriter(new File("E:\\Thiel\\Programme\\MatSim\\00_HannoverModel_1.0\\Input\\Cemdap\\cemdap_output\\Hannover_big_wchildren\\test_work.csv"));


		for (Person person : scenario.getPopulation().getPersons().values()) {
			 
			List<PlanElement> planElements = person.getSelectedPlan().getPlanElements();

			for (PlanElement planElement : planElements) {

				if (planElement instanceof Activity) {
					Activity act = (Activity) planElement;
					if (act.getType().startsWith("work")) {
						//int age = (int) person.getAttributes().getAttribute("age");
						//String license = (String) person.getAttributes().getAttribute("hasLicense");
						//boolean employed = (boolean) person.getAttributes().getAttribute("employed");
						double startX = act.getCoord().getX();
						double startY = act.getCoord().getY();
						
					     
						StringBuilder sb = new StringBuilder();
					        sb.append(act.getType());
					        //sb.append(',');
					        //sb.append(age);
					        sb.append(',');
					        sb.append(startX);
					        sb.append(',');
					        sb.append(startY);
					        sb.append('\n');			        
					        //sb.append(employed);
					        //sb.append('\n');

					        pw.write(sb.toString());
					        
					       break; 
					        
						
					}
			
			
			
		
			    }
				
				
			}
	
		}
		System.out.println("done!");
		pw.close();
	}
	
	
public static void getHomiessXY() throws FileNotFoundException {
		
		
		int studentCounter = 0;
		// Create a Scenario
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		// Fill this Scenario with a population.
		new PopulationReader(scenario).readFile("D:\\Axer\\CEMDAP\\cemdap-vw\\cemdap_output_org\\mergedPlans_filtered_1.0.xml.gz");

		PrintWriter pw = new PrintWriter(new File("E:\\Thiel\\Programme\\MatSim\\00_HannoverModel_1.0\\Input\\Cemdap\\cemdap_output\\Hannover_big_wchildren\\test_Ax.csv"));


		for (Person person : scenario.getPopulation().getPersons().values()) {

//			 String schoolLoc = (String)
//			 person.getAttributes().getAttribute("locationOfSchool");
//			 if (!schoolLoc.equals("-99"))
//			 {
//			 studentCounter++;
//			 }
			List<PlanElement> planElements = person.getSelectedPlan().getPlanElements();

			if (planElements.size()==1)
			{
				int age = (int) person.getAttributes().getAttribute("age");
				Activity act = (Activity) planElements.get(0);
				String license = (String) person.getAttributes().getAttribute("hasLicense");
				boolean employed = (boolean) person.getAttributes().getAttribute("employed");
				double startX = act.getCoord().getX();
				double startY = act.getCoord().getY();
			
				
//				LOG.info("Activity:; "+ act.getType() + ";Age:;" + age + ";counter:;" +studentCounter + ";license:;" + license +";x:;" + startX+";y:;" + startY);
				studentCounter++;
				
				 
			        
				StringBuilder sb = new StringBuilder();
			        sb.append(act.getType());
			        sb.append(',');
			        sb.append(age);
			        sb.append(',');
			        sb.append(startX);
			        sb.append(',');
			        sb.append(startY);
			        sb.append(',');			        
			        sb.append(employed);
			        sb.append('\n');

			        pw.write(sb.toString());
			        
			        
			    }
				
				
			}
		System.out.println("done!");
		pw.close();	
//			for (PlanElement planElement : planElements) {
//
//				if (planElement instanceof Activity) {
//					Activity act = (Activity) planElement;
//					if (act.getType().startsWith("education")) {
//						studentCounter++;
//						break;
//					}
//				}
//			}
//			System.out.println(studentCounter);
		
	}
	
	}

	

