package org.matsim.contrib.noise.data;

import com.sun.istack.internal.NotNull;
import com.vividsolutions.jts.geom.*;
import org.matsim.contrib.noise.handler.NoiseEquations;
import org.matsim.core.utils.collections.QuadTree;

import java.util.*;

/** Separate from {@link NoiseContext} for better testability.
 *
 * @author nkuehnel
 */
public class ShieldingContext {

    private final QuadTree<NoiseBarrier> noiseBarriers;
    private final static double GROUND_HEIGHT = 0.5;

    public ShieldingContext(Collection<? extends NoiseBarrier> noiseBarriers) {
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE;
        double maxY = Double.MIN_VALUE;

        for(NoiseBarrier barrier: noiseBarriers) {

            final Envelope envelope = barrier.getGeometry().getEnvelopeInternal();

            if(envelope.getMinX() < minX) {
                minX = envelope.getMinX();
            }
            if(envelope.getMinY() < minY) {
                minY = envelope.getMinY() ;
            }
            if(envelope.getMaxX() > maxX) {
                maxX = envelope.getMaxX();
            }
            if(envelope.getMaxY() > maxY) {
                maxY = envelope.getMaxY();
            }
        }
        this.noiseBarriers = new QuadTree<>(minX - 100, minY -100, maxX + 100, maxY + 100);
        for(NoiseBarrier barrier: noiseBarriers) {
            this.noiseBarriers.put(barrier.getGeometry().getCentroid().getX(), barrier.getGeometry().getCentroid().getY(), barrier);
        }
    }


    public double determineShieldingCorrection(Point receiverPoint, Point projectedSource, double distance) {
        double correctionTermShielding = 0;
        Geometry lineOfSight = constructLineOfSight(receiverPoint, projectedSource);
        final List<NoiseBarrier> obstructions = getObstructions(receiverPoint, projectedSource, lineOfSight, distance);
        if(!obstructions.isEmpty()) {
            obstructions.sort(Comparator.comparingDouble(o -> receiverPoint.distance(o.getGeometry())));

            Iterator<NoiseBarrier> it = obstructions.iterator();
            NoiseBarrier firstBarrier = it.next();

            final Geometry intersection = lineOfSight.intersection(firstBarrier.getGeometry());
            if(intersection.getCoordinates().length != 2) {
                throw new RuntimeException("Line of sight should intersect obstruction at exactly 2 points.");
            }

            Coordinate firstEdge = intersection.getCoordinates()[0];
            Coordinate lastEdge = intersection.getCoordinates()[1];

            double currentHeight = firstBarrier.getHeight();
            double heightDiff = currentHeight - GROUND_HEIGHT;

            double receiverToFirstEdgeDistance =
                    get3DDistance(heightDiff, receiverPoint.getCoordinate(), firstEdge);
            double depthOfShielding = 0;

            if(it.hasNext()) {

            } else {
                depthOfShielding += firstEdge.distance(lastEdge);
            }

            double lastEdgeToSourceDistance = get3DDistance(heightDiff, lastEdge, projectedSource.getCoordinate());
			correctionTermShielding = NoiseEquations.calculateShieldingCorrection(
					distance, lastEdgeToSourceDistance, receiverToFirstEdgeDistance, depthOfShielding);
        }
        return correctionTermShielding;
    }

    @NotNull
    public List<NoiseBarrier> getObstructions(Point receiver, Point source, Geometry lineOfSight, double distance) {
        final Collection<NoiseBarrier> candidates =
                noiseBarriers.getDisk(receiver.getX(), receiver.getY(), distance);
        final List<NoiseBarrier> obstructing = new ArrayList<>();
        for(NoiseBarrier barrier: candidates) {
            if(isObstructing(receiver, source, lineOfSight, barrier.getGeometry())) {
                obstructing.add(barrier);
            }
        }
        return obstructing;
    }

    public boolean isObstructing(Geometry receiver, Geometry source, Geometry lineOfSight, Geometry barrier) {
        return lineOfSight.crosses(barrier) && !barrier.contains(receiver) && !barrier.contains(source);
    }

    public Geometry constructLineOfSight(Point receiver, Point source) {
        Coordinate[] lineOfSightCoords = new Coordinate[2];
        lineOfSightCoords[0] = receiver.getCoordinate();
        lineOfSightCoords[1] = source.getCoordinate();
        return new GeometryFactory().createLineString(lineOfSightCoords);
    }

    public double get3DDistance(double heightDiff, Coordinate from, Coordinate to) {
        double distanceDiff = from.distance(to);
        return Math.sqrt(heightDiff * heightDiff + distanceDiff * distanceDiff);
    }
}
