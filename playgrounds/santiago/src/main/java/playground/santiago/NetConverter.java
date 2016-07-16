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

package playground.santiago;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.PointFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.OsmNetworkReader;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;

public class NetConverter {
	
	public void createNetwork(String osmFile, String outputFile){
		
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		Network network = scenario.getNetwork();
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, "EPSG:32719");
		OsmNetworkReader onr = new OsmNetworkReader(network, ct);
		onr.parse(osmFile);
		new NetworkCleaner().run(network);
		
		new NetworkWriter(network).write(outputFile);
		
	}
	
	public void convertTransitSchedule(String file){
		
		Config config = ConfigUtils.createConfig();
		config.scenario().setUseTransit(true);
		config.scenario().setUseVehicles(true);
		Scenario scenario = ScenarioUtils.createScenario(config);
		
		TransitScheduleReader ts = new TransitScheduleReader(scenario);
		ts.readFile(file);
		
		PointFeatureFactory.Builder builder = new PointFeatureFactory.Builder();
		builder.setName("nodes");
		builder.addAttribute("id", String.class);
		PointFeatureFactory factory = builder.create();
		
		List<SimpleFeature> features = new ArrayList<SimpleFeature>();
		
		for(TransitStopFacility stop : scenario.getTransitSchedule().getFacilities().values()){
			features.add(factory.createPoint(MGC.coord2Coordinate(stop.getCoord())));
		}
		
		ShapeFileWriter.writeGeometries(features, "C:/Users/Daniel/Documents/work/shared-svn/studies/countries/cl/Kai_und_Daniel/Visualisierungen/stops.shp");
		
	}
	
	public void convertCoordinates(Network net, String outputFile){
		
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation("EPSG:32719", TransformationFactory.WGS84);
		
		for(Node node : net.getNodes().values()){
			Coord newCoord = ct.transform(node.getCoord());
			((Node)node).setCoord(newCoord);
		}
		
		new NetworkWriter(net).write(outputFile);
		
	}
	
	public void convertNet2Shape(Network net, String crs, String outputFile){
		
		SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
		typeBuilder.setCRS(MGC.getCRS(crs));
		typeBuilder.setName("link feature");
		typeBuilder.add("Line String", LineString.class);
		typeBuilder.add("id", String.class);
		typeBuilder.add("length", Double.class);
		typeBuilder.add("freespeed", Double.class);
		typeBuilder.add("capacity", Double.class);
		SimpleFeatureBuilder builder = new SimpleFeatureBuilder(typeBuilder.buildFeatureType());
		
		List<SimpleFeature> features = new ArrayList<SimpleFeature>();

		for(Link link : net.getLinks().values()){
		
			Coord from = link.getFromNode().getCoord();
			Coord to = link.getToNode().getCoord();
			
			SimpleFeature feature = builder.buildFeature(link.getId().toString(), new Object[]{
				new GeometryFactory().createLineString(new Coordinate[]{
						MGC.coord2Coordinate(from), MGC.coord2Coordinate(to)}),
				link.getId().toString(),
				link.getLength(),
				link.getFreespeed(),
				link.getCapacity()
			});
			features.add(feature);
			
		}
		
		ShapeFileWriter.writeGeometries(features, outputFile);
		
	}
	
	public void convertCounts2Shape(String inputFile, String outputFile){
		
		BufferedReader reader = IOUtils.getBufferedReader(inputFile);

		SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
		typeBuilder.setName("shape");
		typeBuilder.add("type", Point.class);
		typeBuilder.add("id", String.class);
		typeBuilder.add("orientation", String.class);
		SimpleFeatureBuilder builder = new SimpleFeatureBuilder(typeBuilder.buildFeatureType());
		
		List<SimpleFeature> features = new ArrayList<SimpleFeature>();
		
		try{
			
			String line = reader.readLine();
			
			while((line = reader.readLine()) != null){
				
				String[] splittedLine = line.split(";");
				
				String pc = splittedLine[0];
				String sentido = splittedLine[6];
				String x = splittedLine[7];
				x = x.replace(',', '.');
				String y = splittedLine[8];
				y = y.replace(',', '.');
				
				SimpleFeature feature = builder.buildFeature(null, new Object[]{
						new GeometryFactory().createPoint(new Coordinate(Double.parseDouble(x), Double.parseDouble(y))),
						pc,
						sentido
					});
					features.add(feature);
				
			}
			
			reader.close();
			
		} catch(IOException e){
			
		}
		
		ShapeFileWriter.writeGeometries(features, outputFile);
		
	}
	
	public void plans2Shape(Population population, String outputFile){
	
		SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
		typeBuilder.setName("shape");
		typeBuilder.add("geometry", Point.class);
		typeBuilder.add("id", String.class);
		typeBuilder.add("actType", String.class);
		SimpleFeatureBuilder builder = new SimpleFeatureBuilder(typeBuilder.buildFeatureType());
		
		List<SimpleFeature> features = new ArrayList<SimpleFeature>();
		
		for(Person person : population.getPersons().values()){
			
			for(PlanElement pe : person.getSelectedPlan().getPlanElements()){
				
				if(pe instanceof Activity){
					
					Activity act = (Activity)pe;
					Coord coord = act.getCoord();
					
					SimpleFeature feature = builder.buildFeature(null, new Object[]{
							new GeometryFactory().createPoint(new Coordinate(coord.getX(), coord.getY())),
							person.getId().toString(),
							act.getType()
						});
						features.add(feature);
					
				}
				
			}
			
		}
		
		ShapeFileWriter.writeGeometries(features, outputFile);
		
	}

}
