package playground.clruch.gfx;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.geometry.CoordinateTransformation;

import playground.clruch.net.OsmLink;

public class MatsimStaticDatabase {

    public static void initializeSingletonInstance( //
            Network network, //
            CoordinateTransformation coordinateTransformation) {

        NavigableMap<String, OsmLink> linkMap = new TreeMap<>();

        for (Link link : network.getLinks().values()) {
            OsmLink osmLink = new OsmLink(link);

            osmLink.coords[0] = coordinateTransformation.transform(link.getFromNode().getCoord());
            osmLink.coords[1] = coordinateTransformation.transform(link.getToNode().getCoord());

            linkMap.put(link.getId().toString(), osmLink);
        }

        INSTANCE = new MatsimStaticDatabase(linkMap);
    }

    public static MatsimStaticDatabase INSTANCE;

    /**
     * rapid lookup from MATSIM side
     */
    private final Map<Link, Integer> linkInteger = new HashMap<>();

    /**
     * rapid lookup from Viewer
     */
    private final List<OsmLink> list;

    private MatsimStaticDatabase(NavigableMap<String, OsmLink> linkMap) {
        list = new ArrayList<>(linkMap.values());
        int index = 0;
        for (OsmLink osmLink : list) {
            linkInteger.put(osmLink.link, index);
            ++index;
        }
    }

    public int getLinkIndex(Link link) {
        return linkInteger.get(link);
    }

    public OsmLink getOsmLink(int index) {
        return list.get(index);
    }

    public Collection<OsmLink> getOsmLinks() {
        return Collections.unmodifiableCollection(list);
    }

    public int getOsmLinksSize() {
        return list.size();
    }

}
