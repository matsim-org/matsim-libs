package org.matsim.contrib.noise;

import org.apache.log4j.Logger;
import org.locationtech.jts.geom.Coordinate;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordUtils;

import javax.inject.Inject;

/**
 * Separate from {@link NoiseContextImpl} for better testability.
 *
 * @author nkuehnel
 */
final class IntersectionContext {

    private final static Logger logger = Logger.getLogger(IntersectionContext.class);
    static final String INTERSECTION_TYPE = "IntersectionType";

    private final QuadTree<Intersection> intersections;

    enum RLS19IntersectionType {

        signalized(3),
        roundabout(2),
        other(0);

        private final double correction;

        RLS19IntersectionType(double correction) {
            this.correction = correction;
        }

        public double getCorrection() {
            return correction;
        }
    }

    private static class Intersection  {
        RLS19IntersectionType type;
        Coordinate coordinate;

        private Intersection(RLS19IntersectionType type, Coordinate coordinate) {
            this.type = type;
            this.coordinate = coordinate;
        }
    }

    @Inject
    IntersectionContext(Network network) {
        final double[] boundingBox = NetworkUtils.getBoundingBox(network.getNodes().values());

        this.intersections = new QuadTree<>(boundingBox[0],boundingBox[1],boundingBox[2],boundingBox[3]);
        for (Node node : network.getNodes().values()) {
            try {
                final Object intersectionType = node.getAttributes().getAttribute(INTERSECTION_TYPE);
                final Coordinate coordinate = CoordUtils.createGeotoolsCoordinate(node.getCoord());
                if(intersectionType != null) {
                    switch ((IntersectionContext.RLS19IntersectionType) intersectionType) {
                        case signalized:
                            this.intersections.put(node.getCoord().getX(), node.getCoord().getY(), new Intersection(RLS19IntersectionType.signalized, coordinate));
                            break;
                        case roundabout:
                            this.intersections.put(node.getCoord().getX(), node.getCoord().getY(), new Intersection(RLS19IntersectionType.roundabout, coordinate));
                            break;
                        case other:
                            break;
                        default:
                    }
                }

            } catch (IllegalArgumentException e) {
                logger.warn("Exception when checking " + node.getId() + " for intersections. Ignoring it.");
            }
        }
    }

    /**
     * Die Stoerwirkung durch das Anfahren und Bremsen der Fahrzeuge an Knotenpunkten wird in Abhaengigkeit
     * vom Knotenpunkttyp {@link RLS19IntersectionType} und von der Entfernung zum Schnittpunkt von sich
     * kreuzenden oder zusammentreffenden Quellinien bestimmt (=nodes)
     *
     * <p> The disturbance caused by the starting and braking of vehicles at junctions is determined
     * according to the type of junction {@link RLS19IntersectionType} and the distance to the point of
     * intersection of intersecting or converging source lines (=nodes)
     */
    double calculateIntersectionCorrection(Coordinate coordinate) {
        if(intersections != null) {
            final Intersection closest = intersections.getClosest(coordinate.x, coordinate.y);
            if(closest != null) {
                final double distance = closest.coordinate.distance(coordinate);
                if (distance < 120) {
                    return closest.type.correction * Math.max(1 - (distance / 120.), 0);
                }
            }
        }
        return 0;
    }
}
