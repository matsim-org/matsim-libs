package playground.michalm.vrp.data.network.shortestpath;

import pl.poznan.put.vrp.dynamic.data.network.Arc;


public interface ShortestPathArc
    extends Arc
{
    ShortestPath getShortestPath();
}
