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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.IOUtils;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Point;

import playground.agarwalamit.mixedTraffic.patnaIndia.PatnaUtils;
import playground.agarwalamit.utils.GeometryUtils;
import playground.agarwalamit.utils.LoadMyScenarios;

/**
 * @author amit
 */

public class PatnaExternalDemandGenerator {
	private static final Logger LOG = Logger.getLogger(PatnaExternalDemandGenerator.class);
	private Scenario scenario;
	private final String inputFilesDir = PatnaUtils.INPUT_FILES_DIR+"/externalDemandInputFiles/";
	private Random random = MatsimRandom.getRandom();
	private final String networkFile = "../../../../repos/runs-svn/patnaIndia/run108/input/network_diff_linkSpeed.xml.gz";
	public static void main(String[] args) {
		new PatnaExternalDemandGenerator().run();
	}

	private void run(){
		scenario = LoadMyScenarios.loadScenarioFromNetwork(networkFile);
		createDemandForAllStations();
		new PopulationWriter(scenario.getPopulation()).write("../../../../repos/runs-svn/patnaIndia/run108/input/outerCordonDemand_10pct.xml.gz");
		System.out.println("Number of persons in the population are "+scenario.getPopulation().getPersons().size());
	}

	private void createDemandForAllStations(){
		//for ext-int-ext trips only one of the input files is taken, because (at least) the total count multiply by directional split will give same from both files.

		//OC1
		createExternalToExternalPlans(inputFilesDir+"/oc1_fatua2Patna.txt", "OC1", "In" );
		createExternalToInternalPlans(inputFilesDir+"/oc1_fatua2Patna.txt","OC1");

		//OC2
		createExternalToExternalPlans(inputFilesDir+"/oc2_fatua2Patna.txt", "OC2", "In");
		createExternalToInternalPlans(inputFilesDir+"/oc2_fatua2Patna.txt", "OC2");

		//OC3
		createExternalToExternalPlans(inputFilesDir+"/oc3_punpun2Patna.txt", "OC3", "In");
		createExternalToInternalPlans(inputFilesDir+"/oc3_punpun2Patna.txt", "OC3");

		//OC4
		createExternalToExternalPlans(inputFilesDir+"/oc4_muz2Patna.txt", "OC4", "In");
		createExternalToInternalPlans(inputFilesDir+"/oc4_muz2Patna.txt", "OC4");

		//OC5
		createExternalToExternalPlans(inputFilesDir+"/oc5_danapur2Patna.txt", "OC5", "In");
		createExternalToInternalPlans(inputFilesDir+"/oc5_danapur2Patna.txt", "OC5");

		//OC6
		createExternalToExternalPlans(inputFilesDir+"/oc6_fatua2Noera.txt", "OC6", "In");
		createExternalToInternalPlans(inputFilesDir+"/oc6_fatua2Noera.txt", "OC6");

		//OC7
		createExternalToExternalPlans(inputFilesDir+"/oc7_danapur2Patna.txt", "OC7", "In");
		createExternalToInternalPlans(inputFilesDir+"/oc7_danapur2Patna.txt", "OC7");
	}

