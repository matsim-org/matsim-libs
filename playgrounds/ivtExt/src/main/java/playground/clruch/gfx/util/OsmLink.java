package playground.clruch.gfx.util;

import org.matsim.api.core.v01.Coord;

public class OsmLink {
    public Coord[] coords = new Coord[2];

    public Coord getAt(double lambda) {
        return new Coord( //
                coords[0].getX() + lambda * (coords[1].getX() - coords[0].getX()), //
                coords[0].getY() + lambda * (coords[1].getY() - coords[0].getY()));
    }
}
