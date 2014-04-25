/* *********************************************************************** *
 * project: org.matsim.*
 * HomeLocationFilter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

import java.util.HashSet;
import java.util.Set;

import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkReaderMatsimV1;
import org.matsim.core.population.PopulationReaderMatsimV5;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;

/**
 * @author dhosse, ikaddoura
 *
 */
public class IKNetworkPopulationWriter {

	private Scenario scenario;
	private SimpleFeatureBuilder builder;
	
	private String networkFile = "/../network.xml";
	private String populationFile = "/../population.xml";
	
	private String networkShapeFile = "/../network.shp";
	private String activitiesShapeFile = "/../activities.shp";

	public static void main(String[] args) {
		
		IKNetworkPopulationWriter main = new IKNetworkPopulationWriter();	
		main.loadScenario();
		
		main.exportNetwork2Shp();
		main.exportActivities2Shp();
		
	}
	
	private void loadScenario() {
		Config config = ConfigUtils.createConfig();
		scenario = ScenarioUtils.createScenario(config);
	}
	
	private void exportActivities2Shp(){
		
		new PopulationReaderMatsimV5(scenario).readFile(populationFile);
		
		SimpleFeatureTypeBuilder tbuilder = new SimpleFeatureTypeBuilder();
		tbuilder.setName("shape");
		tbuilder.add("geometry", Point.class);
		tbuilder.add("type", String.class);
		
		builder = new SimpleFeatureBuilder(tbuilder.buildFeatureType());
		
		Set<SimpleFeature> features = new HashSet<SimpleFeature>();
		
		GeometryFactory gf = new GeometryFactory();
		
		int i = 0;
		
		for(Person p : scenario.getPopulation().getPersons().values()){
			
			for(PlanElement pe : p.getSelectedPlan().getPlanElements()){

				if(pe instanceof Activity){
					
					Activity act = (Activity)pe;
					SimpleFeature feature = builder.buildFeature(Integer.toString(i),new Object[]{
						gf.createPoint(MGC.coord2Coordinate(act.getCoord())),
						act.getType()
					});
					i++;
					features.add(feature);
					
				}
				
			}
			
		}
	
		System.out.println(features.size());
		
		ShapeFileWriter.writeGeometries(features, activitiesShapeFile);
		
	}
	
	private void exportNetwork2Shp(){


		if (this.scenario.getNetwork().getLinks().size() == 0) {
			new NetworkReaderMatsimV1(scenario).parse(this.networkFile);
		}
				
		SimpleFeatureTypeBuilder tbuilder = new SimpleFeatureTypeBuilder();
		tbuilder.setName("shape");
		tbuilder.add("geometry",LineString.class);
		tbuilder.add("id", String.class);
		tbuilder.add("length", Double.class);
		tbuilder.add("capacity", Double.class);
		tbuilder.add("freespeed", Double.class);
		tbuilder.add("modes", String.class);
		
		builder = new SimpleFeatureBuilder(tbuilder.buildFeatureType());
		
		Set<SimpleFeature> features = new HashSet<SimpleFeature>();
		
		GeometryFactory gf = new GeometryFactory();
		
		for(Link link : scenario.getNetwork().getLinks().values()){
			SimpleFeature feature = builder.buildFeature(link.getId().toString(), new Object[]{
					gf.createLineString(new Coordinate[]{
							new Coordinate(MGC.coord2Coordinate(link.getFromNode().getCoord())),
							new Coordinate(MGC.coord2Coordinate(link.getToNode().getCoord()))
					}),
					link.getId(),
					link.getLength(),
					link.getCapacity(),
					link.getFreespeed(),
					link.getAllowedModes().toString()
			});
			features.add(feature);
		}
		
		ShapeFileWriter.writeGeometries(features, networkShapeFile);
		
	}

}
