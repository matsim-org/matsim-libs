/* *********************************************************************** *
 * project: org.matsim.*
 * RndNetworkGenerator.java
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
import org.matsim.plans.MatsimPlansReader;
import org.matsim.plans.Person;
import org.matsim.plans.Plans;
import org.matsim.utils.geometry.CoordI;
import org.xml.sax.SAXException;

import edu.uci.ics.jung.graph.DirectedGraph;
import edu.uci.ics.jung.graph.Edge;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.impl.DirectedSparseEdge;
import edu.uci.ics.jung.graph.impl.DirectedSparseGraph;
import edu.uci.ics.jung.graph.impl.DirectedSparseVertex;
import edu.uci.ics.jung.graph.impl.UndirectedSparseGraph;
import edu.uci.ics.jung.utils.UserDataContainer;

public class RndNetworkGenerator {

	private static final String PERSON_KEY = "person";
	
	private final static UserDataContainer.CopyAction.Shared copyAct = new UserDataContainer.CopyAction.Shared();
	
//	private final static double dist_norm = 20000;
	
	private final static double alpha = 0.01;
	
	private final static double beta = 1;
	
	private final static int seed = 815;
	
	private final static String ID_KEY = "ID";
	
	private final static String DIST_KEY = "dist";
	
	public static DirectedGraph createGraph(Plans plans) {
		Random rnd = new Random(seed);
		DirectedSparseGraph g = new DirectedSparseGraph();
		
		int cnt = 0;
		for(Person p : plans.getPersons().values()) {
			DirectedSparseVertex v = (DirectedSparseVertex) g.addVertex(new DirectedSparseVertex());
			v.addUserDatum(PERSON_KEY, p, copyAct);
			cnt++;
		}
		System.out.println("Added "+cnt+" vertices.");
		
		cnt = 0;
		Set v2set = new HashSet(g.getVertices());
		for(Object v1 : g.getVertices()) {
			for(Object v2 : v2set) {
				if(!v1.equals(v2)) {
					Person p1 = (Person) ((DirectedSparseVertex)v1).getUserDatum(PERSON_KEY);
					Person p2 = (Person) ((DirectedSparseVertex)v2).getUserDatum(PERSON_KEY);
					CoordI c1 = p1.getSelectedPlan().getFirstActivity().getCoord();
					CoordI c2 = p2.getSelectedPlan().getFirstActivity().getCoord();
					
					double dist = c1.calcDistance(c2)/1000;
					
					double F = alpha*beta*Math.pow(dist,beta-1)*Math.exp(-alpha*Math.pow(dist,beta));
					
					rnd.nextDouble();
					if(rnd.nextDouble() < F) {
						DirectedSparseEdge e = new DirectedSparseEdge((DirectedSparseVertex)v1, (DirectedSparseVertex)v2);
						e.addUserDatum(DIST_KEY, dist, copyAct);
						g.addEdge(e);
						cnt++;
						if(cnt % 100 == 0)
							System.out.println("Added " + cnt + " edges.");
					}
				}
			}
			v2set.remove(v1);
		}
		
		System.out.println("Graph density is "+ g.numEdges()/(g.numVertices() * (g.numVertices()-1)));
		return g;
	}
	
	private static Plans loadPopulation(String file) {
		Plans plans = new Plans();
		MatsimPlansReader reader = new MatsimPlansReader(plans);
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
	
	private static void dump(Graph g, String outDir) throws FileNotFoundException, IOException {
		System.out.println("Writing vertices...");
		BufferedWriter writer = org.matsim.utils.io.IOUtils.getBufferedWriter(outDir + "vertices.txt");
		writer.write("id\tperson\tx\ty\tdist");
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
			
			CoordI c = p.getSelectedPlan().getFirstActivity().getCoord();
			writer.write(String.valueOf(c.getX()));
			writer.write("\t");
			writer.write(String.valueOf(c.getY()));
			writer.newLine();
		}
		writer.close();
		
		System.out.println("Writing edges...");
		writer = org.matsim.utils.io.IOUtils.getBufferedWriter(outDir + "edges.txt");
		writer.write("id\tfrom\tto");
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
		Plans plans = loadPopulation("/Users/fearonni/vsp-cvs/studies/DA-Illenberger/data/berlin/plans/plans.sample0.1.xml");
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
}
