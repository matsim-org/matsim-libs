/* *********************************************************************** *
 * project: org.matsim.*
 * RExportTest.java
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
import java.util.Iterator;

import edu.uci.ics.jung.graph.ArchetypeEdge;
import edu.uci.ics.jung.graph.ArchetypeGraph;
import edu.uci.ics.jung.graph.ArchetypeVertex;
import edu.uci.ics.jung.random.generators.ErdosRenyiGenerator;
import edu.uci.ics.jung.utils.UserDataContainer;

/**
 * @author illenberger
 *
 */
public class RExportTest {

	private final static int numVertices = 20000;
	
	private final static double edgeProba = 0.0005;
	
	private final static String ID_KEY = "ID";
	
	private final static String TYPE_KEY = "type";
	
	private final static UserDataContainer.CopyAction.Shared copyAct = new UserDataContainer.CopyAction.Shared();
	
	private final static String VertexFile = "/Users/fearonni/vsp-work/socialnets/devel/rdata/large/vertices.txt";
	
	private final static String EdgeFile = "/Users/fearonni/vsp-work/socialnets/devel/rdata/large/edges.txt";
	
	/**
	 * @param args
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) throws FileNotFoundException, IOException {
		ErdosRenyiGenerator generator = new ErdosRenyiGenerator(numVertices, edgeProba);
		System.out.println("Generating random grapth with " + numVertices + " vertices...");
		ArchetypeGraph g = generator.generateGraph();
		System.out.println("Graph has " + g.getEdges().size() + " edges.");
		
		System.out.println("Adding attributes to vertices...");
		int i = 1;
		for(Object v : g.getVertices()) {
			((ArchetypeVertex)v).addUserDatum(ID_KEY, i, copyAct);
			((ArchetypeVertex)v).addUserDatum(TYPE_KEY, Math.random(), copyAct);
			i++;
		}
		
		System.out.println("Writing vertices...");
		BufferedWriter writer = org.matsim.utils.io.IOUtils.getBufferedWriter(VertexFile);
		writer.write("ID\ttype");
		writer.newLine();
		
		for(Object v : g.getVertices()) {
			writer.write(((ArchetypeVertex)v).getUserDatum(ID_KEY).toString());
			writer.write("\t");
			writer.write(((ArchetypeVertex)v).getUserDatum(TYPE_KEY).toString());
			writer.newLine();
		}
		writer.close();
		
		System.out.println("Writing edges...");
		writer = org.matsim.utils.io.IOUtils.getBufferedWriter(EdgeFile);
		writer.write("ID\tfrom\tto");
		writer.newLine();
		
		i = 1;
		for(Object e : g.getEdges()) {
			writer.write(String.valueOf(i));
			writer.write("\t");
			Iterator it = ((ArchetypeEdge)e).getIncidentVertices().iterator();
			writer.write(((ArchetypeVertex)it.next()).getUserDatum(ID_KEY).toString());
			writer.write("\t");
			writer.write(((ArchetypeVertex)it.next()).getUserDatum(ID_KEY).toString());
			writer.newLine();
			i++;
		}
		writer.close();
		
		System.out.println("Done.");
	}
}
