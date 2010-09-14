/* *********************************************************************** *
 * project: org.matsim.*
 * ReadNetwork.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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


package playground.rost.eaflow.ea_flow;

//import java.util.Map;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkReaderMatsimV1;

public class ReadNetwork {

	public static void main(String[] args) {
		 System.out.println("Ich lebe");
		 ScenarioImpl scenario = new ScenarioImpl();
		 NetworkImpl network = scenario.getNetwork();
		 NetworkReaderMatsimV1 networkReader = new NetworkReaderMatsimV1(scenario);

		 networkReader.readFile("./examples/equil/network.xml");

		 //Map<IdI, ? extends Node> nodes = network.getNodes();
		 //Map<IdI, ? extends Link> links = network.getLinks();

		 //for (Node node : nodes.values()) {


		 for (Node node : network.getNodes().values()) {
		   System.out.println(node.toString() + "\n");
		 }

		 for (Link link : network.getLinks().values()) {
		    System.out.println(link.toString() + "\n");

		 }

		 System.out.println("... immer noch!\n");

	}

}
