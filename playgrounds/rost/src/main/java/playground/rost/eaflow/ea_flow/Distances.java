/* *********************************************************************** *
 * project: org.matsim.*
 * Distances.java
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

//matsim imports
import java.util.Hashtable;

import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkImpl;


public class Distances {

	/**----------- Fields -------------*/
	final NetworkImpl network;
	
	private Hashtable<Node, Integer> distLables = new Hashtable<Node, Integer>();
	
	/**----------- Constructor -------------*/
	
	public Distances(final NetworkImpl network){
		this.network = network;
		for(Node node : network.getNodes().values()){
			distLables.put(node, Integer.MAX_VALUE);
		}
	}

	public Distances(final NetworkImpl network, Node specialNode){
		this.network = network;
		for(Node node : network.getNodes().values()){
			if(node == specialNode){
				distLables.put(node, 0);
			}
			else{
				distLables.put(node, Integer.MAX_VALUE);
			}
		}
	}
	
	/**----------- Getter and Setter -------------*/
	
	// returns false if time > currend distLabel for the node
	public boolean setDistance(Node node, int time){
		if(time > distLables.get(node)){
			System.out.println("Distances konnten oder sollten nicht gesetzt werden.");
			return false;
		}
		distLables.put(node, time);
		return true;
	}
	
	
	public Integer getDistance(Node node){
		return this.getMinTime(node);
	}
	
	
	/**----------- Other Methods -------------*/
	
	public Integer getMinTime(Node node){
		return distLables.get(node);
	}

	public boolean isReachable(Node node, int time){
		if (distLables.get(node) <= time){
			return true;
		}
		return false;
	}
	
	
	/**----------- Print -------------*/
	
	public void printAll(){
		for (Node n : network.getNodes().values()){
			print(n);
		}
		System.out.println();
	}
	
	public void print(Node node){
		System.out.print("Node " + node.getId() + " ist ");
		if (distLables.get(node).equals(Integer.MAX_VALUE)){
			System.out.println("nicht erreichbar.");
		}
		else{
			System.out.println("erreichbar ab Zeitpunkt " + distLables.get(node));
		}
	}
	
	public void print(int time){
		System.out.println("Folgende Knoten sint zum Zeitpunkt " + time + " erreichbar:");
		for (Node node : network.getNodes().values()){
			if(distLables.get(node) == time){
				System.out.println(node.getId());
			}
		}
		System.out.println();
	}
}
