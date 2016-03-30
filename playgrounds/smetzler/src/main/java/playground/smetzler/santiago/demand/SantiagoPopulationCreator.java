package playground.smetzler.santiago.demand;

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

import java.util.*;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.misc.Time;
import org.opengis.feature.simple.SimpleFeature;

import playground.smetzler.santiago.polygon.CreatePtZonesFromTransitStopCoordinates;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

/** 
 * 
 * @author aneumann
 *
 */
public class SantiagoPopulationCreator {
	
	private static final Logger log = Logger.getLogger(SantiagoPopulationCreator.class);

	private final double TIME_DIFFERENCE = 6 * 3600; // 6 hours
	private final double DISTANCE_THRESHOLD = 1000.0; // 1000 m

	public static void main(String[] args) {
		String ptZones;
		String tripTable;
		String outputPlansFile;
		
		if (args.length == 3) {
			ptZones = args[0];
			tripTable = args[1];
			outputPlansFile = args[2];
		} else {
			log.info("Need three arguments:");
			log.info(" 1 - pt zones shape");
			log.info(" 2 - trip table");
			log.info(" 3 - output plans file");
			
			final String directory = "e:/_shared-svn/_data/santiago_pt_demand_matrix/";
			ptZones = directory + "pt_stops_schedule_2013/pt_zones.shp";
			tripTable = directory + "raw_data_2013/MatrizODviajes_zonas777_mediahora_web_abr2013.csv.gz";
			outputPlansFile = directory + "pt_stops_schedule_2013/santiago.xml.gz";
		}
		
		SantiagoPopulationCreator populationSantiago = new SantiagoPopulationCreator();
		populationSantiago.run(ptZones, tripTable, outputPlansFile);
	}

	public Population run(String ptZones, String tripTable, String outputPlansFile) {
		
		// read pt zones
		Map<String, Geometry> ptZoneId2Geometry = readShapeFile(ptZones);
		
		// read the tripTable
		List<TripTableEntry> tripTableEntries = ReadTripTable2013.readGenericCSV(tripTable);

		// create population from trip table entries
		Population population = createPopulation(ptZoneId2Geometry, tripTableEntries);
		writePlans(population, outputPlansFile);

		// merge persons if feasible
		population = findPersonPairs(population);
		writePlans(population, outputPlansFile.replace(".xml.gz", "_paired.xml.gz"));
		
		return population;
	}

	private Population createPopulation(Map<String, Geometry> ptZoneId2Geometry, List<TripTableEntry> tripTableEntries) {
			
			Random random = MatsimRandom.getLocalInstance();
			Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
			Population population = scenario.getPopulation();
			
			for (TripTableEntry tripTableEntry : tripTableEntries) {
				
				int nOfAgentsToCreate = (int) tripTableEntry.avgNumberOfTripsPerWorkingDay;
				// handle the fraction part with a certain probability
				if (random.nextDouble() < (tripTableEntry.avgNumberOfTripsPerWorkingDay - (int) tripTableEntry.avgNumberOfTripsPerWorkingDay)) {
					nOfAgentsToCreate++;
				}
				
				for (int i = 0; i < nOfAgentsToCreate; i++) {
					Id<Person> personId = Id.createPersonId(TransportMode.pt + "-" + tripTableEntry.boardingZone + "-" + tripTableEntry.alightingZone + "-" + Time.writeTime(tripTableEntry.timeOfBoarding) + "-" +  i);
					Person person = population.getFactory().createPerson(personId);
					population.addPerson(person);
					
					Plan plan = population.getFactory().createPlan();
					person.addPlan(plan);
					
					Coord homeLocation = getRandomCoordinateFromPtZone(random, ptZoneId2Geometry, tripTableEntry.boardingZone);
					Coord workLocation = getRandomCoordinateFromPtZone(random, ptZoneId2Geometry, tripTableEntry.alightingZone);
					
					Activity activity;
					
					// first
					activity = population.getFactory().createActivityFromCoord("firstAct", homeLocation);
					activity.setEndTime(getRandomTimeWithinInterval(random, tripTableEntry.timeOfBoarding, ReadTripTable2012.TIME_INTERVAL));
					plan.addActivity(activity);
					
					plan.addLeg(population.getFactory().createLeg(TransportMode.pt));
					
					// second
					activity = population.getFactory().createActivityFromCoord("lastAct", workLocation);
					// no end time known at this moment
	//				activity.setEndTime(getRandomTimeWithinInterval(tripTableEntry.timeOfBoarding, ReadTripTable.TIME_INTERVAL));
					plan.addActivity(activity);
				}
			}
			
			log.info("Created " + population.getPersons().size() + " persons.");
			return population;
		}

