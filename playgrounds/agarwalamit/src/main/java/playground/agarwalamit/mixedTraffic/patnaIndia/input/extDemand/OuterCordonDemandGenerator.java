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
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.IOUtils;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Point;

import playground.agarwalamit.mixedTraffic.patnaIndia.utils.OuterCordonUtils;
import playground.agarwalamit.mixedTraffic.patnaIndia.utils.OuterCordonUtils.PatnaNetworkType;
import playground.agarwalamit.mixedTraffic.patnaIndia.utils.PatnaUtils;
import playground.agarwalamit.utils.GeometryUtils;
import playground.agarwalamit.utils.LoadMyScenarios;

/**
 * @author amit
 */

public class OuterCordonDemandGenerator {
	private static final Logger LOG = Logger.getLogger(OuterCordonDemandGenerator.class);
	private Scenario scenario;
	private final String inputFilesDir = PatnaUtils.INPUT_FILES_DIR+"/raw/counts/externalDemandCountsFile/";
	private final Random random = MatsimRandom.getRandom();
	private final String networkFile = PatnaUtils.INPUT_FILES_DIR+"/simulationInputs/network/"+PatnaUtils.PATNA_NETWORK_TYPE.toString()+"/network.xml.gz";
	public static void main(String[] args) {
		new OuterCordonDemandGenerator().run();
	}

	private void run(){
		String outPlansFile = PatnaUtils.INPUT_FILES_DIR+"/simulationInputs/external/"+PatnaUtils.PATNA_NETWORK_TYPE+"/outerCordonDemand_10pct.xml.gz";
		scenario = LoadMyScenarios.loadScenarioFromNetwork(networkFile);
		createDemandForAllStations();
		new PopulationWriter(scenario.getPopulation()).write(outPlansFile);
		System.out.println("Number of persons in the population are "+scenario.getPopulation().getPersons().size());
	}

	private void createDemandForAllStations(){
		//for ext-int-ext trips only one of the input files is taken, because (at least) the total count multiply by directional split will give same from both files.

		//OC1
		createExternalToExternalDemand(inputFilesDir+"/oc1_fatua2Patna.txt", "OC1", "In" );
		createExternalToInternalDemand(inputFilesDir+"/oc1_fatua2Patna.txt","OC1");

		//OC2
		createExternalToExternalDemand(inputFilesDir+"/oc2_fatua2Patna.txt", "OC2", "In");
		createExternalToInternalDemand(inputFilesDir+"/oc2_fatua2Patna.txt", "OC2");

		//OC3
		createExternalToExternalDemand(inputFilesDir+"/oc3_punpun2Patna.txt", "OC3", "In");
		createExternalToInternalDemand(inputFilesDir+"/oc3_punpun2Patna.txt", "OC3");

		//OC4
		createExternalToExternalDemand(inputFilesDir+"/oc4_muz2Patna.txt", "OC4", "In");
		createExternalToInternalDemand(inputFilesDir+"/oc4_muz2Patna.txt", "OC4");

		//OC5
		createExternalToExternalDemand(inputFilesDir+"/oc5_danapur2Patna.txt", "OC5", "In");
		createExternalToInternalDemand(inputFilesDir+"/oc5_danapur2Patna.txt", "OC5");

		//OC6
		createExternalToExternalDemand(inputFilesDir+"/oc6_fatua2Noera.txt", "OC6", "In");
		createExternalToInternalDemand(inputFilesDir+"/oc6_fatua2Noera.txt", "OC6");

		//OC7
		createExternalToExternalDemand(inputFilesDir+"/oc7_danapur2Patna.txt", "OC7", "In");
		createExternalToInternalDemand(inputFilesDir+"/oc7_danapur2Patna.txt", "OC7");
	}

