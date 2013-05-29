/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package d4d;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.router.PlanRouter;
import org.matsim.core.router.TripRouter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;
import org.matsim.population.algorithms.ParallelPersonAlgorithmRunner;
import org.matsim.population.algorithms.ParallelPersonAlgorithmRunner.PersonAlgorithmProvider;
import org.matsim.population.algorithms.PersonAlgorithm;

import playground.mzilske.cdr.CellTower;
import playground.mzilske.cdr.CellTowers;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

public class RunScenario {

	private static final int POP_REAL = 22000000;


	double minLong = -4.265;
	public CellTowers getCellTowers() {
		return cellTowers;
	}

	double maxLong = -3.671;
	double minLat = 5.175;
	double maxLat = 5.53;

	Random rnd = new Random();

	boolean filter = true;
	
	CellTowers cellTowers;

	private ScenarioImpl scenario;


	public Scenario readScenario(Config config) throws FileNotFoundException  {
		final Map<Id, List<Sighting>> readAllSightings = readNetworkAndSightings(config);
		
		readSampleWithOneRandomPointForEachSightingInNewCell(readAllSightings);


//		runStatistics();

//				ParallelPersonAlgorithmRunner.run(scenario.getPopulation(), 8, new PersonAlgorithmProvider() {
//		
//					@Override
//					public PersonAlgorithm getPersonAlgorithm() {
//						TripRouter tripRouter = new TripRouter();
//						tripRouter.setRoutingModule("unknown", new BushwhackingRoutingModule(scenario.getPopulation().getFactory(), (NetworkImpl) scenario.getNetwork()));
//						return new PlanRouter(tripRouter);
//					}
//		
//				});

		
		
		
		ParallelPersonAlgorithmRunner.run(scenario.getPopulation(), 8, new org.matsim.population.algorithms.XY2Links(scenario));
		ParallelPersonAlgorithmRunner.run(scenario.getPopulation(), 8, new PersonAlgorithmProvider() {

			@Override
			public PersonAlgorithm getPersonAlgorithm() {
				TripRouter tripRouter = new TripRouter();
				tripRouter.setRoutingModule("unknown", new NetworkRoutingModule(scenario.getPopulation().getFactory(), (NetworkImpl) scenario.getNetwork(), new FreeSpeedTravelTime()));
				return new PlanRouter(tripRouter);
			}

		});
		
		Population unfeasiblePeople = null;
		
		for (int i=0; i<20; i++) {
			unfeasiblePeople = new PopulationImpl(scenario);
			for (Person person : scenario.getPopulation().getPersons().values()) {
				Plan plan = person.getSelectedPlan();
				if (!isFeasible(plan)) {
					unfeasiblePeople.addPerson(person);
				}
			}
			System.out.println("Unfeasible plans: " + unfeasiblePeople.getPersons().size() + " of " +scenario.getPopulation().getPersons().size());	
			
			ParallelPersonAlgorithmRunner.run(unfeasiblePeople, 8, new PersonAlgorithm() {

				@Override
				public void run(Person person) {
					Sightings sightingsForThisAgent = new Sightings( readAllSightings.get(person.getId()));
					for (PlanElement planElement : person.getSelectedPlan().getPlanElements()) {
						if (planElement instanceof Activity) {
							Sighting sighting = sightingsForThisAgent.sightings.next();
							ActivityImpl activity = (ActivityImpl) planElement;
							activity.setLinkId(null);
							Geometry cell = cellTowers.getCell(sighting.getCellTowerId());
							Point p = getRandomPointInFeature(cell);
							Coord newCoord = new CoordImpl(p.getX(), p.getY());
							activity.setCoord(newCoord);
						}
					}
				}
				
			});

			ParallelPersonAlgorithmRunner.run(unfeasiblePeople, 8, new org.matsim.population.algorithms.XY2Links(scenario));
			
			
			ParallelPersonAlgorithmRunner.run(unfeasiblePeople, 8, new PersonAlgorithmProvider() {

				@Override
				public PersonAlgorithm getPersonAlgorithm() {
					TripRouter tripRouter = new TripRouter();
					tripRouter.setRoutingModule("car", new NetworkRoutingModule(scenario.getPopulation().getFactory(), (NetworkImpl) scenario.getNetwork(), new FreeSpeedTravelTime()));
					return new PlanRouter(tripRouter);
				}

			});
			
//			ParallelPersonAlgorithmRunner.run(unfeasiblePeople, 8, new PersonAlgorithmProvider() {
//				
//				@Override
//				public PersonAlgorithm getPersonAlgorithm() {
//					TripRouter tripRouter = new TripRouter();
//					tripRouter.setRoutingModule("unknown", new BushwhackingRoutingModule(scenario.getPopulation().getFactory(), (NetworkImpl) scenario.getNetwork()));
//					return new PlanRouter(tripRouter);
//				}
//	
//			});
			
		}
		
		for (Person person : unfeasiblePeople.getPersons().values()) {
			((PopulationImpl) scenario.getPopulation()).getPersons().remove(person.getId());
		}
		

		runStatistics();
		
		return scenario;
	}

