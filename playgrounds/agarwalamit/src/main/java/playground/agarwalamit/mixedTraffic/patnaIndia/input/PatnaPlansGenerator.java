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
package playground.agarwalamit.mixedTraffic.patnaIndia.input;

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
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

import playground.agarwalamit.mixedTraffic.patnaIndia.PatnaConstants;
import playground.agarwalamit.mixedTraffic.patnaIndia.PatnaConstants.PatnaActivityTypes;
/**
 * @author amit
 */
public class PatnaPlansGenerator {

	public PatnaPlansGenerator(String outputDir) {
		this.outputDir = outputDir;
	}

	private  final Logger logger = Logger.getLogger(PatnaPlansGenerator.class);
	private final String zoneFile = PatnaConstants.inputFilesDir+"/wardFile/Wards.shp";	
	private final int ID1 =0;				
	private final int ID2 = 100000;
	private final int ID3= 200000;
	private final Random random = MatsimRandom.getRandom();

	private Scenario scenario;
	private String outputDir;
	private Collection<SimpleFeature> features ;

	public static void main (String []args) {
		new PatnaPlansGenerator(PatnaConstants.inputFilesDir).startProcessingAndWritePlans();
	}

	public void startProcessingAndWritePlans() {
		this.features = readZoneFilesAndReturnFeatures();

		String planFile1 = PatnaConstants.inputFilesDir+"/Urban_PlanFile.CSV"; // urban plans for all zones except 27 to 42.
		String planFile2 = PatnaConstants.inputFilesDir+"/27TO42zones.CSV";// urban plans for zones 27 to 42
		String planFile3 = PatnaConstants.inputFilesDir+"/Slum_PlanFile.CSV";	

		Config config = ConfigUtils.createConfig();
		scenario = ScenarioUtils.createScenario(config);

		filesReader(planFile1, ID1);
		filesReader(planFile2, ID2);
		filesReader(planFile3, ID3);

		new PopulationWriter(scenario.getPopulation()).write(this.outputDir+"/initial_plans.xml.gz");
		logger.info("Writing Plan file is finished.");
	}

	private Collection<SimpleFeature> readZoneFilesAndReturnFeatures() {
		ShapeFileReader reader = new ShapeFileReader();
		return reader.readFileAndInitialize(this.zoneFile);
	}

	private void filesReader (String planFile, int startId) {
		Iterator<SimpleFeature> iterator = features.iterator();

		String line;
		try (BufferedReader bufferedReader = new BufferedReader(new FileReader(planFile)) ) {
			line = bufferedReader.readLine();

			while ((line)!= null ) {
				String[] parts = line.split(",");
				String fromZoneId = parts [5];
				String toZoneId = parts [6]; 
				String tripPurpose = parts [7];

				Coord homeZoneCoordTransform = null ;
				Coord workZoneCoordTransform = null ;
				Point p=null, q = null;

				Population population = scenario.getPopulation();
				PopulationFactory factory = population.getFactory();

				toZoneId = getCorrectZoneNumber(toZoneId);

				if (! fromZoneId.equals(toZoneId))	{														
					while (iterator.hasNext()){
						SimpleFeature feature = iterator.next();
						int Id = (Integer) feature.getAttribute("ID1");
						String zoneId  = String.valueOf(Id);

						if(fromZoneId.equals(zoneId) ) {
							p = getRandomPointsFromWard(feature, random);
							Coord fromZoneCoord = new Coord(p.getX(), p.getY());
							homeZoneCoordTransform = PatnaConstants.COORDINATE_TRANSFORMATION.transform(fromZoneCoord);
						}
						else if (toZoneId.equals(zoneId)){
							q = getRandomPointsFromWard(feature, random);
							Coord toZoneCoord = new Coord(q.getX(), q.getY());
							workZoneCoordTransform= PatnaConstants.COORDINATE_TRANSFORMATION.transform(toZoneCoord);
						}
					}
				} else if (fromZoneId.equals(toZoneId)) {
					// intraZonal trips
					while (iterator.hasNext()){

						SimpleFeature feature = iterator.next();

						p = getRandomPointsFromWard(feature, random);
						Coord fromZoneCoord = new Coord(p.getX(), p.getY());
						homeZoneCoordTransform = PatnaConstants.COORDINATE_TRANSFORMATION.transform(fromZoneCoord);

						q = getRandomPointsFromWard(feature, random);
						Coord toZoneCoord = new Coord(q.getX(), q.getY());
						workZoneCoordTransform= PatnaConstants.COORDINATE_TRANSFORMATION.transform(toZoneCoord);
					}
				}

				Person person = factory.createPerson(Id.create(Integer.toString(startId++),Person.class));
				population.addPerson(person);

				String travelMode = getTravelMode(parts [8]);

				Plan plan = createPlan( workZoneCoordTransform, homeZoneCoordTransform, travelMode, tripPurpose);
				person.addPlan(plan);

				line = bufferedReader.readLine();
				iterator = features.iterator();
			}
		} catch (IOException e) {
			throw new RuntimeException("File is not read. Reason : "+e);
		}
	}

	private String getTravelMode( String travelModeFromSurvey) {
		String travelMode = null;
		switch (travelModeFromSurvey) {
		case "1":	// Bus
		case "2":	// Mini Bus
		case "5":	// Motor driven 3W
		case "7" :	// train
			travelMode = "pt";	break;								
		case "3":	
			travelMode = "car";	break;
		case "4":	// all 2 W motorized 
			travelMode = "motorbike";	break;							
		case "6" :	//bicycle
		case "9" :	//CycleRickshaw
			travelMode = "bike";	break;						
		case "8" : 
			travelMode = "walk";	break;
		case "9999" : 
			travelMode = randomModeSlum();	break;				// 480 such trips are found 
		case "999999" : 
			travelMode = randomModeUrban(); break; 			// for zones 27 to 42
		}
		return travelMode;
	}