	private void createExternalToInternalDemand(final String file, final String countingStationNumber){
		int noOfPersonsAdded = 0;

		Population population = scenario.getPopulation();
		PopulationFactory pf = population.getFactory();
		Map<Double, Map<String,Double>> timebin2mode2count = readFileAndReturnMap(file);

		Map<String, List<SimpleFeature>> area2ZonesLists = getInternalZoneFeaturesForExtInternalTrips();

		String countingStationKey = OuterCordonUtils.getCountingStationKey(countingStationNumber, "In");
		Link originActLink = getLinkFromOuterCordonKey(countingStationNumber, true);
		Link destinationActLink = getLinkFromOuterCordonKey(countingStationNumber, false); 

		for(double timebin : timebin2mode2count.keySet()){
			for(String mode : timebin2mode2count.get(timebin).keySet()){
				double commutersTrafficShare = OuterCordonUtils.getDirectionalFactorFromOuterCordonKey(countingStationKey, "E2I");
				// this share result in more or less same number of persons as from the aggregated counts data.

				double count = Math.round( timebin2mode2count.get(timebin).get(mode) * commutersTrafficShare * OuterCordonUtils.SAMPLE_SIZE );

				for(int ii=0; ii< count; ii++){ // create person
					String prefix = countingStationKey+"_E2I_";
					Id<Person> personId = Id.createPersonId(prefix+ population.getPersons().size());

					Person p = pf.createPerson(personId);
					population.addPerson(p);
					noOfPersonsAdded++;

					List<Activity> zonalActs = new ArrayList<>(); // get the coordinates of the all possible zonal activity locations
					for( String area : area2ZonesLists.keySet() ){ 
						Point randomPointInZone = GeometryUtils.getRandomPointsInsideFeatures(area2ZonesLists.get(area));
						Coord middleActCoord = PatnaUtils.COORDINATE_TRANSFORMATION.transform( new Coord(randomPointInZone.getX(),randomPointInZone.getY()) );
						Activity act = pf.createActivityFromCoord("E2I_mid_"+area.substring(0, 3), middleActCoord);
						zonalActs.add(act);
					}

					// fix activity duration such that, all plans are differntiated based on ONLY zonal activity locations.
					double firstActEndTime = (timebin-1)*3600. + random.nextDouble()*3600.;
					double secondActEndTime = firstActEndTime + 5*3600. + random.nextDouble() * 3. *3600.;

					if((secondActEndTime > 21*3600. && secondActEndTime <24*3600.)  ) {
						if ( countingStationNumber.equals("OC3") ) {
							secondActEndTime = secondActEndTime - (1*3600.+random.nextDouble()* 3 *3600.); //preponing departure time of higher counts around mid night
						} else if ( countingStationNumber.equals("OC6")){
							//							secondActEndTime = secondActEndTime - (3*3600+random.nextDouble()*4*3600);
							secondActEndTime = 9*3600.+random.nextDouble()*6*3600.; 
							//after 22:00 counts are zero, thus pushing all agents after 20:00 to non-peak hours (where demand is too less). After simulation, there will be some traffic between 20:00- 22:00 to balance counts
						}
					}

					if(secondActEndTime >= 24*3600 ) { // midAct - startAct - midAct ==> this will give count in both time bins for desired counting station
						if(countingStationNumber.equals("OC3") ) {
							secondActEndTime =  secondActEndTime - (16*3600 + random.nextDouble()*3600*4);
						} else if(countingStationNumber.equals("OC6")) {
							secondActEndTime = 6.5*3600 + random.nextDouble()*3600*8;
						} else secondActEndTime =  secondActEndTime - 24*3600; 

						Activity midAct = pf.createActivityFromLinkId( "E2I_Start", destinationActLink.getId());
						midAct.setEndTime( (timebin-1)*3600 + random.nextDouble()*3600 ); // end this act in the current timebin

						for (Activity originAct : zonalActs) {
							originAct.setEndTime(secondActEndTime);
							Activity destinationAct = pf.createActivityFromCoord(originAct.getType(), originAct.getCoord());
							Plan plan = createPlan(originAct, midAct, destinationAct, mode);
							p.addPlan(plan);
						}
					} else { // startAct - midAct - startAct
						Activity originAct = pf.createActivityFromLinkId( "E2I_Start", originActLink.getId());
						originAct.setEndTime( firstActEndTime );
						Activity destinationAct = pf.createActivityFromLinkId( "E2I_Start", destinationActLink.getId());

						for (Activity midAct : zonalActs) {
							midAct.setEndTime(secondActEndTime);
							Plan plan = createPlan(originAct, midAct, destinationAct, mode);
							p.addPlan(plan);
						}
					}
				}
			}
		}
		LOG.info(noOfPersonsAdded+" external to internal presons are added to the population for counting station "+countingStationNumber);
		if(noOfPersonsAdded==0)LOG.warn("No external to internal presons are added to the population for counting station "+countingStationNumber);
	}

	private Plan createPlan(final Activity firstAct, final Activity middleAct, final Activity lastAct, final String legMode ){
		PopulationFactory pf = scenario.getPopulation().getFactory();
		Plan plan = pf.createPlan();
		plan.addActivity(firstAct);
		plan.addLeg(pf.createLeg(legMode));
		plan.addActivity(middleAct);
		plan.addLeg(pf.createLeg(legMode));
		plan.addActivity(lastAct);
		return plan;
	}