	private boolean isFeasible(Plan plan) {
		double currentTime = 0.0;
		for (PlanElement planElement : plan.getPlanElements()) {
			if (planElement instanceof Leg) {
				LegImpl leg = (LegImpl) planElement;
				double arrivalTime = leg.getArrivalTime();
				currentTime = arrivalTime;
			} else if (planElement instanceof Activity) {
				ActivityImpl activity = (ActivityImpl) planElement;
				double sightingTime = activity.getEndTime();
				if (sightingTime < currentTime) {
					return false;
				}
			}
		}
		return true;
	}

	public Map<Id, List<Sighting>> readNetworkAndSightings(Config config)
			throws FileNotFoundException {
		scenario = (ScenarioImpl) ScenarioUtils.createScenario(config);
		readNetwork();
		readPosts();

		Map<Id, List<Sighting>> readAllSightings = readAllSightings();
		return readAllSightings;
	}

	private Map<Id, List<Sighting>> readAllSightings() throws FileNotFoundException {
		final Map<Id, List<Sighting>> allSightings = new HashMap<Id, List<Sighting>>();

		allSightings.putAll(readSightings("2011-12-07 00:00:00", "/Users/zilske/d4d/POS_SAMPLE_0.TSV", 0));
		allSightings.putAll(readSightings("2011-12-19 00:00:00", "/Users/zilske/d4d/POS_SAMPLE_1.TSV", 1));
		allSightings.putAll(readSightings("2012-01-02 00:00:00", "/Users/zilske/d4d/POS_SAMPLE_2.TSV", 2));
		allSightings.putAll(readSightings("2012-01-16 00:00:00", "/Users/zilske/d4d/POS_SAMPLE_3.TSV", 3));
		return allSightings;
	}

