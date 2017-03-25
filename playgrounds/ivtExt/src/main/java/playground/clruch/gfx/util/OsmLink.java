package playground.clruch.gfx.util;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;

public class OsmLink {
    public final Link link; // <- storage of link is not absolutely necessary...
    public Coord[] coords = new Coord[2];

    public OsmLink(Link link) {
        this.link = link;
    }

    public Coord getAt(double lambda) {
        return new Coord( //
                coords[0].getX() + lambda * (coords[1].getX() - coords[0].getX()), //
                coords[0].getY() + lambda * (coords[1].getY() - coords[0].getY()));
    }
}
