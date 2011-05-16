package playground.michalm.vrp.data.model;

import org.matsim.api.core.v01.population.*;

import pl.poznan.put.vrp.dynamic.data.model.Route;

public class VRPRoute
    extends Route
{
    //for each customer an arc must be remembered
    //(arc = LinkNetworkRoute that is built from Path which is an output of Dijkstra)
    //Depot--C0--C1--C2--...--C(n-1)--Depot => n customers, n+1 arcs 
    //such arc must be initialized properly. This is because Paths are found only for a limited
    //number of time slots, therefore, each Path starts at the beginning of each time slot.
    //On the other hand, arcs should have properly set times...
    public LinkNetworkRoute[] arcs;
    
    
    
}