	private CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, "EPSG:3395");


	Coord min = ct.transform(new CoordImpl(minLong, minLat));
	Coord max = ct.transform(new CoordImpl(minLat, maxLat));

	
	void readPosts() {
		final Map<String, CellTower> cellTowerMap = new HashMap<String, CellTower>();
		TabularFileParser tfp = new TabularFileParser();
		TabularFileParserConfig tabularFileParserConfig = new TabularFileParserConfig();
		tabularFileParserConfig.setFileName("/Users/zilske/d4d/ANT_POS.TSV");
		tabularFileParserConfig.setDelimiterRegex("\t");
		tfp.parse(tabularFileParserConfig, new TabularFileHandler() {

			@Override
			public void startRow(String[] row) {
				CoordImpl longLat = new CoordImpl(Double.parseDouble(row[1]), Double.parseDouble(row[2]));
				Coord coord = ct.transform(longLat);
				if (Double.isNaN(coord.getX()) || Double.isNaN(coord.getY())) {
					throw new RuntimeException("Bad latlong: " + coord);
				}
				String cellTowerId = row[0];
				cellTowerMap.put(cellTowerId, new CellTower(cellTowerId,coord));
			}
		});
		
		cellTowers = new CellTowers(cellTowerMap);
		// getLinksCrossingCells();
	}

	



	private void readNetwork() {
		new MatsimNetworkReader(scenario).readFile("/Users/zilske/d4d/output/network.xml");
	}

	private void readSampleWithOneRandomPointForEachSightingInNewCell(final Map<Id, List<Sighting>> sightings) throws FileNotFoundException {
		final Map<Activity, Geometry> cellsOfSightings = new HashMap<Activity, Geometry>();
		for (Entry<Id, List<Sighting>> sightingsPerPerson : sightings.entrySet()) {
			for (Sighting sighting : sightingsPerPerson.getValue()) {
				Geometry cell = cellTowers.getCell(sighting.getCellTowerId());
				Point p = getRandomPointInFeature(cell);
				Coord coord = new CoordImpl(p.getX(), p.getY());
				Activity activity = scenario.getPopulation().getFactory().createActivityFromCoord("sighting", coord);
				cellsOfSightings.put(activity, cell);
				activity.setEndTime(sighting.getDateTime());
				Id personId = sightingsPerPerson.getKey();
				Person person = scenario.getPopulation().getPersons().get(personId);
				if (person == null) {
					person = scenario.getPopulation().getFactory().createPerson(personId);
					person.addPlan(scenario.getPopulation().getFactory().createPlan());
					person.getSelectedPlan().addActivity(activity);
					scenario.getPopulation().addPerson(person);
				} else {
					Activity lastActivity = (Activity) person.getSelectedPlan().getPlanElements().get(person.getSelectedPlan().getPlanElements().size()-1);
					if (cell != cellsOfSightings.get(lastActivity)) {
						Leg leg = scenario.getPopulation().getFactory().createLeg("unknown");
						person.getSelectedPlan().addLeg(leg);
						person.getSelectedPlan().addActivity(activity);
					}
				}
			}
		}

	}

	private Map<Id, List<Sighting>> readSightings(String startDate, String filename, final int populationIdSuffix) {
		final Map<Id, List<Sighting>> sightings = new HashMap<Id, List<Sighting>>();

		final DateTimeFormatter dateTimeFormat = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");
		final DateTime beginning = dateTimeFormat.parseDateTime(startDate);
		TabularFileParser tfp = new TabularFileParser();
		TabularFileParserConfig tabularFileParserConfig = new TabularFileParserConfig();
		tabularFileParserConfig.setFileName(filename);
		tabularFileParserConfig.setDelimiterRegex("\t");
		TabularFileHandler handler = new TabularFileHandler() {

			@Override
			public void startRow(String[] row) {

				DateTime sightingTime = dateTimeFormat.parseDateTime(row[1]);

				IdImpl personId = new IdImpl(row[0] + "_" + Integer.toString(populationIdSuffix));
				String cellTowerId = row[2];


				long timeInSeconds = (sightingTime.getMillis() - beginning.getMillis()) / 1000;



				if (!cellTowerId.equals("-1")) {
					Geometry cell = cellTowers.getCell(cellTowerId);
					if (cell.getNumGeometries() != 0) {
						if (interestedInTime(timeInSeconds)) {
							List<Sighting> sightingsOfPerson = sightings.get(personId);
							if (sightingsOfPerson == null) {
								sightingsOfPerson = new ArrayList<Sighting>();
								sightings.put(personId, sightingsOfPerson);
							}

							Sighting sighting = new Sighting(personId, timeInSeconds, cellTowerId);
							sightingsOfPerson.add(sighting);
						}
					}
				}




			}

			private boolean interestedInTime(long timeInSeconds) {
				if ((timeInSeconds >= 0.0) && (timeInSeconds < 24 * 60 * 60)) {
					return true;
				} else {
					return false;
				}
			}
		};
		tfp.parse(tabularFileParserConfig, handler);
		return sightings;
	}


	private Point getRandomPointInFeature(Geometry ft) {
		Point p = null;
		double x, y;
		do {
			x = ft.getEnvelopeInternal().getMinX() + rnd.nextDouble() * (ft.getEnvelopeInternal().getMaxX() - ft.getEnvelopeInternal().getMinX());
			y = ft.getEnvelopeInternal().getMinY() + rnd.nextDouble() * (ft.getEnvelopeInternal().getMaxY() - ft.getEnvelopeInternal().getMinY());
			p = MGC.xy2Point(x, y);
		} while (!ft.contains(p));
		return p;
	}


	private void runStatistics() {
		System.out.println(scenario.getPopulation().getPersons().size());
		new InitialStatistics("").run(scenario.getPopulation());

		Population cityPopulation = new PopulationImpl(scenario);
		Population nonCityPopulation = new PopulationImpl(scenario);

		for (Person person : scenario.getPopulation().getPersons().values()) {
			Plan plan = person.getSelectedPlan();
			if (planContainsActivityInCity(plan)) {
				cityPopulation.addPerson(person);
			} else {
				nonCityPopulation.addPerson(person);
			}

		}
		System.out.println(cityPopulation.getPersons().size());
		new InitialStatistics("-capital-only").run(cityPopulation);
		new PopulationWriter(cityPopulation, null).write("/Users/zilske/d4d/output/population-capital-only.xml");
		System.out.println(nonCityPopulation.getPersons().size());
		new InitialStatistics("-countryside-only").run(nonCityPopulation);
		new PopulationWriter(nonCityPopulation, null).write("/Users/zilske/d4d/output/population-countryside-only.xml");
	}


	private boolean planContainsActivityInCity(Plan plan) {
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof Activity) {
				Activity activity = (Activity) pe;
				Coord coord = activity.getCoord();
				if (coord.getX() >= min.getX() && coord.getX() < max.getX() && coord.getY() >= min.getY() && coord.getY() < max.getY()) {
					return true;
				}
			}
		}
		return false;
	}

	public static void main(String[] args) throws FileNotFoundException {
		Config config = ConfigUtils.createConfig();
		RunScenario scenarioReader = new RunScenario();
		Scenario scenario = scenarioReader.readScenario(config);
		new PopulationWriter(scenario.getPopulation(), null).write("/Users/zilske/d4d/output/population.xml");
		int sampleSize = scenario.getPopulation().getPersons().size();
		System.out.println("Created " + sampleSize + " people. That's a " + ((double) sampleSize) / POP_REAL  + " sample.");
	}



}
