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

package playground.ikaddoura.analysis.shapes;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

/**
 * 
 * Reads in a shapeFile and performs a zone-based analysis:
 * Calculates the number of home activities, work activities, all activities per zone.
 * Calculates the congestion costs of the whole day mapped back to the causing agents' home zones (toll payments 'caused').
 * Calculates the congestion costs of the whole day mapped back to the causing agents' home zones in relation to the number of home activities (avg. toll payments 'caused')
 * Calculates the congestion costs of the whole day mapped back to the affected agents' home zones (toll payments 'affected').
 * Calculates the congestion costs of the whole day mapped back to the affected agents' home zones in relation to the number of home activities (avg. toll payments 'affected')
 * 
 * The shape file has to contain a grid (e.g. squares, hexagons) which can be created using a QGIS plugin called MMQGIS.
 * 
 * @author ikaddoura
 *
 */
public class IKGISAnalyzer {
	
	private final String homeActivity;
	private final String workActivity;
	
	// the number of persons a single agent represents
	private final int scalingFactor;
		
	private static final Logger log = Logger.getLogger(IKGISAnalyzer.class);
	private Map<Integer, Geometry> zoneId2geometry = new HashMap<Integer, Geometry>();
	
	public IKGISAnalyzer(
			String shapeFileZones,
			int scalingFactor,
			String homeActivity,
			String workActivity) {
		
		this.scalingFactor = scalingFactor;
		this.homeActivity = homeActivity;
		this.workActivity = workActivity;
		
		log.info("Reading zone shapefile...");
		Collection<SimpleFeature> features;
		features = ShapeFileReader.getAllFeatures(shapeFileZones);
		int featureCounter = 0;
		for (SimpleFeature feature : features) {
			this.zoneId2geometry.put(featureCounter, (Geometry) feature.getDefaultGeometry());
			featureCounter++;
		}
		log.info("Reading zone shapefile... Done.");
	}

	public void analyzeZones_congestionCost(Scenario scenario, String runDirectory, Map<Id<Person>, Double> causingAgentId2amountSum, Map<Id<Person>, Double> affectedAgentId2amountSum) {
		
		log.info("Number of causing agent IDs: " + causingAgentId2amountSum.size());
		log.info("Number of affected agent IDs: " + affectedAgentId2amountSum.size());

		String outputPath1 = runDirectory + "spatial_analysis/congestionCost_zones/";
		
		File file = new File(outputPath1);
		file.mkdirs();
				
		// home activities
		log.info("Analyzing Home activities per zone...");
		Map<Integer,Integer> zoneNr2homeActivities = getZoneNr2activityLocations(homeActivity, scenario.getPopulation(), this.zoneId2geometry, this.scalingFactor);
		log.info("Analyzing Home activities per zone... Done.");
		
		// work activities
		log.info("Analyzing Work activities per zone...");
		Map<Integer,Integer> zoneNr2workActivities = getZoneNr2activityLocations(workActivity, scenario.getPopulation(), this.zoneId2geometry, this.scalingFactor);
		log.info("Analyzing Work activities per zone... Done.");

		// all activities
		log.info("Analyzing all activities per zone...");
		Map<Integer,Integer> zoneNr2activities = getZoneNr2activityLocations(null, scenario.getPopulation(), this.zoneId2geometry, this.scalingFactor);
		log.info("Analyzing all activities per zone... Done.");
		
		// toll payments mapped back to home location
		log.info("Mapping back congestion cost back to home location...");
		Map<Integer,Double> zoneNr2tollPaymentsCaused = getZoneNr2totalAmount(scenario.getPopulation(), causingAgentId2amountSum, this.zoneId2geometry, this.scalingFactor);
		Map<Integer,Double> zoneNr2tollPaymentsAffected = getZoneNr2totalAmount(scenario.getPopulation(), affectedAgentId2amountSum, this.zoneId2geometry, this.scalingFactor);
		log.info("Mapping back congestion cost back to home location... Done.");

		// toll payments mapped back to home location in relation to home activities
		log.info("Mapping back congestion cost back to home location (in relation to home activities)...");
		Map<Integer,Double> zoneNr2AvgTollPaymentsCaused = getZoneId2avg(zoneNr2tollPaymentsCaused, zoneNr2homeActivities);
		Map<Integer,Double> zoneNr2AvgTollPaymentsAffected = getZoneId2avg(zoneNr2tollPaymentsAffected, zoneNr2homeActivities);
		log.info("Mapping back congestion cost back to home location (in relation to home activities)... Done.");
		
		log.info("Writing shape file...");
		IKShapeFileWriter shapeFileWriter = new IKShapeFileWriter();
		shapeFileWriter.writeShapeFileGeometry(
				this.zoneId2geometry, zoneNr2homeActivities, zoneNr2workActivities, zoneNr2activities,
				zoneNr2tollPaymentsCaused, zoneNr2AvgTollPaymentsCaused,
				zoneNr2tollPaymentsAffected, zoneNr2AvgTollPaymentsAffected,
				outputPath1 + scenario.getConfig().controler().getLastIteration() + ".congestionCost_zones.shp");
		log.info("Writing shape file... Done.");
	}
	
