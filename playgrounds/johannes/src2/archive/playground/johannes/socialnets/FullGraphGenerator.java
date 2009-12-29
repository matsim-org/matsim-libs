/* *********************************************************************** *
 * project: org.matsim.*
 * FullGraphGenerator.java
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
package playground.johannes.socialnets;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.config.Config;
import org.matsim.gbl.Gbl;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.population.MatsimPopulationReader;
import org.matsim.population.Person;
import org.matsim.population.Population;
import org.matsim.utils.geometry.Coord;
import org.xml.sax.SAXException;

import edu.uci.ics.jung.graph.Edge;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.UndirectedGraph;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.impl.UndirectedSparseEdge;
import edu.uci.ics.jung.graph.impl.UndirectedSparseGraph;
import edu.uci.ics.jung.graph.impl.UndirectedSparseVertex;
import edu.uci.ics.jung.utils.UserDataContainer;

/**
 * @author illenberger
 *
 */
public class FullGraphGenerator {

private static final String PERSON_KEY = "person";
	
	private final static UserDataContainer.CopyAction.Shared copyAct = new UserDataContainer.CopyAction.Shared();
	
//	private final static double dist_norm = 20000;
	
	private final static double alpha = 0.05;
	
	private final static double beta = 1;
	
	private final static int seed = 815;
	
	private final static String ID_KEY = "ID";
	
	private final static String DIST_KEY = "dist";
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
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
		Population plans = loadPopulation("/Users/fearonni/vsp-cvs/studies/DA-Illenberger/data/berlin/plans/plans.sample0.1.xml");
		System.out.println("Creating graph...");
		Graph g = createGraph(plans);
		
		try {
			System.out.println("Dumping graph...");
			dump(g, "/Users/fearonni/vsp-work/socialnets/devel/rdata/");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static Population loadPopulation(String file) {
		Population plans = new Population();
		MatsimPopulationReader reader = new MatsimPopulationReader(plans);
		try {
			reader.parse(file);
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
		
		return plans;
	}
	
	private static UndirectedGraph createGraph(Population plans) {
		Random rnd = new Random(seed);
		UndirectedSparseGraph g = new UndirectedSparseGraph();
		
		int cnt = 0;
		for(Person p : plans.getPersons().values()) {
			UndirectedSparseVertex v = (UndirectedSparseVertex) g.addVertex(new UndirectedSparseVertex());
			v.addUserDatum(PERSON_KEY, p, copyAct);
			cnt++;
		}
		System.out.println("Added "+cnt+" vertices.");
		
		cnt = 0;
		Set v2set = new HashSet(g.getVertices());
		for(Object v1 : g.getVertices()) {
			for(Object v2 : v2set) {
				if(!v1.equals(v2)) {
					Person p1 = (Person) ((UndirectedSparseVertex)v1).getUserDatum(PERSON_KEY);
					Person p2 = (Person) ((UndirectedSparseVertex)v2).getUserDatum(PERSON_KEY);
					Coord c1 = p1.getSelectedPlan().getFirstActivity().getCoord();
					Coord c2 = p2.getSelectedPlan().getFirstActivity().getCoord();
					
					double dist = c1.calcDistance(c2)/1000;
					
					UndirectedSparseEdge e = new UndirectedSparseEdge((UndirectedSparseVertex)v1, (UndirectedSparseVertex)v2);
					e.addUserDatum(DIST_KEY, dist, copyAct);
					g.addEdge(e);
					cnt++;
					if(cnt % 100 == 0)
						System.out.println("Added " + cnt + " edges.");
					
				}
			}
			v2set.remove(v1);
		}
		
		System.out.println("Graph density is "+ g.numEdges()/(g.numVertices() * (g.numVertices()-1)));
		return g;
	}

	private static void dump(Graph g, String outDir) throws FileNotFoundException, IOException {
		System.out.println("Writing vertices...");
		BufferedWriter writer = org.matsim.utils.io.IOUtils.getBufferedWriter(outDir + "vertices.full.txt");
		writer.write("id\tperson\tx\ty");
		writer.newLine();
		
		int i = 1;
		for(Object v : g.getVertices()) {
			((Vertex)v).addUserDatum(ID_KEY, i, copyAct);
			writer.write(String.valueOf(i));
			i++;
			writer.write("\t");
			
			Person p = (Person) ((Vertex)v).getUserDatum(PERSON_KEY);
			writer.write(p.getId().toString());
			writer.write("\t");
			
			Coord c = p.getSelectedPlan().getFirstActivity().getCoord();
			writer.write(String.valueOf(c.getX()));
			writer.write("\t");
			writer.write(String.valueOf(c.getY()));
			writer.newLine();
		}
		writer.close();
		
		System.out.println("Writing edges...");
		writer = org.matsim.utils.io.IOUtils.getBufferedWriter(outDir + "edges.full.txt");
		writer.write("id\tfrom\tto\tdist");
		writer.newLine();
		
		i = 1;
		for(Object e : g.getEdges()) {
			writer.write(String.valueOf(i));
			writer.write("\t");
			Iterator it = ((Edge)e).getIncidentVertices().iterator();
			writer.write(((Vertex)it.next()).getUserDatum(ID_KEY).toString());
			writer.write("\t");
			writer.write(((Vertex)it.next()).getUserDatum(ID_KEY).toString());
			writer.write("\t");
			writer.write(String.valueOf(((Edge)e).getUserDatum(DIST_KEY)));
			writer.newLine();
			i++;
		}
		writer.close();
	}
}
