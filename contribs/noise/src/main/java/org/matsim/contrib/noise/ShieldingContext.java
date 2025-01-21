package org.matsim.contrib.noise;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.algorithm.RobustLineIntersector;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.utils.geometry.GeometryUtils;

import jakarta.inject.Inject;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * Separate from {@link NoiseContextImpl} for better testability.
 *
 * @author nkuehnel
 */
final class ShieldingContext {

    private final static Logger logger = LogManager.getLogger(ShieldingContext.class);

    private final static double GROUND_HEIGHT = 0.5;
    private final ShieldingCorrection shieldingCorrection;

    private BarrierContext barrierContext;

    @Inject
    ShieldingContext(Config config, ShieldingCorrection shieldingCorrection, BarrierContext barrierContext) {
        this.shieldingCorrection = shieldingCorrection;
        this.barrierContext = barrierContext;
    }

    ShieldingContext(ShieldingCorrection shieldingCorrection, BarrierContext barrierContext) {
        this.shieldingCorrection = shieldingCorrection;
        this.barrierContext = barrierContext;
    }

    /**
     * determines the shielding value z for a receiver point for a given link emission source
     */
    double determineShieldingValue(Coordinate rpPoint, LineSegment segment) {
        double correctionTermShielding = 0;
        final Coordinate midPoint = segment.midPoint();
        midPoint.z = GROUND_HEIGHT;

        LineString projectedLineOfSight = constructLineOfSight(rpPoint, midPoint);

        NavigableMap<Double, Coordinate> edgeCandidates = getObstructionEdges(rpPoint, midPoint, projectedLineOfSight);
        edgeCandidates.put(projectedLineOfSight.getLength(), midPoint);

        Coordinate lastFixedEdge = rpPoint;
        Coordinate tmpEdge = rpPoint;
        double currentHeight = GROUND_HEIGHT;

        List<Coordinate> consideredEdges = new ArrayList<>();

        double distToCurrentEdge = 0;
        while (lastFixedEdge != midPoint) {
            if (edgeCandidates.isEmpty()) {
                logger.warn("Skipping obstacle as distance appears to be 0.");
                return correctionTermShielding;
            }
            Iterator<Coordinate> edgesIterator = edgeCandidates.values().iterator();
            double maxSlope = Double.NEGATIVE_INFINITY;
            double tmpDistance = 0;
            while (edgesIterator.hasNext()) {
                Coordinate edge = edgesIterator.next();
                double distance = lastFixedEdge.distance(edge);
                double slope = (edge.z - currentHeight) / distance;
                if (slope >= maxSlope) {
                    maxSlope = slope;
                    tmpEdge = edge;
                    tmpDistance = distance;
                }
            }
            lastFixedEdge = tmpEdge;
            distToCurrentEdge += tmpDistance;
            currentHeight = tmpEdge.z;
            consideredEdges.add(lastFixedEdge);
            edgeCandidates = edgeCandidates.tailMap(distToCurrentEdge, false);
        }

        consideredEdges.remove(midPoint);

        if (consideredEdges.isEmpty()) {
            return correctionTermShielding;
        }

        final double firstEdgeYDiff = GROUND_HEIGHT - consideredEdges.get(0).z;
        double firstEdgeDistance = rpPoint.distance(consideredEdges.get(0));
        double receiverToFirstEdgeDistance
                = Math.sqrt(firstEdgeYDiff * firstEdgeYDiff + firstEdgeDistance * firstEdgeDistance);

        double shieldingDepth = 0;

        Iterator<Coordinate> it = consideredEdges.iterator();
        Coordinate edgeTemp = it.next();
        while (it.hasNext()) {
            Coordinate edge = it.next();
            double xyDiff = edgeTemp.distance(edge);
            double zDiff = edgeTemp.z - edge.z;
            shieldingDepth += Math.sqrt(xyDiff * xyDiff + zDiff * zDiff);
            edgeTemp = edge;
        }

        final double lastEdgeSourceXYDiff = midPoint.distance(edgeTemp);
        final double lastEdgeSourceZDiff = GROUND_HEIGHT - edgeTemp.z;
        double lastEdgeToSourceDistance = Math.sqrt(lastEdgeSourceXYDiff * lastEdgeSourceXYDiff
                + lastEdgeSourceZDiff * lastEdgeSourceZDiff);

        correctionTermShielding = shieldingCorrection.calculateShieldingCorrection(
                rpPoint.distance(midPoint), lastEdgeToSourceDistance, receiverToFirstEdgeDistance, shieldingDepth);
        return correctionTermShielding;
    }

