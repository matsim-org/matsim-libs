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

package playground.juliakern.toi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

public class ShapeConverterPop {

//	static String shapeFile = "input/oslo/Dataset/eksport_stort_datasett_test_2.shp";
	//static String shapeFile = "input/oslo/Start_og_stopp_i_TRD_fra_RVU2/testplott5_end.shp";
	static String shapeFile = "input/oslo/Data201404/datasett_reiser_3_shape.shp";
	static String networkFile = "input/oslo/trondheim_network_with_lanes.xml";
	static String plansFile = "input/oslo/plans_from_eksport_stort_datasett.xml";
	static Collection<SimpleFeature> features;
	static Logger logger = Logger.getLogger(ShapeConverterPop.class);
	private static int countKnownActTypes =0;
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		Config config = new Config();
		config.addCoreModules();
		Controler controler = new Controler(config);
		controler.getConfig().controler().setOverwriteFileSetting(
				true ?
						OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
						OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );

		ShapeFileReader sfr = new ShapeFileReader();
		features = sfr.readFileAndInitialize(shapeFile);
		
		ActivityParams home = new ActivityParams("home");
		config.planCalcScore().addActivityParams(home);
		home.setTypicalDuration(16*3600);
		ActivityParams work = new ActivityParams("work");
		config.planCalcScore().addActivityParams(work);
		work.setTypicalDuration(8*3600);
		ActivityParams other = new ActivityParams("other");
		other.setTypicalDuration(1*3600);
		config.planCalcScore().addActivityParams(other);
		
		Scenario scenario = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(scenario).readFile(networkFile);
		Population pop = fillScenario(scenario);
		new PopulationWriter(pop, scenario.getNetwork()).write(plansFile);

		
		//Node node1 = network.createAndAddNode(scenario.createId("1"), scenario.createCoord(0.0, 10000.0));
		logger.info("Population size "+ pop.getPersons().size());
		logger.info("number of features" + features.size());
		logger.info("features with know acttype " + countKnownActTypes);
		

		
		}
