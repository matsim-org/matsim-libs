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
package playground.droeder.laemmer;

import java.util.Set;
import java.util.TreeSet;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.run.NetworkCleaner;

/**
 * @author droeder
 *
 */
public class ArterialNetGenerator {
	
	private static double[] intersectionDistances = {1000, 720, 280, 740, 360, 460, 510, 635, 420, 390};
	
	public static void main(String[] args){
		createGrid("C:/Users/Daniel/Desktop/test/arterial.xml");
	}
	
	private static void createGrid(String outFile){
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network net = sc.getNetwork();
		NetworkFactory fac = net.getFactory();
		
		// create Nodes
		double currentX = 1;
		double y = 0;
		Node n, base, home;
		Link l;

		// first node
		base = fac.createNode(sc.createId(String.valueOf(currentX) + "-" + String.valueOf(y)), sc.createCoord(currentX, y));
		net.addNode(base);
		n = fac.createNode(sc.createId("W"), sc.createCoord(currentX, y));
		net.addNode(n);
		l = fac.createLink(sc.createId(n.getId().toString() + "_" + base.getId().toString()), n, base);
		net.addLink(l);
		l = fac.createLink(sc.createId(base.getId().toString() + "_" + n.getId().toString()), base, n);
		net.addLink(l);
		
		for(double d: intersectionDistances){
			currentX += d;
			y = 0;
			n = fac.createNode(sc.createId(String.valueOf(currentX) + "-" + String.valueOf(y)), sc.createCoord(currentX, y));
			net.addNode(n);
			l = fac.createLink(sc.createId(n.getId().toString() + "_" + base.getId().toString()), n, base);
			l.setNumberOfLanes(2.);
			net.addLink(l);
			l = fac.createLink(sc.createId(base.getId().toString() + "_" + n.getId().toString()), base, n);
			l.setNumberOfLanes(2.);
			net.addLink(l);
			base = n;

			y = -1000;
			n = fac.createNode(sc.createId(String.valueOf(currentX) + "-" + String.valueOf(y)), sc.createCoord(currentX, y));
			net.addNode(n);
			net.addLink(fac.createLink(sc.createId(n.getId().toString() + "_" + base.getId().toString()), n, base));
			net.addLink(fac.createLink(sc.createId(base.getId().toString() + "_" + n.getId().toString()), base, n));
			
			home = fac.createNode(sc.createId(String.valueOf("S") + "-" + String.valueOf(currentX)), sc.createCoord(currentX, y));
			net.addNode(home);
			net.addLink(fac.createLink(sc.createId(n.getId().toString() + "_" + home.getId().toString()), n, home));
			net.addLink(fac.createLink(sc.createId(home.getId().toString() + "_" + n.getId().toString()), home, n));
			
		
			y = 1000;
			n = fac.createNode(sc.createId(String.valueOf(currentX) + "-" + String.valueOf(y)), sc.createCoord(currentX, y));
			net.addNode(n);
			net.addLink(fac.createLink(sc.createId(n.getId().toString() + "_" + base.getId().toString()), n, base));
			net.addLink(fac.createLink(sc.createId(base.getId().toString() + "_" + n.getId().toString()), base, n));
			
			home = fac.createNode(sc.createId(String.valueOf("N") + "-" + String.valueOf(currentX)), sc.createCoord(currentX, y));
			net.addNode(home);
			net.addLink(fac.createLink(sc.createId(n.getId().toString() + "_" + home.getId().toString()), n, home));
			net.addLink(fac.createLink(sc.createId(home.getId().toString() + "_" + n.getId().toString()), home, n));
		}
		
		
		//last Node
		currentX += 1000;
		y = 0;
		n = fac.createNode(sc.createId(String.valueOf(currentX) + "-" + String.valueOf(y)), sc.createCoord(currentX, y));
		net.addNode(n);
		net.addLink(fac.createLink(sc.createId(n.getId().toString() + "_" + base.getId().toString()), n, base));
		net.addLink(fac.createLink(sc.createId(base.getId().toString() + "_" + n.getId().toString()), base, n));
		base = n;
		
		home = fac.createNode(sc.createId("E"), sc.createCoord(currentX, y));
		net.addNode(home);
		net.addLink(fac.createLink(sc.createId(n.getId().toString() + "_" + home.getId().toString()), n, home));
		net.addLink(fac.createLink(sc.createId(home.getId().toString() + "_" + n.getId().toString()), home, n));
		
		
		Set<String> modes = new TreeSet<String>();
		modes.add(TransportMode.car);
		for (Link link : net.getLinks().values()) {
			Double length = CoordUtils.calcDistance(link.getFromNode().getCoord(), link.getToNode().getCoord());
			Double capacity = 3600.;
			Double freeSpeed = 13.8;
			if(length < 1){
				length = 100.;
				capacity = 9999.;
				freeSpeed = 1000.;
			}
			link.setLength(length);
			link.setCapacity(capacity);
			link.setFreespeed(freeSpeed);
			link.setAllowedModes(modes);
//			link.setNumberOfLanes(1.0);
		}
		new NetworkWriter(net).write(outFile);
		new NetworkCleaner().run(outFile, outFile.split(".xml")[0] + "_clean.xml");
	}

}
