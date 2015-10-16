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

package playground.mzilske.d4d;

import org.geotools.data.DataUtilities;
import org.geotools.feature.SchemaException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import playground.mzilske.cdr.*;

import java.io.FileNotFoundException;
import java.util.*;

public class CreatePopulation {

	
	Zones zones;
	
	private static final int POP_REAL = 22000000;

	private double minLong = -4.265;
	private double maxLong = -3.671;
	private double minLat = 5.175;
	private double maxLat = 5.53;

	private Random rnd = new Random();

	private ScenarioImpl scenario;


	private Coord min = D4DConsts.ct.transform(new Coord(minLong, minLat));
	private Coord max = D4DConsts.ct.transform(new Coord(maxLong, maxLat));


	public Scenario readScenario(Config config) throws FileNotFoundException  {
		final SightingsImpl allSightings = new SightingsImpl();
		allSightings.getSightingsPerPerson().putAll(readNetworkAndSightings(config));
		
		zones.buildCells();
		PopulationFromSightings.createPopulationWithTwoPlansEach(scenario, new CellularCoverageLinkToZoneResolver(zones, scenario.getNetwork()), allSightings);



		
		PopulationFromSightings.preparePopulation(scenario, new CellularCoverageLinkToZoneResolver(zones, scenario.getNetwork()), allSightings);
		

		runStatistics();
		
		return scenario;
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

		allSightings.putAll(readSightings("2011-12-07 00:00:00", D4DConsts.D4D_DIR + "SET2TSV/POS_SAMPLE_0.TSV", 0));
		allSightings.putAll(readSightings("2011-12-19 00:00:00", D4DConsts.D4D_DIR + "SET2TSV/POS_SAMPLE_1.TSV", 1));
		allSightings.putAll(readSightings("2012-01-02 00:00:00", D4DConsts.D4D_DIR + "SET2TSV/POS_SAMPLE_2.TSV", 2));
		allSightings.putAll(readSightings("2012-01-16 00:00:00", D4DConsts.D4D_DIR + "SET2TSV/POS_SAMPLE_3.TSV", 3));
		return allSightings;
	}

	void readPosts() {
		final Map<String, CellTower> cellTowerMap = new HashMap<String, CellTower>();
		TabularFileParser tfp = new TabularFileParser();
		TabularFileParserConfig tabularFileParserConfig = new TabularFileParserConfig();
		tabularFileParserConfig.setFileName(D4DConsts.D4D_DIR + "ANT_POS.TSV");
		tabularFileParserConfig.setDelimiterRegex("\t");
		tfp.parse(tabularFileParserConfig, new TabularFileHandler() {

			@Override
			public void startRow(String[] row) {
				Coord longLat = new Coord(Double.parseDouble(row[1]), Double.parseDouble(row[2]));
				Coord coord = D4DConsts.ct.transform(longLat);
				if (Double.isNaN(coord.getX()) || Double.isNaN(coord.getY())) {
					throw new RuntimeException("Bad latlong: " + coord);
				}
				String cellTowerId = row[0];
				cellTowerMap.put(cellTowerId, new CellTower(cellTowerId,coord));
			}
		});
		
		zones = new Zones(cellTowerMap);
		// getLinksCrossingCells();
	}

	



	private void readNetwork() {
		String filename = D4DConsts.WORK_DIR + "network-simplified.xml";
		new MatsimNetworkReader(scenario).readFile(filename);
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

				Id<Person> personId = Id.create(row[0] + "_" + Integer.toString(populationIdSuffix), Person.class);
				String cellTowerId = row[2];


				long timeInSeconds = (sightingTime.getMillis() - beginning.getMillis()) / 1000;



				if (!cellTowerId.equals("-1")) {
					CellTower cellTower = zones.cellTowers.get(cellTowerId);
					if (cellTower != null) {
						if (interestedInTime(timeInSeconds)) {
							List<Sighting> sightingsOfPerson = sightings.get(personId);
							if (sightingsOfPerson == null) {
								sightingsOfPerson = new ArrayList<Sighting>();
								sightings.put(personId, sightingsOfPerson);
							}

							Sighting sighting = new Sighting(personId, timeInSeconds, cellTowerId);
							sightingsOfPerson.add(sighting);
							cellTower.nSightings++;
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



	private void runStatistics() {
		System.out.println(scenario.getPopulation().getPersons().size());
		new InitialStatistics("").run(scenario.getPopulation());

        Population cityPopulation = PopulationUtils.createPopulation(scenario.getConfig(), scenario.getNetwork());
        Population nonCityPopulation = PopulationUtils.createPopulation(scenario.getConfig(), scenario.getNetwork());

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
		new PopulationWriter(cityPopulation, null).write(D4DConsts.WORK_DIR + "population-capital-only.xml");
		System.out.println(nonCityPopulation.getPersons().size());
		new InitialStatistics("-countryside-only").run(nonCityPopulation);
		new PopulationWriter(nonCityPopulation, null).write(D4DConsts.WORK_DIR + "population-countryside-only.xml");
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
		CreatePopulation scenarioReader = new CreatePopulation();
		Scenario scenario = scenarioReader.readScenario(config);
		new PopulationWriter(scenario.getPopulation(), null).write(D4DConsts.WORK_DIR + "population.xml");
		int sampleSize = scenario.getPopulation().getPersons().size();
		System.out.println("Created " + sampleSize + " people. That's a " + ((double) sampleSize) / POP_REAL  + " sample.");
		scenarioReader.writeToShapefile();
	}

	public Zones getCellTowers() {
		return zones;
	}
	

	private void writeToShapefile() {
		String baseFilename = D4DConsts.WORK_DIR + "cells.shp";

		Collection<SimpleFeature> features = new ArrayList<SimpleFeature>();
		SimpleFeatureType type;

		try {
			type = DataUtilities.createType("Antenna",
					"ant:Polygon, ID:String, nSightings:Integer" 
					);

			CoordinateReferenceSystem crs = MGC.getCRS(D4DConsts.TARGET_CRS);
			type = DataUtilities.createSubType( type, null, crs );
			for (CellTower cellTower : zones.cellTowers.values()) {
				String cellTowerIdString = cellTower.id;
				SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(type);
				featureBuilder.add(zones.getCell(cellTowerIdString));
				featureBuilder.add(cellTowerIdString);
				featureBuilder.add(cellTower.nSightings);
				features.add(featureBuilder.buildFeature(cellTowerIdString));
			}
		} catch (SchemaException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ShapeFileWriter.writeGeometries(features, baseFilename);

	}

	
}