	/**
	 * @param countingDirection "In" for outside to Patna, "Out" for Patna to outside.
	 */
	private void createExternalToExternalDemand(final String file, final String countingStationNumber, final String countingDirection){
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

				double throughTrafficShare = OuterCordonUtils.getDirectionalFactorFromOuterCordonKey(countingStationKey, "E2E");

				// the through traffic share is increased by 10% because total persons generated were about 10% short. This is not uniform for all counting stations.
				throughTrafficShare = 1.0 * throughTrafficShare;

				double personCount = Math.round( timebin2mode2count.get(timebin).get(mode) * throughTrafficShare * OuterCordonUtils.SAMPLE_SIZE );

				for (int jj =1;jj<=7;jj++){ // for other outer cordon locations
					String destinationCountingStation = "OC".concat(String.valueOf(jj));
					double destinationPersonCount =  Math.round( OuterCordonUtils.getExtExtTripShareBetweenCountingStations(countingStationNumber, destinationCountingStation) * personCount );

					Link destinationActLink = null;
					if(countingDirection.equalsIgnoreCase("In")){ // --> trip originates at given counting stationNumber
						destinationActLink = getLinkFromOuterCordonKey(destinationCountingStation, false );
					} else {// --> trip terminates at given counting stationNumber
						throw new RuntimeException("For external to external counts use other counting direction, i.e. the "+countingStationNumber+ "should be assumed as origin and not destination.");
					}

					for(int ii = 0; ii< destinationPersonCount; ii++){ //persons
						String prefix = countingStationKey+"_E2E_";
						Id<Person> personId = Id.createPersonId(prefix+ population.getPersons().size());
						Person p = pf.createPerson(personId);

						double actEndTime = (timebin-1)*3600 + random.nextDouble()*3600;

						Plan plan = pf.createPlan();
						Activity firstAct = pf.createActivityFromLinkId("E2E_Start", originActLink.getId());
						firstAct.setEndTime(actEndTime );
						plan.addActivity(firstAct);
						plan.addLeg(pf.createLeg(mode));

						Activity lastAct = pf.createActivityFromLinkId("E2E_End", destinationActLink.getId());
						plan.addActivity(lastAct);
						p.addPlan(plan);

						population.addPerson(p);
						noOfPersonsAdded++;
					}
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

			if ( PatnaUtils.PATNA_NETWORK_TYPE.equals(PatnaNetworkType.osmNetwork))  {
				switch(countingStationNumber) 
				{
				case "OC1": link = "5672-5876-5732"; break;
				case "OC2": link = "OC2_in"; break;
				case "OC3": link = "1207-999-1335"; break;
				case "OC4": link = "OC4_in"; break;
				case "OC5": link = "9427"; break;
				case "OC6": link = "2060"; break;
				case "OC7": link = "741"; break;
				default : throw new RuntimeException("A connecting link in the desired direction is not found. Aborting ...");
				} 
			} else {
				switch(countingStationNumber)
				{
				case "OC1": link = "1386010000-1385710000-1385110000-1380010000"; break;
				case "OC2": link = "OC2_in"; break;
				case "OC3": link = "476810000-475210000-477510000"; break;
				case "OC4": link = "OC4_in"; break;
				case "OC5": link = "OC5_in"; break;
				case "OC6": link = "919-828-1033-1022-1035-101710000-1053-106110000-106010000-1058-69210000-146010000"; break;
				case "OC7": link = "1973-2158-215210000-2163-2169"; break;
				default : throw new RuntimeException("A connecting link in the desired direction is not found. Aborting ...");
				}
			}
		} else {

			if ( PatnaUtils.PATNA_NETWORK_TYPE.equals(PatnaNetworkType.osmNetwork))  {
				switch(countingStationNumber)
				{
				case "OC1": link = "5733-5877-5673"; break;
				case "OC2": link = "OC2_out"; break;
				case "OC3": link = "1334-998-1206"; break;
				case "OC4": link = "OC4_out"; break;
				case "OC5": link = "9428"; break;
				case "OC6": link = "2059"; break;
				case "OC7": link = "740"; break;
				default : throw new RuntimeException("A connecting link in the desired direction is not found. Aborting ...");
				} 
			}else {
				switch(countingStationNumber)
				{
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
		}
		Id<Link> linkId = Id.createLinkId(link);
		return scenario.getNetwork().getLinks().get(linkId);
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
				double timebin = Double.valueOf(parts[0]); //time bin
				double carCount = Double.valueOf(parts[1]); // car
				double motorbikeCount = Double.valueOf(parts[2]); //2w
				double truckCount = Double.valueOf(parts[3]); //truck
				double bikeCount = Double.valueOf(parts[4]); //bike

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
