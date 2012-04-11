package playground.michalm.vrp.data.network;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;

import pl.poznan.put.vrp.dynamic.data.network.Vertex;


public interface MatsimVertex
    extends Vertex
{
    //usually getCoord() == getLink().getCoord(); however not always!
    Coord getCoord();


    Link getLink();
}
