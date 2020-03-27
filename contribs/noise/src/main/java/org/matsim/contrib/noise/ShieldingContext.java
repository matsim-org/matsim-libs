package org.matsim.contrib.noise;

import org.apache.log4j.Logger;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;
import org.locationtech.jts.index.strtree.STRtree;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.geometry.GeometryUtils;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * Separate from {@link NoiseContext} for better testability.
 *
 * @author nkuehnel
 */
final class ShieldingContext {

    private final static Logger logger = Logger.getLogger(ShieldingContext.class);

    //STRtree increases performance by ~40% by reducing the amount of potential
    //obstruction candidates. nkuehnel, mar '20
    private final STRtree noiseBarriers;
    private final static double GROUND_HEIGHT = 0.5;

    ShieldingContext(Collection<? extends NoiseBarrier> noiseBarriers) {

        this.noiseBarriers = new STRtree();
        for (NoiseBarrier barrier : noiseBarriers) {
            try {
                this.noiseBarriers.insert(barrier.getGeometry().getEnvelopeInternal(), barrier);
            } catch (IllegalArgumentException e) {
                logger.warn("Could not add noise barrier " + barrier.getId() + " to quad tree. Ignoring it.");
            }
        }
    }

    /**
     * determines the shielding correction for a receiver point for a given link emission source
     */
    double determineShieldingCorrection(ReceiverPoint receiverPoint, Link link, Coord projectedCoord) {
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

        if(!edgeCandidates.isEmpty()) {
            Coordinate lastFixedEdge = rpPoint.getCoordinate();
            Coordinate tmpEdge = rpPoint.getCoordinate();
            double currentHeight = GROUND_HEIGHT;

            List<Coordinate> consideredEdges = new ArrayList<>();

            double distToCurrentEdge = 0;
            while (lastFixedEdge != projectedPoint.getCoordinate()) {
                if(edgeCandidates.isEmpty()) {
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

            if(consideredEdges.isEmpty()) {
                return correctionTermShielding;
            }

            final double firstEdgeYDiff = GROUND_HEIGHT - consideredEdges.get(0).z;
            double firstEdgeDistance = rpPoint.getCoordinate().distance(consideredEdges.get(0));
            double receiverToFirstEdgeDistance
                    = Math.sqrt(firstEdgeYDiff * firstEdgeYDiff + firstEdgeDistance * firstEdgeDistance);

            double shieldingDepth = 0;

            Iterator<Coordinate> it = consideredEdges.iterator();
            Coordinate edgeTemp = it.next();
            while(it.hasNext()) {
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

    /**
     * Returns an ordered map of distances and coords to all obstruction edges
     */
    private ConcurrentSkipListMap<Double, Coordinate> getObstructionEdges(Point receiver, Point source, LineString directLineOfSight,
                                                                          LineString fromLineOfSight, LineString toLineOfSight) {
        final Collection<NoiseBarrier> candidates =
                noiseBarriers.query(directLineOfSight.getEnvelopeInternal());

        ConcurrentSkipListMap<Double, Coordinate> edgeCandidates = new ConcurrentSkipListMap<>();
        for (NoiseBarrier noiseBarrier : candidates) {
            //prepared geometry for repeated geometry relate-checks reduces run-time by 30%,
            //nkuehnel, mar '20
            final PreparedGeometry prepare = PreparedGeometryFactory.prepare(noiseBarrier.getGeometry());
            if (isObstructing(receiver, source, fromLineOfSight, toLineOfSight, prepare)) {
                Geometry intersection = directLineOfSight.intersection(noiseBarrier.getGeometry());
                for (Coordinate coordinate : intersection.getCoordinates()) {
                    coordinate.z = noiseBarrier.getHeight();
                    final double distance = receiver.getCoordinate().distance(coordinate);
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
     *
     * Uses prepared geometry for the barrier polygon to cache intersection graphs.
     */
    private boolean isObstructing(Geometry receiver, Geometry source, Geometry fromLineOfSight,
                                  Geometry toLineOfSight, PreparedGeometry barrier) {
        return barrier.intersects(fromLineOfSight)
                && barrier.intersects(toLineOfSight)
                && !barrier.contains(receiver)
                && !barrier.contains(source);
    }

    private LineString constructLineOfSight(Point receiver, Point source) {
        Coordinate[] lineOfSightCoords = new Coordinate[2];
        lineOfSightCoords[0] = receiver.getCoordinate();
        lineOfSightCoords[1] = source.getCoordinate();
        return new GeometryFactory().createLineString(lineOfSightCoords);
    }
}
