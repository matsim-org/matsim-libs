package org.matsim.contrib.noise.data;

import com.vividsolutions.jts.geom.*;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.noise.handler.NoiseEquations;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.GeometryUtils;

import java.util.*;

/**
 * Separate from {@link NoiseContext} for better testability.
 *
 * @author nkuehnel
 */
public class ShieldingContext {

    private final static Logger logger = Logger.getLogger(ShieldingContext.class);

    private final QuadTree<NoiseBarrier> noiseBarriers;
    private final static double GROUND_HEIGHT = 0.5;

    public ShieldingContext(Collection<? extends NoiseBarrier> noiseBarriers) {
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE;
        double maxY = Double.MIN_VALUE;

        for (NoiseBarrier barrier : noiseBarriers) {

            final Envelope envelope = barrier.getGeometry().getEnvelopeInternal();

            if (envelope.getMinX() < minX) {
                minX = envelope.getMinX();
            }
            if (envelope.getMinY() < minY) {
                minY = envelope.getMinY();
            }
            if (envelope.getMaxX() > maxX) {
                maxX = envelope.getMaxX();
            }
            if (envelope.getMaxY() > maxY) {
                maxY = envelope.getMaxY();
            }
        }
        this.noiseBarriers = new QuadTree<>(minX - 100, minY - 100, maxX + 100, maxY + 100);
        for (NoiseBarrier barrier : noiseBarriers) {
            try {
                this.noiseBarriers.put(barrier.getGeometry().getCentroid().getX(), barrier.getGeometry().getCentroid().getY(), barrier);
            } catch (IllegalArgumentException e) {
                logger.warn("Could not add noise barrier " + barrier.getId() + " to quad tree. Ignoring it.");
            }
        }
    }


    public double determineShieldingCorrection(ReceiverPoint receiverPoint, Link link, Coord projectedCoord) {
        double correctionTermShielding = 0;
        final Point rpPoint = GeometryUtils.createGeotoolsPoint(receiverPoint.getCoord());
        final Point projectedPoint = GeometryUtils.createGeotoolsPoint(projectedCoord);
        projectedPoint.getCoordinate().z = GROUND_HEIGHT;

        final Point fromPoint = GeometryUtils.createGeotoolsPoint(link.getFromNode().getCoord());
        final Point toPoint = GeometryUtils.createGeotoolsPoint(link.getToNode().getCoord());


        Geometry projectedLineOfSight = constructLineOfSight(rpPoint, projectedPoint);
        Geometry fromLineOfSight = constructLineOfSight(rpPoint, fromPoint);
        Geometry toLineOfSight = constructLineOfSight(rpPoint, toPoint);
        final List<NoiseBarrier> obstructions = getObstructions(rpPoint, projectedPoint, projectedLineOfSight, fromLineOfSight, toLineOfSight);
        if (!obstructions.isEmpty()) {
            obstructions.sort(Comparator.comparingDouble(o -> rpPoint.distance(o.getGeometry())));

            List<Coordinate> edgeCandidates = new ArrayList<>();
            for (NoiseBarrier barrier : obstructions) {
                Geometry intersection = projectedLineOfSight.intersection(barrier.getGeometry());
                for (Coordinate coordinate : intersection.getCoordinates()) {
                    coordinate.z = barrier.getHeight();
                    edgeCandidates.add(coordinate);
                }
            }
            edgeCandidates.add(projectedPoint.getCoordinate());

            Coordinate lastFixedEdge = rpPoint.getCoordinate();
            Coordinate tmpEdge = rpPoint.getCoordinate();
            double currentHeight = GROUND_HEIGHT;
            double maxSlope = Double.NEGATIVE_INFINITY;

            List<Coordinate> consideredEdges = new ArrayList<>();

            for (Coordinate edge : edgeCandidates) {
                double slope = (edge.z - currentHeight) / lastFixedEdge.distance(edge);
                if (slope > maxSlope) {
                    maxSlope = slope;
                    tmpEdge = edge;
                } else {
                    maxSlope = Double.NEGATIVE_INFINITY;
                    lastFixedEdge = tmpEdge;
                    currentHeight = tmpEdge.z;
                    consideredEdges.add(lastFixedEdge);
                    slope = (edge.z - currentHeight) / lastFixedEdge.distance(edge);
                    if (slope > maxSlope) {
                        maxSlope = slope;
                        tmpEdge = edge;
                    }
                }
            }

            consideredEdges.remove(projectedPoint);

            final double firstEdgeYDiff = GROUND_HEIGHT - consideredEdges.get(0).z;
            double firstEdgeDistance = rpPoint.getCoordinate().distance(consideredEdges.get(0));
            double receiverToFirstEdgeDistance
                    = Math.sqrt(firstEdgeYDiff * firstEdgeYDiff + firstEdgeDistance * firstEdgeDistance);

            double shieldingDepth = 0;
            Iterator<Coordinate> it = consideredEdges.iterator();
            Coordinate edgeTemp = it.next();
            double heightTemp = edgeTemp.z;

            for (; it.hasNext(); ) {
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

            correctionTermShielding = NoiseEquations.calculateShieldingCorrection(
                    rpPoint.distance(projectedPoint), lastEdgeToSourceDistance, receiverToFirstEdgeDistance, shieldingDepth);
        }
        return correctionTermShielding;
    }

    private List<NoiseBarrier> getObstructions(Point receiver, Point source, Geometry directLineOfSight,
                                               Geometry fromLineOfSight, Geometry toLineOfSight) {
        final Collection<NoiseBarrier> candidates =
                noiseBarriers.getDisk(receiver.getX(), receiver.getY(), Math.max(100, directLineOfSight.getLength() *3));
        final List<NoiseBarrier> obstructing = new ArrayList<>();
        for (NoiseBarrier barrier : candidates) {
            if (isObstructing(receiver, source, directLineOfSight, fromLineOfSight, toLineOfSight, barrier.getGeometry())) {
                obstructing.add(barrier);
            }
        }
        return obstructing;
    }

    private boolean isObstructing(Geometry receiver, Geometry source, Geometry lineOfSight,
                                  Geometry fromLineOfSight, Geometry toLineOfSight, Geometry barrier) {
        return !barrier.contains(receiver)
                && !barrier.intersects(source)
                && lineOfSight.intersects(barrier)
                && fromLineOfSight.intersects(barrier)
                && toLineOfSight.intersects(barrier);
    }

    private Geometry constructLineOfSight(Point receiver, Point source) {
        Coordinate[] lineOfSightCoords = new Coordinate[2];
        lineOfSightCoords[0] = receiver.getCoordinate();
        lineOfSightCoords[1] = source.getCoordinate();
        return new GeometryFactory().createLineString(lineOfSightCoords);
    }
}