	private Population findPersonPairs(Population population) {

		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Population processedPopulation = scenario.getPopulation();
		int personPairsFound = 0;

		// sort persons according to their departure time
		LinkedList<Person> sortedPersons = new LinkedList<Person>(population.getPersons().values());
		Collections.sort(sortedPersons, new DepartureTimeComarator());

		Person[] sortedPersonArray = new Person[sortedPersons.size()];
		sortedPersons.toArray(sortedPersonArray);
		sortedPersons = null;
		
		int lastIndexWithTimeDifferenceOk = 0;

		for (int personIndex = 0; personIndex < sortedPersonArray.length; personIndex++) {
			Person person = sortedPersonArray[personIndex];
			
			if (person != null) {
				// not deleted yet - so find a candidate for merging

				boolean timeIndexFound = false;
				
				// check all other persons for candidates
				for (int candidatePersonIndex = lastIndexWithTimeDifferenceOk; candidatePersonIndex < sortedPersonArray.length; candidatePersonIndex++) {
					Person candidatePerson = sortedPersonArray[candidatePersonIndex];
					
					if (candidatePerson != null) {
						// possible candidate - check it out

						if(!timeIndexFound){
							if(departureTimeDifferenceLargeEnough(person, candidatePerson)){
								// time is ok
								timeIndexFound = true;
								lastIndexWithTimeDifferenceOk = candidatePersonIndex;
							}
						}

						if (timeIndexFound) {

							if(activitiesNearEnough(person, candidatePerson)) {
								// space is ok

								personPairsFound++;

								// merge those two persons
								person = mergePersons(person, candidatePerson);

								// remove the candidate
								sortedPersonArray[candidatePersonIndex] = null;

								// abort the search
								break;
							}
						}

					}



				}

				// store the merged or nonmerged Person away
				processedPopulation.addPerson(person);
			}

		}

		log.info("Found " + personPairsFound + " pairs.");
		log.info("The paired population contains " + processedPopulation.getPersons().size() + " agents");
		return processedPopulation;
	}

	
	/**
	 * For educational purposes...
	 */
	private Population findPersonPairsList(Population population) {

		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Population processedPopulation = scenario.getPopulation();
		int personPairsFound = 0;

		// sort persons according to their departure time
		LinkedList<Person> sortedPersons = new LinkedList<Person>(population.getPersons().values());
		Collections.sort(sortedPersons, new DepartureTimeComarator());

		Person person = sortedPersons.pollFirst();
		while (person != null) {

			// check all other person for candidates
			for (Iterator<Person> iterator = sortedPersons.iterator(); iterator.hasNext();) {
				Person candidatePerson = iterator.next();

				if(departureTimeDifferenceLargeEnough(person, candidatePerson)){
					// time is ok
					if(activitiesNearEnough(person, candidatePerson)) {
						// space is ok

						personPairsFound++;

						// merge those two persons
						person = mergePersons(person, candidatePerson);

						// remove the candidate
						iterator.remove();

						break;
					}
				}
			}

			// store the merged or nonmerged Person away
			processedPopulation.addPerson(person);

			// get the next one
			person = sortedPersons.pollFirst();
		}

		log.info("Found " + personPairsFound + " pairs.");
		log.info("The paired population contains " + processedPopulation.getPersons().size() + " agents");
		return processedPopulation;
	}

	private Person mergePersons(Person person, Person candidatePerson) {
		
		// move the activity end time
		Activity activity = (Activity) person.getSelectedPlan().getPlanElements().get(2);
		Activity activityToGetEndTimeFrom = (Activity) candidatePerson.getSelectedPlan().getPlanElements().get(0);
		
		activity.setEndTime(activityToGetEndTimeFrom.getEndTime());
		
		// change act type to middle
		((Activity) person.getSelectedPlan().getPlanElements().get(2)).setType("middleAct");
		
		// copy the leg and the last activity
		person.getSelectedPlan().addLeg((Leg) candidatePerson.getSelectedPlan().getPlanElements().get(1));
		
		person.getSelectedPlan().addActivity((Activity) candidatePerson.getSelectedPlan().getPlanElements().get(2));
		
		return person;
	}


	private boolean activitiesNearEnough(Person person, Person candidatePerson) {
		Activity personFromActivity = (Activity) person.getSelectedPlan().getPlanElements().get(0);
		Activity personToActivity = (Activity) person.getSelectedPlan().getPlanElements().get(2);
		
		Activity candidatePersonFromActivity = (Activity) candidatePerson.getSelectedPlan().getPlanElements().get(0);
		Activity candidatePersonToActivity = (Activity) candidatePerson.getSelectedPlan().getPlanElements().get(2);
		
		double distanceA = CoordUtils.calcEuclideanDistance(personToActivity.getCoord(), candidatePersonFromActivity.getCoord());
		if (distanceA > DISTANCE_THRESHOLD) {
			// those two are too fare away - abort
			return false;
		}
		
		double distanceB = CoordUtils.calcEuclideanDistance(personFromActivity.getCoord(), candidatePersonToActivity.getCoord());
		if (distanceB > DISTANCE_THRESHOLD) {
			// those two are too fare away - abort
			return false;
		}
		
		// we passed both tests - this could be a candidate
		return true;
	}


