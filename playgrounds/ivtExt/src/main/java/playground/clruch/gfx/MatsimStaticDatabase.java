package playground.clruch.gfx;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.geometry.CoordinateTransformation;

import playground.clruch.gfx.util.OsmLink;

public class MatsimStaticDatabase {
    public final Map<String, OsmLink> linkMap = new HashMap<>();

    public static MatsimStaticDatabase of( //
            Network network, //
            CoordinateTransformation coordinateTransformation) {
        MatsimStaticDatabase db = new MatsimStaticDatabase();

        for (Link link : network.getLinks().values()) {
            OsmLink osmLink = new OsmLink();

            osmLink.coords[0] = coordinateTransformation.transform(link.getFromNode().getCoord());
            osmLink.coords[1] = coordinateTransformation.transform(link.getToNode().getCoord());

            // System.out.println(osmLink.coords[0]);
            // System.out.println(osmLink.coords[1]);

            db.linkMap.put(link.getId().toString(), osmLink);

        }

        return db;
    }

    private MatsimStaticDatabase() {

    }

}
