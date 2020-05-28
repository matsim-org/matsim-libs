package org.matsim.contrib.emissions.utils;

import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.geotools.MGC;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RasteredNetwork {

    private final Map<Id<Link>, List<Coord>> linkMap;
    private final double cellSize;

    RasteredNetwork(Network network, Geometry bounds, double cellSize) {

        this.cellSize = cellSize;
        linkMap = rasterizeNetwork(network, bounds, cellSize);
    }

    RasteredNetwork(Network network, double cellSize) {
        this(network, null, cellSize);
    }

    List<Coord> getCellCoords(Id<Link> forLink) {
        return linkMap.get(forLink);
    }

    public boolean hasLink(Id<Link> linkId) {
        return linkMap.containsKey(linkId);
    }

    double getCellSize() {
        return cellSize;
    }

    private static Coord createCoord(int x, int y, double cellSize) {
        return new Coord(x * cellSize - (cellSize / 2), y * cellSize - cellSize / 2);
    }

    private static boolean containsLink(Link link, Geometry geometry) {
        return geometry.contains(MGC.coord2Point(link.getFromNode().getCoord())) || geometry.contains(MGC.coord2Point(link.getToNode().getCoord()));
    }

    private Map<Id<Link>, List<Coord>> rasterizeNetwork(Network network, final Geometry bounds, final double cellSize) {

        var networkStream = network.getLinks().values().parallelStream();

        if (bounds != null) {
            networkStream = networkStream.filter(link -> containsLink(link, bounds));
        }

        return networkStream
                .map(link -> Tuple.of(link.getId(), rasterizeLink(link, cellSize)))
                .collect(Collectors.toMap(Tuple::getFirst, Tuple::getSecond));
    }

    /**
     * Rasterizes links into squares. Uses Bresenham's line drawing algorithm, which is supposed to be fast
     * Maybe the result is too chunky, but it'll do as a first try
     *
     * @param link Matsim network link
     * @return all cells which are 'touched' by the link
     */
    private List<Coord> rasterizeLink(Link link, double cellSize) {

        int x0 = (int) (link.getFromNode().getCoord().getX() / cellSize);
        int x1 = (int) (link.getToNode().getCoord().getX() / cellSize);
        int y0 = (int) (link.getFromNode().getCoord().getY() / cellSize);
        int y1 = (int) (link.getToNode().getCoord().getY() / cellSize);
        int dx = Math.abs(x1 - x0);
        int dy = -Math.abs(y1 - y0);
        int err = dx + dy, e2;

        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;

        List<Coord> result = new ArrayList<>();

        if (dx == 0 && dy == 0) {
            // the algorithm doesn't really support lines shorter than the cell size.
            // do avoid complicated computation within the loop, catch this case here
            result.add(createCoord(x0, y0, cellSize));
            return result;
        }

        do {
            result.add(createCoord(x0, y0, cellSize));
            e2 = err + err;
            if (e2 >= dy) {
                err += dy;
                x0 += sx;
            }
            if (e2 <= dx) {
                err += dx;
                y0 += sy;
            }
        } while (x0 != x1 || y0 != y1);

        return result;
    }
}
