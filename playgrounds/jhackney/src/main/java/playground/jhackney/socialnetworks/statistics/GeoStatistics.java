package playground.jhackney.socialnetworks.statistics;

import java.util.Iterator;
import java.util.LinkedHashMap;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.world.MappedLocation;

import playground.jhackney.socialnetworks.socialnet.SocialNetEdge;
import playground.jhackney.socialnetworks.socialnet.SocialNetwork;
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
	 * analysis with JUNG statistical package. Aggregates the network to a
	 * geographic Layer.
	 *
	 * @author jhackney
	 */
	Graph gg;
	Population plans;
	SocialNetwork snet;
	LinkedHashMap<MappedLocation, Vertex> locVertex = new LinkedHashMap<MappedLocation, Vertex>();
	LinkedHashMap<Vertex, MappedLocation> vertexLoc = new LinkedHashMap<Vertex,MappedLocation>();
	LinkedHashMap<Edge, Double> edgeStrength = new LinkedHashMap<Edge,Double>();
//	Collection<Location> locations;
	ActivityFacilities facilities;

	public GeoStatistics(Population plans, SocialNetwork snet, ActivityFacilities facilities) {
		this.facilities = facilities;
		this.plans=plans;
		this.snet=snet;
//		gg=makeJungGraph();
		this.plans=plans;
	}

	public Graph makeJungGraph() {
		// TODO fix
		Graph g = new UndirectedSparseGraph();
		for (Person aPerson : this.plans.getPersons().values()) {
			Vertex v;
//			Choose the first location in the plan and assume it's where the person lives

			ActivityFacility aHome = this.facilities.getFacilities().get(((PlanImpl) aPerson.getSelectedPlan()).getFirstActivity().getFacilityId());
			//			Each facility should only have one location but UpMapping is a TreeMap so pick the first entry
			// MappedLocation aLoc = ((ActivityFacilityImpl) aHome).getUpMapping().get(((ActivityFacilityImpl) aHome).getUpMapping().firstKey());
			MappedLocation aLoc = null;
			if(this.locVertex.containsKey(aLoc)){
				v=this.locVertex.get(aLoc);
//				System.out.println("  ### GEOSTAT: Graph contains vertex "+ v+" "+aLoc.getId());
				//and its population should be increased by 1
				int pop = Integer.parseInt(v.getUserDatum("population").toString())+1;
				v.removeUserDatum("population");
				v.addUserDatum("population", pop, UserData.SHARED);
			}else{
				v = new UndirectedSparseVertex();
//				System.out.println("   ### GEOSTAT: Making new vertex "+v+" "+aLoc.getId());
				this.locVertex.put(aLoc, v);
				this.vertexLoc.put(v,aLoc);
				// Add the Person ID to the user data container for the vertex
				v.addUserDatum("locationId", aLoc.getId(), UserData.SHARED);
				v.addUserDatum("population", 1, UserData.SHARED);
				// Add the vertex to the graph
				g.addVertex(v);
			}
		}
		Iterator<SocialNetEdge> ilinks = this.snet.getLinks().iterator();
		while (ilinks.hasNext()) {
			SocialNetEdge link = ilinks.next();

			Person personA = link.getPersonFrom();
			Person personB = link.getPersonTo();

			ActivityFacilityImpl aHome=(ActivityFacilityImpl) this.facilities.getFacilities().get(((PlanImpl) personA.getSelectedPlan()).getFirstActivity().getFacilityId());
			ActivityFacilityImpl bHome=(ActivityFacilityImpl) this.facilities.getFacilities().get(((PlanImpl) personB.getSelectedPlan()).getFirstActivity().getFacilityId());
			// MappedLocation aLoc = aHome.getUpMapping().get(aHome.getUpMapping().firstKey());
			// MappedLocation bLoc = bHome.getUpMapping().get(bHome.getUpMapping().firstKey());

			MappedLocation aLoc = null;
			MappedLocation bLoc = null;
			
			Vertex aVertex = this.locVertex.get(aLoc);
			Vertex bVertex = this.locVertex.get(bLoc);

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

	public LinkedHashMap<MappedLocation,Vertex> getLocVertex(){
		return this.locVertex;
	}
	public LinkedHashMap<Vertex, MappedLocation> getVertexLoc(){
		return this.vertexLoc;
	}
	public LinkedHashMap<Edge,Double> getEdgeStrength(){
		return this.edgeStrength;
	}
}

