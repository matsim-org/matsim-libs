/* *********************************************************************** *
 * project: org.matsim.*
 * FakeTravelTimeCost.java
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

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;

//matsim imports

/**
 * Class representing a Node in the time-expandet network with his own successor
 *
 */
public class TimeNode {

	/**
	 * the node
	 */
	private Node node;
	
	/**
	 * the time, where we look at the node
	 */
	private int time;
	
	/**
	 * successor of the node at the time
	 */
	private Link succ;
	
	/**
	 * default Constructor setting the Arguments
	 * @param node Node used
	 * @param time time
	 * @param succ successor of node in a path
	 */
	TimeNode(Node node, int time, Link succ){
		this.node = node;
		this.time = time;
		this.succ = succ;
	}
	
	/**
	 * default Constructor setting the Arguments
	 */
	TimeNode(){
		this.node = null;
		this.time = -1;
		this.succ = null;
	}
	
	/**
	 * Getter for the node
	 */
	public Node getNode(){
		return node;
	}
	
	/**
	 * Setter for the node
	 * @param node Node used
	 */
	public void setNode(Node node){
		this.node = node;
	}
	
	/**
	 * Getter for the time, where we look at the node
	 */
	public int getTime(){
		return time;
	}
	
	/**
	 * Setter for the time, where we look at the node
	 * @param time time
	 */
	public void setTime(int time){
		this.time = time;
	}
	
	/**
	 * Getter for the successor link
	 */
	public Link getSuccessorLink(){
		return succ;
	}
	
	/**
	 * Setter for the successor link
	 * @param succ successor link of the node
	 */
	public void setSuccessorLink(Link succ){
		this.succ = succ;
	}
	
	/**
	 * Getter for the successor-node
	 */
	public Node getSuccessorNode(){
		if(succ.getToNode().equals(node)){
			return succ.getFromNode();
		}
		else if(succ.getFromNode().equals(node)){
			return succ.getToNode();
		}
		return null;
	}
	
	/**
	 * print the TimeNode
	 */
	public void print(){
		System.out.println("Node: " + node.getId());
		System.out.println("Time: " + time);
		if(succ == null){
			System.out.println("Successor-Link: null");
		}
		else{
			System.out.println("Successor-Link: (" + succ.getFromNode().getId() + "," + succ.getToNode().getId() + ")");
		}
	}
}
