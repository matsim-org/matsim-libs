/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.agarwalamit.mixedTraffic.patnaIndia.old;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.api.internal.MatsimWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
/**
 * @author amit
 */
public class createPlans4Patna {

	private static final Logger logger = Logger.getLogger(createPlans4Patna.class);

	static int ID1 =0;				
	static int ID2 = 100000;
	static int ID3= 200000;
	static CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84,"EPSG:24345");

	public static void main (String []args) throws IOException {


		String zoneFile = "../../patnaIndiaSim/input/wardFile/Wards.shp";		
		String planFile1 = "../../patnaIndiaSim/input/Urban_PlanFile.CSV";
		String planFile2 = "../../patnaIndiaSim/input/27TO42zones.CSV";
		String planFile3 = "../../patnaIndiaSim/input/Slum_PlanFile.CSV";	

		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		Population population = scenario.getPopulation() ;																																			
		Network network = scenario.getNetwork();

		filesReader(planFile1, zoneFile, scenario, ID1);
		filesReader(planFile2, zoneFile, scenario, ID2);
		filesReader(planFile3, zoneFile, scenario, ID3);

		MatsimWriter populationwriter = new PopulationWriter(population, network); 
		populationwriter.write(MyFirstControler.outputDir+"/plans.xml");
		logger.info("Writing Plan file is finished.");

	}
	private static void filesReader (String planFile, String zoneFile, Scenario scenario, int startId) throws IOException 	{

		ShapeFileReader reader = new ShapeFileReader();
		Collection<SimpleFeature> features = reader.readFileAndInitialize(zoneFile);
		Iterator<SimpleFeature> iterator = features.iterator();
		
		BufferedReader bufferedReader = new BufferedReader(new FileReader(planFile));
		String line = bufferedReader.readLine();

		while ((line)!= null ) {

			String[] parts = line.split(",");
			String fromZoneId = parts [5];
			String toZoneId = parts [6]; 
			String tripPurpose = parts [7];

			SimpleFeature toZoneFeature = null;
			SimpleFeature fromZoneFeature = null;

			Coord homeZoneCoordTransform = null ;
			Coord workZoneCoordTransform = null ;
			Point p=null, q = null;
			Random random = new Random ();

			Population population = scenario.getPopulation();
			PopulationFactory factory = population.getFactory();

			// for trips terminating in zone 73,74,75 and 76 are replaced by zone 6,1,3 and 3 respectively

			if (((Integer.parseInt(toZoneId)) > 72)) { 	
				int id2 = 0 ;
				switch ((Integer.parseInt(toZoneId))) {
				case 73 : id2=6;break;
				case 74 : id2=1;break;
				case 75 : id2=3;break;
				case 76 : id2=3;break;
				}
				toZoneId = String.valueOf(id2);
			}
			if (! fromZoneId.equals(toZoneId))	{														

				while (iterator.hasNext()){

					SimpleFeature feature = iterator.next();
					int Id = (Integer) feature.getAttribute("ID1");
					String zoneId  = String.valueOf(Id);

					if(fromZoneId.equals(zoneId) ) {
						fromZoneFeature = feature; 
						p = getRandomPointsFromWard(feature, random);
						Coord fromZoneCoord = new Coord(p.getX(), p.getY());
						homeZoneCoordTransform = ct.transform(fromZoneCoord);
					}
					else if (toZoneId.equals(zoneId)){
						toZoneFeature = feature;
						q = getRandomPointsFromWard(feature, random);
						Coord toZoneCoord = new Coord(q.getX(), q.getY());
						workZoneCoordTransform= ct.transform(toZoneCoord);
					}
				}
			} 
			// intraZonal trips
			else if (fromZoneId.equals(toZoneId)) {

				while (iterator.hasNext()){

					SimpleFeature feature = iterator.next();
					int Id = (Integer) feature.getAttribute("ID1");
					String zoneId  = String.valueOf(Id);

					fromZoneFeature = feature; 
					p = getRandomPointsFromWard(feature, random);
					Coord fromZoneCoord = new Coord(p.getX(), p.getY());
					homeZoneCoordTransform = ct.transform(fromZoneCoord);

					toZoneFeature = feature;
					q = getRandomPointsFromWard(feature, random);
					Coord toZoneCoord = new Coord(q.getX(), q.getY());
					workZoneCoordTransform= ct.transform(toZoneCoord);
				}
			}
			//			for (int j=0; j<1; j++){ //run with 1% sample
			Person person = factory.createPerson(Id.create(Integer.toString(startId++),Person.class));
			population.addPerson(person);
			Plan plan = factory.createPlan();
			person.addPlan(plan);

			String travelMode = parts [8];
			int modeTravel = Integer.parseInt(travelMode);

			switch (modeTravel) {
			case 1:	 travelMode = "pt";	break;								// Bus
			case 2:	 travelMode = "pt";	break;								// Mini Bus
			case 3:  travelMode = "car";	break;
			case 4:  travelMode = "motorbike";	break;							// all 2 W motorized 
			case 5:  travelMode = "pt";	break;								// Motor driven 3W
			case 6 : travelMode = "bike";	break;						//bicycle
			case 7 : travelMode = "pt";	break;								// train
			case 8 : travelMode = "walk";	break;
			case 9 : travelMode = "bike";	break;						//CycleRickshaw
			case 9999 : travelMode = randomModeSlum();	break;				// 480 such trips are found in which mode was not available so chosing a random mode 
			case 999999 : travelMode = randomModeUrban(); break; 			// for zones 27 to 42
			}
			createActivities(scenario, plan, random, workZoneCoordTransform, homeZoneCoordTransform, travelMode, tripPurpose);
			//			}
			line = bufferedReader.readLine();
			iterator = features.iterator();
		}
	}

	private static void createActivities (Scenario scenario,Plan plan, Random random, Coord toZoneFeatureCoord, Coord fromZoneFeatureCoord, String mode, String tripPurpose) {

		Random random2 = new Random();
		Population population = scenario.getPopulation();
		PopulationFactory populationFactory = population.getFactory();

		Activity homeAct = populationFactory.createActivityFromCoord("home", fromZoneFeatureCoord); 

		// to get different timing for different trip purpose
		int tPurpose = Integer.parseInt(tripPurpose);
		double homeleaveTime=0, workLeaveTime =0;
		switch (tPurpose) {
		// t1 is related to t to make sure that no one leaves from work before leaving home.
		//		case 1 : {homeleaveTime = 8*3600+random2.nextInt(91)*60; workLeaveTime =homeleaveTime+9*3600; break; }  //working hours between 8 to 9:30 and work Duration is 9 hours
		//		case 2 : {homeleaveTime = 6.5*3600+random2.nextInt(121)*60;workLeaveTime=homeleaveTime+8*3600;break;}  // educational hours between 6:30 to 8:30 hours
		//		case 3 : {homeleaveTime= 10*3600+random2.nextInt(121)*60; workLeaveTime = homeleaveTime+(7+random2.nextInt(3))*3600; break;}  // social duration between 7 to 9 hours
		//		case 4 : {homeleaveTime = 7*3600+random2.nextInt(301)*60; workLeaveTime= homeleaveTime+(4+random2.nextInt(7))*3600; break;} // other hours between 4 to 10 hours, also starting time between 8 to 1pm and random end time can shift it to at most 2 pm
		//		case 9999 : {homeleaveTime = 7*3600+random2.nextInt(301)*60; workLeaveTime= homeleaveTime+(4+random2.nextInt(7))*3600; break;} // not given time


		//		case 1 : {homeleaveTime = 8.5*3600+random2.nextInt(91)*60; workLeaveTime =homeleaveTime+9*3600; break; }  //working hours between 8 to 9:30 and work Duration is 9 hours
		//		case 2 : {homeleaveTime = 7.5*3600+random2.nextInt(121)*60;workLeaveTime=homeleaveTime+8*3600;break;}  // educational hours between 6:30 to 8:30 hours
		//		case 3 : {homeleaveTime= 10*3600+random2.nextInt(121)*60; workLeaveTime = homeleaveTime+(7+random2.nextInt(3))*3600; break;}  // social duration between 7 to 9 hours
		//		case 4 : {homeleaveTime = 7.5*3600+random2.nextInt(301)*60; workLeaveTime= homeleaveTime+(4+random2.nextInt(7))*3600; break;} // other hours between 4 to 10 hours, also starting time between 8 to 1pm and random end time can shift it to at most 2 pm
		//		case 9999 : {homeleaveTime = 8*3600+random2.nextInt(301)*60; workLeaveTime= homeleaveTime+(4+random2.nextInt(7))*3600; break;} // not given time

		case 1 : {homeleaveTime = 8*3600+random2.nextInt(91)*60; workLeaveTime =homeleaveTime+8*3600; break; }  //working hours between 8 to 9:30 and work Duration is 9 hours
		case 2 : {homeleaveTime = 7*3600+random2.nextInt(121)*60;workLeaveTime=homeleaveTime+7*3600;break;}  // educational hours between 6:30 to 8:30 hours
		case 3 : {homeleaveTime= 10*3600+random2.nextInt(121)*60; workLeaveTime = homeleaveTime+5*3600; break;}  // social duration between 7 to 9 hours
		case 4 : {homeleaveTime = 7.5*3600+random2.nextInt(301)*60; workLeaveTime= homeleaveTime+7*3600; break;} // other hours between 4 to 10 hours, also starting time between 8 to 1pm and random end time can shift it to at most 2 pm
		case 9999 : {homeleaveTime = 8*3600+random2.nextInt(301)*60; workLeaveTime= homeleaveTime+7*3600; break;} // not given time

		}
		homeAct.setEndTime(homeleaveTime); 								
		//		homeAct.setEndTime(8*3600);
		plan.addActivity(homeAct);

		Leg leg = populationFactory.createLeg(mode);
		plan.addLeg(leg);

		Activity workAct = populationFactory.createActivityFromCoord("work", toZoneFeatureCoord);
		workAct.setEndTime(workLeaveTime); 
		//		workAct.setEndTime(18*3600);
		plan.addActivity(workAct);
		plan.addLeg(populationFactory.createLeg(mode));

		Activity homeActII = populationFactory.createActivityFromCoord("home", homeAct.getCoord());				//finally the second home activity - it is clear that this activity needs to be on the same link
		plan.addActivity(homeActII);
	}

	private static Point getRandomPointsFromWard (SimpleFeature feature, Random random) {
		Point p = null;
		double x,y;
		do {
			x = feature.getBounds().getMinX()+random.nextDouble()*(feature.getBounds().getMaxX()-feature.getBounds().getMinX());
			y = feature.getBounds().getMinY()+random.nextDouble()*(feature.getBounds().getMaxY()-feature.getBounds().getMinY());
			p= MGC.xy2Point(x, y);
		} while (!((Geometry) feature.getDefaultGeometry()).contains(p));
		return p;
	}
	// this method is for slum population as 480 plans don't have information about travel mode so one random mode is assigned out of these four modes. 
	private static String randomModeSlum () {
		//		share of each vehicle is given in table 5-13 page 91 in CMP Patna
		//		pt - 15, car -0, 2W - 7, Bicycle -39 and walk 39
		Random rnd = new Random();
		int rndNr = rnd.nextInt(100);
		String travelMode = null;
		if (rndNr <= 15)  travelMode = "pt";				
		else if (rndNr > 15 && rndNr <= 22) travelMode = "motorbike";					
		else if (rndNr > 22 && rndNr <= 61) travelMode = "bike";					
		else if (rndNr > 61 && rndNr <= 100) travelMode = "walk";					
		return travelMode;
	}

	private static String randomModeUrban () {
		//		share of each vehicle is given in table 5-13 page 91 in CMP Patna
		//		pt - 23, car -5, 2W - 25, Bicycle -33 and walk 14
		Random rnd = new Random();
		int rndNr = rnd.nextInt(100);
		String travelMode = null;
		if (rndNr <=23 )  travelMode = "pt";										
		else if (rndNr > 23 && rndNr <= 48) travelMode = "motorbike";					
		else if (rndNr > 48 && rndNr <= 53 ) travelMode = "car";				
		else if (rndNr > 53 && rndNr <= 86) travelMode = "bike";					
		else if (rndNr > 86 && rndNr <= 100) travelMode = "walk";			
		return travelMode;
	}
}