    /**
     * Returns an ordered map of distances and coords to all obstruction edges
     */
    private ConcurrentSkipListMap<Double, Coordinate> getObstructionEdges(Point receiver, Point source, LineString directLineOfSight,
                                                                          LineString fromLineOfSight, LineString toLineOfSight) {
        final Collection<NoiseBarrier> candidates =
                barrierContext.query(directLineOfSight.getEnvelopeInternal());

        ConcurrentSkipListMap<Double, Coordinate> edgeCandidates = new ConcurrentSkipListMap<>();
        for (NoiseBarrier noiseBarrier : candidates) {
            if (isObstructing(receiver, source, fromLineOfSight, toLineOfSight, noiseBarrier.getGeometry())) {
                //direct implementation intersects() and intersection() here is up to 15x faster than
                //using intersects() and intersection() directly on the jts geometry. nkuehnel, aug '20
                final Set<Coordinate> intersections = intersection((Polygon) noiseBarrier.getGeometry().getGeometry(), directLineOfSight.getCoordinates());
                for (Coordinate coordinate : intersections) {
                    coordinate.z = noiseBarrier.getHeight();
                    final double distance = receiver.getCoordinate().distance(coordinate);
                    edgeCandidates.put(distance, coordinate);
                }
            }
        }
        return edgeCandidates;
    }


    /**
     * Returns an ordered map of distances and coords to all obstruction edges
     */
    private ConcurrentSkipListMap<Double, Coordinate> getObstructionEdges(Coordinate receiver, Coordinate source, LineString directLineOfSight) {
        final Collection<NoiseBarrier> candidates =
                barrierContext.query(directLineOfSight.getEnvelopeInternal());

        ConcurrentSkipListMap<Double, Coordinate> edgeCandidates = new ConcurrentSkipListMap<>();
        for (NoiseBarrier noiseBarrier : candidates) {
            if (isObstructing(receiver, source, noiseBarrier.getGeometry(), directLineOfSight)) {
                //direct implementation intersects() and intersection() here is up to 15x faster than
                //using intersects() and intersection() directly on the jts geometry. nkuehnel, aug '20
                final Set<Coordinate> intersections = intersection((Polygon) noiseBarrier.getGeometry().getGeometry(), directLineOfSight.getCoordinates());

                for (Coordinate coordinate : intersections) {
                    coordinate.z = noiseBarrier.getHeight();
                    final double distance = receiver.distance(coordinate);
                    edgeCandidates.put(distance, coordinate);
                }
            }
        }
        return edgeCandidates;
    }

    /**
     * Checks whether a barrier is obstructing the receiver point-emission source relation.
     * The following has to be met:
     * 1) the barrier must not contain the receiver point
     * 2) the direct projected line of sight must intersect the barrier
     * 3) the line of sight from receiver to the from-Node of the link intersects the barrier
     * 4) the line of sight from receiver to the to-Node of the link intersects the barrier
     * <p>
     * Uses prepared geometry for the barrier polygon to cache intersection graphs.
     */
    private boolean isObstructing(Geometry receiver, Geometry source, LineString fromLineOfSight,
                                  LineString toLineOfSight, PreparedGeometry barrier) {
        return intersects((Polygon) barrier.getGeometry(), fromLineOfSight)
                && intersects((Polygon) barrier.getGeometry(), toLineOfSight)
                && !barrier.contains(receiver)
                && !barrier.contains(source);
    }

    private boolean isObstructing(Coordinate receiver, Coordinate source,
                                  PreparedGeometry barrier, LineString direct) {
        final GeometryFactory geometryFactory = new GeometryFactory();
        Point receiverPoint = geometryFactory.createPoint(receiver);
        Point sourcePoint = geometryFactory.createPoint(source);
        return intersects((Polygon) barrier.getGeometry(), direct)
                && !barrier.contains(receiverPoint)
                && !barrier.contains(sourcePoint);
    }


    private LineString constructLineOfSight(Point receiver, Point source) {
        Coordinate[] lineOfSightCoords = new Coordinate[2];
        lineOfSightCoords[0] = receiver.getCoordinate();
        lineOfSightCoords[1] = source.getCoordinate();
        return new GeometryFactory().createLineString(lineOfSightCoords);
    }

    private LineString constructLineOfSight(Coordinate receiver, Coordinate source) {
        Coordinate[] lineOfSightCoords = new Coordinate[2];
        lineOfSightCoords[0] = receiver;
        lineOfSightCoords[1] = source;
        return new GeometryFactory().createLineString(lineOfSightCoords);
    }


    private Set<Coordinate> intersection(Polygon polygon, Coordinate[] coords) {
        return intersection(polygon.getExteriorRing(), coords);
    }

    private Set<Coordinate> intersection(LineString ring, Coordinate[] coords) {
        final RobustLineIntersector intersector = new RobustLineIntersector();

        Set<Coordinate> intersections = null;
        for (int i = 1; i < ring.getNumPoints(); ++i) {
            Coordinate p1 = ring.getCoordinateN(i);
            Coordinate p2 = ring.getCoordinateN(i - 1);
            intersector.computeIntersection(coords[0], coords[1], p1, p2);
            if (intersector.hasIntersection()) {
                if (intersections == null) {
                    intersections = new HashSet<>();
                }
                intersections.add(intersector.getIntersection(0));
            }
        }
        return intersections == null ? Collections.emptySet() : intersections;
    }


