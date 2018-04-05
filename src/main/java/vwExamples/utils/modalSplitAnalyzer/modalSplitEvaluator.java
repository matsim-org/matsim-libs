/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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
package vwExamples.utils.modalSplitAnalyzer;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.algorithms.PersonAlgorithm;
import org.matsim.core.population.io.StreamingPopulationReader;
import org.matsim.core.population.io.StreamingPopulationWriter;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Subtour;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.pt.router.TransitActsRemover;
import org.opengis.feature.simple.SimpleFeature;
import org.matsim.core.router.EmptyStageActivityTypes;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.TripRouter;

import com.vividsolutions.jts.geom.Geometry;

import vwExamples.utils.ManipulateAndFilterPopulationBerlin;

/**
 * @author  axer
 *
 */




public class modalSplitEvaluator {
	private Map<String,PersonValidator> groups = new HashMap<>();
	Set<String> zones = new HashSet<>();
	static Map<String, Geometry> zoneMap = new HashMap<>();
	static String zoneList[] = {"0101","0102","0201","0202","0203","0204","0307","0403","0405","0701","0702"};
	String shapeFile = "C:\\Users\\Joschka\\Documents\\shared-svn\\projects\\vw_rufbus\\projekt2\\drt_test_Scenarios\\vw-berlin\\Prognoseraum_EPSG_31468.shp" ;
	String shapeFeature = "SCHLUESSEL";
	StageActivityTypes stageActs;

	
	public static void main(String[] args) {
		modalSplitEvaluator tde = new modalSplitEvaluator();
	
		tde.run("C:\\Users\\Joschka\\Documents\\shared-svn\\projects\\vw_rufbus\\projekt2\\drt_test_Scenarios\\vw-berlin\\be_251.output_plans_selected.xml.gz");
	
	}
	
	public void readShape(String shapeFile, String featureKeyInShapeFile) {
		Collection <SimpleFeature> features = ShapeFileReader.getAllFeatures(shapeFile);
		for (SimpleFeature feature : features) {
			String id =  feature.getAttribute(featureKeyInShapeFile).toString();
			Geometry geometry = (Geometry) feature.getDefaultGeometry();
			zones.add(id);
			zoneMap.put(id, geometry);
		}
	}
	
	public static boolean isWithinZone(Coord coord, Map<String, Geometry> zoneMap){
		//Function assumes Shapes are in the same coordinate system like MATSim simulation
		

		for (String zone : zoneMap.keySet()) {
			Geometry geometry = zoneMap.get(zone);
			if(geometry.intersects(MGC.coord2Point(coord)))
				{
				//System.out.println("Coordinate in "+ zone);
				return true;
				}
			}
		
		
		return false;	
	}
	
	public static boolean isWithinSpecificZone(Coord coord, Map<String, Geometry> zoneMap, String[] zoneList){
		//Assumes the same coordinate system for shapes and plans
		

		for (String zone : zoneMap.keySet()) {
			
			//If the zone does fit to the require zoneList
			if(Arrays.asList(zoneList).contains(zone)) {
				Geometry geometry = zoneMap.get(zone);
				if(geometry.intersects(MGC.coord2Point(coord)))
					{
					//System.out.println("Coordinate in "+ zone);
					return true;
					}
			}
		}
		
		return false;	
	}
	
	public static String getSubtourMode(Subtour subTour) 
	{
		//ToDo: Inefficient way to get the legMode. Reduce loops!
		String subtourMode = null;
		List<Trip> trips = subTour.getTrips();
		
		for (Trip trip : trips)
		{
																
			List<Leg> legList = trip.getLegsOnly();
			
			//Checks, if any leg of this subTour leaves a area (e.g. the service area)
			for (Leg leg : legList)
			{
				//Get mode of this subtour by its first leg
				if (subtourMode==null)
				{
					subtourMode = leg.getMode();
					return subtourMode;
				}

			}
								
		}
		return subtourMode;
		
	}
	
