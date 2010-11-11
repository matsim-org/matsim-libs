/* *********************************************************************** *
 * project: org.matsim.*
 * NetGenMain.java
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

/**
 * @author Daniel Dressler
 *
 */

package playground.dressler.util.netgen;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkFactoryImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.utils.geometry.CoordImpl;


public class NetGenMain {

	static NetworkImpl network = null;
	static HashMap<Node, Integer> demands = null;

	public boolean _debug =false;


	public void debug(boolean debug){
		_debug=debug;
	}
	
	public static void createGrid(NetGenParams params) {		
		
		Random rand = new Random(params.randseed);
		
		network = NetworkImpl.createNetwork();
		demands = new HashMap<Node, Integer>(); 
		
		Node[][] gridNodes = new Node[params.xdim][params.ydim];			
		
		for (int x = 0; x < params.xdim; x++) {
			for (int y = 0; y < params.ydim; y++) {
				String ids = "x"+x+"y"+y;				
				IdImpl id = new IdImpl(ids);
				CoordImpl coord = new CoordImpl(x,y);
				gridNodes[x][y] = network.createAndAddNode(id, coord);								
			}
		}
		
		// capacities [x][y][0 = horizontal, 1 = vertical]		
		// per second
		double[][][] gridCaps = new double[params.xdim][params.ydim][2];
		
		// lengths [x][y][0 = horizontal, 1 = vertical]
		// for freespeed 1.000
		double[][][] gridLengths = new double[params.xdim][params.ydim][2];
		
		for (int x = 0; x < params.xdim; x++) {
			for (int y = 0; y < params.ydim; y++) {
				for (int dir = 0; dir < 2; dir++) {
					double c = rand.nextGaussian();
					c *= params.capvariance;
					c += params.capmean;
					
					c = Math.round(c);					
					if (c < 0) c = 0;
										
					double l = rand.nextGaussian();
					l *= params.lengthvariance;
					l += params.lengthmean;
					
					l = Math.round(l);
					if (l < 0) l = 0;
					
					gridCaps[x][y][dir] = c;
					gridLengths[x][y][dir] = l;
					//System.out.println(l + " " + c);
				}								
			}
		}

		 // create all outgoing arcs
		for (int x = 0; x < params.xdim; x++) {
			for (int y = 0; y < params.ydim; y++) {
						
				if (x > 0) {
					addLink(network, gridNodes, x,y,x-1,y,gridLengths[x-1][y][0], gridCaps[x-1][y][0]);										
				}
				if (x < params.xdim - 1) {
					addLink(network, gridNodes, x,y,x+1,y,gridLengths[x][y][0], gridCaps[x][y][0]);
				}
				if (y > 0) {
					addLink(network, gridNodes, x,y,x,y-1,gridLengths[x][y-1][1], gridCaps[x][y-1][1]);
				} 
				if (y < params.ydim -1) {
					addLink(network, gridNodes, x,y,x,y+1,gridLengths[x][y][1], gridCaps[x][y][1]);
				}
			}
		}
		
		// create demands
		
		int sinksupply = -87654321; // hopefully infinite
		
		if (params.numbersinks > params.ydim) {
			System.out.println("Too many sinks: " + params.numbersinks + " ydim: " + params.ydim);
			params.numbersinks = params.ydim;
		}
		
		for (int n = 0; n < params.numbersinks; n++) {
			boolean placed = false;
			do {
				int y = rand.nextInt(params.ydim);
				Node sink = gridNodes[0][y];
				if (!demands.containsKey(sink)) {
					demands.put(sink, sinksupply);
					placed = true;
					//System.out.println("Placing sink: x = 0, y = "+y);
				}
			} while (!placed);
		}
		
		// create sources
		List<Node> sources = new ArrayList<Node>();
		HashMap<Node, Boolean> isSource = new HashMap<Node, Boolean>();
		
		if (params.numbersources  == 0) {
			// all except the x = 0 column, where the sinks are
			for (int x = 1; x < params.xdim; x++) {
				for (int y = 1; y < params.ydim; y++) {
					sources.add(gridNodes[x][y]);
					isSource.put(gridNodes[x][y], true);
				}
			}
		} else {
			for (int n = 0; n < params.numbersources; n++) {
				boolean placed = false;
				do {
				    int x = rand.nextInt(params.xdim - 1) + 1;
				    int y = rand.nextInt(params.ydim);
				    
				    Boolean already = isSource.get(gridNodes[x][y]);
				    if (already == null || already == false) {
				      sources.add(gridNodes[x][y]);
				      isSource.put(gridNodes[x][y], true);
				      placed = true;
				    }				    
				} while (!placed);
			}
		}
		
		// place the demands, one uniformly distributed supply unit at a time
		for (int n = 0; n < params.totalsupply; n++) {
			int i = rand.nextInt(sources.size());
			Node source = sources.get(i);
			Integer d = demands.get(source);
			if (d == null) d = 0;
			d += 1;
			demands.put(source, d);						
		}
		
	}
	
