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
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.geotools.data.shapefile.shp.ShapeType;
import org.geotools.data.shapefile.shp.ShapefileWriter;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.api.experimental.facilities.Facility;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.triangulate.VoronoiDiagramBuilder;

public class RunScenario {
	
	double minLong = -4.265;
	double maxLong = -3.671;
	double minLat = 5.175;
	double maxLat = 5.53;
	
	
	boolean filter = true;
	
	private ScenarioImpl scenario;

	public Scenario readScenario(Config config)  {
		scenario = (ScenarioImpl) ScenarioUtils.createScenario(config);
		readNetwork();
		readPosts();
		try {
			readSample();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return scenario;
	}
	
	private CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, "EPSG:3395");
	

	Coord min = ct.transform(new CoordImpl(minLong, minLat));
	Coord max = ct.transform(new CoordImpl(minLat, maxLat));
	private Map<Coordinate, Polygon> siteToCell;
	
	private void readPosts() {
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
				IdImpl facilityId = new IdImpl(row[0]);
				scenario.getActivityFacilities().createFacility(facilityId, coord);
			}
		});
		buildCells();
	}
	
	private void buildCells() {
		GeometryFactory gf = new GeometryFactory();
		VoronoiDiagramBuilder vdb = new VoronoiDiagramBuilder();
		Collection<Coordinate> coords = new ArrayList<Coordinate>();
		for (ActivityFacility facility : scenario.getActivityFacilities().getFacilities().values()) {
			coords.add(coordinate(facility));
		}
		vdb.setSites(coords);
		GeometryCollection diagram = (GeometryCollection) vdb.getDiagram(gf);
		siteToCell = new HashMap<Coordinate, Polygon>();
		for (int i=0; i<diagram.getNumGeometries(); i++) {
			Polygon cell = (Polygon) diagram.getGeometryN(i);
			siteToCell.put((Coordinate) cell.getUserData(), cell); // user data of the cell is set to the Coordinate of the site by VoronoiDiagramBuilder
		}
		FileOutputStream shp = null;
		FileOutputStream shx = null;
		try {
			shp = new FileOutputStream("/Users/zilske/d4d/output/myshape.shp");
			shx = new FileOutputStream("/Users/zilske/d4d/output/myshape.shx");
			ShapefileWriter writer = new ShapefileWriter( shp.getChannel(),shx.getChannel());
			writer.write(diagram, ShapeType.POLYGON);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				shp.close();
				shx.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		voronoiStatistics();
	}

	private Coordinate coordinate(ActivityFacility facility) {
		return new Coordinate(facility.getCoord().getX(), facility.getCoord().getY());
	}
	
	private void readNetwork() {
		new MatsimNetworkReader(scenario).readFile("/Users/zilske/d4d/output/network.xml");
	}

	
	private void readSample() throws FileNotFoundException {
		final DateTimeFormatter dateTimeFormat = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");
		final DateTime beginning = dateTimeFormat.parseDateTime("2011-12-06 00:00:00");
		TabularFileParser tfp = new TabularFileParser();
		TabularFileParserConfig tabularFileParserConfig = new TabularFileParserConfig();
		tabularFileParserConfig.setFileName("/Users/zilske/d4d/POS_SAMPLE_0.TSV");
		tabularFileParserConfig.setDelimiterRegex("\t");
		tfp.parse(tabularFileParserConfig, new TabularFileHandler() {
			
			@Override
			public void startRow(String[] row) {
				String cellTowerId = row[2];
				if (!cellTowerId.equals("-1")) {
					Facility facility = scenario.getActivityFacilities().getFacilities().get(new IdImpl(cellTowerId));
					Coord coord = facility.getCoord();
					Activity activity = scenario.getPopulation().getFactory().createActivityFromCoord("sighting", coord);
					((ActivityImpl) activity).setFacilityId(facility.getId());
					DateTime sighting = dateTimeFormat.parseDateTime(row[1]);
					activity.setEndTime((sighting.getMillis() - beginning.getMillis()) / 1000);
					IdImpl personId = new IdImpl(row[0]);
					Person person = scenario.getPopulation().getPersons().get(personId);
					if (person == null) {
						person = scenario.getPopulation().getFactory().createPerson(personId);
						person.addPlan(scenario.getPopulation().getFactory().createPlan());
						scenario.getPopulation().addPerson(person);
					} else {
						
						Leg leg = scenario.getPopulation().getFactory().createLeg("unknown");
						person.getSelectedPlan().addLeg(leg);
						
						
					}
					person.getSelectedPlan().addActivity(activity);
				}
			}
		});

		// runStatistics();
	}
	
	private void voronoiStatistics() {
		for (ActivityFacility facility : scenario.getActivityFacilities().getFacilities().values()) {
			Polygon cell = siteToCell.get(coordinate(facility));
			System.out.println(cell.getArea());
		}

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
		System.out.println(nonCityPopulation.getPersons().size());
		new InitialStatistics("-countryside-only").run(nonCityPopulation);
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
	
	public static void main(String[] args) {
		Config config = ConfigUtils.createConfig();
		RunScenario scenarioReader = new RunScenario();
		Scenario scenario = scenarioReader.readScenario(config);
	}

}