	private String getCorrectZoneNumber(String zoneId) {
		// for trips terminating in zone 73,74,75 and 76 are replaced by zone 6,1,3 and 3 respectively
		String outZoneId = "NULL" ;
		switch(zoneId) {
		case "73" : 
			outZoneId = "6"; break;
		case "74" : 
			outZoneId = "1"; break;
		case "75" : 
		case "76" : 
			outZoneId = "3"; break;
		default : outZoneId = zoneId; break;
		}
		return outZoneId;
	}

	private  Plan createPlan ( Coord toZoneFeatureCoord, Coord fromZoneFeatureCoord, String mode, String tripPurpose) {

		Population population = scenario.getPopulation();
		PopulationFactory populationFactory = population.getFactory();

		Plan plan = populationFactory.createPlan();

		double homeActEndTime=0.; 
		double secondActEndTimeLeaveTime =0.;
		String secondActType = null;

		switch (tripPurpose) {

		case "1" : {//work act starts between 8 to 9:30 and duration is 8 hours
			homeActEndTime = 8.0*3600. + random.nextInt(91)*60.; 
			secondActEndTimeLeaveTime = homeActEndTime + 8*3600.; 
			secondActType = PatnaActivityTypes.valueOf("work").toString();
			break; 
		}  
		case "2" : { // educational act starts between between 6:30 to 8:30 hours and duration is assumed about 7 hours
			homeActEndTime = 6.5*3600. + random.nextInt(121)*60.; 
			secondActEndTimeLeaveTime = homeActEndTime + 7*3600.;
			secondActType = PatnaActivityTypes.valueOf("educational").toString();
			break;
		}  
		case "3" : {// social duration between 5 to 7 hours
			homeActEndTime= 10.*3600. ; 
			secondActEndTimeLeaveTime = homeActEndTime+ 5.*3600. + random.nextInt(121)*60.; 
			secondActType = PatnaActivityTypes.valueOf("social").toString();
			break;
		}  
		case "4" : { // other act duration between 5 to 7 hours
			homeActEndTime = 8.*3600 ; 
			secondActEndTimeLeaveTime= homeActEndTime + 5.*3600. + random.nextInt(121)*60.; 
			secondActType = PatnaActivityTypes.valueOf("other").toString();
			break;
		} 
		case "9999" : { // no data
			homeActEndTime = 8.*3600. + random.nextInt(121)*60.; 
			secondActEndTimeLeaveTime= homeActEndTime + 7*3600.; 
			secondActType = PatnaActivityTypes.valueOf("unknown").toString();
			break;
		} 
		}


		Activity homeAct = populationFactory.createActivityFromCoord(PatnaActivityTypes.valueOf("home").toString(), fromZoneFeatureCoord);
		homeAct.setEndTime(homeActEndTime); 								
		plan.addActivity(homeAct);
		plan.addLeg(populationFactory.createLeg(mode));

		Activity secondAct = populationFactory.createActivityFromCoord(secondActType, toZoneFeatureCoord);
		secondAct.setEndTime(secondActEndTimeLeaveTime); 
		plan.addActivity(secondAct);
		plan.addLeg(populationFactory.createLeg(mode));

		Activity homeActII = populationFactory.createActivityFromCoord("home", homeAct.getCoord());	
		plan.addActivity(homeActII);
		return plan;
	}

	private  Point getRandomPointsFromWard (SimpleFeature feature, Random random) {
		Point p = null;
		double x,y;
		do {
			x = feature.getBounds().getMinX()+random.nextDouble()*(feature.getBounds().getMaxX()-feature.getBounds().getMinX());
			y = feature.getBounds().getMinY()+random.nextDouble()*(feature.getBounds().getMaxY()-feature.getBounds().getMinY());
			p= MGC.xy2Point(x, y);
		} while (!((Geometry) feature.getDefaultGeometry()).contains(p));
		return p;
	}


	private  String randomModeSlum () {
		// this method is for slum population as 480 plans don't have information about travel mode so one random mode is assigned out of these four modes.
		//		share of each vehicle is given in table 5-13 page 91 in CMP Patna
		//		pt - 15, car -0, 2W - 7, Bicycle -39 and walk 39
		Random rnd = new Random();
		int rndNr = rnd.nextInt(100);
		String travelMode = null;
		if (rndNr <= 15)  travelMode = "pt";				
		else if ( rndNr <= 22) travelMode = "motorbike";					
		else if ( rndNr <= 61) travelMode = "bike";					
		else  travelMode = "walk";					
		return travelMode;
	}

	private  String randomModeUrban () {
		//		share of each vehicle is given in table 5-13 page 91 in CMP Patna
		//		pt - 23, car -5, 2W - 25, Bicycle -33 and walk 14
		Random rnd = new Random();
		int rndNr = rnd.nextInt(100);
		String travelMode = null;
		if (rndNr <=23 )  travelMode = "pt";										
		else if ( rndNr <= 48) travelMode = "motorbike";					
		else if ( rndNr <= 53 ) travelMode = "car";				
		else if ( rndNr <= 86) travelMode = "bike";					
		else  travelMode = "walk";			
		return travelMode;
	}
}