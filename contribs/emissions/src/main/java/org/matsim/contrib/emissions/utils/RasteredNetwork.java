package org.matsim.contrib.emissions.utils;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.collections.Tuple;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RasteredNetwork {

    private final Map<Id<Link>, List<Coord>> linkMap;
    private final double cellSize;

    private int bla = 0;

    RasteredNetwork(Network network, double cellSize) {

        this.cellSize = cellSize;
        linkMap = rasterizeNetwork(network, cellSize);
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

    private Map<Id<Link>, List<Coord>> rasterizeNetwork(Network network, final double cellSize) {

        return network.getLinks().values().stream()
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

        do {
            bla++;
            result.add(new Coord(x0 * cellSize - (cellSize / 2), y0 * cellSize - cellSize / 2));
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

        if (bla % 1000 == 0) {
            System.out.println(bla);
        }
        return result;
    }
}