//		NetworkWriter nw = new NetworkWriter(network);
//		nw.write("input/oslo/trondheim_network.xml");
	private static Population fillScenario(Scenario scenario) {
		
		Population population = scenario.getPopulation();
		
		PopulationFactory populationFactory = population.getFactory();
		
		Map<Id<Person>, List<Trip>> personid2trips= new HashMap<>();
		
		for(SimpleFeature sf: features){
			
			
			
//			Double time = getTimeInSeconds((String) sf.getAttribute("klokkeslet"));
			
			Double time = (Integer)sf.getAttribute("HH24")*3600.
					+ (Integer)sf.getAttribute("MIN")*60.;
			/* 2014-02-11 12:25:26,120  INFO ShapeConverterPop:66 SimpleFeatureImpl:testplott5_end=[SimpleFeatureImpl.Attribute: 
			the_geom<Point id=testplott5_end.1>=POINT (270060 7036918), 
			SimpleFeatureImpl.Attribute: ID_2<ID_2 id=testplott5_end.1>=1.0, 
			SimpleFeatureImpl.Attribute: rand<rand id=testplott5_end.1>=1.0, 
			SimpleFeatureImpl.Attribute: B0<B0 id=testplott5_end.1>=60927.0, 
					SimpleFeatureImpl.Attribute: reisenr<reisenr id=testplott5_end.1>=6.0, 
					SimpleFeatureImpl.Attribute: start_x<start_x id=testplott5_end.1>=263186.0, 
					SimpleFeatureImpl.Attribute: start_y<start_y id=testplott5_end.1>=7044679.0, 
					SimpleFeatureImpl.Attribute: ende_x<ende_x id=testplott5_end.1>=270060.0, 
					SimpleFeatureImpl.Attribute: ende_y<ende_y id=testplott5_end.1>=7036918.0, 
					SimpleFeatureImpl.Attribute: klokkeslet<klokkeslet id=testplott5_end.1>=1899-12-30 12:29:57.000, 
					SimpleFeatureImpl.Attribute: ukedag<ukedag id=testplott5_end.1>=NULL, 
					SimpleFeatureImpl.Attribute: Formal_enk<Formal_enk id=testplott5_end.1>=Other, 
					SimpleFeatureImpl.Attribute: Form_enk_n<Form_enk_n id=testplott5_end.1>=Annet, 
					SimpleFeatureImpl.Attribute: trmh_enkel<trmh_enkel id=testplott5_end.1>=Car, 
					SimpleFeatureImpl.Attribute: trmh_enk_1<trmh_enk_1 id=testplott5_end.1>=Bil, 
					SimpleFeatureImpl.Attribute: ID_NUM2<ID_NUM2 id=testplott5_end.1>=6092701]
			*/
			if (time>=0.0) {
				
																																																																																																																																																																																																									String ids = Long.toString((Long)sf.getAttribute("ID2"));
				Id<Person> personId = Id.create(ids, Person.class);
				
				Double startx = new Double((Integer) sf.getAttribute("START_X"));
				Double starty = new Double((Integer) sf.getAttribute("START_Y"));
				Double endx = new Double((Integer) sf.getAttribute("ENDE_X"));
				Double endy = new Double((Integer) sf.getAttribute("ENDE_Y"));

				Coord startCoordinates = new Coord(startx, starty);
				Coord endCoordinates = new Coord(endx, endy);
				
				String actType = (String) sf.getAttribute("SKOM1"); // activity type
				actType = actType.toLowerCase();
				
				if(!(actType.equals("home")||actType.equals("work")||actType.equals("commute")||actType.equals("other"))){
					logger.warn("new act type " + actType);
				}else{
					countKnownActTypes ++;
				}
				
				String legmode = (String) sf.getAttribute("TRMH1"); //TODO
				legmode = legmode.toLowerCase();
				
				int tripnumber = (Integer) sf.getAttribute("REISENR");
				
				// map: person id 2 list of trips
				Trip trip = new Trip(startCoordinates, endCoordinates, time, legmode, actType, tripnumber);
				
				if(personid2trips.containsKey(personId)){
					personid2trips.get(personId).add(trip);
					
				}else{
					
					personid2trips.put(personId, new ArrayList<Trip>());
					personid2trips.get(personId).add(trip);
				}
				
			}else{
			//	logger.info(sf.getAttributes().toString());
			}
			
		
			
//			// 2014-02-11 12:25:26,120  INFO ShapeConverterPop:66 SimpleFeatureImpl:testplott5_end=[SimpleFeatureImpl.Attribute: the_geom<Point id=testplott5_end.1>=POINT (270060 7036918), SimpleFeatureImpl.Attribute: ID_2<ID_2 id=testplott5_end.1>=1.0, SimpleFeatureImpl.Attribute: rand<rand id=testplott5_end.1>=1.0, SimpleFeatureImpl.Attribute: B0<B0 id=testplott5_end.1>=60927.0, SimpleFeatureImpl.Attribute: reisenr<reisenr id=testplott5_end.1>=6.0, SimpleFeatureImpl.Attribute: start_x<start_x id=testplott5_end.1>=263186.0, SimpleFeatureImpl.Attribute: start_y<start_y id=testplott5_end.1>=7044679.0, SimpleFeatureImpl.Attribute: ende_x<ende_x id=testplott5_end.1>=270060.0, SimpleFeatureImpl.Attribute: ende_y<ende_y id=testplott5_end.1>=7036918.0, SimpleFeatureImpl.Attribute: klokkeslet<klokkeslet id=testplott5_end.1>=1899-12-30 12:29:57.000, SimpleFeatureImpl.Attribute: ukedag<ukedag id=testplott5_end.1>=NULL, SimpleFeatureImpl.Attribute: Formal_enk<Formal_enk id=testplott5_end.1>=Other, SimpleFeatureImpl.Attribute: Form_enk_n<Form_enk_n id=testplott5_end.1>=Annet, SimpleFeatureImpl.Attribute: trmh_enkel<trmh_enkel id=testplott5_end.1>=Car, SimpleFeatureImpl.Attribute: trmh_enk_1<trmh_enk_1 id=testplott5_end.1>=Bil, SimpleFeatureImpl.Attribute: ID_NUM2<ID_NUM2 id=testplott5_end.1>=6092701]
//			logger.info(sf.toString());
//			logger.info(sf.getDescriptor().toString());
//			logger.info(sf.getProperties().toString());
//			//2014-02-11 12:22:19,526  INFO ShapeConverterPop:67 [SimpleFeatureImpl.Attribute: the_geom<Point id=testplott5_end.1>=POINT (270060 7036918), SimpleFeatureImpl.Attribute: ID_2<ID_2 id=testplott5_end.1>=1.0, SimpleFeatureImpl.Attribute: rand<rand id=testplott5_end.1>=1.0, SimpleFeatureImpl.Attribute: B0<B0 id=testplott5_end.1>=60927.0, SimpleFeatureImpl.Attribute: reisenr<reisenr id=testplott5_end.1>=6.0, SimpleFeatureImpl.Attribute: start_x<start_x id=testplott5_end.1>=263186.0, SimpleFeatureImpl.Attribute: start_y<start_y id=testplott5_end.1>=7044679.0, SimpleFeatureImpl.Attribute: ende_x<ende_x id=testplott5_end.1>=270060.0, SimpleFeatureImpl.Attribute: ende_y<ende_y id=testplott5_end.1>=7036918.0, SimpleFeatureImpl.Attribute: klokkeslet<klokkeslet id=testplott5_end.1>=1899-12-30 12:29:57.000, SimpleFeatureImpl.Attribute: ukedag<ukedag id=testplott5_end.1>=NULL, SimpleFeatureImpl.Attribute: Formal_enk<Formal_enk id=testplott5_end.1>=Other, SimpleFeatureImpl.Attribute: Form_enk_n<Form_enk_n id=testplott5_end.1>=Annet, SimpleFeatureImpl.Attribute: trmh_enkel<trmh_enkel id=testplott5_end.1>=Car, SimpleFeatureImpl.Attribute: trmh_enk_1<trmh_enk_1 id=testplott5_end.1>=Bil, SimpleFeatureImpl.Attribute: ID_NUM2<ID_NUM2 id=testplott5_end.1>=6092701]
//			for(Object a: sf.getAttributes()){
//				logger.info(a.toString());
//			}
//			break;
			
			
		}
		
		for(Id<Person> id: personid2trips.keySet()){
			
			Trip sortedTrips[] = getSortedTrips(personid2trips.get(id));
			
			if(sortedTrips.length>1){ // only persons with more than one trip
			Person person = populationFactory.createPerson(id);
			population.addPerson(person);
			Plan plan = populationFactory.createPlan();
			person.addPlan(plan);
			
		
			
			String firstActType = sortedTrips[sortedTrips.length-1].getActivityType();
			Activity firstAct = populationFactory.createActivityFromCoord(firstActType, sortedTrips[0].getStartCoord());
			firstAct.setEndTime(sortedTrips[0].getTime());
			plan.addActivity(firstAct);
			
			
			for(int i = 0; i<sortedTrips.length; i++){
				// add leg
				plan.addLeg(populationFactory.createLeg(sortedTrips[i].getLeg()));
				// add end activity
				Activity nextAct = populationFactory.createActivityFromCoord(sortedTrips[i].getActivityType(), sortedTrips[i].getEndCoord());
				if(i<sortedTrips.length-1){
					nextAct.setEndTime(sortedTrips[i+1].getTime());
				}
				plan.addActivity(nextAct);
			}
			
			
			
		}
		
		}
		
		return population;
	}
	
	
	private static Trip[] getSortedTrips(List<Trip> list) {
		
		int maxTripnr =0;
		for(Trip trip: list){
			if(trip.getNumber()>maxTripnr)maxTripnr=trip.getNumber();
		}
		
		Trip[] sortedTripsBig = new Trip[maxTripnr];
		
		for(Trip trip: list){
			int tripnumber = trip.getNumber();
			sortedTripsBig[tripnumber-1]= trip;
		}
		
		int nextToTry =0;
		Trip[] sortedTrips = new Trip[list.size()];
		for(int i=0; i< sortedTrips.length; i++){
			while(sortedTripsBig[nextToTry]==null){
				nextToTry++;
			}
			sortedTrips[i]=sortedTripsBig[nextToTry];
			nextToTry++;
		}
		return sortedTrips;
	}
	private static Double getTimeInSeconds(String timeString) {
		logger.info(timeString);
		try{
		String[] split = timeString.split(" ");
		String[] split2 = split[1].split(":");
		Double time = 60 *60 * Double.parseDouble(split2[0]) 
				+ 60 * Double.parseDouble(split2[1]) 
				+ Double.parseDouble(split2[2]);
		return time;
		}catch(ArrayIndexOutOfBoundsException e){
		//	logger.warn("couldnt parse time from " + timeString);
		}
		return -1.;
	}
		
		
	
//	}
}
