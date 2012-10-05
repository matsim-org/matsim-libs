/* *********************************************************************** *
 * project: org.matsim.*
 * LangeStreckeSzenario													   *
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

package playground.ssix;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.scenario.ScenarioUtils;

public class DreieckStreckeSzenario {

	/**
	 * @param args
	 */
	
	private Scenario scenario;
	
	private double length;
	private int subdivisionFactor;//all sides of the triangle will be divided into subdivisionFactor links
	private int networkCapacity;//the capacity all links of the network will have
	
	public DreieckStreckeSzenario(double length, int subdivisionFactor, int networkCapacity){
		this.length = length;
		this.subdivisionFactor = subdivisionFactor;
		this.networkCapacity = networkCapacity;

		Config config = ConfigUtils.createConfig();
		config.addQSimConfigGroup(new QSimConfigGroup());
		this.scenario = ScenarioUtils.createScenario(config);
	}
	
	public static void main(String[] args) {
		new DreieckStreckeSzenario(5000.0,2,500).run();
	}
	
	public void run(){
		fillNetworkData();
		createPopulation((long)1000,3);
		//TODO
	}

	private void fillNetworkData(){
		Network network = scenario.getNetwork();
		int capMax = 10*networkCapacity;
		
		//NODES
		//nodes of the triangle base
		for (int i = 0; i<subdivisionFactor+1; i++){
			double x=0, y=0;
			if (i>0){
				for (int j=0; j<i; j++){
					x += this.length/this.subdivisionFactor;
				}
			}
			Coord coord = scenario.createCoord(x, y);
			Id id = new IdImpl((long)i);
			
			Node node = scenario.getNetwork().getFactory().createNode(id, coord);
			network.addNode(node);	
		}
		//nodes of the triangle right side
		for (int i = 0; i<this.subdivisionFactor; i++){
			double x = this.length - (this.length/(2*this.subdivisionFactor))*(i+1);
			double y = Math.sqrt(3.)*(this.length-x);
			Coord coord = scenario.createCoord(x, y);
			Id id = new IdImpl((long)(this.subdivisionFactor+i+1));
			
			Node node = scenario.getNetwork().getFactory().createNode(id, coord);
			network.addNode(node);
		}
		//nodes of the triangle left side
		for (int i = 0; i<this.subdivisionFactor-1; i++){
			double x = this.length/2 - (this.length/(2*this.subdivisionFactor))*(i+1);
			double y = Math.sqrt(3.)*x;
			Coord coord = scenario.createCoord(x, y);
			Id id = new IdImpl((long)(2*this.subdivisionFactor+i+1));
			
			Node node = scenario.getNetwork().getFactory().createNode(id, coord);
			network.addNode(node);
		}
		//additional startNode and endNode for home and work activities
		Coord coord = scenario.createCoord(-50.0, 0.0);
		Id startId = new IdImpl(-1);
		Node startNode = scenario.getNetwork().getFactory().createNode(startId, coord);
		network.addNode(startNode);
		coord = scenario.createCoord(this.length+50.0, 0.0);
		Id endId = new IdImpl(3*this.subdivisionFactor);
		Node endNode = scenario.getNetwork().getFactory().createNode(endId, coord);
		network.addNode(endNode);
		
		//LINKS
		//all triangle links
		for (int i = 0; i<3*this.subdivisionFactor; i++){
			Id idFrom = new IdImpl((long)i);
			Id idTo;
			if (i != 3*this.subdivisionFactor-1)
				idTo = new IdImpl((long)(i+1));
			else
				idTo = new IdImpl(0);
			Node from = network.getNodes().get(idFrom);
			Node to = network.getNodes().get(idTo);
			
			Link link = this.scenario.getNetwork().getFactory().createLink(idFrom, from, to);
			link.setCapacity(this.networkCapacity);
			link.setFreespeed(50./3.6);
			link.setLength(calculateLength(from,to));
			link.setNumberOfLanes(1.);
			network.addLink(link);
		}
		//additional startLink and endLink for home and work activities
		Link startLink = this.scenario.getNetwork().getFactory().createLink(startId, startNode, this.scenario.getNetwork().getNodes().get(new IdImpl(0)));
		startLink.setCapacity(capMax);
		startLink.setFreespeed(50./3.6);
		startLink.setLength(50.);
		startLink.setNumberOfLanes(1.);
		network.addLink(startLink);
		Link endLink = this.scenario.getNetwork().getFactory().createLink(endId, this.scenario.getNetwork().getNodes().get(new IdImpl(this.subdivisionFactor)), endNode);
		endLink.setCapacity(capMax);
		endLink.setFreespeed(50./3.6);
		endLink.setLength(50.);
		endLink.setNumberOfLanes(1.);
		network.addLink(endLink);
		
		//check with .xml and Visualizer
		//NetworkWriter writer = new NetworkWriter(network);
		//writer.write("./output/dreieck_network.xml");
	}
	
	private void createPopulation(long numberOfPeople, int sekundenfrequenz){
		//TODO
	}
	
	private double calculateLength(Node from, Node to){
		double x1 = from.getCoord().getX();
		double y1 = from.getCoord().getY();
		double x2 = to.getCoord().getX();
		double y2 = to.getCoord().getY();
		return Math.sqrt((x2-x1)*(x2-x1)+(y2-y1)*(y2-y1));
	}
}
