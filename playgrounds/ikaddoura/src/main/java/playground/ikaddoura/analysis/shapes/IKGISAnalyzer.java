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
import java.io.IOException;
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
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import playground.ikaddoura.analysis.extCost.ExtCostEventHandler;
import playground.ikaddoura.internalizationCar.MarginalCongestionHandlerImplV3;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

/**
 * 
 * Reads in a shapeFile and performs a zone-based analysis:
 * Calculates the number of home activities, work activities, all activities per zone.
 * Calculates the tolls paid during the day mapped back to the agents' home zones (toll payments).
 * Calculates the tolls paid during the day mapped back to the agents's home zones in relation to the number of home activities (avg. toll payments)
 * 
 * The shape file has to contain a grid (e.g. squares, hexagons) which can be created using a QGIS plugin called MMQGIS.
 * 
 * @author ikaddoura
 *
 */
public class IKGISAnalyzer {
	
	private static final Logger log = Logger.getLogger(IKGISAnalyzer.class);

	// Run1
	private final static String runNumber1 = "baseCase";
	private final static String runDirectory1 = "../../runs-svn/berlin_internalizationCar/output/baseCase_2/";
		
	// Run2
//	private final static String runNumber2 = "internalization";
//	private final static String runDirectory2 = "../../runs-svn/berlin_internalizationCar/output/internalization_2/";
		
	private final String shapeFileZones = "/Users/ihab/Documents/workspace/shared-svn/studies/ihab/berlin/shapeFiles/Berlin/berlin_grid_2500.shp";

//	final double scalingFactor = 10.;
	
	private final String outputPath1 = runDirectory1 + "analysis/gridBasedAnalysis/moneyAmounts/";
//	private String outputPath2 = runDirectory2 + "analysis/gridBasedAnalysis/moneyAmounts/";
	
	private final String homeActivity = "home";
	private final String workActivity = "work";
	
	private Map<Integer, Geometry> zoneId2geometry = new HashMap<Integer, Geometry>();
	
	private IKShapeFileWriter shapeFileWriter = new IKShapeFileWriter();
			
	public static void main(String[] args) throws IOException {
		IKGISAnalyzer main = new IKGISAnalyzer();
		main.run();
	}
	
	private Scenario loadScenario(String configFile, String netFile) {
		Config config = ConfigUtils.loadConfig(configFile);
		config.network().setInputFile(netFile);
		config.plans().setInputFile(null);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		return scenario;
	}
	
	public void run() throws IOException {
		
		log.info("Reading zone shapefile...");
		Collection<SimpleFeature> features;
		features = ShapeFileReader.getAllFeatures(shapeFileZones);
		int featureCounter = 0;
		for (SimpleFeature feature : features) {
			this.zoneId2geometry.put(featureCounter, (Geometry) feature.getDefaultGeometry());
			featureCounter++;
		}
		
		log.info("Reading zone shapefile... Done.");
		
		log.info("Loading scenario...");
		Scenario scenario1 = loadScenario(runDirectory1 + "output_config.xml.gz", runDirectory1 + "output_network.xml.gz");
		MatsimPopulationReader mpr = new MatsimPopulationReader(scenario1);
		mpr.readFile(runDirectory1 + "output_plans.xml.gz");
		log.info("Loading scenario... Done.");
		
		// do some analysis here
		log.info("Analyzing the scenario...");
		EventsManager events = EventsUtils.createEventsManager();

		// Compute marginal congestion events based on normal events file.
		MarginalCongestionHandlerImplV3 congestionHandler = new MarginalCongestionHandlerImplV3(events, (ScenarioImpl) scenario1);
		events.addHandler(congestionHandler);
		
		// Analyze external cost per person based on marginal congestion events.
		ExtCostEventHandler extCostHandler = new ExtCostEventHandler(scenario1, false);
		events.addHandler(extCostHandler);
		
		log.info("Reading events file...");
		MatsimEventsReader reader = new MatsimEventsReader(events);
		String eventsFile1 = runDirectory1 + "/ITERS/it." + scenario1.getConfig().controler().getLastIteration() + "/" + scenario1.getConfig().controler().getLastIteration() + ".events.xml.gz";
		reader.readFile(eventsFile1);
		log.info("Reading events file... Done.");
		
		Map<Id, Double> personId2amountSum = extCostHandler.getPersonId2amountSumAllAgents();
		log.info("Analyzing the scenario... Done.");
		
		File file = new File(outputPath1);
		file.mkdirs();
				
		this.analyzeZones(personId2amountSum, scenario1.getPopulation(), outputPath1 + runNumber1 + "." + scenario1.getConfig().controler().getLastIteration() + "extCost_Zones.shp");
	
		System.out.println();
		System.out.println("Done.");
	}