	public void analyzeZones_welfare(String runId, Scenario scenario, String runDirectory, Map<Id<Person>, Double> personId2userBenefits, Map<Id<Person>, Double> personId2tollPayments, Map<Id<Person>, Double> personId2welfareContribution) {
		String outputPath = runDirectory + "spatial_analysis/welfare_zones/";
		
		File file = new File(outputPath);
		file.mkdirs();
				
		// home activities
		log.info("Analyzing Home activities per zone...");
		Map<Integer,Integer> zoneNr2homeActivities = getZoneNr2activityLocations(homeActivity, scenario.getPopulation(), this.zoneId2geometry, this.scalingFactor);
		log.info("Analyzing Home activities per zone... Done.");
		
		// work activities
		log.info("Analyzing Work activities per zone...");
		Map<Integer,Integer> zoneNr2workActivities = getZoneNr2activityLocations(workActivity, scenario.getPopulation(), this.zoneId2geometry, this.scalingFactor);
		log.info("Analyzing Work activities per zone... Done.");

		// all activities
		log.info("Analyzing all activities per zone...");
		Map<Integer,Integer> zoneNr2activities = getZoneNr2activityLocations(null, scenario.getPopulation(), this.zoneId2geometry, this.scalingFactor);
		log.info("Analyzing all activities per zone... Done.");
		
		// absolute numbers mapped back to home location
		log.info("Mapping back absolute toll payments and user benefits back to home location...");
		Map<Integer,Double> zoneNr2tollPayments = getZoneNr2totalAmount(scenario.getPopulation(), personId2tollPayments, this.zoneId2geometry, this.scalingFactor);
		Map<Integer,Double> zoneNr2userBenefits = getZoneNr2totalAmount(scenario.getPopulation(), personId2userBenefits, this.zoneId2geometry, this.scalingFactor);
		Map<Integer,Double> zoneNr2welfareContribution = getZoneNr2totalAmount(scenario.getPopulation(), personId2welfareContribution, this.zoneId2geometry, this.scalingFactor);
		log.info("Mapping back absolute toll payments and user benefits back to home location... Done.");

		// relative numbers mapped back to home location
		log.info("Mapping back relative numbers to home location (in relation to home activities)...");
		Map<Integer,Double> zoneNr2AvgTollPayments = getZoneId2avg(zoneNr2tollPayments, zoneNr2homeActivities);
		Map<Integer,Double> zoneNr2AvgUserBenefits = getZoneId2avg(zoneNr2userBenefits, zoneNr2homeActivities);
		Map<Integer,Double> zoneNr2AvgWelfareContribution = getZoneId2avg(zoneNr2welfareContribution, zoneNr2homeActivities);
		log.info("Mapping back relative numbers to home location (in relation to home activities)... Done.");
		
		log.info("Writing shape file...");
		IKWelfareShapeFileWriter shapeFileWriter = new IKWelfareShapeFileWriter();
		shapeFileWriter.writeShapeFileGeometry(
				this.zoneId2geometry, zoneNr2homeActivities, zoneNr2workActivities, zoneNr2activities,
				zoneNr2tollPayments, zoneNr2userBenefits, zoneNr2welfareContribution,
				zoneNr2AvgTollPayments, zoneNr2AvgUserBenefits, zoneNr2AvgWelfareContribution,
				outputPath + scenario.getConfig().controler().getLastIteration() + ".welfare_" + runId + ".shp");
		log.info("Writing shape file... Done.");
		
	}
	