	public static List<Integer> getReplaceableTripIndices(Subtour subTour) {
		
			String subtourMode = null; 	
			List<Trip> trips = subTour.getTrips();
			String [] chainedModes = {TransportMode.car,TransportMode.bike};
			
			subtourMode = getSubtourMode(subTour);
			
			System.out.println(subtourMode);

			List<Integer> replaceableTrips = new ArrayList<Integer>();
			
			//Dealing with a chained mode all trips need to be within the specified area
			if (Arrays.asList(chainedModes).contains(subtourMode)&&(subtourMode != null))
			{
				//System.out.println("Chained mode");
				
				
				for (Trip trip : trips)
				{
																		
					List<Leg> legList = trip.getLegsOnly();
					
					//Checks, if any leg of this subTour leaves a area (e.g. the service area)
					for (Leg leg : legList)
					{
						
						if (isWithinZone(trip.getDestinationActivity().getCoord(), zoneMap) && isWithinZone(trip.getOriginActivity().getCoord(), zoneMap))
						{
							replaceableTrips.add(trips.indexOf(trip));
						}
						//A trip of a chained mode leaves the specified area, drop all element of replaceableTrips
						//and return a cleaned list
						else {
							//System.out.println("Agent leaves area!");
							replaceableTrips.clear();
							return replaceableTrips;
							
						}

					}
										
				}
				
				return 	replaceableTrips;
				
			}
			
			
			//Not dealing with a chained mode, get trip indices that are within specified area
			else if (!Arrays.asList(chainedModes).contains(subtourMode)&&(subtourMode != null))
			{
				//System.out.println("Non Chained mode");
				
				for (Trip trip : trips)
				{
																		
					List<Leg> legList = trip.getLegsOnly();
					
					//Checks, if any leg of this subTour leaves a area (e.g. the service area)
					for (Leg leg : legList)
					{
						//Only add trip indices that are within the specified area
						if (isWithinZone(trip.getDestinationActivity().getCoord(), zoneMap) && isWithinZone(trip.getOriginActivity().getCoord(), zoneMap))
						{
							replaceableTrips.add(trips.indexOf(trip));
						}
						
					}
										
				}
			return 	replaceableTrips;
				
			}
			
			return replaceableTrips;

	}
	
