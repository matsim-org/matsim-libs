/* *********************************************************************** *
 * project: org.matsim.*
 * PajekNetWriterWrapper.java
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

package playground.jhackney.io;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.TreeMap;

import org.matsim.basic.v01.Id;
import org.matsim.population.Person;
import org.matsim.population.Population;

import playground.jhackney.module.socialnet.SocialNetEdge;
import playground.jhackney.module.socialnet.SocialNetwork;
import edu.uci.ics.jung.graph.Edge;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.decorators.EdgeWeightLabeller;
import edu.uci.ics.jung.graph.decorators.StringLabeller;
import edu.uci.ics.jung.graph.decorators.StringLabeller.UniqueLabelException;
import edu.uci.ics.jung.graph.impl.DirectedSparseEdge;
import edu.uci.ics.jung.graph.impl.DirectedSparseGraph;
import edu.uci.ics.jung.graph.impl.DirectedSparseVertex;
import edu.uci.ics.jung.graph.impl.UndirectedSparseEdge;
import edu.uci.ics.jung.graph.impl.UndirectedSparseGraph;
import edu.uci.ics.jung.graph.impl.UndirectedSparseVertex;
import edu.uci.ics.jung.io.PajekNetWriter;
import edu.uci.ics.jung.utils.UserData;

public class JUNGPajekNetWriterWrapper {

    StringLabeller sl;
    EdgeWeightLabeller ewl;
    String filename;
    String outputPath;
    Population plans;
	Graph g;
    // This is a map of MatSim Person ID to the JUNG vertex object
    // (The JUNG Vertex UserDatum container is used
    // to get the Person ID given a Vertex object
    TreeMap<Id, Vertex> verticesPersons = new TreeMap<Id, Vertex>();

    public JUNGPajekNetWriterWrapper(String outputPath, SocialNetwork snet, Population plans) {

	this.plans=plans;
	this.outputPath=outputPath;
	// First instantiate the JUNG-compatible graph structure
	if (snet.UNDIRECTED) {
	    g = new UndirectedSparseGraph();
	} else {
	    g = new DirectedSparseGraph();
	    // fillDirectedGraph(g, snet, plans);
	}
	// Then fill the graph using the MatSim ego nets
	fillGraph(g,snet, plans);
	sl =StringLabeller.getLabeller(g);
	// for all vertices in the graph
	Iterator vit = g.getVertices().iterator();
	while(vit.hasNext()){
	    Vertex v = (Vertex) vit.next();
	    int iD = Integer.valueOf(v.getUserDatum("personId").toString());
	    String dummy = Integer.valueOf(iD).toString();
	    try {
		sl.setLabel(v, dummy);
	    } catch (UniqueLabelException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	    }
	}
	// for all edges in the graph
	// Attaches the edge age as a weight
	ewl = EdgeWeightLabeller.getLabeller(g);
	Iterator eit = g.getEdges().iterator();
	while(eit.hasNext()){
	    Edge e = (Edge) eit.next();
	    double age = Double.parseDouble( e.getUserDatum("timeLastUsed").toString())- Integer.parseInt(e.getUserDatum("timeMet").toString());
	    // The edge weight might be the edge age, but if it is 0 (new edge) then Pajek ignores it.
	    ewl.setNumber(e, age+1.);
	}
    }
    public void write(){
	PajekNetWriter pnw = new PajekNetWriter();
	String filename=outputPath+"/snetFinal.net";
	try {
	    pnw.save(g, filename, sl, ewl);
	} catch (IOException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
    }
    /**
     * Glues MatSim egonets together to make a JUNG graph object for further
     * analysis with JUNG statistical package.
     *
     * @author jhackney
     * @param snet
     * @param plans
     * @throws UniqueLabelException
     */
    private void fillGraph(Graph g, SocialNetwork snet, Population plans) {

	Collection<Person> personList = plans.getPersons().values();
	Vertex v;
	Edge e;
	Iterator iperson = personList.iterator();
	while (iperson.hasNext()) {
	    Person p = (Person) iperson.next();
	    if (snet.UNDIRECTED) {
		v = new UndirectedSparseVertex();
	    } else {
		v = new DirectedSparseVertex();
	    }
	    verticesPersons.put(p.getId(), v);
	    // Add the Person ID to the user data container for the vertex
	    v.addUserDatum("personId", p.getId(), UserData.SHARED);
	    // Add the vertex to the graph
	    g.addVertex(v);
	}
	Iterator ilinks = snet.getLinks().iterator();
	while (ilinks.hasNext()) {
	    SocialNetEdge myLink = (SocialNetEdge) ilinks.next();
	    Vertex egoVertex = verticesPersons.get(myLink.getPersonFrom().getId());
	    Vertex alterVertex = verticesPersons.get(myLink.getPersonTo().getId());
	    if (snet.UNDIRECTED) {
		e = new UndirectedSparseEdge(egoVertex, alterVertex);
	    } else {
		e = new DirectedSparseEdge(egoVertex, alterVertex);
	    }
	    // Add the link attributes in the UserDatum container
	    e.addUserDatum("timeMet", myLink.getTimeMade(), UserData.SHARED);
	    e.addUserDatum("timeLastUsed", myLink.getTimeLastUsed(), UserData.SHARED);
	    e.addUserDatum("type",myLink.getType(),UserData.SHARED);
	    e.addUserDatum("visitNum",myLink.getTimesMet(),UserData.SHARED);
	    //System.out.println(myLink.getTimeMade()-myLink.getTimeLastUsed());
	    // Add the link to the graph
	    g.addEdge(e);
	}
    }
}
