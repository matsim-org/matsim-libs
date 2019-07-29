/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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
package ft.cemdap4H.planspreprocessing;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.util.random.WeightedRandomSelection;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.algorithms.PersonAlgorithm;
import org.matsim.core.population.io.StreamingPopulationReader;
import org.matsim.core.population.io.StreamingPopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import playground.vsp.demandde.cemdap.output.Cemdap2MatsimUtils;

/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class ReassignZonesByAttractiveness {
	Map<String, Geometry> zoneMap = new HashMap<>();
	Map<String, WVIZonalAttractiveness> attractiveness;
	String[] refineActivities = {"leisure", "education", "shopping","other"};   
	List<String> refineActivitiesList = Arrays.asList(refineActivities);
	
	
	
	public static void main(String[] args) {
		new ReassignZonesByAttractiveness().run("D:\\Matsim\\Axer\\Hannover\\ZIM\\input\\add_data\\shp\\Statistische_Bezirke_Hannover_Region.shp", "D:\\Matsim\\Axer\\Hannover\\ZIM\\input\\add_data\\Weights_All.txt", "D:\\Matsim\\Axer\\Hannover\\Base\\vw243_cadON_ptSpeedAdj.0.1\\vw243_cadON_ptSpeedAdj.0.1.output_plans.xml.gz", "D:\\Matsim\\Axer\\Hannover\\Base\\vw243_cadON_ptSpeedAdj.0.1\\vw243_cadON_ptSpeedAdj.0.1.output_plans_shiftStud.xml.gz");
	}
	
	
	void run(String shapeFile, String attFile, String inputPlansFile, String outputPlansFile){
		readShape(shapeFile, "NO");
		attractiveness = WVIZonalAttractiveness.readZonalAttractiveness(attFile, zoneMap);
		StreamingPopulationReader spr = new StreamingPopulationReader(ScenarioUtils.createScenario(ConfigUtils.createConfig()));
		StreamingPopulationWriter spw = new StreamingPopulationWriter();
		spw.startStreaming(outputPlansFile);
		Set<Id<Person>> personset = new HashSet<Id<Person>>();
		spr.addAlgorithm(new PersonAlgorithm() {
			
			
			@Override
			public void run(Person person) {
				
				PersonUtils.removeUnselectedPlans(person);
				
				person.getPlans().stream()
				.flatMap(pl -> pl.getPlanElements().stream()).filter(Leg.class::isInstance)
				.forEach(pe -> ((Leg) pe).setRoute(null));

				
				Coord homeCoord = getHomeCoord(person);
				
				if (isStudent(person))
				{
					for (Plan plan : person.getPlans()){
						int counter = 0;
						for (PlanElement pe : plan.getPlanElements()){
							
							if (pe instanceof Activity){
								Activity act = (Activity) pe;
								String activityShort = act.getType().split("_")[0];
								Coord defaultCoord =act.getCoord();

								if (refineActivitiesList.contains(activityShort)){
									String zone = findZone(act.getCoord(),activityShort);
									if (zone!=null){
										Coord preceedingCoord = findPreviousActivity(plan,counter);
										Coord proceedingCoord = findNextActivity(plan, counter);
//										Map<String, WVIZonalAttractiveness> attractiveness = findAttractiveness(zone);
										Coord newActivityCoord = findNewActivityCoord(preceedingCoord,proceedingCoord,attractiveness,activityShort);
										act.setCoord(newActivityCoord);
										act.setLinkId(null);
										Coord modifiedCoord =newActivityCoord;
										
										
										if (defaultCoord.getX() == newActivityCoord.getX() )
										{
											System.out.println(activityShort+" Coordinate not modified!" +modifiedCoord + " --> " +newActivityCoord );
										}
										else {
											//System.out.println(activityShort+" Coordinate correct modified!" +modifiedCoord + " --> " +newActivityCoord );
										}
									}
									else {
										//Number of persons where location refinement is erroneous
										
										if (act.getCoord().getX()==homeCoord.getX())
										{
											System.out.println(act.getType());
											personset.add(person.getId());

										break;
										}
									}
								}
							}
							//Counter of actual plan element
							counter++;
						}
						}
					//System.out.println(person.getId());
				}
				
				
				

			}
			
			private boolean isStudent(Person person)
			{
				
				
				for (PlanElement pe : person.getSelectedPlan().getPlanElements()){
					
					if (pe instanceof Activity){
						Activity act = (Activity) pe;
						if (act.getType().contains("education"))
						{
							return true;
						}
					}
					
					
				}
				return false;
				
			}
			
			private Coord getHomeCoord(Person person)
			{
				
				
				for (PlanElement pe : person.getSelectedPlan().getPlanElements()){
					
					if (pe instanceof Activity){
						Activity act = (Activity) pe;
						if (act.getType().contains("home"))
						{
							return act.getCoord();
						}
					}
					
					
				}
				return null;
				
			}

			private Coord findNewActivityCoord(Coord preceedingCoord, Coord proceedingCoord,
					Map<String, WVIZonalAttractiveness> attractiveness, String activityType) {
				WeightedRandomSelection<String> wrs = new WeightedRandomSelection<>();
				for (WVIZonalAttractiveness a : attractiveness.values()){
					double v;
					
//					System.out.println("Refine: "+activityType);
					switch (activityType){
						case "work":
							v = a.getAttractivenessWork();
							break;
						case "other":
							v = a.getAttractivenessOther();
							break;
						case "shopping":
							v = a.getAttractivenessShop();
							break;
						case "leisure":
							v = a.getAttractivenessLeisure();
							break;
						case "education":
							v = a.getAttractivenessEducation();
							break;
						default:
							throw new RuntimeException("activitytype unknown: " + activityType);
					}
				double distance = CoordUtils.calcEuclideanDistance(preceedingCoord, a.getZoneCentroid())+CoordUtils.calcEuclideanDistance(proceedingCoord, a.getZoneCentroid());
				
//				//If distance == 0, because all acts are on the same place initialized assume a constant distance of 7500 m
//				if (distance==0.0)
//				{
//					System.out.println("No distance given, Coords are equal!. Use constant distance for grav. model");
//					distance = 7500;
//					
//				}
//				
				double avalue = v/(distance*distance);
//				System.out.println(avalue);
				
				
				wrs.add(a.getZoneId(), avalue);
				}
				String selectedZone = wrs.select();
				
				
				Geometry geometry = zoneMap.get(selectedZone);
				Envelope envelope = geometry.getEnvelopeInternal();
				while (true) {
					Point point = Cemdap2MatsimUtils.getRandomCoordinate(envelope);
					if (point.within(geometry)) {
						return new Coord(point.getX(), point.getY());
					}
				}
				
			}

//			private Map<String, WVIZonalAttractiveness> findAttractiveness(String zone) {
//				
//				if (zone.startsWith("3")) return wobAttractiveness;
//				else if ( zone.startsWith("1")) return bsAttractiveness;
//				else throw new RuntimeException();
//				
//			}

//			private Coord findProceedingAnchorActivity(Plan plan, int counter) {
//				int actc = counter-2;
//				Coord c = null;
//				if (actc<0){
//					actc = 0;
//					}
//					Activity act = (Activity) plan.getPlanElements().get(counter-2);
//					c = act.getCoord();
//				
//				return c;
//			}

//			private Coord findPreceedingAnchorActivity(Plan plan) {
//				Activity actL = (Activity) plan.getPlanElements().get(plan.getPlanElements().size()-1);
//				return actL.getCoord();
//			}
			
			private Coord findPreviousActivity(Plan plan, int counter) {
				
				
				// This case handles the situation where the first plan element is a non home activity
				if (counter==0)
				{
					//Plan starts with an secondary activity such as leisure, shopping etc
					Activity actL = (Activity) plan.getPlanElements().get(counter);
					return actL.getCoord();
				}
				
				//-2 means (leg + act)
				Activity actL = (Activity) plan.getPlanElements().get(counter-2);
				return actL.getCoord();
			}
			
			private Coord findNextActivity(Plan plan, int counter) {
				//-2 means (leg + act)
				
				int index = counter+2;
				
				// This case handles the situation where the last plan element is a non home activity
				// Size - 1 equals the last index of our plan elements
				if (index > plan.getPlanElements().size()-1 )
				{
					Activity actL = (Activity) plan.getPlanElements().get(counter);
					return actL.getCoord();
				}
				
				Activity actL = (Activity) plan.getPlanElements().get(counter+2);
				return actL.getCoord();
			}
			
			private String findZone(Coord coord, String actTypeShort) {
				Point p = MGC.coord2Point(coord);
				for (Entry<String, Geometry> e : zoneMap.entrySet()){
					if (e.getValue().contains(p)){
						if (e.getKey().equals("350")&&actTypeShort.equals("work")){
							return null;
							//VW Werk, change nothing
						}
						return e.getKey();
					}
				}
				
				return null;
			}
		});
		spr.addAlgorithm(spw);
		spr.readFile(inputPlansFile);
		spw.closeStreaming();
		System.out.println(personset.size());
		
	}
	
	
	
	private void readShape(String shapeFile, String featureKeyInShapeFile) {
		Collection <SimpleFeature> features = ShapeFileReader.getAllFeatures(shapeFile);
		for (SimpleFeature feature : features) {
			String id =  feature.getAttribute(featureKeyInShapeFile).toString();
			
			Geometry geometry = (Geometry) feature.getDefaultGeometry();
			zoneMap.put(id, geometry);
		}
	}
}