	public static boolean judgeLeg(Activity prevAct,Activity nextAct, Map<String, Geometry> zoneMap, String[] zoneList) {
		boolean prevActInZone = false;
		boolean nextActInZone = false;
			
		
//		if(isWithinSpecificZone(prevAct.getCoord(), zoneMap,zoneList)) prevActInZone= true;
//		if(isWithinSpecificZone(nextAct.getCoord(), zoneMap,zoneList)) nextActInZone= true;
				
		if(isWithinZone(prevAct.getCoord(), zoneMap)) prevActInZone= true;
		if(isWithinZone(nextAct.getCoord(), zoneMap)) nextActInZone= true;
		
		
		if ((prevActInZone == true) && (nextActInZone == true) ) {
//			System.out.println("Leg in Zone: "+plan.getPerson().getId().toString());
			return true;
		}
		else return false;
		}
	
	
	public  void run(String populationFile) {
		//We have three groups of agents
		//groups.put("all", new AnyPerson());
		
		readShape(shapeFile,shapeFeature);
		
//		groups.put("braunschweig", new LivesBraunschweig());
//		groups.put("wolfsburg", new LivesWolfsburg());
		groups.put("berlin", new LivesBerlin());
		
		
		for (Entry<String, PersonValidator> e : groups.entrySet()){
			//Iterate over each group
			Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
			StreamingPopulationReader spr = new StreamingPopulationReader(scenario);
			
			//A hash map that stores the mode as key and the traveled distances
			//ptSlow or any mode that contains pt will be casted to pt
			Map<String,TravelDistanceActivity> distanceActivityPerModeWithinTraffic = new HashMap<>();
			distanceActivityPerModeWithinTraffic.put(TransportMode.car, new TravelDistanceActivity(TransportMode.car));
			distanceActivityPerModeWithinTraffic.put(TransportMode.ride, new TravelDistanceActivity(TransportMode.ride));
			distanceActivityPerModeWithinTraffic.put(TransportMode.bike, new TravelDistanceActivity(TransportMode.bike));
			distanceActivityPerModeWithinTraffic.put(TransportMode.pt, new TravelDistanceActivity(TransportMode.pt));
			distanceActivityPerModeWithinTraffic.put(TransportMode.walk, new TravelDistanceActivity(TransportMode.walk));
			distanceActivityPerModeWithinTraffic.put(TransportMode.drt, new TravelDistanceActivity(TransportMode.drt));
			
			Map<String,TravelDistanceActivity> distanceActivityPerModeAllTraffic = new HashMap<>();
			distanceActivityPerModeAllTraffic.put(TransportMode.car, new TravelDistanceActivity(TransportMode.car));
			distanceActivityPerModeAllTraffic.put(TransportMode.ride, new TravelDistanceActivity(TransportMode.ride));
			distanceActivityPerModeAllTraffic.put(TransportMode.bike, new TravelDistanceActivity(TransportMode.bike));
			distanceActivityPerModeAllTraffic.put(TransportMode.pt, new TravelDistanceActivity(TransportMode.pt));
			distanceActivityPerModeAllTraffic.put(TransportMode.walk, new TravelDistanceActivity(TransportMode.walk));
			distanceActivityPerModeAllTraffic.put(TransportMode.drt, new TravelDistanceActivity(TransportMode.drt));
			
			
			//The stream read gets a new PersonAlgorithm which does stuff for each person
			
			spr.addAlgorithm(new PersonAlgorithm() 
				{
					
					@Override
					public void run(Person person) {
						
						//This line ensures, that a person algorithm is operating on a person!
						if (e.getValue().isValidPerson(person)){
							//Work only with the selected plan of an agent 
							Plan plan = person.getSelectedPlan();
							
							
							Leg prevLeg = null;
							Activity prevAct = null;
							
							//Convert pure transit_walks to walk
							for (PlanElement pe : plan.getPlanElements()) {
								
								if (pe instanceof Activity) 
									{
									if (prevLeg!=null && prevAct!=null) 
										{
										//pt interaction walks are counted as regular walks
										if (!((Activity) pe).getType().equals("pt interaction") &&!prevAct.getType().equals("pt interaction")&&prevLeg.getMode().equals(TransportMode.transit_walk)) 
											{
											prevLeg.setMode("walk");
											}
										}
									prevAct = (Activity) pe;
									}
								else if (pe instanceof Leg) {
									prevLeg = (Leg) pe;
								}  
								
							}

							//Remove transit acts from plan
							//Cleaned plan without transit_walks and no TransitActs
							new TransitActsRemover().run(plan);
							Activity lastAct = null;
							Leg lastLeg = null;
							
							//Iterate over plan elements: activities or legs
							for (PlanElement pe : plan.getPlanElements()){
								
								//If this plan element is an activity
								if (pe instanceof Activity){
									
									//A leg could only exist, if an activity has been left
									if (lastAct != null) 
									{
										//Calculate distance per leg
										int distance_km;
										
										//Distance calculation for pt legs
										if (lastLeg.getMode().equals(TransportMode.pt)){
											distance_km = (int) ((CoordUtils.calcEuclideanDistance(((Activity) pe).getCoord(), lastAct.getCoord())*1.3)/1000.0);
											
										} else 
										{
										//Distance calculcation for all other legs	
											distance_km = (int) (lastLeg.getRoute().getDistance()/1000.0);
										}
										//System.out.println(lastLeg.getMode());
										
										//Within traffic
										
										Activity previousAct =PopulationUtils.getPreviousActivity(plan, lastLeg);
										Activity successiveAct =PopulationUtils.getNextActivity(plan, lastLeg);
										
										System.out.println("Agent: "+person.getId().toString());
										for (Subtour subTour : TripStructureUtils.getSubtours(plan, EmptyStageActivityTypes.INSTANCE))
										{
											
											List<Integer> replaceTripIndices = getReplaceableTripIndices(subTour);
											
//											for (Integer tripIndex : replaceTripIndices)
//											{
//												Trip originalTrip = subTour.getTrips().get(tripIndex);
//												System.out.println(originalTrip);
//												
//											}

											System.out.println(replaceTripIndices.toString());
										}
										
										if (judgeLeg(previousAct,successiveAct,zoneMap,zoneList)) {
											distanceActivityPerModeWithinTraffic.get(lastLeg.getMode()).addLeg(((Activity) pe).getType(), distance_km);
										}
										
										
										//All traffic
										distanceActivityPerModeAllTraffic.get(lastLeg.getMode()).addLeg(((Activity) pe).getType(), distance_km);										
										
										lastAct = (Activity) pe;
	
									} else 
										{
										lastAct = (Activity) pe;
										}
								
								//If this plan element is a leg
								} else if (pe instanceof Leg){
									lastLeg = (Leg) pe;
									if (lastLeg.getMode().equals("bicycle"))
									{
										lastLeg.setMode(TransportMode.bike);
									}
									
									else if (lastLeg.getMode().equals("ptSlow"))
									{
										lastLeg.setMode(TransportMode.pt);
									}
									
											
								}
							}
						}
					}
	
					
				});
			
			spr.readFile(populationFile);
			//Iterate over distance distr. per mode a write a table
			
			
			for (TravelDistanceActivity tda : distanceActivityPerModeWithinTraffic.values()){
				tda.writeTable(populationFile.replace(".xml.gz", "_distanceStatsWithinTraffic_"+tda.mode+"_"+e.getKey()+".csv"));
				//Within Traffic: Considers only traffic of residents within this zone or zones where origin and destination is within zone or zones
				System.out.println("Within Traffic - Mode: "+tda.mode +" "+tda.getNumerOfTrips() ); 
			}
			
			//Initialize absolute numbers of trips
			ModalShare withinTrafficModalShare = new ModalShare(distanceActivityPerModeWithinTraffic);
			
			System.out.println(withinTrafficModalShare.modeTripsMapRelative.toString());
			
			for (TravelDistanceActivity tda : distanceActivityPerModeAllTraffic.values()){
				tda.writeTable(populationFile.replace(".xml.gz", "_distanceStatsAllTraffic_"+tda.mode+"_"+e.getKey()+".csv"));
				//All traffic: Considers all traffic of residents within this zone or zones
				System.out.println("All Traffic - Mode: "+tda.mode +" "+tda.getNumerOfTrips() ); 
			}
			
			//Initialize absolute numbers of trips
			ModalShare allTrafficModalShare = new ModalShare(distanceActivityPerModeAllTraffic);
			System.out.println(allTrafficModalShare.modeTripsMapRelative.toString());
			
			StreamingPopulationReader spr2 = new StreamingPopulationReader(scenario);
			StreamingPopulationWriter spw = new StreamingPopulationWriter();
			spw.startStreaming(" name des output files");
			spr.addAlgorithm(new PersonAlgorithm() {
				
				@Override
				public void run(Person person) {
					// TODO Auto-generated method stub
					// etwasmit der Person machen (mode ersetzen)
					
				}
			});
			spr.addAlgorithm(spw);
			spr.readFile(populationFile);
			spw.closeStreaming();
			
			
			
		}
		
		
	}
	
