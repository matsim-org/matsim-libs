/* *********************************************************************** *
 * project: org.matsim.*
 * RunMunich.java
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
package playground.benjamin.scenarios.munich.controller;

import java.util.Collection;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

/**
 * @author benjamin
 *
 */
public class RunMunichZone30 {
	public static Logger logger = Logger.getLogger(RunMunichZone30.class);
	
	static String configFile = "../../runs-svn/detEval/test/input/config_munich_1pct_baseCase_newController.xml";
	static String zone30Shape = "../../runs-svn/detEval/test/input/zone30.shp";
	
	public static void main(String[] args) {
		Scenario scenario = ScenarioUtils.loadScenario(ConfigUtils.loadConfig(configFile));
		
		Collection<SimpleFeature> featuresInZone30 = ShapeFileReader.getAllFeatures(zone30Shape);
		setZone30(scenario.getNetwork(), featuresInZone30);
		
		RunMunichZone30Controller controler = new RunMunichZone30Controller(scenario);
		
		Scenario ptRoutingScenario = ScenarioUtils.loadScenario(ConfigUtils.loadConfig(configFile));
		controler.setPtRoutingNetwork(ptRoutingScenario.getNetwork());
		
		controler.run();
	}

	private static void setZone30(Network net, Collection<SimpleFeature> zone30) {
		for(Link link : net.getLinks().values()){
			Id linkId = link.getId();
			Link ll = (Link) net.getLinks().get(linkId);
			if(isLinkInShape(ll, zone30)){
				logger.info("Changing freespeed of link " + ll.getId() + " from " + ll.getFreespeed() + " to 8.3333333334.");
				ll.setFreespeed(30 / 3.6);
				if(ll.getNumberOfLanes() == 1){
					logger.info("Changing type of link " + ll.getId() + " from " + NetworkUtils.getType(ll) + " to 75.");
					NetworkUtils.setType( ll, (String) "75");
					logger.info("Changing capacity of link " + ll.getId() + " from " + ll.getCapacity() + " to 11200.");
					ll.setCapacity(11200);
				}
				else{
					logger.info("Changing type of link " + ll.getId() + " from " + NetworkUtils.getType(ll) + " to 83.");
					NetworkUtils.setType( ll, (String) "83");
					logger.info("Changing capacity of link " + ll.getId() + " from " + ll.getCapacity() + " to 20000.");
					ll.setCapacity(20000);
				}
			}
		}
	}

	private static boolean isLinkInShape(Link link, Collection<SimpleFeature> zone30) {
		boolean isInShape = false;
		Coord coord = link.getCoord();
		GeometryFactory factory = new GeometryFactory();
		Geometry geo = factory.createPoint(new Coordinate(coord.getX(), coord.getY()));
		for(SimpleFeature feature : zone30){
			if(((Geometry) feature.getDefaultGeometry()).contains(geo)){
				isInShape = true;
				break;
			}
		}
		return isInShape;
	}

}