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
package playground.agarwalamit.utils.networkProcessing;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.PointFeatureFactory;
import org.matsim.core.utils.gis.PolylineFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * @author amit
 */
public class MatsimNetwork2Shape {

	private static final String CLUTER_DESKTOP_LOCATION = "/Users/aagarwal/Desktop/ils/agarwal/siouxFalls/";
	private static final String MATSIM_NETWORK = CLUTER_DESKTOP_LOCATION+"/output/run0/output_network.xml.gz";
	private static final String OUT_SHAPE_LOCATION = "./clusterOutput/networkShape/";
	
	public static void main(String[] args) {
		Config config = ConfigUtils.createConfig();
        config.network().setInputFile(MATSIM_NETWORK);
        Scenario scenario = ScenarioUtils.loadScenario(config);
        Network network = scenario.getNetwork();
        
        CoordinateReferenceSystem crs = MGC.getCRS("EPSG:3459");//i have tried 2842 3659,2455  32035 and  32135
        Collection<SimpleFeature> features = new ArrayList<>();
        PolylineFeatureFactory linkFactory = new PolylineFeatureFactory.Builder().setCrs(crs).
        		setName("link").
                addAttribute("ID", String.class).
                addAttribute("fromID", String.class).
                addAttribute("toID", String.class).
                addAttribute("length", Double.class).
                addAttribute("type", String.class).
                addAttribute("capacity", Double.class).
                addAttribute("freespeed", Double.class).
                create();
        for(Link link :network.getLinks().values()){
        	Coordinate fromNodeCoordinate = new Coordinate(link.getFromNode().getCoord().getX(), link.getFromNode().getCoord().getY());
        	Coordinate toNodeCoordinate = new Coordinate(link.getToNode().getCoord().getX(), link.getToNode().getCoord().getY());
        	Coordinate linkCoordinate = new Coordinate(link.getCoord().getX(), link.getCoord().getY());
        	SimpleFeature ft = linkFactory.createPolyline(new Coordinate [] {fromNodeCoordinate, linkCoordinate, toNodeCoordinate},
					new Object [] {link.getId().toString(), link.getFromNode().getId().toString(),link.getToNode().getId().toString(), link.getLength(), NetworkUtils.getType(
                            link), link.getCapacity(), link.getFreespeed()}, null);
			features.add(ft);
        }
       new File("./clusterOutput/networkShape/").mkdir();
        ShapeFileWriter.writeGeometries(features, OUT_SHAPE_LOCATION+"network_links.shp");
        features = new ArrayList<>();
		PointFeatureFactory nodeFactory = new PointFeatureFactory.Builder().
				setCrs(crs).
				setName("nodes").
				addAttribute("ID", String.class).
				create();

		for (Node node : network.getNodes().values()) {
			SimpleFeature ft = nodeFactory.createPoint(node.getCoord(), new Object[] {node.getId().toString()}, null);
			features.add(ft);
		}
		ShapeFileWriter.writeGeometries(features, OUT_SHAPE_LOCATION+"network_nodes.shp");
	}
}