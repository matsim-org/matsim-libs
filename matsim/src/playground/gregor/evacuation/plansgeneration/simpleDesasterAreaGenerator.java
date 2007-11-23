/* *********************************************************************** *
 * project: org.matsim.*
 * simpleDesasterAreaGenerator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.gregor.evacuation.plansgeneration;

import java.util.HashMap;
import java.util.Iterator;

import org.matsim.basic.v01.Id;
import org.matsim.config.Config;
import org.matsim.gbl.Gbl;
import org.matsim.mobsim.QueueLink;
import org.matsim.mobsim.QueueNetworkLayer;
import org.matsim.mobsim.QueueNode;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.Node;
import org.matsim.world.Coord;
import org.matsim.world.World;

import playground.gregor.evacuation.EvacuationAreaLink;

public class simpleDesasterAreaGenerator {

	private static int maxX = 0;
	private static int maxY = 0;
	private static int minX = 0;
	private static int minY = 0;
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		World world = Gbl.createWorld();
		Config config = Gbl.createConfig(new String[] {"./configs/evacuationConf.xml"});
		
		QueueNetworkLayer network = new QueueNetworkLayer();
		new MatsimNetworkReader(network).readFile("networks/navteq_network_zurich.xml");
		
		QueueNode pNA = (QueueNode) network.getNode("101453987");
		QueueNode pNB = (QueueNode) network.getNode("101457303");
		
		QueueLink A  = (QueueLink) network.getLinks().get("35718");
		QueueLink B  = (QueueLink) network.getLinks().get("50823");
		maxX = (int)A.getFromNode().getCoord().getX();
		maxY = (int)A.getFromNode().getCoord().getY();
		minX = (int)B.getFromNode().getCoord().getX();
		minY = (int)B.getFromNode().getCoord().getY();
		if (maxX < minX){
			int temp = maxX;
			maxX = minX;
			minX = temp;
		}
		if (maxY < minY){
			int temp = maxY;
			maxY = minY;
			minY = temp;
		}
		double maxDist = pNA.getCoord().calcDistance(pNB.getCoord());
		double latestDeadline = 60;
		
		HashMap<Id,EvacuationAreaLink> links = new HashMap<Id,EvacuationAreaLink>();
		Iterator it = network.getLinks().iterator();
		while (it.hasNext()) {
			QueueLink link = (QueueLink) it.next();
			Coord coord = (Coord)link.getCenter();
			Node a = link.getFromNode();
			Node b = link.getToNode();
			if (isSaveNode(a)) continue;
			if (isSaveNode(b)) continue;
			double distA = coord.calcDistance(pNA.getCoord());
			double distB = coord.calcDistance(pNA.getCoord());
			double deadlineOffset = latestDeadline * Math.min(distA,distB)/maxDist;
			EvacuationAreaLink el = new EvacuationAreaLink((Id)link.getId(),3600.0 * 9 + 45*60+60.0 * deadlineOffset );
			links.put(el.getId(),el);
			
		}
		
		EvacuationNetFileWriter enfw = new EvacuationNetFileWriter(links);
		enfw.writeFile("./networks/evacuationarea_zurich_navteq.xml");

	}

	private static boolean isSaveNode(Node node){
		if (node.getCoord().getX() > maxX) return true;
		if (node.getCoord().getX() < minX) return true;		
		if (node.getCoord().getY() > maxY) return true;
		if (node.getCoord().getY() < minY) return true;
		
		return false;
	}
	
}
