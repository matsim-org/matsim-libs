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
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;




public class CopyOfCreatePlans {
	

		private static final Logger logger = Logger.getLogger(CopyOfCreatePlans.class);


		/**
		 * @param args
		 */
		public static void main(String[] args) {

			
			//logger.getRootLogger().setLevel(Level.OFF);
			Config config = ConfigUtils.createConfig();
			
			config.controler().setOutputDirectory("output/oslo/");
			config.addCoreModules();
			
			ActivityParams home = new ActivityParams("home");
			home.setTypicalDuration(16*3600);
			ActivityParams work = new ActivityParams("work");
			work.setTypicalDuration(8*3600);
			config.planCalcScore().addActivityParams(home);
			config.planCalcScore().addActivityParams(work);
			
			
			Scenario scenario = ScenarioUtils.createScenario(config);
			
			new MatsimNetworkReader(scenario).readFile("input/oslo/trondheim.xml");
			Population pop = fillScenario(scenario);
			
			Controler controler = new Controler(scenario);
			controler.getConfig().controler().setOverwriteFileSetting(
					true ?
							OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
							OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );

			//controler.run();
			
			new PopulationWriter(pop, scenario.getNetwork()).write("input/oslo/plans.xml");
		}
		private static Population fillScenario(Scenario scenario) {
			Population population = scenario.getPopulation();
			
			PopulationFactory populationFactory = population.getFactory();
			Person person = populationFactory.createPerson(Id.create("1", Person.class));
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
			
			return population;
		}
}