	class ModalShare{
		Integer totalTrips = 0;
		Map<String,Integer> modeTripsMap = new HashMap<>();
		Map<String,Double> modeTripsMapRelative = new HashMap<>();
		
	
		public ModalShare(Map<String,TravelDistanceActivity> distanceActivityPerMode) {
			for (TravelDistanceActivity tda : distanceActivityPerMode.values())
			{
				modeTripsMap.put(tda.mode, tda.getNumerOfTrips());
				totalTrips+=tda.getNumerOfTrips();
			}
			
			for (Entry<String, Integer> entry : modeTripsMap.entrySet())
			{
				int trips = entry.getValue();
				double share = (double) trips/this.totalTrips;
				modeTripsMapRelative.put(entry.getKey(), share);
			}
		}
		

	}
	

	class TravelDistanceActivity{
		String mode;
		Map<String,int[]> activityDistanceBins = new TreeMap<>();
		
		public TravelDistanceActivity(String mode) {
			this.mode = mode;
		}
		
		public void addLeg(String activity, int distance){
			//Extract the activity that related to this leg
			activity = activity.split("_")[0];
			if (!activityDistanceBins.containsKey(activity)){
				activityDistanceBins.put(activity, new int[51]);
			}
			if (distance>50) distance = 50;
			activityDistanceBins.get(activity)[distance]++;
		}
		
