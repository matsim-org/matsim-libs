package org.matsim.contrib.analysis.spatial;

import java.util.UUID;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;

class SpatialTestUtils {

    private static GeometryFactory geometryFactory = new GeometryFactory();

    static Geometry createRect(double maxX, double maxY) {
        return geometryFactory.createPolygon(new Coordinate[]{
                new Coordinate(0, 0), new Coordinate(maxX, 0),
                new Coordinate(maxX, maxY), new Coordinate(0, maxY),
                new Coordinate(0, 0) // close the ring
        });
    }

    static Link createRandomLink(NetworkFactory factory, double maxX, double maxY) {
        Node fromNode = SpatialTestUtils.createRandomNode(factory, maxX, maxY);
        Node toNode = SpatialTestUtils.createRandomNode(factory, maxX, maxY);
        return factory.createLink(Id.createLinkId(UUID.randomUUID().toString()), fromNode, toNode);
    }

    private static Node createRandomNode(NetworkFactory factory, double maxX, double maxY) {
        Coord coord = new Coord(getRandomValue(maxX), getRandomValue(maxY));
        return factory.createNode(Id.createNodeId(UUID.randomUUID().toString()), coord);
    }

    static double getRandomValue(double upperBounds) {
        return Math.random() * upperBounds;
    }
}