	private static void addLink(NetworkImpl network, Node[][] gridNodes, int x1, int y1, int x2, int y2, double length, double cap) {
		Node fromNode = gridNodes[x1][y1];
		Node toNode = gridNodes[x2][y2];
		Id id = new IdImpl("x"+x1+"y"+y1+"x"+x2+"y"+y2);
		//System.out.println(length + " " + cap);
		network.createAndAddLink(id, fromNode, toNode, length, 1.0d, cap, 1.0d);		
	}
	

	public static void writeNetworkDAT (String outfile, NetGenParams params, Network network, HashMap<Node,Integer> demands) {
		FileWriter fout;
        try {
            fout = new FileWriter(outfile);
            BufferedWriter out = new BufferedWriter(fout);
            out.write("% generated by NetGen");
            out.newLine();
            out.write("% Parameters: ");
            out.newLine();
            out.write(params.toString());
            out.newLine();
            out.write("N " + network.getNodes().size());
            out.newLine();

            HashMap<Node,Integer> newNodeNames = new HashMap<Node,Integer>();
            int max = 0;
            for (Node node : network.getNodes().values()) {
            	try {
            		int i = Integer.parseInt(node.getId().toString());
            		if (i > 0) 	newNodeNames.put(node,i);
            		if (i > max) max = i;
            	} catch (Exception except) {

                }
            }

            for (Node node : network.getNodes().values()) {
            	try {
            		int i = Integer.parseInt(node.getId().toString());
            	} catch (Exception except) {
            		max += 1;
                    newNodeNames.put(node, max);
                    out.write("% node " + max + " was '" + node.getId()+ "'");
                    out.newLine();
                }
            }

            for (Node node : network.getNodes().values()) {
            	if (demands.containsKey(node)) {
            		int d = demands.get(node);
            		/*if (d > 0) {
            			out.write("S " + newNodeNames.get(node) + " " + d);
            			out.newLine();
            		}
            		if (d < 0) {
            			out.write("T " + newNodeNames.get(node) + " " + (-d));
            			out.newLine();
            		}   */
            		
            		// write new format with coordinates for pretty pictures
            		out.write("V " + newNodeNames.get(node) + " " + d + " "+ node.getCoord().getX() + " " + node.getCoord().getY());
        			out.newLine();
            	}
            }

            for (Link link : network.getLinks().values()) {
                out.write("E " + (newNodeNames.get(link.getFromNode())) + " " + (newNodeNames.get(link.getToNode())) + " " + (int) link.getCapacity() + " " + (int) link.getLength());
                out.newLine();
            }

            out.close();
            fout.close();
        } catch (Exception except) {
            System.out.println(except.getMessage());
        }
	}

	public static void main(String[] args){
	/*	if (args.length!=3 && args.length!=1 && args.length!=0){
			System.out.println("USAGE: NetworkRounder <inputfile> <outputfile> <cap> OR JUST: NetworkRounder <cap>");
			return;
		} */
		
		String outputfileDAT = null;
		String outputfileMATSIM = null;
		
		//outputfileDAT = "/homes/combi/dressler/V/code/grids/test.dat";
		outputfileDAT = "/homes/combi/dressler/V/code/grids/";
		
		NetGenParams params = new NetGenParams();
						
		params.xdim = 500;
		params.ydim = 50;
//		params.capmean = 3;
//		params.capvariance = params.capmean / 2;
//		params.lengthmean = 20;
//		params.lengthvariance = params.lengthmean / 2;
		params.randseed = 3;
        params.numbersources = 0;
		params.totalsupply = 10000;
		
		if (outputfileDAT.charAt(outputfileDAT.length() - 1) == '/') {
			// only a directory, add default name
			outputfileDAT += params.defaultName() + ".dat";
		}
		
		createGrid(params);
		
		/*for (Link link : network.getLinks().values()) {
			System.out.println(link.getId() + " l:" + link.getLength() + " c:"+ link.getCapacity());
		}*/		
		
		
//		if (outputfile_forEAF != null) {
//		  NetworkImpl network = roundNetwork(inputfile,cap, flowCapacityFactor, lengthFactor, true);
//		  new NetworkWriter(network).write(outputfile_forEAF);
//		}
		if (outputfileDAT != null) {
			  writeNetworkDAT(outputfileDAT, params, network, demands);
		}
	}


}
