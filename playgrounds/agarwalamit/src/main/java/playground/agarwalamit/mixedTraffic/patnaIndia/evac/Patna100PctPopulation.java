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
package playground.agarwalamit.mixedTraffic.patnaIndia.evac;

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
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Point;

import playground.agarwalamit.mixedTraffic.patnaIndia.PatnaUtils;
import playground.agarwalamit.utils.GeometryUtils;
import playground.agarwalamit.utils.LoadMyScenarios;

/**
 * @author amit
 */

public class Patna100PctPopulation {

	// take the home coord from initial plans (in ward) and then assign these agents to nearest link, see core method of doing this.
	private final Logger logger = Logger.getLogger(Patna100PctPopulation.class);
	private final Id<Link> safeLinkId = Id.createLinkId("safeLink_Patna");

	private Scenario scenario;
	private final String outputDir = "../../../repos/runs-svn/patnaIndia/run105/input/";

	private Collection<SimpleFeature> features ;

	public static void main(String[] args) {
		new Patna100PctPopulation().run();
	}

	private void run() {
		this.features = readZoneFilesAndReturnFeatures();

		String planFile1 = "../../../repos/runs-svn/patnaIndia/inputs/Urban_PlanFile.CSV";
		String planFile2 = "../../../repos/runs-svn/patnaIndia/inputs/27TO42zones.CSV";
		String planFile3 = "../../../repos/runs-svn/patnaIndia/inputs/Slum_PlanFile.CSV";	

		String netFile = "../../../repos/runs-svn/patnaIndia/inputs/network.xml";

		scenario = LoadMyScenarios.loadScenarioFromNetwork(netFile);

		filesReader(planFile1, "nonSlum_");
		filesReader(planFile2, "nonSlum_");
		filesReader(planFile3, "slum_");

		new PopulationWriter(scenario.getPopulation()).write(outputDir+"/patna_evac_plans_100Pct.xml.gz");
		logger.info("Writing Plan file is finished.");
	}

	private Collection<SimpleFeature> readZoneFilesAndReturnFeatures() {
		ShapeFileReader reader = new ShapeFileReader();
		return reader.readFileAndInitialize(PatnaUtils.ZONE_FILE);
	}

	private void filesReader (final String planFile, final String idPrefix) {

		Iterator<SimpleFeature> iterator = features.iterator();

		String line;
		try (BufferedReader bufferedReader = new BufferedReader(new FileReader(planFile)) ) {
			line = bufferedReader.readLine();

			while (line!= null ) {
				String[] parts = line.split(",");
				String fromZoneId = parts [5];

				Coord homeZoneCoordTransform = null ;
				Point p=null;

				while (iterator.hasNext()){
					SimpleFeature feature = iterator.next();
					int id = (Integer) feature.getAttribute("ID1");
					String zoneId  = String.valueOf(id);

					if(fromZoneId.equals(zoneId) ) {
						p = GeometryUtils.getRandomPointsInsideFeature(feature);
						Coord fromZoneCoord = new Coord(p.getX(), p.getY());
						homeZoneCoordTransform = PatnaUtils.COORDINATE_TRANSFORMATION.transform(fromZoneCoord);
					}
				}

				for (int j=0; j<100; j++){ //run with 100% sample

					String travelMode = getTravelMode(parts [8]);
					if(PatnaUtils.URBAN_MAIN_MODES.contains(travelMode)) {
						Population pop = scenario.getPopulation();
						PopulationFactory populationFactory = pop.getFactory();

						Person person = populationFactory.createPerson(Id.createPersonId(idPrefix+pop.getPersons().size()));
						pop.addPerson(person);
						Plan plan = populationFactory.createPlan();
						person.addPlan(plan);

						Link link = NetworkUtils.getNearestLink(scenario.getNetwork(), homeZoneCoordTransform);

						Activity home = populationFactory.createActivityFromLinkId("home", link.getId()); 
						home.setEndTime(9*3600); 								

						plan.addActivity(home);

						Leg leg = populationFactory.createLeg(travelMode);
						plan.addLeg(leg);

						Activity evacAct = populationFactory.createActivityFromLinkId("evac", safeLinkId);
						plan.addActivity(evacAct);
					}
				}
				line = bufferedReader.readLine();
				iterator = features.iterator();
			}
		} catch (IOException e) {
			throw new RuntimeException("File not found. Aborting...");
		}
	}

	private String getTravelMode( final String travelModeFromSurvey) {
		String travelMode ;
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
		default : throw new RuntimeException("Travel mode input code is not recognized. Aborting ...");
		}
		return travelMode;
	}

	// this method is for slum population as 480 plans don't have information about travel mode so one random mode is assigned out of these four modes. 
	private String randomModeSlum () {
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

	private String randomModeUrban () {
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