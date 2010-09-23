/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.andreas.bln.pop.generate;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.misc.ConfigUtils;



/**
 *
 * @author aneumann
 *
 */
public class MergeCoord {

	private static final Logger log = Logger.getLogger(TabReader.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			Config config = ConfigUtils.loadConfig("./src/playground/andreas/bln/config.xml");

			MergeCoord myMergeCoord = new MergeCoord();

			ArrayList<String[]> tripMap = myMergeCoord.readTrips("Z:/population/input/WEGE.csv");
			HashMap<Integer, HashMap<Integer, CoordImpl>> coordMap = myMergeCoord.readCoord("Z:/population/input/coord32");

			// 1 Renew coordinates if within perimeter of old ones
			myMergeCoord.nearOldCoord(tripMap, coordMap, 5000.0);

			// Renew coordinates if person is still on travel after 24 hours
//			myMergeCoord.getCoordForLateArrivals(tripMap, coordMap,
//					"C:/Users/aneumann/Java/eclwrk/matsim_20071126/0.events.txt");

			// 2 Renew coordinates if person get leg with mode walk which take more than 20 minutes
			myMergeCoord.getCoordForWalkingTooMuch(tripMap, coordMap,
					"C:/Users/aneumann/Java/eclwrk/matsim_20071126/bb_cl.xml.gz",
					"C:/Users/aneumann/Java/eclwrk/matsim_20071126/0.plans.xml.gz");

			// 3 Set all transport modes to car, if person still walks more than 60 min
			myMergeCoord.setAllstillWalkingTooMuchToCar(tripMap, coordMap,
					"C:/Users/aneumann/Java/eclwrk/matsim_20071126/bb_cl.xml.gz",
					"C:/Users/aneumann/Java/eclwrk/matsim_20071126/1.plans.xml.gz");

			// Write it out
			myMergeCoord.writeTripsToFile(tripMap, "Z:/population/input/WEGE3.csv");

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void nearOldCoord(ArrayList<String[]> tripMap, HashMap<Integer, HashMap<Integer, CoordImpl>> coordMap, double perimeter_radius){
		// Copy every new coord if inbetween 5km of the old one
		int unchanged = 0;
		int changed = 0;
		int tripsTooFarAway = 0;

		for (String[] trip : tripMap) {

			Integer personId = Integer.valueOf(trip[1]);
			Integer wayId  = Integer.valueOf(trip[2]);

			try {
				CoordImpl oldCoord = new CoordImpl(Double.parseDouble(trip[11]), Double.parseDouble(trip[12]));
				CoordImpl newCoord = coordMap.get(personId).get(wayId);

				double distance = newCoord.calcDistance(oldCoord);

				if(distance < perimeter_radius){
					trip[11] = String.valueOf(newCoord.getX());
					trip[12] = String.valueOf(newCoord.getY());
					changed++;
				} else {
					tripsTooFarAway++;
				}

			} catch (Exception e) {
				unchanged++;
			}
		}

		log.info("Finished merging coord. " + unchanged + " unchanged + " + changed + " changed entries + " + tripsTooFarAway + " too far away = " + (changed + unchanged + tripsTooFarAway));
	}

	private void getCoordForLateArrivals(ArrayList<String[]> tripMap, HashMap<Integer, HashMap<Integer, CoordImpl>> coordMap, String filename) throws IOException{

		int unchanged = 0;
		int changed = 0;
		int tripNonWalking = 0;

		log.info("Start reading file " + filename);
		TreeSet<Integer> agentIds = EventReader.readFile(filename);
		log.info("...finished reading and found " + agentIds.size() + " agent ids arriving after 24h non char id.");

		for (String[] trip : tripMap) {

			Integer personId = Integer.valueOf(trip[1]);
			Integer wayId  = Integer.valueOf(trip[2]);

			if(agentIds.contains(personId)){

				try {
//					CoordImpl oldCoord = new CoordImpl(Double.parseDouble(trip[11]), Double.parseDouble(trip[12]));
					CoordImpl newCoord = coordMap.get(personId).get(wayId);

					if(Integer.parseInt(trip[48]) == 3 || Integer.parseInt(trip[48]) == 0 || Integer.parseInt(trip[48]) == 1 || Integer.parseInt(trip[48]) == 2
							|| Integer.parseInt(trip[48]) == 4 || Integer.parseInt(trip[48]) == 5 || Integer.parseInt(trip[48]) == 6 || Integer.parseInt(trip[48]) == 7){
						if(newCoord.getX() < 4700000.0 && newCoord.getX() > 4400000.0 && newCoord.getY() > 5690000.0 && newCoord.getY() < 5910000.0){
							trip[11] = String.valueOf(newCoord.getX());
							trip[12] = String.valueOf(newCoord.getY());
							changed++;
						} else {
							unchanged++;
						}

					} else {
						tripNonWalking++;
					}

				} catch (Exception e) {
					unchanged++;
				}

			}

			//	info:	Integer.parseInt(trip[48]), 0: "keine Angabe", 1: "Fuss", 2: "Rad"
			//			3: "MIV", 4: "OEV", 5: "Rad/OEV", 6: "IV/OEV", 7: "sonstiges"

		}
		log.info("Finished merging coord. " + unchanged + " unchanged + " + changed + " changed entries + " + tripNonWalking + " non walking = " + (changed + unchanged + tripNonWalking));
	}

	private void getCoordForWalkingTooMuch(ArrayList<String[]> tripMap, HashMap<Integer, HashMap<Integer, CoordImpl>> coordMap, String networkFilename, String plansFilename){

		TreeSet<Integer> agentIds = new TreeSet<Integer>();

		int unchanged = 0;
		int changed = 0;
		int tripNonWalking = 0;

		ScenarioImpl scenario = new ScenarioImpl();
		log.info("Start reading file " + plansFilename);
		new MatsimNetworkReader(scenario).readFile(networkFilename);

		log.info("Start reading file " + plansFilename);
		Population population = scenario.getPopulation();
		PopulationReader plansReader = new MatsimPopulationReader(scenario);
		plansReader.readFile(plansFilename);

		// Find persons with walking trips over 60 minutes long
		for (Person person : population.getPersons().values()) {
			for(Plan plan : person.getPlans()){
				for (PlanElement planelement : plan.getPlanElements()) {

					if(planelement instanceof Leg){
						if(((Leg) planelement).getMode() == TransportMode.walk){
							if(((Leg) planelement).getTravelTime() > 60 * 60){
								try {
//									Integer.valueOf(person.getId().toString());
									agentIds.add(Integer.valueOf(person.getId().toString()));
								} catch (Exception e) {
									e.printStackTrace();
								}
							}
						}
					}

				}
			}
		}

		log.info("Found " + agentIds.size() + " person ids to fix, cause of wakling too much.");

		// Try to fix them
		for (String[] trip : tripMap) {

			Integer personId = Integer.valueOf(trip[1]);
			Integer wayId  = Integer.valueOf(trip[2]);

			if(agentIds.contains(personId)){

				try {
//					CoordImpl oldCoord = new CoordImpl(Double.parseDouble(trip[11]), Double.parseDouble(trip[12]));
					CoordImpl newCoord = coordMap.get(personId).get(wayId);

//					if(Integer.parseInt(trip[48]) == 3 || Integer.parseInt(trip[48]) == 0 || Integer.parseInt(trip[48]) == 1 || Integer.parseInt(trip[48]) == 2
//							|| Integer.parseInt(trip[48]) == 4 || Integer.parseInt(trip[48]) == 5 || Integer.parseInt(trip[48]) == 6 || Integer.parseInt(trip[48]) == 7){
					if(newCoord.getX() < 4700000.0 && newCoord.getX() > 4460000.0 && newCoord.getY() > 5690000.0 && newCoord.getY() < 5910000.0){
						trip[11] = String.valueOf(newCoord.getX());
						trip[12] = String.valueOf(newCoord.getY());
						changed++;
					} else {
						unchanged++;
					}

//					} else {
//						tripNonWalking++;
//					}

				} catch (Exception e) {
					unchanged++;
				}

			}
			//	info:	Integer.parseInt(trip[48]), 0: "keine Angabe", 1: "Fuss", 2: "Rad"
			//			3: "MIV", 4: "OEV", 5: "Rad/OEV", 6: "IV/OEV", 7: "sonstiges"

		}
		log.info("Walking too much: Finished merging coord. " + unchanged + " unchanged + " + changed + " changed entries + " + tripNonWalking + " non walking = " + (changed + unchanged + tripNonWalking));
	}

	private void setAllstillWalkingTooMuchToCar(ArrayList<String[]> tripMap, HashMap<Integer, HashMap<Integer, CoordImpl>> coordMap, String networkFilename, String plansFilename){

		TreeSet<Integer> agentIds = new TreeSet<Integer>();

		int unchanged = 0;
		int changed = 0;

		ScenarioImpl scenario = new ScenarioImpl();
		log.info("Start reading file " + plansFilename);
		new MatsimNetworkReader(scenario).readFile(networkFilename);

		log.info("Start reading file " + plansFilename);
		Population population = scenario.getPopulation();
		PopulationReader plansReader = new MatsimPopulationReader(scenario);
		plansReader.readFile(plansFilename);


		// Find persons with walking trips over 60 minutes long
		for (Person person : population.getPersons().values()) {
			for(Plan plan : person.getPlans()){
				for (PlanElement planelement : plan.getPlanElements()) {

					if(planelement instanceof Leg){
						if(((Leg) planelement).getMode() == TransportMode.walk){
							if(((Leg) planelement).getTravelTime() > 60 * 60){
								try {
//									Integer.valueOf(person.getId().toString());
									agentIds.add(Integer.valueOf(person.getId().toString()));
								} catch (Exception e) {
									e.printStackTrace();
								}
							}
						}
					}

				}
			}
		}

		log.info("Found " + agentIds.size() + " person ids to fix, cause of STILL walking too much.");

		// Try to fix them
		for (String[] trip : tripMap) {

			Integer personId = Integer.valueOf(trip[1]);

			if(agentIds.contains(personId)){
				trip[48] = "3";
				changed++;
			} else {
				unchanged++;
			}

			//	info:	Integer.parseInt(trip[48]), 0: "keine Angabe", 1: "Fuss", 2: "Rad"
			//			3: "MIV", 4: "OEV", 5: "Rad/OEV", 6: "IV/OEV", 7: "sonstiges"

		}
		log.info("STILL Walking too much: Finished merging coord. " + unchanged + " unchanged + " + changed + " changed entries = " + (changed + unchanged));
	}

	private ArrayList<String[]> readTrips(String filename) throws IOException{

		log.info("Start reading file " + filename);
		ArrayList<String[]> unsortedTripData = TabReader.readFile(filename);
		log.info("...finished reading " + unsortedTripData.size() + " entries in trip file.");

		return unsortedTripData;
	}

	private HashMap<Integer,HashMap<Integer,CoordImpl>> readCoord(String filename) throws IOException{

		HashMap<Integer, HashMap<Integer, CoordImpl>> coordData = new HashMap<Integer, HashMap<Integer, CoordImpl>>();
		int withoutCoord = 0;

		log.info("Start reading file " + filename);
		ArrayList<String[]> unsortedCoordData = TabReader.readFile(filename);
		log.info("...finished reading " + unsortedCoordData.size() + " entries in coord file.");

		for (String[] coordDataString : unsortedCoordData) {
			Integer personId = Integer.valueOf(coordDataString[0]);
			Integer wayId = Integer.valueOf(coordDataString[1]);
			CoordImpl coord = null;
			try {
				coord = new CoordImpl(Double.parseDouble(coordDataString[2]), Double.parseDouble(coordDataString[3]));
			} catch (Exception e) {
				withoutCoord++;
			}

			if(coordData.get(personId) != null){
//					if(coordData.get(personId).get(wayId) != null){
				coordData.get(personId).put(wayId, coord);
//					}// else{
//						log.warn("Already coord Object saved for " + personId + " " + wayId);
//					}
			} else {
				HashMap<Integer, CoordImpl> newCoordMap = new HashMap<Integer, CoordImpl>();
				newCoordMap.put(wayId, coord);
				coordData.put(personId, newCoordMap);
			}
		}

		log.info("Finished reading coords. " + withoutCoord + " entries without coords found");
		return coordData;
	}

	private void writeTripsToFile(ArrayList<String[]> trips, String filename){

		BufferedWriter writer;
		try {
			writer = new BufferedWriter(new FileWriter(new File(filename)));

			writer.write("STARTZEIT,Ordnr2,wegnr2,Startzeit2,Zielzeit2,ZIELZEIT,ZIELNAME,ZIELORT,ZIELSTR,ZIELBEZNUM,WOHN,ZIELX,ZIELY,VONTVZ,NACHTVZ,VONVD,NACHVD,VONBEZ,NACHBEZ,ENTF,ORDNRPER,WEGNR,ordnr3,wegnr3,vontvz2,nachtvz2,vonvd2,nachVD2,vonBez2,nachBez2,Entf2,ZWECK,ANDZWECK,ZW1,ZW2,ZW3,ZW4,RZ_1,RZ_2,VM,ANDVM,VM1,VM2,VM3,VM4,VM5,VM6,VM_1,VM_2,WEG,ZEIT,WEIWEGE");
			writer.newLine();

			for (String[] strings : trips) {
				for (String string : strings) {
					writer.write(string);
					writer.write(",");
				}
				writer.newLine();
			}

			writer.flush();
			writer.close();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		log.info("Finished writing trips to " + filename);
	}

}