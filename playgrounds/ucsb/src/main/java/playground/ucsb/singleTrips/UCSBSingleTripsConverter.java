/* *********************************************************************** *
 * project: org.matsim.*
 * UCSBSingleTripsConverter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.ucsb.singleTrips;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.opengis.feature.simple.SimpleFeature;

import playground.ucsb.UCSBUtils;

/**
 * @author balmermi
 *
 */
public class UCSBSingleTripsConverter {

	private final static Logger log = Logger.getLogger(UCSBSingleTripsConverter.class);
	
	private final Map<String, SimpleFeature> features;
	
	public UCSBSingleTripsConverter(Map<String,SimpleFeature> features) {
		this.features = features;
	}
	
	public final void createPlansFromTripFile(String tripFile, Population population, double fraction) {
		String[] nameParts = tripFile.split("[/\\.]", -1);
		String timeSlide = nameParts[nameParts.length-2].substring(0,2);
		String type = nameParts[nameParts.length-2].substring(2);
		log.info("create plans (timeSlide="+timeSlide+";type="+type+") from trip file + "+tripFile+"...");
		int lineCnt = 0;
		int personCnt = 0;
		int ommitCnt = 0;
		BufferedReader br = IOUtils.getBufferedReader(tripFile);
		try {
			// there is no header
			String curr_line = null;
			while ((curr_line = br.readLine()) != null) {
				lineCnt++;
				String[] entries = curr_line.split(",", -1);
				String fromTAZ = entries[0].trim();
				String toTAZ = entries[1].trim();
				double numTrips = 0;
				for (int i=2; i<entries.length-1;i++) {
					numTrips += new Double(entries[i]).doubleValue();
				}
				long numPlans = Math.round(numTrips);
				
				for (int i=0; i<numPlans; i++) {
					if (UCSBUtils.r.nextDouble() < fraction) {
						boolean ok = true;
						if (features.get(fromTAZ) == null) {
							log.warn(lineCnt+": no fromTAZ found with id="+fromTAZ+". Ommiting trip.");
							ok = false;
						}
						if (features.get(toTAZ) == null) {
							log.warn(lineCnt+": no toTAZ found with id="+fromTAZ+". Ommiting trip.");
							ok = false;
						}
						if (ok) {
							personCnt++;
							Person person = population.getFactory().createPerson(Id.create(timeSlide+type+"-"+fromTAZ+"-"+toTAZ+"-"+i, Person.class));
							population.addPerson(person);
							Plan plan = population.getFactory().createPlan();
							person.addPlan(plan);
							Coord coord = UCSBUtils.getRandomCoordinate(features.get(fromTAZ));
							Activity activity = population.getFactory().createActivityFromCoord(type,coord);
							activity.setStartTime(0);
							double endTime = -1;
							if (timeSlide.equals("AM")) { endTime = 6*3600+UCSBUtils.r.nextDouble()*5*3600; }
							else if (timeSlide.equals("MD")) { endTime = 11*3600+UCSBUtils.r.nextDouble()*3*3600; }
							else if (timeSlide.equals("PM")) { endTime = 14*3600+UCSBUtils.r.nextDouble()*6*3600; }
							else if (timeSlide.equals("NT")) { endTime = 20*3600+UCSBUtils.r.nextDouble()*3*3600; }
							else { throw new RuntimeException("line "+lineCnt+": timeSlide="+timeSlide+" not known."); }
							activity.setEndTime(endTime);
							plan.addActivity(activity);
							Leg leg = population.getFactory().createLeg(TransportMode.car);
							leg.setDepartureTime(endTime);
							leg.setTravelTime(0);
							plan.addLeg(leg);
							coord = UCSBUtils.getRandomCoordinate(features.get(fromTAZ));
							activity = population.getFactory().createActivityFromCoord(type,coord);
							activity.setStartTime(endTime);
							activity.setEndTime(24*3600);
							plan.addActivity(activity);
						}
						else {
							ommitCnt++;
						}
					}
				}
			}
			log.info(lineCnt+" lines parsed.");
			log.info(personCnt+" persons added.");
			log.info(ommitCnt+" persons omitted (because of unknown TAZ id).");
			log.info(population.getPersons().size()+" persons in the database.");
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		log.info("done. (create)");
	}
	
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {

//		String localInBase = "D:/balmermi/documents/eclipse/input/raw/america/usa/losAngeles/UCSB/0000";
//		String localOutBase = "D:/balmermi/documents/eclipse/output/ucsb";
//		args = new String[] {
//				localInBase+"/demand/goods_trips",
//				localInBase+"/geographics/TAZ/taz_Project_UTM_Zone_11N.shp",
//				"ID",
//				"0.002",
//				localOutBase+"/demand"
//		};

		if (args.length != 5) {
			log.error("UCSBSingleTripsConverter inputBase tazShapeFile tazIdName popFraction outputBase");
			System.exit(-1);
		}
		
		// store input parameters
		String inputBase = args[0];
		String tazShapeFile = args[1];
		String tazIdName = args[2];
		Double popFraction = Double.parseDouble(args[3]);
		String outputBase = args[4];

		// print input parameters
		log.info("inputBase: "+outputBase);
		log.info("tazShapeFile: "+tazShapeFile);
		log.info("tazIdName: "+tazIdName);
		log.info("popFraction: "+popFraction);
		log.info("outputBase: "+outputBase);
		
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		log.info("reading "+tazShapeFile+" file...");
		Map<String,SimpleFeature> features = UCSBUtils.getFeatureMap(tazShapeFile, tazIdName);
		log.info("done. (reading)");
		UCSBSingleTripsConverter converter = new UCSBSingleTripsConverter(features);
		File file = new File(inputBase);
		for (int i=0; i<file.list().length; i++) {
			if (file.list()[i].endsWith(".txt.gz")) {
				converter.createPlansFromTripFile(inputBase+"/"+file.list()[i],scenario.getPopulation(),popFraction);
			}
		}
		new PopulationWriter(scenario.getPopulation(),null).write(outputBase+"/plans.singleTrips.xml.gz");
	}
}
