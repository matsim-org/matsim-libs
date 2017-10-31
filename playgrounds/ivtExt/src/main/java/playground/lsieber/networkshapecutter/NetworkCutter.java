package playground.lsieber.networkshapecutter;

import java.io.IOException;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Network;

public class NetworkCutter {

    /** Cutts a network based on the other inputs.
     * 1. Polygon cutting:
     * A string with the path to the Shapefil (.shp) (and as well to the .shx file) needds to be given.
     * 
     * 2. Radius cutting:
     * The cutter reduces the network to the circle around the coordinates of the center with a radius. */
    public Network filter(Network network, String shapefilePath) throws IOException {
        return new NetworkShapeFilter().filter(network, shapefilePath + ".shp");
    }

    public Network filter(Network network, Coord coord, double radius) throws IOException {
        return new NetworkRadiusFilter().filter(network, coord, radius);
    }

}