	private void createExternalToInternalPlans(final String file, final String countingStationNumber){
		int noOfPersonsAdded = 0;

		Population population = scenario.getPopulation();
		PopulationFactory pf = population.getFactory();
		Map<Double, Map<String,Double>> timebin2mode2count = readFileAndReturnMap(file);

		Map<String, List<SimpleFeature>> area2ZonesLists = getInternalZoneFeaturesForExtInternalTrips();

		String countingStationKey = OuterCordonUtils.getCountingStationKey(countingStationNumber, "In");
		Link originActLink = getLinkFromOuterCordonKey(countingStationNumber, true);
		Link destinationActLink = NetworkUtils.getConnectingLink(originActLink.getToNode(), originActLink.getFromNode()); 

		for(double timebin : timebin2mode2count.keySet()){
			for(String mode : timebin2mode2count.get(timebin).keySet()){
				double directionSplitFactor = OuterCordonUtils.getDirectionalFactorFromOuterCordonKey(countingStationKey, "E2I");
				double count = Math.round(timebin2mode2count.get(timebin).get(mode)* directionSplitFactor * OuterCordonUtils.SAMPLE_SIZE);

				for(int ii=0; ii< count; ii++){ // create person
					String prefix = countingStationKey+"_E2I_";
					Id<Person> personId = Id.createPersonId(prefix+ population.getPersons().size());
					Person p = pf.createPerson(personId);
					population.addPerson(p);
					noOfPersonsAdded++;
					for( String area : area2ZonesLists.keySet() ){ // create a plan for each zone (ext-int-ext)
						Plan plan = pf.createPlan();
						Activity originAct = pf.createActivityFromLinkId( "E2I_Start", originActLink.getId());
						originAct.setEndTime( (timebin-1)*3600 + random.nextDouble()*3600);

						Point randomPointInZone = GeometryUtils.getRandomPointsInsideFeatures(area2ZonesLists.get(area));
						Coord middleActCoord = PatnaUtils.COORDINATE_TRANSFORMATION.transform( new Coord(randomPointInZone.getX(),randomPointInZone.getY()) );

						Activity middleAct = pf.createActivityFromCoord("E2I_mid_"+area.substring(0, 3), middleActCoord);
						//ZZ_TODO : here the act duration is assigned randomly between 6 to 8 hours. This means, the agent will be counted in reverse direction of the same counting station.
						double middleActEndTime = originAct.getEndTime() + 6*3600 + random.nextDouble() * 7200;
						Activity destinationAct = pf.createActivityFromLinkId( "E2I_Start", destinationActLink.getId());
						
						if((middleActEndTime > 20*3600 && middleActEndTime <24*3600) && (countingStationNumber.equals("OC3") || countingStationNumber.equals("OC6")) ) {
							middleActEndTime = middleActEndTime - (3*3600+random.nextDouble()*3*3600); //pushing departure time of higher counts aroung mid night
						}

						if(middleActEndTime >= 24*3600 ) { // midAct - startAct - midAct ==> this will give count in both time bins for desired counting station
							if(countingStationNumber.equals("OC3") || countingStationNumber.equals("OC6")) {
								middleActEndTime =  middleActEndTime - (16*3600 + random.nextDouble()*3600*4);//trying to delay end of the midact to get lesser vehicles in early hours of the counting stations.
							}
							else middleActEndTime =  middleActEndTime - 24*3600; 
							middleAct.setEndTime( middleActEndTime );
							plan.addActivity(middleAct);
							plan.addLeg(pf.createLeg(mode));
							destinationAct.setEndTime( (timebin-1)*3600 + random.nextDouble()*3600 ); // end this in the current timebin
							plan.addActivity(destinationAct);
							plan.addLeg(pf.createLeg(mode));
							plan.addActivity( pf.createActivityFromCoord(middleAct.getType(), middleActCoord) );
						} else { // startAct - midAct - startAct
							plan.addActivity(originAct);
							plan.addLeg(pf.createLeg(mode));
							middleAct.setEndTime( middleActEndTime );
							plan.addActivity(middleAct);
							plan.addLeg(pf.createLeg(mode));
							plan.addActivity(destinationAct);	
						}
						p.addPlan(plan);
					}
				}
			}
		}
		LOG.info(noOfPersonsAdded+" external to internal presons are added to the population for counting station "+countingStationNumber);
		if(noOfPersonsAdded==0)LOG.warn("No external to internal presons are added to the population for counting station "+countingStationNumber);
	}
	
	/**
	 * @param countingDirection "In" for outside to Patna, "Out" for Patna to outside.
	 */
	private void createExternalToExternalPlans(final String file, final String countingStationNumber, final String countingDirection){
		int noOfPersonsAdded = 0;

		Population population = scenario.getPopulation();
		PopulationFactory pf = population.getFactory();
		Map<Double, Map<String,Double>> timebin2mode2count = readFileAndReturnMap(file);

		String countingStationKey = OuterCordonUtils.getCountingStationKey(countingStationNumber, countingDirection); 
		Link originActLink = null; 

		if(countingDirection.equalsIgnoreCase("In")){// --> trip originates at counting stationNumber
			originActLink = getLinkFromOuterCordonKey(countingStationNumber, true);
		} else {
			throw new RuntimeException("Only trips orginating at the given counting station should be processed. Aborting ...");
		}

		for(double timebin : timebin2mode2count.keySet()){
			for(String mode : timebin2mode2count.get(timebin).keySet()){
				double directionSplitFactor = OuterCordonUtils.getDirectionalFactorFromOuterCordonKey(countingStationKey, "E2E");
				double count = Math.round(timebin2mode2count.get(timebin).get(mode)* directionSplitFactor * OuterCordonUtils.SAMPLE_SIZE);
				for(int ii=0; ii< count; ii++){ // create person
					String prefix = countingStationKey+"_E2E_";
					Id<Person> personId = Id.createPersonId(prefix+ population.getPersons().size());
					Person p = pf.createPerson(personId);

					for (int jj =1;jj<=7;jj++){ // 6 plans for each outer cordon location

						if(countingStationNumber.equalsIgnoreCase("OC"+jj)) continue; // excluding same origin- destination

						if(countingStationNumber.equalsIgnoreCase("OC1") && jj==3 || countingStationNumber.equals("OC3") && jj==1 ) {
							continue; // excluding ext-ext trip between OC1 -- OC3 and OC3 -- OC1
						}

						double actEndTime ;
						Link destinationActLink = null;
						if(countingDirection.equalsIgnoreCase("In")){// --> trip originates at given counting stationNumber
							destinationActLink = getLinkFromOuterCordonKey("OC"+jj, false );
							actEndTime = (timebin-1)*3600+random.nextDouble()*3600;
						} else {// --> trip terminates at given counting stationNumber
							throw new RuntimeException("For external to external counts use other counting direction, i.e. the "+countingStationNumber+ "should be assumed as origin and not destination.");
						}

						Plan plan = pf.createPlan();
						Activity firstAct = pf.createActivityFromLinkId("E2E_Start", originActLink.getId());
						firstAct.setEndTime(actEndTime );
						plan.addActivity(firstAct);
						plan.addLeg(pf.createLeg(mode));

						Activity lastAct = pf.createActivityFromLinkId("E2E_End", destinationActLink.getId());
						plan.addActivity(lastAct);
						p.addPlan(plan);
					}
					population.addPerson(p);
					noOfPersonsAdded++;
				}
			}
		}
		LOG.info(noOfPersonsAdded+" external to external presons are added to the population for counting station "+countingStationNumber);
		if(noOfPersonsAdded==0)LOG.warn("No external to external presons are added to the population for counting station "+countingStationNumber);
	}

