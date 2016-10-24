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

package playground.jbischoff.av.preparation;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.misc.Time;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

/**
 * @author  jbischoff
 *
 */
public class TinyScenarioPreparator {
	Scenario scenario;
	CoordinateTransformation dest = TransformationFactory.getCoordinateTransformation(TransformationFactory.DHDN_GK4,"EPSG:25833");
	private Geometry geometry;

	public static void main(String[] args) {
		
		TinyScenarioPreparator pm = new TinyScenarioPreparator();
		
		String networkFile = "C:/Users/Joschka/Documents/runs-svn/bvg.run132.25pct/bvg.run132.25pct.output_network.xml.gz";
		String popFile = "C:/Users/Joschka/Documents/runs-svn/bvg.run192.100pct/ITERS/it.100/bvg.run192.100pct.100.plans.selected.xml.gz";
		String newpopFile = "C:/Users/Joschka/Documents/shared-svn/projects/audi_av/population.xml.gz";
		String newNetworkFile = "C:/Users/Joschka/Documents/shared-svn/projects/audi_av/network.xml.gz";
		pm.run(networkFile,popFile,newpopFile,newNetworkFile);
		
		
	}

	public void run(String networkFile, String popFile, String newpopFile, String newNetworkFile) {
		this.geometry = readShapeFileAndExtractGeometry("C:/Users/Joschka/Documents/shared-svn/projects/audi_av/shp/untersuchungsraumAll.shp");
		scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario.getNetwork()).readFile(networkFile);
		new PopulationReader(scenario).readFile(popFile);
		convertNetwork(scenario.getNetwork());
		
		Scenario newScen = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		PopulationFactory factory = newScen.getPopulation().getFactory();
		
		System.out.println("filter");
		for (Person p : scenario.getPopulation().getPersons().values()){
			boolean clone = false;
			Plan plan = p.getSelectedPlan();
			
				for (PlanElement pe : plan.getPlanElements()){
					
					if (pe instanceof Leg){
					Leg leg = (Leg) pe;
					if (leg.getMode().equals("car")){
						Route route = leg.getRoute();
						if (routeInBox(route)){
							route = null;
							leg.setMode("taxi");
							leg.setDepartureTime(Time.UNDEFINED_TIME);
							clone = true;
						}
					
					}
				}
					else if (pe instanceof Activity){
						Coord c = dest.transform(((Activity) pe).getCoord());
//						((Activity) pe).getCoord().setXY(c.getX(), c.getY());
						((Activity) pe).setCoord( c );
					}
				}
			if	(clone){
			Person p2 =  factory.createPerson(p.getId());
			p2.addPlan(plan);
			newScen.getPopulation().addPerson(p2);
			}
			
					
		} 
		new PopulationWriter(newScen.getPopulation()).write(newpopFile);
		new NetworkWriter(scenario.getNetwork()).write(newNetworkFile);
		
	}

	private void convertNetwork(Network network) {
		for (Node node : network.getNodes().values()){
			
			((Node)node).setCoord(dest.transform(node.getCoord()));
			
		}
		
	}

	
	
	private boolean routeInBox(Route route) {
		int i = 0;
		try{
		Coord start = scenario.getNetwork().getLinks().get(route.getStartLinkId()).getCoord();
		Coord end = scenario.getNetwork().getLinks().get(route.getEndLinkId()).getCoord();
		return (coordIsInBox(start)&&coordIsInBox(end));
		} catch (NullPointerException e){
			System.err.println("null  " + i++);
			return false;
		}
		
	}
	
	private boolean startInBox(Route route) {
		Coord start = scenario.getNetwork().getLinks().get(route.getStartLinkId()).getCoord();
		
		return (coordIsInBox(start));
	}
	private boolean endInBox(Route route) {
		Coord end = scenario.getNetwork().getLinks().get(route.getEndLinkId()).getCoord();
		
		return (coordIsInBox(end));
	}

	private boolean coordIsInBox(Coord coord) {
		Geometry g = MGC.coord2Point(coord);
		
		return (this.geometry.contains(g));
	}
	
	static Geometry readShapeFileAndExtractGeometry(String filename){
	
		Geometry geometry = null;	
		for (SimpleFeature ft : ShapeFileReader.getAllFeatures(filename)) {
			
				GeometryFactory geometryFactory= new GeometryFactory();
				WKTReader wktReader = new WKTReader(geometryFactory);

				try {
					geometry = wktReader.read((ft.getAttribute("the_geom")).toString());
					System.out.println("Geometry set.");

				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			 
		}	
		return geometry;
	}
	

}
