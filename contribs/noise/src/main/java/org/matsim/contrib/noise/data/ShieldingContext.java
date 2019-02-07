package org.matsim.contrib.noise.data;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.index.quadtree.Quadtree;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.noise.handler.NoiseEquations;
import org.matsim.core.utils.geometry.GeometryUtils;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * Separate from {@link NoiseContext} for better testability.
 *
 * @author nkuehnel
 */
public class ShieldingContext {

    private final static Logger logger = Logger.getLogger(ShieldingContext.class);

    private final Quadtree noiseBarriers;
    private final static double GROUND_HEIGHT = 0.5;

    ShieldingContext(Collection<? extends NoiseBarrier> noiseBarriers) {

        this.noiseBarriers = new Quadtree();
        for (NoiseBarrier barrier : noiseBarriers) {
            try {
                this.noiseBarriers.insert(barrier.getGeometry().getEnvelopeInternal(), barrier);
            } catch (IllegalArgumentException e) {
                logger.warn("Could not add noise barrier " + barrier.getId() + " to quad tree. Ignoring it.");
            }
        }
    }

    double determineShieldingCorrection(ReceiverPoint receiverPoint, Link link, Coord projectedCoord) {
        double correctionTermShielding = 0;
        final Point rpPoint = GeometryUtils.createGeotoolsPoint(receiverPoint.getCoord());
        final Point projectedPoint = GeometryUtils.createGeotoolsPoint(projectedCoord);
        projectedPoint.getCoordinate().z = GROUND_HEIGHT;

        final Point fromPoint = GeometryUtils.createGeotoolsPoint(link.getFromNode().getCoord());
        final Point toPoint = GeometryUtils.createGeotoolsPoint(link.getToNode().getCoord());

        Geometry projectedLineOfSight = constructLineOfSight(rpPoint, projectedPoint);
        Geometry fromLineOfSight = constructLineOfSight(rpPoint, fromPoint);
        Geometry toLineOfSight = constructLineOfSight(rpPoint, toPoint);

        NavigableMap<Double, Coordinate> edgeCandidates = getObstructionEdges(rpPoint, projectedPoint, projectedLineOfSight, fromLineOfSight, toLineOfSight);
        edgeCandidates.put(projectedLineOfSight.getLength(), projectedPoint.getCoordinate());

        if(!edgeCandidates.isEmpty()) {
            Coordinate lastFixedEdge = rpPoint.getCoordinate();
            Coordinate tmpEdge = rpPoint.getCoordinate();
            double currentHeight = GROUND_HEIGHT;

            List<Coordinate> consideredEdges = new ArrayList<>();

            double distToCurrentEdge = 0;
            while (lastFixedEdge != projectedPoint.getCoordinate()) {
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
                logger.warn("No edge candiates found. Skippig obstacle");
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

    private ConcurrentSkipListMap<Double, Coordinate> getObstructionEdges(Point receiver, Point source, Geometry directLineOfSight,
                                                                          Geometry fromLineOfSight, Geometry toLineOfSight) {
        final Collection<NoiseBarrier> candidates =
                noiseBarriers.query(directLineOfSight.getEnvelopeInternal());
        ConcurrentSkipListMap<Double, Coordinate> edgeCandidates = new ConcurrentSkipListMap<>();
        candidates.stream().forEach(noiseBarrier -> {
            if (isObstructing(receiver, source, directLineOfSight, fromLineOfSight, toLineOfSight, noiseBarrier.getGeometry())) {
                Geometry intersection = directLineOfSight.intersection(noiseBarrier.getGeometry());
                for (Coordinate coordinate : intersection.getCoordinates()) {
                    coordinate.z = noiseBarrier.getHeight();
                    final double distance = receiver.getCoordinate().distance(coordinate);
                    edgeCandidates.put(distance, coordinate);
                }
            }
        });
        return edgeCandidates;
    }

    private boolean isObstructing(Geometry receiver, Geometry source, Geometry lineOfSight,
                                  Geometry fromLineOfSight, Geometry toLineOfSight, Geometry barrier) {
        return !barrier.contains(receiver)
                && !barrier.contains(source)
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
