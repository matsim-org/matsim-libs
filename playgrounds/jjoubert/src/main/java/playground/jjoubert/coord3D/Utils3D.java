/* *********************************************************************** *
 * project: org.matsim.*
 * Utils3D.java                                                                        *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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
/**
 * 
 */
package playground.jjoubert.coord3D;

import java.io.BufferedWriter;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.utils.objectattributes.ObjectAttributes;

/**
 * A number of utilities to deal with 3D networks.
 * 
 * @author jwjoubert
 */
public class Utils3D {
	private final static Logger LOG = Logger.getLogger(Utils3D.class);
	
	public static double calculateAngle(Link link){
		if(!link.getFromNode().getCoord().hasZ() || !link.getToNode().getCoord().hasZ()){
			LOG.error("From node: " + link.getFromNode().getCoord().toString());
			LOG.error("To node: " + link.getToNode().getCoord().toString());
			throw new IllegalArgumentException("Cannot calculate grade if both nodes on the link do not have elevation (z) set.");
		}
		
		Coord c1 = link.getFromNode().getCoord();
		Coord c2 = link.getToNode().getCoord();
		
		double length = link.getLength();
		double angle = Math.asin((c2.getZ()-c1.getZ())/length);
		
		return angle;
	}

	public static double calculateGrade(Link link){
		if(!link.getFromNode().getCoord().hasZ() || !link.getToNode().getCoord().hasZ()){
			LOG.error("From node: " + link.getFromNode().getCoord().toString());
			LOG.error("To node: " + link.getToNode().getCoord().toString());
			throw new IllegalArgumentException("Cannot calculate grade if both nodes on the link do not have elevation (z) set.");
		}
		
		Coord c1 = link.getFromNode().getCoord();
		Coord c2 = link.getToNode().getCoord();
		
		double length = link.getLength();
		double grade = (c2.getZ()-c1.getZ())/length;
		
		return grade;
	}
	
	public static Scenario elevateEquilNetwork(){
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		
		/* Read in the basic equil network. */
		new MatsimNetworkReader(sc.getNetwork()).parse("../../matsim/examples/equil/network.xml");
		
		/* Give elevation details to the nodes. The upper nodes are elevated
		 * by increments of 1%, and the lower nodes by increments of -1%. The
		 * result is that the shorter section will have double the grade, but
		 * with the opposite sign. */
		sc.getNetwork().getNodes().get(Id.createNodeId("3")).getCoord().setZ(400.0);
		sc.getNetwork().getNodes().get(Id.createNodeId("4")).getCoord().setZ(300.0);
		sc.getNetwork().getNodes().get(Id.createNodeId("5")).getCoord().setZ(200.0);
		sc.getNetwork().getNodes().get(Id.createNodeId("6")).getCoord().setZ(100.0);
		sc.getNetwork().getNodes().get(Id.createNodeId("8")).getCoord().setZ(-100.0);
		sc.getNetwork().getNodes().get(Id.createNodeId("9")).getCoord().setZ(-200.0);
		sc.getNetwork().getNodes().get(Id.createNodeId("10")).getCoord().setZ(-300.0);
		sc.getNetwork().getNodes().get(Id.createNodeId("11")).getCoord().setZ(-400.0);
		
		/* The remaining nodes MUST have elevation set, so we set them to 0. */
		sc.getNetwork().getNodes().get(Id.createNodeId("1")).getCoord().setZ(0.0);
		sc.getNetwork().getNodes().get(Id.createNodeId("2")).getCoord().setZ(0.0);
		sc.getNetwork().getNodes().get(Id.createNodeId("7")).getCoord().setZ(0.0);
		sc.getNetwork().getNodes().get(Id.createNodeId("12")).getCoord().setZ(0.0);
		sc.getNetwork().getNodes().get(Id.createNodeId("13")).getCoord().setZ(0.0);
		sc.getNetwork().getNodes().get(Id.createNodeId("14")).getCoord().setZ(0.0);
		sc.getNetwork().getNodes().get(Id.createNodeId("15")).getCoord().setZ(0.0);
		
		/* Adjust all links to have 2 lanes. */
		for(Link link : sc.getNetwork().getLinks().values()){
			link.setNumberOfLanes(2.0);

			/* Fix link 6's length to be the same as all the others. */
			if(link.getId().equals(Id.createLinkId("6"))){
				link.setLength(10000);
			}
		}
		
		return sc;
	}

	
 	public static Coord getBottomLeftCoordinate(Network network){
 		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation("WGS84_SA_Albers", "WGS84");
		double minX = Double.POSITIVE_INFINITY;
		double minY = Double.POSITIVE_INFINITY;
		for(Node n : network.getNodes().values()){
			Coord c = ct.transform(n.getCoord());
			minX = Math.min(minX, c.getX());
			minY = Math.min(minY, c.getY());
		}
		return CoordUtils.createCoord(minX, minY);
	}
	

}
