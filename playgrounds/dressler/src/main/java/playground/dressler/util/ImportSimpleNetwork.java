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


package playground.dressler.util;

//import java.util.Map;

//java imports
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.utils.geometry.CoordImpl;

public class ImportSimpleNetwork {

	private NetworkLayer _network = null;
	private HashMap<Node, Integer> _demands = null;
	private HashMap<Integer, Node> _newnodes = null;
	private String _filename; 
	/**
	 * @param args
	 * @throws IOException 
	 * @throws IOException 
	 */
	
	public ImportSimpleNetwork(final String filename) {
		_filename = filename;
	}
	
	public NetworkLayer getNetwork() throws IOException {
		if (_network == null) {
		  readSimpleNetwork(_filename);
		}
		return _network;
	}
	
	public HashMap<Node, Integer> getDemands () throws IOException {
		if (_demands == null) {
		  readSimpleNetwork(_filename);
		}
		return _demands;
	}
	
	private void readSimpleNetwork(final String filename) throws IOException{
		BufferedReader in = new BufferedReader(new FileReader(filename));
		
		_network = new NetworkLayer();
		_network.setCapacityPeriod(1.0);
		_demands = new HashMap<Node, Integer>();
		_newnodes = new HashMap<Integer,Node>();
		
		String inline = null;
		
		// read nodes
		while ((inline = in.readLine()) != null) {
			String[] line = inline.split(" ");			
			if (line[0].equals("S") || line[0].equals("T")) {
			  int i = Integer.valueOf(line[1].trim());
			  addNodeIfNecessary(i);
			} else if (line[0].equals("E")) {
				int i;
				i = Integer.valueOf(line[1].trim());
				addNodeIfNecessary(i);
				
				i = Integer.valueOf(line[2].trim());
				addNodeIfNecessary(i);
			} else if (line[0].equals("V")) {
				// this line has coordinates.
				// update them if necessary
				
				int i = Integer.valueOf(line[1].trim());
				addNodeIfNecessary(i);
				double x = Double.valueOf(line[3].trim());
				double y = Double.valueOf(line[4].trim());
				((NodeImpl) _newnodes.get(i)).setCoord((Coord) new CoordImpl(x, y)); 
			}
		}
		in.close();
		
		// read edges
		in = new BufferedReader(new FileReader(filename));
		
		int count = 0;
		while ((inline = in.readLine()) != null) {
			String[] line = inline.split(" ");			
			if (line[0].equals("E")) {
				count++;
				int v = Integer.valueOf(line[1].trim());
				int w = Integer.valueOf(line[2].trim());
				int u = Integer.valueOf(line[3].trim());
				int t = Integer.valueOf(line[4].trim());
//				Link link = new LinkImpl(new IdImpl(count), _newnodes.get(v), _newnodes.get(w), _network, t, 1.0, u, 1);
	
				System.err.println( "I replaced the (deprecated) LinkImpl constructor by a factory.  Pls check if things are still working. Kai") ;
				System.exit(-1) ;
				Link link = _network.getFactory().createLink( new IdImpl(count), _newnodes.get(v).getId(), _newnodes.get(w).getId() ) ;
				link.setLength(t);
				link.setFreespeed(1.0) ;
				link.setCapacity(u) ;
				link.setNumberOfLanes(1) ;
				
				_network.addLink(link);
			}
		}		
		in.close();
		
		// read demands
		
		// read edges
		in = new BufferedReader(new FileReader(filename));
		
		while ((inline = in.readLine()) != null) {
			String[] line = inline.split(" ");			
			if (line[0].equals("S")) {
				int v = Integer.valueOf(line[1].trim());
				int d = Integer.valueOf(line[2].trim());
				_demands.put(_newnodes.get(v),d);
			} else if (line[0].equals("T")) {
				int v = Integer.valueOf(line[1].trim());
				int d = Integer.valueOf(line[2].trim());
				_demands.put(_newnodes.get(v),-d);
			} else if (line[0].equals("V")) {
				int v = Integer.valueOf(line[1].trim());
				int d = Integer.valueOf(line[2].trim());
				_demands.put(_newnodes.get(v),d);
			}
		}		
		in.close();
		
	}
	
	private void addNodeIfNecessary(int v) {
		if (!_newnodes.containsKey(v)) {
			Coord coord = new CoordImpl(0.0, 0.0);
			NodeImpl node = new NodeImpl(new IdImpl(v));
			node.setCoord(coord);			
			_newnodes.put(v, node);
			_network.addNode(node);
		}
	}

}
