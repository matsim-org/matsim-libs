package org.matsim.socialnetworks.statistics;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeMap;

import org.matsim.facilities.Facility;
import org.matsim.plans.Person;
import org.matsim.plans.Plans;
import org.matsim.socialnetworks.socialnet.SocialNetEdge;
import org.matsim.socialnetworks.socialnet.SocialNetwork;
import org.matsim.world.Location;

import edu.uci.ics.jung.graph.Edge;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.impl.UndirectedSparseEdge;
import edu.uci.ics.jung.graph.impl.UndirectedSparseGraph;
import edu.uci.ics.jung.graph.impl.UndirectedSparseVertex;
import edu.uci.ics.jung.utils.UserData;

public class GeoStatistics {
	/**
	 * Glues MatSim egonets together to make a JUNG graph object for further
	 * analysis with JUNG statistical package.
	 *
	 * @author jhackney
	 */
	Graph gg;
	Plans plans;
	SocialNetwork snet;
	HashMap<Location, Vertex> locVertex = new HashMap<Location, Vertex>();
	HashMap<Vertex, Location> vertexLoc = new HashMap<Vertex,Location>();
	HashMap<Edge, Double> edgeStrength = new HashMap<Edge,Double>();
	Collection<Location> locations;

	public GeoStatistics(Plans plans, SocialNetwork snet) {

		this.plans=plans;
		this.snet=snet;
//		gg=makeJungGraph();
		this.plans=plans;
	}

	public Graph makeJungGraph() {
		// TODO fix
		Graph g = new UndirectedSparseGraph();
		Iterator<Person> iperson = plans.getPersons().values().iterator();
		while (iperson.hasNext()) {
			Vertex v;
			Person aPerson = iperson.next();
//			Choose the first "home" location in the sorted set and assume it's where the person lives
			Facility aHome= aPerson.getKnowledge().getFacilities("home").get(aPerson.getKnowledge().getFacilities("home").firstKey());
//			Each facility should only have one location but UpMapping is a TreeMap so pick the first entry
			Location aLoc = aHome.getUpMapping().get(aHome.getUpMapping().firstKey());
			if(locVertex.containsKey(aLoc)){
				v=locVertex.get(aLoc);
				System.out.println("  ### GEOSTAT: Graph contains vertex "+ v+" "+aLoc.getId());
			}else{
				v = new UndirectedSparseVertex();
				System.out.println("   ### GEOSTAT: Making new vertex "+v+" "+aLoc.getId());
				locVertex.put(aLoc, v);
				vertexLoc.put(v,aLoc);
				// Add the Person ID to the user data container for the vertex
				v.addUserDatum("locationId", aLoc.getId(), UserData.SHARED);
				// Add the vertex to the graph
				g.addVertex(v);
			}
		}
		Iterator<SocialNetEdge> ilinks = snet.getLinks().iterator();
		while (ilinks.hasNext()) {
			SocialNetEdge link = (SocialNetEdge) ilinks.next();

			Person personA = link.getPersonFrom();
			Person personB = link.getPersonTo();

			Facility aHome= personA.getKnowledge().getFacilities("home").get(personA.getKnowledge().getFacilities("home").firstKey());
			Facility bHome= personB.getKnowledge().getFacilities("home").get(personB.getKnowledge().getFacilities("home").firstKey());

			Location aLoc = aHome.getUpMapping().get(aHome.getUpMapping().firstKey());
			Location bLoc = bHome.getUpMapping().get(bHome.getUpMapping().firstKey());

			Vertex aVertex = locVertex.get(aLoc);
			Vertex bVertex = locVertex.get(bLoc);

			if(aVertex.getNeighbors().contains(bVertex)){
				//this edge exists already in the graph
				Edge e = aVertex.findEdge(bVertex);
				//and its strength should be increased by 1.0
				double xxx = Double.parseDouble(e.getUserDatum("strength").toString())+1.0;
				e.removeUserDatum("strength");
				e.addUserDatum("strength", xxx, UserData.SHARED);

//				System.out.println(" ### GEOSTATS III ###");
//				System.out.println(e+" "+aVertex+" "+bVertex+" "+e.getUserDatum("strength"));
			}else if(bVertex.getNeighbors().contains(aVertex)){
				//this edge exists already in the graph
				Edge e = bVertex.findEdge(aVertex);
				//and its strength should be increased by 1.0
				double xxx = Double.parseDouble(e.getUserDatum("strength").toString())+1.0;
				e.removeUserDatum("strength");
				e.addUserDatum("strength", xxx, UserData.SHARED);

//				System.out.println(" ### GEOSTATS II ###");
//				System.out.println(e+" "+bVertex+" "+aVertex+" "+e.getUserDatum("strength"));
			}else{
				//this edge should be added to the graph
				if(!(aVertex.equals(bVertex))){
					Edge e= new UndirectedSparseEdge(aVertex, bVertex);
					g.addEdge(e);
					//and its strength set = 1.0
					e.addUserDatum("strength", 1.0, UserData.SHARED);

//					System.out.println(" ### GEOSTATS I ###");
//					System.out.println(e+" "+e.getUserDatum("strength"));
				}
			}
		}
		return g;
	}

	public HashMap<Location,Vertex> getLocVertex(){
		return this.locVertex;
	}
	public HashMap<Vertex, Location> getVertexLoc(){
		return this.vertexLoc;
	}
	public HashMap<Edge,Double> getEdgeStrength(){
		return this.edgeStrength;
	}
}