	/**
	 * @return the adjacent link (previous link for origin link and next link for destination link) corresponding to the counting station.
	 */
	private Link getLinkFromOuterCordonKey(final String countingStationNumber, final boolean isOrigin){
		// it looks, better is to identify the origin destination link and then use them.
		String link = null;
		if(isOrigin){
			switch(countingStationNumber){
			case "OC1": link = "1386010000-1385710000-1385110000-1380010000"; break;
			case "OC2": link = "OC2_in"; break;
			case "OC3": link = "476810000-475210000-477510000"; break;
			case "OC4": link = "OC4_in"; break;
			case "OC5": link = "OC5_in"; break;
			case "OC6": link = "919-828-1033-1022-1035-101710000-1053-106110000-106010000-1058-69210000-146010000"; break;
			case "OC7": link = "1973-2158-215210000-2163-2169"; break;
			default : throw new RuntimeException("A connecting link in the desired direction is not found. Aborting ...");
			}
		} else {
			switch(countingStationNumber){
			case "OC1": link = "13800-13851-13857-13860"; break;
			case "OC2": link = "OC2_out"; break;
			case "OC3": link = "4775-4752-4768"; break;
			case "OC4": link = "OC4_out"; break;
			case "OC5": link = "OC5_out"; break;
			case "OC6": link = "1460-692-105810000-1060-1061-105310000-1017-103510000-102210000-103310000-82810000-91910000"; break;
			case "OC7": link = "216910000-216310000-2152-215810000-197310000"; break;
			default : throw new RuntimeException("A connecting link in the desired direction is not found. Aborting ...");
			}
		}
		Id<Link> linkId = Id.createLinkId(link);
		return scenario.getNetwork().getLinks().get(linkId);
		
		//		Id<Link> linkId = OuterCordonUtils.getCountStationLinkId(countingStationKey);
		//
		//		Link inLink = scenario.getNetwork().getLinks().get(linkId);
		//		Link reverlseLink = NetworkUtils.getConnectingLink(inLink.getToNode(), inLink.getFromNode());
		//		if(isOrigin) {
		//		 Iterator<? extends Link> it = inLink.getFromNode().getInLinks().values().iterator();
		//			while(it.hasNext() ){
		//				Link l = it.next();
		//				if(! l.equals(reverlseLink) ) return l;
		//			}
		//		} else {
		//			Iterator<? extends Link> it = inLink.getToNode().getOutLinks().values().iterator();
		//			while(it.hasNext() ){
		//				Link l = it.next();
		//				if(! l.equals(reverlseLink) ) return l;
		//			}
		//		}
		//		throw new RuntimeException("A connecting link in the desired direction is not found. Aborting ...");
	}

	private Map<String, List<SimpleFeature>> getInternalZoneFeaturesForExtInternalTrips(){
		Map<String, List<SimpleFeature>> requiredFeatures = new HashMap<>();
		ShapeFileReader reader = new ShapeFileReader();
		Collection<SimpleFeature> features = reader.readFileAndInitialize(PatnaUtils.ZONE_FILE);
		Iterator<SimpleFeature> iterator = features.iterator();
		// first store simpleFeature by id

		Map<Integer,SimpleFeature> id2SimpleFeature = new HashMap<>();

		while(iterator.hasNext()){
			SimpleFeature feature = iterator.next();
			id2SimpleFeature.put( (Integer) feature.getAttribute("ID1"), feature );
		}

		Map<String, List<Integer>> area2zoneIds = OuterCordonUtils.getAreaType2ZoneIds();
		for(String area : area2zoneIds.keySet()){
			List<SimpleFeature> fs = new ArrayList<>();
			for(int zone : area2zoneIds.get(area)){
				fs.add(id2SimpleFeature.get(zone));
			}
			requiredFeatures.put(area, fs);
		}
		return requiredFeatures;
	}

	private Map<Double, Map<String,Double>> readFileAndReturnMap(final String inputFile){
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
				double truckCount = Double.valueOf(parts[3]);
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