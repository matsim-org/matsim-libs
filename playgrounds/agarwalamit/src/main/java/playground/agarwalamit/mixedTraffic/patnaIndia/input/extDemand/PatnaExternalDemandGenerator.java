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
package playground.agarwalamit.mixedTraffic.patnaIndia.input.extDemand;

import java.io.BufferedReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.utils.io.IOUtils;

import playground.agarwalamit.mixedTraffic.patnaIndia.PatnaUtils;
import playground.agarwalamit.utils.LoadMyScenarios;

/**
 * @author amit
 */

public class PatnaExternalDemandGenerator {

	private Scenario scenario;
	private final String inputFilesDir = PatnaUtils.INPUT_FILES_DIR+"/externalDemandInputFiles/";
	private Random random = MatsimRandom.getRandom();
	private final String networkFile = "../../../../repos/runs-svn/patnaIndia/run108/input/network_diff_linkSpeed.xml.gz";

	public static void main(String[] args) {
		new PatnaExternalDemandGenerator().run();
	}
	
	private void run(){
		scenario = LoadMyScenarios.loadScenarioFromNetwork(networkFile);
		createExternalToExternalDemandForAllStations();
	}

	private void createExternalToExternalDemandForAllStations(){
		//OC1
		createExternalToExternalPlans(inputFilesDir+"/oc1_patna2Fatua.txt", OuterCordonUtils.getCountingStationKey("OC1", "Out"));
		createExternalToExternalPlans(inputFilesDir+"/oc1_fatua2Patna.txt", OuterCordonUtils.getCountingStationKey("OC1", "In"));
		//OC2
		createExternalToExternalPlans(inputFilesDir+"/oc2_patna2Fatua.txt", OuterCordonUtils.getCountingStationKey("OC2", "Out"));
		createExternalToExternalPlans(inputFilesDir+"/oc2_fatua2Patna.txt", OuterCordonUtils.getCountingStationKey("OC2", "In"));
		//OC3
		createExternalToExternalPlans(inputFilesDir+"/oc3_patna2Punpun.txt", OuterCordonUtils.getCountingStationKey("OC3", "Out"));
		createExternalToExternalPlans(inputFilesDir+"/oc3_punpun2Patna.txt", OuterCordonUtils.getCountingStationKey("OC3", "In"));
		//OC4
		createExternalToExternalPlans(inputFilesDir+"/oc4_patna2Muz.txt", OuterCordonUtils.getCountingStationKey("OC4", "Out"));
		createExternalToExternalPlans(inputFilesDir+"/oc4_muz2Patna.txt", OuterCordonUtils.getCountingStationKey("OC4", "In"));
		//OC5
		createExternalToExternalPlans(inputFilesDir+"/oc5_patna2Danapur.txt", OuterCordonUtils.getCountingStationKey("OC5", "Out"));
		createExternalToExternalPlans(inputFilesDir+"/oc5_danapur2Patna.txt", OuterCordonUtils.getCountingStationKey("OC5", "In"));
		//OC6
		createExternalToExternalPlans(inputFilesDir+"/oc6_Noera2Fatua.txt", OuterCordonUtils.getCountingStationKey("OC6", "Out"));
		createExternalToExternalPlans(inputFilesDir+"/oc6_fatua2Noera.txt", OuterCordonUtils.getCountingStationKey("OC6", "In"));
		//OC7
		createExternalToExternalPlans(inputFilesDir+"/oc7_patna2Danapur.txt", OuterCordonUtils.getCountingStationKey("OC7", "Out"));
		createExternalToExternalPlans(inputFilesDir+"/oc7_danapur2Patna.txt", OuterCordonUtils.getCountingStationKey("OC7", "In"));
	}

	private void createExternalToExternalPlans(String file, String countingStationKey){
		Population population = scenario.getPopulation();
		PopulationFactory pf = population.getFactory();
		Map<Double, Map<String,Double>> timebin2mode2count = readFileAndReturnMap(file);

		for(double timebin : timebin2mode2count.keySet()){
			for(String mode : timebin2mode2count.get(timebin).keySet()){
				double directionSplitFactor = OuterCordonUtils.getDirectionalFactorFromOuterCordonKey(countingStationKey, "E2E");
				double count = Math.round(timebin2mode2count.get(timebin).get(mode)* directionSplitFactor / PatnaUtils.COUNT_SCALE_FACTOR);

				for(int ii=0; ii< count; ii++){ // create person
					String prefix = countingStationKey+"_E2E_";
					Id<Person> personId = Id.createPersonId(prefix+ population.getPersons().size());
					Person p = pf.createPerson(personId);
					population.addPerson(p);

					Id<Link> firstActLink = getLinkIdFromOuterCordonKey(countingStationKey, true);
					for (int jj =1;jj<=7;jj++){ // 6 plans for each outer cordon location

						String countingStationKeyForLastActLink = OuterCordonUtils.getCountingStationKey("OC"+jj, "Out"); // ext to ext-- so going out
						if(countingStationKey.equalsIgnoreCase(countingStationKeyForLastActLink)) continue; // excluding same origin- destination

						Plan plan = pf.createPlan();
						Activity firstAct = pf.createActivityFromLinkId(countingStationKey+"_Start", firstActLink);
						firstAct.setEndTime((timebin-1)*3600+random.nextDouble()*3600);
						plan.addActivity(firstAct);
						plan.addLeg(pf.createLeg(mode));

						Id<Link> lastActLink = getLinkIdFromOuterCordonKey(countingStationKeyForLastActLink, false);
						Activity lastAct = pf.createActivityFromLinkId(countingStationKeyForLastActLink+"_End", lastActLink);
						plan.addActivity(lastAct);
						p.addPlan(plan);
					}
				}
			}
		}
	}

	/**
	 * @return the adjacent link (previous link for origin link and next link for destination link) corresponding to the counting station.
	 */
	private Id<Link> getLinkIdFromOuterCordonKey(String countingStationKey, boolean isOrigin){
		Id<Link> linkId = OuterCordonUtils.getCountStationLinkId(countingStationKey);
		Link link = scenario.getNetwork().getLinks().get(linkId);
		if(isOrigin) {
			return link.getFromNode().getInLinks().keySet().iterator().next();
		} else {
			return link.getToNode().getOutLinks().keySet().iterator().next();
		}
	}

	private Map<Double, Map<String,Double>> readFileAndReturnMap(String inputFile){
		Map<Double, Map<String,Double>> time2mode2count = new HashMap<>();
		try (BufferedReader reader = IOUtils.getBufferedReader(inputFile)){
			String line = reader.readLine();
			while(line != null ) {
				if( line.startsWith("time") ){
					line = reader.readLine();
					continue;
				}
				Map<String, Double> mode2Count = new HashMap<>();
				String parts[]	= line.split("\t");
				double timebin = Double.valueOf(parts[0]);
				double carCount = Double.valueOf(parts[1]);
				double motorbikeCount = Double.valueOf(parts[2]);
				double truckCount = Double.valueOf(parts[2]);
				double bikeCount = Double.valueOf(parts[4]);

				mode2Count.put("car", carCount);
				mode2Count.put("motorbike", motorbikeCount);
				mode2Count.put("truck", truckCount);
				mode2Count.put("bike", bikeCount);
				time2mode2count.put(timebin, mode2Count);
				line=reader.readLine();
			}
		} catch (Exception e) {
			throw new RuntimeException("File is not read. Reason "+e);
		}
		return time2mode2count;
	}
}