    private static boolean intersects(Polygon polygon, LineString string) {
        if (!polygon.getEnvelopeInternal().intersects(string.getEnvelopeInternal())) {
            return false;
        }
        final RobustLineIntersector intersector = new RobustLineIntersector();
        Coordinate[] coords = string.getCoordinates();
        final LineString ring = polygon.getExteriorRing();
        for (int i = 1; i < ring.getNumPoints(); ++i) {
            Coordinate p1 = ring.getCoordinateN(i);
            Coordinate p2 = ring.getCoordinateN(i - 1);
            intersector.computeIntersection(coords[0], coords[1], p1, p2);
            if (intersector.hasIntersection()) {
                return true;
            }
        }
        return false;
    }


    /**
     * determines the shielding value z for a receiver point for a given link emission source
     */
    double determineShieldingValue(ReceiverPoint receiverPoint, Link link, Coord projectedCoord) {
        double correctionTermShielding = 0;
        final Point rpPoint = GeometryUtils.createGeotoolsPoint(receiverPoint.getCoord());
        final Point projectedPoint = GeometryUtils.createGeotoolsPoint(projectedCoord);
        projectedPoint.getCoordinate().z = GROUND_HEIGHT;

        final Point fromPoint = GeometryUtils.createGeotoolsPoint(link.getFromNode().getCoord());
        final Point toPoint = GeometryUtils.createGeotoolsPoint(link.getToNode().getCoord());

        LineString projectedLineOfSight = constructLineOfSight(rpPoint, projectedPoint);
        LineString fromLineOfSight = constructLineOfSight(rpPoint, fromPoint);
        LineString toLineOfSight = constructLineOfSight(rpPoint, toPoint);

        NavigableMap<Double, Coordinate> edgeCandidates = getObstructionEdges(rpPoint, projectedPoint, projectedLineOfSight, fromLineOfSight, toLineOfSight);
        edgeCandidates.put(projectedLineOfSight.getLength(), projectedPoint.getCoordinate());

        Coordinate lastFixedEdge = rpPoint.getCoordinate();
        Coordinate tmpEdge = rpPoint.getCoordinate();
        double currentHeight = GROUND_HEIGHT;

        List<Coordinate> consideredEdges = new ArrayList<>();

        double distToCurrentEdge = 0;
        while (lastFixedEdge != projectedPoint.getCoordinate()) {
            if (edgeCandidates.isEmpty()) {
                logger.warn("Skipping obstacle as distance appears to be 0.");
                return correctionTermShielding;
            }
            Iterator<Coordinate> edgesIterator = edgeCandidates.values().iterator();
            double maxSlope = Double.NEGATIVE_INFINITY;
            double tmpDistance = 0;
            while (edgesIterator.hasNext()) {
                Coordinate edge = edgesIterator.next();
                double distance = lastFixedEdge.distance(edge);
                double slope = (edge.z - currentHeight) / distance;
                if (slope >= maxSlope) {
                    maxSlope = slope;
                    tmpEdge = edge;
                    tmpDistance = distance;
                }
            }
            lastFixedEdge = tmpEdge;
            distToCurrentEdge += tmpDistance;
            currentHeight = tmpEdge.z;
            consideredEdges.add(lastFixedEdge);
            edgeCandidates = edgeCandidates.tailMap(distToCurrentEdge, false);
        }

        consideredEdges.remove(projectedPoint.getCoordinate());

        if (consideredEdges.isEmpty()) {
            return correctionTermShielding;
        }

        final double firstEdgeYDiff = GROUND_HEIGHT - consideredEdges.getFirst().z;
        double firstEdgeDistance = rpPoint.getCoordinate().distance(consideredEdges.getFirst());
        double receiverToFirstEdgeDistance
                = Math.sqrt(firstEdgeYDiff * firstEdgeYDiff + firstEdgeDistance * firstEdgeDistance);

        double shieldingDepth = 0;

        Iterator<Coordinate> it = consideredEdges.iterator();
        Coordinate edgeTemp = it.next();
        while (it.hasNext()) {
            Coordinate edge = it.next();
            double xyDiff = edgeTemp.distance(edge);
            double zDiff = edgeTemp.z - edge.z;
            shieldingDepth += Math.sqrt(xyDiff * xyDiff + zDiff * zDiff);
            edgeTemp = edge;
        }

        final double lastEdgeSourceXYDiff = projectedPoint.getCoordinate().distance(edgeTemp);
        final double lastEdgeSourceZDiff = GROUND_HEIGHT - edgeTemp.z;
        double lastEdgeToSourceDistance = Math.sqrt(lastEdgeSourceXYDiff * lastEdgeSourceXYDiff
                + lastEdgeSourceZDiff * lastEdgeSourceZDiff);

        correctionTermShielding = shieldingCorrection.calculateShieldingCorrection(
                rpPoint.distance(projectedPoint), lastEdgeToSourceDistance, receiverToFirstEdgeDistance, shieldingDepth);
        return correctionTermShielding;
    }
}