		public Integer getNumerOfTrips()
		{
			Integer numberOfTrips = 0;
			for (int i = 0; i<51;i++){
				for (int[] v : activityDistanceBins.values() ){
					//Sum up all trips
					numberOfTrips+=v[i];
				}
				
			}
			return numberOfTrips;
		}
		
		public void writeTable(String filename){
			BufferedWriter bw = IOUtils.getBufferedWriter(filename);
			try {
				bw.write("Mode;"+mode);
				bw.newLine();
				bw.write("distance");
				for (String s : activityDistanceBins.keySet()){
					if (s.equals("other")) bw.write(";"+"4 - private Erledigung");
					else if (s.equals("home")) bw.write(";"+"1 - Wohnung");
					else if (s.equals("work")) bw.write(";"+"2 - Arbeit");
					else if (s.equals("shopping")) bw.write(";"+"3 - Einkauf");
					else if (s.equals("education")) bw.write(";"+"5 - Ausbildung");
					else if (s.equals("leisure")) bw.write(";"+"6 - Freizeit");

					
				}
				for (int i = 0; i<51;i++){
					bw.newLine();
					bw.write(Integer.toString(i));
					for (int[] v : activityDistanceBins.values() ){
						bw.write(";"+v[i]);
					}
					
				}
				bw.flush();
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}	
		}
	
	class LivesBraunschweig implements PersonValidator{

		/* (non-Javadoc)
		 * @see analysis.traveldistances.PersonValidator#isValidPerson(org.matsim.api.core.v01.population.Person)
		 */
		@Override
		public boolean isValidPerson(Person person) {
			Id<Person> id = person.getId();
//			//Braunschweig
			if ((id.toString().startsWith("1"))&&(id.toString().split("_")[0].length()==3)) 
				return true;
			else return false;
		}
		
	}
	class LivesWolfsburg implements PersonValidator{

		/* (non-Javadoc)
		 * @see analysis.traveldistances.PersonValidator#isValidPerson(org.matsim.api.core.v01.population.Person)
		 */
		@Override
		public boolean isValidPerson(Person person) {
			Id<Person> id = person.getId();
			if ((id.toString().startsWith("3"))&&(id.toString().split("_")[0].length()==3)) 
				return true;
			else return false;
			
		}
		
		
	}
	
	class LivesBerlin implements PersonValidator{

		/* (non-Javadoc)
		 * @see analysis.traveldistances.PersonValidator#isValidPerson(org.matsim.api.core.v01.population.Person)
		 */
		@Override
		public boolean isValidPerson(Person person) {

			
			
			for (PlanElement pe : person.getSelectedPlan().getPlanElements()) {
				if (pe instanceof Activity) {
					if (((Activity)pe).getType().contains("home")) {
						
						Activity activity = ((Activity)pe);
						Coord coord = activity.getCoord();
						//if (isWithinSpecificZone(coord,zoneMap,zoneList)){
						if (isWithinZone(coord,zoneMap)){
							//System.out.println("Relevant agent found: "+person.getId());
							return true;
							
							
						}
						
						}
					}
				}
			return false;
			
		}
		
			
	
		
	}
	
	class AnyPerson implements PersonValidator{

		/* (non-Javadoc)
		 * @see analysis.traveldistances.PersonValidator#isValidPerson(org.matsim.api.core.v01.population.Person)
		 */
		@Override
		public boolean isValidPerson(Person person) {
			return true;
		}
		
		
	}
	
}
