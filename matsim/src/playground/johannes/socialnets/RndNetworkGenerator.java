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

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.config.Config;
import org.matsim.controler.ScenarioData;
import org.matsim.gbl.Gbl;
import org.matsim.plans.Act;
import org.matsim.plans.Person;
import org.matsim.plans.Plans;
import org.matsim.utils.geometry.CoordI;
import org.matsim.utils.geometry.shared.Coord;

import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.impl.UndirectedSparseEdge;
import edu.uci.ics.jung.graph.impl.UndirectedSparseGraph;
import edu.uci.ics.jung.graph.impl.UndirectedSparseVertex;
import edu.uci.ics.jung.io.GraphMLFile;
import edu.uci.ics.jung.io.GraphMLFileHandler;

public class RndNetworkGenerator {
	
	private static final Logger logger = Logger.getLogger(RndNetworkGenerator.class);

	private final static double alpha = 0.01;
	
//	private final static String ID_KEY = "ID";
//	
//	private final static String DIST_KEY = "dist";
	
	@SuppressWarnings("unchecked")
	public static Graph createGraph(Plans plans) {
		logger.info("Generating social network...");
		
		Random rnd = new Random(Gbl.getConfig().global().getRandomSeed());
		UndirectedSparseGraph g = new UndirectedSparseGraph();
		/*
		 * Create a vertex for each person.
		 */
		for(Person p : plans.getPersons().values()) {
			UndirectedSparseVertex v =  new UndirectedSparseVertex();
			g.addVertex(v);
			v.addUserDatum(UserDataKeys.ID, p.getId().toString(), UserDataKeys.COPY_ACT);
			Act act = p.getSelectedPlan().getFirstActivity();
			v.addUserDatum(UserDataKeys.X_COORD, act.getCoord().getX(), UserDataKeys.COPY_ACT);
			v.addUserDatum(UserDataKeys.Y_COORD, act.getCoord().getY(), UserDataKeys.COPY_ACT);
		}
		logger.info(String.format("Created %1$s vertices.", g.numVertices()));
		/*
		 * Insert random ties between persons.
		 */
		Set<Vertex> pendingVertices = new HashSet<Vertex>(g.getVertices());
		for(Object v1 : g.getVertices()) {
			for(Vertex v2 : pendingVertices) {
				if(!v1.equals(v2)) {
					rnd.nextDouble();
					if(rnd.nextDouble() <= getTieProba((Vertex) v1, v2)) {
						UndirectedSparseEdge e = new UndirectedSparseEdge((UndirectedSparseVertex)v1, (UndirectedSparseVertex)v2);
//						e.addUserDatum(DIST_KEY, dist, copyAct);
						g.addEdge(e);
						if(g.numEdges() % 1000 == 0)
							logger.info(String.format("Inserted %1$s edges...", g.numEdges()));
					}
				}
			}
			pendingVertices.remove(v1);
		}
		logger.info(String.format("Inserted %1$s edges.", g.numEdges()));
		logger.info(String.format("Graph density is %1$s.", g.numEdges()/(double)(g.numVertices() * (g.numVertices()-1))));
		
		return g;
	}
	
	private static double getTieProba(Vertex v1, Vertex v2) {
//		Person p1 = (Person) ((UndirectedSparseVertex)v1).getUserDatum(UserDataKeys.PERSON_KEY);
//		Person p2 = (Person) ((UndirectedSparseVertex)v2).getUserDatum(UserDataKeys.PERSON_KEY);
		Double x1 = (Double) v1.getUserDatum(UserDataKeys.X_COORD);
		Double y1 = (Double) v1.getUserDatum(UserDataKeys.Y_COORD);
		CoordI c1 = new Coord(x1,y1);
		Double x2 = (Double) v2.getUserDatum(UserDataKeys.X_COORD);
		Double y2 = (Double) v2.getUserDatum(UserDataKeys.Y_COORD);
		CoordI c2 = new Coord(x2,y2);
		
		double dist = c1.calcDistance(c2)/1000.0;
		
		return alpha * 1/dist;
	}
	
//	private static void dump(Graph g, String outDir) throws FileNotFoundException, IOException {
//		System.out.println("Writing vertices...");
//		BufferedWriter writer = org.matsim.utils.io.IOUtils.getBufferedWriter(outDir + "vertices.txt");
//		writer.write("id\tperson\tx\ty\tdist");
//		writer.newLine();
//		
//		int i = 1;
//		for(Object v : g.getVertices()) {
//			((Vertex)v).addUserDatum(ID_KEY, i, copyAct);
//			writer.write(String.valueOf(i));
//			i++;
//			writer.write("\t");
//			
//			Person p = (Person) ((Vertex)v).getUserDatum(PERSON_KEY);
//			writer.write(p.getId().toString());
//			writer.write("\t");
//			
//			CoordI c = p.getSelectedPlan().getFirstActivity().getCoord();
//			writer.write(String.valueOf(c.getX()));
//			writer.write("\t");
//			writer.write(String.valueOf(c.getY()));
//			writer.newLine();
//		}
//		writer.close();
//		
//		System.out.println("Writing edges...");
//		writer = org.matsim.utils.io.IOUtils.getBufferedWriter(outDir + "edges.txt");
//		writer.write("id\tfrom\tto");
//		writer.newLine();
//		
//		i = 1;
//		for(Object e : g.getEdges()) {
//			writer.write(String.valueOf(i));
//			writer.write("\t");
//			Iterator it = ((Edge)e).getIncidentVertices().iterator();
//			writer.write(((Vertex)it.next()).getUserDatum(ID_KEY).toString());
//			writer.write("\t");
//			writer.write(((Vertex)it.next()).getUserDatum(ID_KEY).toString());
//			writer.write("\t");
//			writer.write(String.valueOf(((Edge)e).getUserDatum(DIST_KEY)));
//			writer.newLine();
//			i++;
//		}
//		writer.close();
//	}
	
	public static void main(String args[]) {
		Config config = Gbl.createConfig(args);
		ScenarioData data = new ScenarioData(config);
		
		Plans plans = data.getPopulation();
		Graph g = createGraph(plans);
		GraphMLFileHandler gmlHandler = new PersonGraphMLFileHandler();
		GraphMLFile gmlFile = new GraphMLFile(gmlHandler);
		logger.info("Saving social network...");
		gmlFile.save(g, config.getParam("randomGraphGenerator", "outputFile"));
		logger.info("Done.");
	}
}
