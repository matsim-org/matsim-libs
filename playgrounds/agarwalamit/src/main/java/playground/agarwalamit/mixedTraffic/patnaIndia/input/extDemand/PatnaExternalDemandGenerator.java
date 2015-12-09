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
		new PopulationWriter(scenario.getPopulation()).write("../../../../repos/runs-svn/patnaIndia/run108/input/outerCordonDemand.xml.gz");
		System.out.println("Number of persons in the population are "+scenario.getPopulation().getPersons().size());
	}

	private void createDemandForAllStations(){
		//OC1
		createExternalToExternalPlans(inputFilesDir+"/oc1_patna2Fatua.txt", "OC1", "Out" );
		createExternalToExternalPlans(inputFilesDir+"/oc1_fatua2Patna.txt", "OC1", "In" );

		//for ext-int-ext trips only one of the input files is taken, because (at least) the total count multiply by directional split will give same from both files. 
		createExternalToInternalPlans(inputFilesDir+"/oc1_fatua2Patna.txt","OC1");

		//OC2
		createExternalToExternalPlans(inputFilesDir+"/oc2_patna2Fatua.txt", "OC2", "Out");
		createExternalToExternalPlans(inputFilesDir+"/oc2_fatua2Patna.txt", "OC2", "In");

		createExternalToInternalPlans(inputFilesDir+"/oc2_fatua2Patna.txt", "OC2");

		//OC3
		createExternalToExternalPlans(inputFilesDir+"/oc3_patna2Punpun.txt", "OC3", "Out");
		createExternalToExternalPlans(inputFilesDir+"/oc3_punpun2Patna.txt", "OC3", "In");

		createExternalToInternalPlans(inputFilesDir+"/oc3_punpun2Patna.txt", "OC3");

		//OC4
		createExternalToExternalPlans(inputFilesDir+"/oc4_patna2Muz.txt", "OC4", "Out");
		createExternalToExternalPlans(inputFilesDir+"/oc4_muz2Patna.txt", "OC4", "In");

		createExternalToInternalPlans(inputFilesDir+"/oc4_muz2Patna.txt", "OC4");

		//OC5
		createExternalToExternalPlans(inputFilesDir+"/oc5_patna2Danapur.txt", "OC5", "Out");
		createExternalToExternalPlans(inputFilesDir+"/oc5_danapur2Patna.txt", "OC5", "In");

		createExternalToInternalPlans(inputFilesDir+"/oc5_danapur2Patna.txt", "OC5");

		//OC6
		createExternalToExternalPlans(inputFilesDir+"/oc6_noera2Fatua.txt", "OC6", "Out");
		createExternalToExternalPlans(inputFilesDir+"/oc6_fatua2Noera.txt", "OC6", "In");

		createExternalToInternalPlans(inputFilesDir+"/oc6_fatua2Noera.txt", "OC6");

		//OC7
		createExternalToExternalPlans(inputFilesDir+"/oc7_patna2Danapur.txt", "OC7", "Out");
		createExternalToExternalPlans(inputFilesDir+"/oc7_danapur2Patna.txt", "OC7", "In");

		createExternalToInternalPlans(inputFilesDir+"/oc7_danapur2Patna.txt", "OC7");
	}

	private void createExternalToInternalPlans(final String file, final String countingStationNumber){
		Population population = scenario.getPopulation();
		PopulationFactory pf = population.getFactory();
		Map<Double, Map<String,Double>> timebin2mode2count = readFileAndReturnMap(file);
		
		Map<String, List<SimpleFeature>> area2ZonesLists = getInternalZoneFeaturesForExtInternalTrips();
		
		String countingStationKey = OuterCordonUtils.getCountingStationKey(countingStationNumber, "In");
		Coord firstLastActCoord = getLinkFromOuterCordonKey(countingStationKey, true).getCoord();

		for(double timebin : timebin2mode2count.keySet()){
			for(String mode : timebin2mode2count.get(timebin).keySet()){
				double directionSplitFactor = OuterCordonUtils.getDirectionalFactorFromOuterCordonKey(countingStationKey, "E2I");
				double count = Math.round(timebin2mode2count.get(timebin).get(mode)* directionSplitFactor / PatnaUtils.COUNT_SCALE_FACTOR);

				for(int ii=0; ii< count; ii++){ // create person
					String prefix = countingStationKey+"_E2I_";
					Id<Person> personId = Id.createPersonId(prefix+ population.getPersons().size());
					Person p = pf.createPerson(personId);
					population.addPerson(p);
					for( String area : area2ZonesLists.keySet() ){ // create a plan for each zone (ext-int-ext)
						Plan plan = pf.createPlan();
						Activity firstAct = pf.createActivityFromCoord( countingStationKey, firstLastActCoord);
						firstAct.setEndTime( (timebin-1)*3600 + random.nextDouble()*3600);
						plan.addActivity(firstAct);
						plan.addLeg(pf.createLeg(mode));
						
						Point randomPointInZone = GeometryUtils.getRandomPointsInsideFeatures(area2ZonesLists.get(area));
						Coord middleActCoord = PatnaUtils.COORDINATE_TRANSFORMATION.transform( new Coord(randomPointInZone.getX(),randomPointInZone.getY()) );
						
						Activity middleAct = pf.createActivityFromCoord("Ext-Int-middleAct", middleActCoord);
						//ZZ_TODO : here the act duration is assigned randomly between 7 to 8 hours. This means, the agent will be counted in reverse direction of the same counting station.
						double middleActEndTime = firstAct.getEndTime() + 7*3600 + random.nextDouble() * 3600;
						if(middleActEndTime > 24*3600 ) middleActEndTime = middleActEndTime - 24*3600;
						middleAct.setEndTime( middleActEndTime );
						plan.addActivity(middleAct);
						plan.addLeg(pf.createLeg(mode));
						Activity lastAct = pf.createActivityFromCoord( countingStationKey, firstLastActCoord);
						plan.addActivity(lastAct);
						p.addPlan(plan);
					}
				}
			}
		}
	}

	/**
	 * @param countingDirection "In" for outside to Patna, "Out" for Patna to outside.
	 */
	private void createExternalToExternalPlans(final String file, final String countingStationNumber, final String countingDirection){
 		Population population = scenario.getPopulation();
		PopulationFactory pf = population.getFactory();
		Map<Double, Map<String,Double>> timebin2mode2count = readFileAndReturnMap(file);

		String countingStationKey = OuterCordonUtils.getCountingStationKey(countingStationNumber, countingDirection); 
		Id<Link> firstActLink = null; 
		String firstActType = null;
		Id<Link> lastActLink = null;
		String lastActType = null;
		
		if(countingDirection.equalsIgnoreCase("In")){// --> trip originates at counting stationNumber
			firstActLink = getLinkFromOuterCordonKey(countingStationKey, true).getId();
			firstActType = countingStationKey+"_Start";
		} else {// --> trip terminates at counting stationNumber
			lastActLink = getLinkFromOuterCordonKey(countingStationKey, false).getId();
			lastActType = countingStationKey+"_End";
		}

		for(double timebin : timebin2mode2count.keySet()){
			for(String mode : timebin2mode2count.get(timebin).keySet()){
				double directionSplitFactor = OuterCordonUtils.getDirectionalFactorFromOuterCordonKey(countingStationKey, "E2E");
				double count = Math.round(timebin2mode2count.get(timebin).get(mode)* directionSplitFactor / PatnaUtils.COUNT_SCALE_FACTOR);

				for(int ii=0; ii< count; ii++){ // create person
					String prefix = countingStationKey+"_E2E_";
					Id<Person> personId = Id.createPersonId(prefix+ population.getPersons().size());
					Person p = pf.createPerson(personId);

					for (int jj =1;jj<=7;jj++){ // 6 plans for each outer cordon location
						
						if(countingStationNumber.equalsIgnoreCase("OC"+jj)) continue; // excluding same origin- destination
						
						double actEndTime ;
						if(countingDirection.equalsIgnoreCase("In")){// --> trip originates at given counting stationNumber
							String countingStationKeyForOtherActLink = OuterCordonUtils.getCountingStationKey("OC"+jj, "In");
							lastActLink = getLinkFromOuterCordonKey(countingStationKeyForOtherActLink, false ).getId();
							actEndTime = (timebin-1)*3600+random.nextDouble()*3600;
							lastActType = countingStationKeyForOtherActLink+"_End";
						} else {// --> trip terminates at given counting stationNumber
							String countingStationKeyForOtherActLink = OuterCordonUtils.getCountingStationKey("OC"+jj, "In"); 
							firstActLink = 	getLinkFromOuterCordonKey( countingStationKeyForOtherActLink, true ).getId();
							double travelTime = 30*60; // ZZ_TODO : it is assumed that agent will take 30 min to reach the destination counting station in desired time bin.
							actEndTime = (timebin-1)*3600 - travelTime + random.nextDouble()*3600 - 30*60 ; 
							firstActType = countingStationKeyForOtherActLink+"_Start";
						}
						
						if(actEndTime < 0)	actEndTime = random.nextDouble()*900; //for time bin =1, actEndTime (in the else statment) can be negative, thus assining sometime between initial 15 mins.
						
						Plan plan = pf.createPlan();
						Activity firstAct = pf.createActivityFromLinkId(firstActType, firstActLink);
						firstAct.setEndTime(actEndTime );
						plan.addActivity(firstAct);
						plan.addLeg(pf.createLeg(mode));

						Activity lastAct = pf.createActivityFromLinkId(lastActType, lastActLink);
						plan.addActivity(lastAct);
						p.addPlan(plan);
					}
					population.addPerson(p);
				}
			}
		}
	}

	/**
	 * @return the adjacent link (previous link for origin link and next link for destination link) corresponding to the counting station.
	 */
	private Link getLinkFromOuterCordonKey(final String countingStationKey, final boolean isOrigin){
		Id<Link> linkId = OuterCordonUtils.getCountStationLinkId(countingStationKey);
		Link link = scenario.getNetwork().getLinks().get(linkId);
		if(isOrigin) {
			return link.getFromNode().getInLinks().values().iterator().next();
		} else {
			return link.getToNode().getOutLinks().values().iterator().next();
		}
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