	private Map<Integer, Double> getZoneId2avg(Map<Integer, Double> zoneNr2tollPayments, Map<Integer, Integer> zoneNr2homeActivities) {
		Map<Integer, Double> zoneId2avgToll = new HashMap<Integer, Double>();
		for (Integer zoneId : zoneNr2tollPayments.keySet()) {
			zoneId2avgToll.put(zoneId, zoneNr2tollPayments.get(zoneId) / zoneNr2homeActivities.get(zoneId));
		}

		return zoneId2avgToll;
	}

	private Map<Integer, Double> getZoneNr2totalAmount(Population population, Map<Id<Person>, Double> personId2amountSum, Map<Integer, Geometry> zoneId2geometry, int scalingFactor) {
		Map<Integer, Double> zoneNr2totalAmount = new HashMap<Integer, Double>();	
		
		SortedMap<Id<Person>,Coord> personId2homeCoord = getPersonId2Coordinates(population, homeActivity);
		
		for (Id<Person> personId : personId2amountSum.keySet()) {
			if (personId2homeCoord.containsKey(personId)){
				for (Integer zoneId : zoneId2geometry.keySet()) {
					Geometry geometry = zoneId2geometry.get(zoneId);
					Point p = MGC.coord2Point(personId2homeCoord.get(personId)); 
					
					if (p.within(geometry)){
						if (zoneNr2totalAmount.get(zoneId) == null){
							zoneNr2totalAmount.put(zoneId, personId2amountSum.get(personId) * scalingFactor);
						} else {
							double tollPayments = zoneNr2totalAmount.get(zoneId);
							zoneNr2totalAmount.put(zoneId, tollPayments + (personId2amountSum.get(personId) * scalingFactor) );
						}
					}
				}
			} else {
				// person doesn't have a home activity
			}
		}
		
		return zoneNr2totalAmount;
	}

	private Map<Integer, Integer> getZoneNr2activityLocations(String activity, Population population, Map<Integer, Geometry> zoneNr2zoneGeometry, int scalingFactor) {
		Map<Integer, Integer> zoneNr2activity = new HashMap<Integer, Integer>();	

		SortedMap<Id<Person>,Coord> personId2activityCoord = getPersonId2Coordinates(population, activity);
		
		for (Coord coord : personId2activityCoord.values()) {
			for (Integer nr : zoneNr2zoneGeometry.keySet()) {
				Geometry geometry = zoneNr2zoneGeometry.get(nr);
				Point p = MGC.coord2Point(coord); 
				
				if (p.within(geometry)){
					if (zoneNr2activity.get(nr) == null){
						zoneNr2activity.put(nr, scalingFactor);
					} else {
						int activityCounter = zoneNr2activity.get(nr);
						zoneNr2activity.put(nr, activityCounter + scalingFactor);
					}
				}
			}
		}
		return zoneNr2activity;
	}
	
	private SortedMap<Id<Person>, Coord> getPersonId2Coordinates(Population population, String activity) {
		SortedMap<Id<Person>,Coord> personId2coord = new TreeMap<Id<Person>,Coord>();
		
		for(Person person : population.getPersons().values()){
			
			for (PlanElement pE : person.getSelectedPlan().getPlanElements()){
				
				if (pE instanceof Activity){
					Activity act = (Activity) pE;
					
					if (act.getType().equals(activity) || activity == null) {
						
						Coord coord = act.getCoord();
						personId2coord.put(person.getId(), coord);
					
					} else {
						//  other activity type
					}
				}
			}
		}
		return personId2coord;
	}
		
}