	private void analyzeZones(Map<Id, Double> personId2amountSum, Population population, String outputFile) {
		// home activities
		Map<Integer,Integer> zoneNr2homeActivities = getZoneNr2activityLocations(homeActivity, population, this.zoneId2geometry);
		
		// work activities
		Map<Integer,Integer> zoneNr2workActivities = getZoneNr2activityLocations(workActivity, population, this.zoneId2geometry);

		// all activities
		Map<Integer,Integer> zoneNr2activities = getZoneNr2activityLocations(null, population, this.zoneId2geometry);
		
		// toll payments mapped back to home location
		Map<Integer,Double> zoneNr2tollPayments = getZoneNr2tollPayments(population, personId2amountSum, this.zoneId2geometry);
		
		// toll payments mapped back to home location in relation to home activities
		Map<Integer,Double> zoneNr2AvgTollPayments = getZoneId2avgToll(zoneNr2tollPayments, zoneNr2homeActivities);
		
		shapeFileWriter.writeShapeFileGeometry(this.zoneId2geometry, zoneNr2homeActivities, zoneNr2workActivities, zoneNr2activities, zoneNr2tollPayments, zoneNr2AvgTollPayments, outputFile);
	}
	
	private Map<Integer, Double> getZoneId2avgToll(Map<Integer, Double> zoneNr2tollPayments, Map<Integer, Integer> zoneNr2homeActivities) {
		Map<Integer, Double> zoneId2avgToll = new HashMap<Integer, Double>();
		for (Integer zoneId : zoneNr2tollPayments.keySet()) {
			zoneId2avgToll.put(zoneId, zoneNr2tollPayments.get(zoneId) / zoneNr2homeActivities.get(zoneId));
		}

		return zoneId2avgToll;
	}

	private Map<Integer, Double> getZoneNr2tollPayments(Population population, Map<Id, Double> personId2amountSum, Map<Integer, Geometry> zoneId2geometry) {
		Map<Integer, Double> zoneNr2tollPayments = new HashMap<Integer, Double>();	
		
		SortedMap<Id,Coord> personId2homeCoord = getPersonId2Coordinates(population, homeActivity);
		
		for (Id personId : personId2amountSum.keySet()) {
			if (personId2homeCoord.containsKey(personId)){
				for (Integer zoneId : zoneId2geometry.keySet()) {
					Geometry geometry = zoneId2geometry.get(zoneId);
					Point p = MGC.coord2Point(personId2homeCoord.get(personId)); 
					
					if (p.within(geometry)){
						if (zoneNr2tollPayments.get(zoneId) == null){
							zoneNr2tollPayments.put(zoneId, personId2amountSum.get(personId));
						} else {
							double tollPayments = zoneNr2tollPayments.get(zoneId);
							zoneNr2tollPayments.put(zoneId, tollPayments + personId2amountSum.get(personId));
						}
					}
				}
			} else {
				// person doesn't have a home activity
			}
		}
		
		return zoneNr2tollPayments;
	}

	private Map<Integer, Integer> getZoneNr2activityLocations(String activity, Population population, Map<Integer, Geometry> zoneNr2zoneGeometry) {
		Map<Integer, Integer> zoneNr2activity = new HashMap<Integer, Integer>();	

		SortedMap<Id,Coord> personId2activityCoord = getPersonId2Coordinates(population, activity);
		
		for (Coord coord : personId2activityCoord.values()) {
			for (Integer nr : zoneNr2zoneGeometry.keySet()) {
				Geometry geometry = zoneNr2zoneGeometry.get(nr);
				Point p = MGC.coord2Point(coord); 
				
				if (p.within(geometry)){
					if (zoneNr2activity.get(nr) == null){
						zoneNr2activity.put(nr, 1);
					} else {
						int activityCounter = zoneNr2activity.get(nr);
						zoneNr2activity.put(nr, activityCounter + 1);
					}
				}
			}
		}
		return zoneNr2activity;
	}
	
	private SortedMap<Id, Coord> getPersonId2Coordinates(Population population, String activity) {
		SortedMap<Id,Coord> personId2coord = new TreeMap<Id,Coord>();
		
		for(Person person : population.getPersons().values()){
			
			for (PlanElement pE : person.getSelectedPlan().getPlanElements()){
				
				if (pE instanceof Activity){
					Activity act = (Activity) pE;
					
					if (act.getType().equals(activity) || act.getType().equals(null)) {
						
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
