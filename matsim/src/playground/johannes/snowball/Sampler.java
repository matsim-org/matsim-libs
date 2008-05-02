/* *********************************************************************** *
 * project: org.matsim.*
 * Sampler.java
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

/**
 * 
 */
package playground.johannes.snowball;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.config.Config;
import org.matsim.gbl.Gbl;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.plans.MatsimPlansReader;
import org.matsim.plans.Plans;
import org.xml.sax.SAXException;

import playground.johannes.socialnets.RndNetworkGenerator;

import edu.uci.ics.jung.graph.DirectedEdge;
import edu.uci.ics.jung.graph.DirectedGraph;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.utils.UserDataContainer;

/**
 * @author illenberger
 *
 */
public class Sampler {
	
	private final static UserDataContainer.CopyAction.Shared copyAct = new UserDataContainer.CopyAction.Shared();

	public static final String PERSON_KEY = "person";
	
	private static final String WAVE_KEY = "wave";
	
	private Random rnd = new Random();
	
	public void run(DirectedGraph g, int waves, int initialEgos) {
		Collection<Vertex> egos = selectedIntialEgos(g, initialEgos);
		
		for(int wave = 1; wave <= waves; wave++) {
			System.out.println("Sampling wave "+ wave+"...");
			egos = runWave(egos, wave);
		}
	}

	private Collection<Vertex> selectedIntialEgos(DirectedGraph g, int count) {
		List<Vertex> edges = new LinkedList<Vertex>(g.getVertices());
		Collections.shuffle(edges, rnd);
		return edges.subList(0, count);
	}

	private Collection<Vertex> runWave(Collection<Vertex> egos, int wave) {
		/*
		 * All alters acquired in this wave.
		 */
		Collection<Vertex> allAlters = new LinkedList<Vertex>();
		/*
		 * Expand to each ego's alters.
		 */
		for(Vertex ego : egos) {
			Collection<Vertex> alters = expand(ego);
			allAlters.addAll(alters);
			/*
			 * Add an entry to the alter's list of wave visits.
			 */
			for(Vertex alter : alters) {
				List<Integer> visits = (List<Integer>) alter.getUserDatum(WAVE_KEY);
				if(visits == null) {
					visits = new ArrayList<Integer>();
					alter.addUserDatum(WAVE_KEY, visits, copyAct);
				}
				visits.add(wave);
			}
		}
		
		return allAlters;
	}
	
	private Collection<Vertex> expand(Vertex ego) {
		List<Vertex> sampledAlters = new LinkedList<Vertex>();
		Set<DirectedEdge> ties = ego.getOutEdges();
		
		for(DirectedEdge e : ties) {
			Vertex alter = e.getDest();
			/*
			 * Do not go back to the ego.
			 */
			if(alter.equals(ego)) {
				rnd.nextDouble();
				if(getSampleProba(ego, e.getDest()) > rnd.nextDouble()) {
					sampledAlters.add(alter);
				}
			}
		}
		
		return sampledAlters;
	}
	
	private double getSampleProba(Vertex ego, Vertex alter) {
		return 0.5;
	}
	
	public static void main(String args[]) {
		Config config = new Config();
		config.addCoreModules();
		Gbl.setConfig(config);
		Gbl.createWorld();
		
		System.out.println("Loading network...");
		String networkFile = "/Users/fearonni/vsp-cvs/studies/DA-Illenberger/data/berlin/network/0.net.xml";
		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(networkFile);
		Gbl.getWorld().setNetworkLayer(network);
		
		System.out.println("Loading plans...");
		Plans plans = new Plans();
		MatsimPlansReader reader = new MatsimPlansReader(plans);
		try {
			reader.parse("/Users/fearonni/vsp-cvs/studies/DA-Illenberger/data/berlin/plans/plans.sample0.1.xml");
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Creating graph...");
		DirectedGraph g = RndNetworkGenerator.createGraph(plans);
		
		
		Sampler s = new Sampler();
		s.run(g, 3, 50);
		
		PajekWriter w = new PajekWriter();
		System.out.println("Writing graph...");
		w.write(g, "/Users/fearonni/vsp-work/socialnets/devel/snowball/network.net");
	}
}
