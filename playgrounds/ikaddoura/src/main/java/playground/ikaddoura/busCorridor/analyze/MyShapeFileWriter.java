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

package playground.ikaddoura.busCorridor.analyze;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.PointFeatureFactory;
import org.matsim.core.utils.gis.PolylineFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;

public class MyShapeFileWriter implements Runnable {
	
	static String networkFile = "../../shared-svn/studies/ihab/busCorridor/input_final/network.xml";
	static String populationFile = "../../shared-svn/studies/ihab/busCorridor/input_final/population.xml";
	
	private GeometryFactory geometryFactory = new GeometryFactory();
	ArrayList<SimpleFeature> FeatureList = new ArrayList<SimpleFeature>();
	
	public static void main(String[] args) {
	MyShapeFileWriter potsdamAnalyse = new MyShapeFileWriter();
	potsdamAnalyse.run();
	}

	@Override
	public void run() {
		
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(networkFile);
		config.plans().setInputFile(populationFile);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		SortedMap<Id,Coord> homeKoordinaten = getHomeKoordinaten(scenario);
		SortedMap<Id,Coord> workKoordinaten = getWorkKoordinaten(scenario);
	
	writeShapeFilePoints(scenario, homeKoordinaten, "../../shared-svn/studies/ihab/busCorridor/output_analyse/pointShapeFile_home.shp");
	writeShapeFilePoints(scenario, workKoordinaten, "../../shared-svn/studies/ihab/busCorridor/output_analyse/pointShapeFile_work.shp");
	writeShapeFileLines(scenario);

	}

	private SortedMap<Id, Coord> getWorkKoordinaten(Scenario scenario) {
		SortedMap<Id,Coord> id2koordinaten = new TreeMap<Id,Coord>();
		for(Person person : scenario.getPopulation().getPersons().values()){
			for (PlanElement pE : person.getSelectedPlan().getPlanElements()){
				if (pE instanceof Activity){
					Activity act = (Activity) pE;
					if (act.getType().equals("work")){
						Coord coord = act.getCoord();
						id2koordinaten.put(person.getId(), coord);
					}
					else {}
				}
			}
		}
		return id2koordinaten;
	}

	private SortedMap<Id, Coord> getHomeKoordinaten(Scenario scenario) {
		SortedMap<Id,Coord> id2koordinaten = new TreeMap<Id,Coord>();
		for(Person person : scenario.getPopulation().getPersons().values()){
			for (PlanElement pE : person.getSelectedPlan().getPlanElements()){
				if (pE instanceof Activity){
					Activity act = (Activity) pE;
					if (act.getType().equals("home")){
						Coord coord = act.getCoord();
						id2koordinaten.put(person.getId(), coord);
					}
					else {}
				}
			}
		}
		return id2koordinaten;
	}
	
	private void writeShapeFileLines(Scenario scenario) {
		PolylineFeatureFactory factory = initFeatureType1();
		Collection<SimpleFeature> features = createFeatures1(scenario, factory);
		ShapeFileWriter.writeGeometries(features, "../../shared-svn/studies/ihab/busCorridor/output_analyse/lineShapeFile.shp");
		System.out.println("ShapeFile geschrieben (Netz)");			
	}
	
	private void writeShapeFilePoints(Scenario scenario, SortedMap<Id,Coord> koordinaten, String outputFile) {
		if (koordinaten.isEmpty()==true){
			System.out.println("Map ist leer!");
		}
		else {
			PointFeatureFactory factory = initFeatureType2();
			Collection<SimpleFeature> features = createFeatures2(scenario, koordinaten, factory);
			ShapeFileWriter.writeGeometries(features,  outputFile);
			System.out.println("ShapeFile geschrieben (Points)");	
		}
	}
	
	private PolylineFeatureFactory initFeatureType1() {
		return new PolylineFeatureFactory.Builder().
				setCrs(MGC.getCRS(TransformationFactory.WGS84_UTM35S)).
				setName("link").
				addAttribute("ID", String.class).
				create();
	}
	
	private PointFeatureFactory initFeatureType2() {
		return new PointFeatureFactory.Builder().
				setCrs(MGC.getCRS(TransformationFactory.WGS84_UTM35S)).
				setName("point").
				addAttribute("PersonID", String.class).
				create();
	}
	
	private Collection<SimpleFeature> createFeatures1(Scenario scenario, PolylineFeatureFactory factory) {
		ArrayList<SimpleFeature> liste = new ArrayList<SimpleFeature>();
		for (Link link : scenario.getNetwork().getLinks().values()){
			liste.add(getFeature1(link, factory));
		}
		return liste;
	}
	
	private Collection<SimpleFeature> createFeatures2(Scenario scenario, SortedMap<Id,Coord> koordinaten, PointFeatureFactory factory) {
		ArrayList<SimpleFeature> liste = new ArrayList<SimpleFeature>();
		for (Entry<Id,Coord> entry : koordinaten.entrySet()){
			liste.add(getFeature2((Coord)entry.getValue(), (Id)entry.getKey(), factory));
		}
		return liste;
	}

	private SimpleFeature getFeature1(Link link, PolylineFeatureFactory factory) {
		return factory.createPolyline(
				new Coordinate[] {MGC.coord2Coordinate(link.getFromNode().getCoord()), MGC.coord2Coordinate(link.getToNode().getCoord())},
				new Object[] { link.getId().toString() },
				link.getId().toString());
	}
	
	private SimpleFeature getFeature2(Coord coord, Id id, PointFeatureFactory factory) {
		return factory.createPoint(coord, new Object[] { id.toString() }, id.toString());
	}
}