	private boolean departureTimeDifferenceLargeEnough(Person person, Person candidatePerson) {
		double departureTimePerson = this.getDepartureTimeFromPerson(person);
		double departureTimeCandidatePerson = this.getDepartureTimeFromPerson(candidatePerson);
		
		if (departureTimePerson + this.TIME_DIFFERENCE < departureTimeCandidatePerson) {
			return true;
		} else {
			return false;
		}
	}

	private Coord getRandomCoordinateFromPtZone(Random random, Map<String, Geometry> ptZoneId2Geometry, String ptZoneId) {
		Geometry geometry = ptZoneId2Geometry.get(ptZoneId);
		
		if (geometry == null) {
			throw new RuntimeException("No geometry for zone " + ptZoneId);
		}

		Coord coordinate;
		Point candidatePoint;

		do {
			double widthOfTheGeometry = geometry.getEnvelopeInternal().getMaxX() - geometry.getEnvelopeInternal().getMinX();
			double heightOfTheGeometry = geometry.getEnvelopeInternal().getMaxY() - geometry.getEnvelopeInternal().getMinY();
			
			double x = geometry.getEnvelopeInternal().getMinX() + random.nextDouble()	* widthOfTheGeometry;
			double y = geometry.getEnvelopeInternal().getMinY() + random.nextDouble()	* heightOfTheGeometry;
			
			candidatePoint = MGC.xy2Point(x, y);
			coordinate = new Coord(candidatePoint.getX(), candidatePoint.getY());
		} while (!geometry.contains(candidatePoint));

		return coordinate;
	}
	

	private double getDepartureTimeFromPerson(Person person) {
		Activity activity = (Activity) person.getSelectedPlan().getPlanElements().get(0);
		return activity.getEndTime();
	}

	private double getRandomTimeWithinInterval(Random random, double time, double range) {
		// no need to cut end times later than 24 hours
		return time + range * random.nextDouble();
	}

	private Map<String, Geometry> readShapeFile(String ptZones) {
		Map<String, Geometry> ptZoneId2Geometry = new HashMap<String, Geometry>();
		
		for (SimpleFeature feature : ShapeFileReader.getAllFeatures(ptZones)) {
			String zoneId = (String) feature.getAttribute(CreatePtZonesFromTransitStopCoordinates.NAME_IDENTIFIER);
			Geometry geometry = (Geometry) feature.getDefaultGeometry();
			
			ptZoneId2Geometry.put(zoneId, geometry);
		}
		
		log.info("Read " + ptZoneId2Geometry.size() + " zones from " + ptZones);
		return ptZoneId2Geometry;
	}

	private void writePlans(Population population, String outputPlansFile) {
		PopulationWriter populationWriter100pct = new PopulationWriter(population, null);
		populationWriter100pct.write(outputPlansFile);
		
		PopulationWriter populationWriter50pct = new PopulationWriter(population, null, 0.5);
		populationWriter50pct.write(outputPlansFile.replace(".xml.gz", "_50pct.xml.gz"));
		
		PopulationWriter populationWriter25pct = new PopulationWriter(population, null, 0.25);
		populationWriter25pct.write(outputPlansFile.replace(".xml.gz", "_25pct.xml.gz"));
		
		PopulationWriter populationWriter10pct = new PopulationWriter(population, null, 0.1);
		populationWriter10pct.write(outputPlansFile.replace(".xml.gz", "_10pct.xml.gz"));

		PopulationWriter populationWriter5pct = new PopulationWriter(population, null, 0.05);
		populationWriter5pct.write(outputPlansFile.replace(".xml.gz", "_05pct.xml.gz"));
		
		PopulationWriter populationWriter2pct = new PopulationWriter(population, null, 0.02);
		populationWriter2pct.write(outputPlansFile.replace(".xml.gz", "_02pct.xml.gz"));
		
		PopulationWriter populationWriter1pct = new PopulationWriter(population, null, 0.01);
		populationWriter1pct.write(outputPlansFile.replace(".xml.gz", "_01pct.xml.gz"));
	}

	private class DepartureTimeComarator implements Comparator<Person>{

		@Override
		public int compare(Person personA, Person personB) {
			double departureTimePersonA = getDepartureTimeFromPerson(personA);
			double departureTimePersonB = getDepartureTimeFromPerson(personB);

			if (departureTimePersonA < departureTimePersonB) {
				return -1;
			} else if (departureTimePersonA > departureTimePersonB) {
				return 1;
			} else {
				return 0;
			}
		}

		private double getDepartureTimeFromPerson(Person person) {
			Activity activity = (Activity) person.getSelectedPlan().getPlanElements().get(0);
			return activity.getEndTime();
		}
	}
}