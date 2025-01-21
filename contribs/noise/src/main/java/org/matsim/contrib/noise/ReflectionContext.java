package org.matsim.contrib.noise;

import jakarta.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.algorithm.Angle;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.geom.util.AffineTransformation;
import org.matsim.core.config.Config;

import java.util.*;

/**
 * Separate from {@link NoiseContextImpl} for better testability.
 *
 * @author nkuehnel
 */
public final class ReflectionContext {

    static final double SCAN_LINE_LENGTH = 1;

    private final static Logger logger = LogManager.getLogger(ReflectionContext.class);

    private Set<LineSegment> visibleEdges;
    private Coordinate receiver;

    private final BarrierContext barrierContext;
    private final GeometryFactory geomFactory = new GeometryFactory();

    record ReflectionTuple(LineSegment facade, LineSegment reflectionLink) { }

    @Inject
    ReflectionContext(BarrierContext barrierContext) {
        this.barrierContext = barrierContext;
    }

    ReflectionContext(Collection<FeatureNoiseBarrierImpl> barriers, Config config, BarrierContext barrierContext) {
        this.barrierContext = barrierContext;
    }

    void setCurrentReceiver(NoiseReceiverPoint nrp) {

        receiver = new Coordinate(nrp.getCoord().getX(), nrp.getCoord().getY());

        final Collection<NoiseBarrier> candidates =
                barrierContext.query(new GeometryFactory().createPoint(receiver).buffer(200).getEnvelopeInternal());

        visibleEdges = new HashSet<>();

        for (NoiseBarrier noiseBarrier : candidates) {
            if (noiseBarrier.getGeometry().contains(geomFactory.createPoint(receiver))) {
                continue;
            }
            final LineString exteriorRing = ((Polygon) noiseBarrier.getGeometry().getGeometry()).getExteriorRing();
            final Coordinate[] coordinates = exteriorRing.getCoordinates();
            List<LineSegment> edges = new ArrayList<>();
            for (int k = 0; k < coordinates.length - 1; k++) {
                edges.add(new LineSegment(coordinates[k], coordinates[k + 1]));
            }
            visibleEdges.addAll(findVisibleEdgesOfPolygon(edges, receiver));
        }
    }


    private Set<LineSegment> findVisibleEdgesOfPolygon(List<LineSegment> polygonEdges, Coordinate coordinate) {

        Coordinate coordXinc = new Coordinate(coordinate.x + 1, coordinate.y);

        double minAngle = Double.POSITIVE_INFINITY;
        double maxAngle = Double.NEGATIVE_INFINITY;

        Set<LineSegment> fixedSegments = new HashSet<>();

        for (LineSegment segment : polygonEdges) {
            for (int i = 0; i < 2; i++) {
                double v = Angle.angleBetweenOriented(coordXinc, coordinate, segment.getCoordinate(i));
                if (v < 0) {
                    v = 2 * Math.PI + v;
                }
                if (v > maxAngle) {
                    maxAngle = v;
                }
                if (v < minAngle) {
                    minAngle = v;
                }
            }
        }

        double increment = (maxAngle - minAngle) / 10;

        for (double angle = minAngle; angle <= maxAngle; angle += increment) {
            double x = coordinate.x + Math.cos(angle) * SCAN_LINE_LENGTH;
            double y = coordinate.y + Math.sin(angle) * SCAN_LINE_LENGTH;
            LineSegment scanLine = new LineSegment(coordinate, new Coordinate(x, y));

            LineSegment candidate = null;
            for (LineSegment segment : polygonEdges) {
                final int i = scanLine.orientationIndex(segment.p0);
                final int i1 = scanLine.orientationIndex(segment.p1);
                if (i - i1 != 0) {
                    if (candidate != null) {

                        final int i2 = candidate.orientationIndex(coordinate);
                        final int i3 = candidate.orientationIndex(segment.p0);
                        final int i4 = candidate.orientationIndex(segment.p1);
                        if ((i2 + i3) * i4 > 0) {
                            candidate = segment;
                        }
                    } else {
                        candidate = segment;
                    }
                }
            }
            if (candidate != null) {
                fixedSegments.add(candidate);
            }
        }
        return fixedSegments;
    }

    Set<ReflectionTuple> getReflections(LineSegment originalLink) {
        if (receiver == null) {
            return Collections.emptySet();
        }

        final LineString temp = originalLink.toGeometry(geomFactory);

        Set<ReflectionTuple> reflections = new HashSet<>();
        for (LineSegment facade : visibleEdges) {
            if (hit(facade, originalLink)) {
                final AffineTransformation transformation = AffineTransformation.reflectionInstance(facade.p0.x, facade.p0.y, facade.p1.x, facade.p1.y);

                final Geometry transform = transformation.transform(temp);

                LineSegment mirrored = new LineSegment(transform.getCoordinates()[0], transform.getCoordinates()[1]);

                LineSegment seg = new LineSegment(mirrored.p1, receiver);
                LineSegment seg2 = new LineSegment(mirrored.p0, receiver);
                final Coordinate coordinate1 = facade.lineIntersection(seg);
                final Coordinate coordinate2 = facade.lineIntersection(seg2);

                if (coordinate1 == null || coordinate2 == null) {
                    continue;
                }

                final double fraction1 = facade.segmentFraction(coordinate1);
                final double fraction2 = facade.segmentFraction(coordinate2);
                if (fraction1 == fraction2) {
                    continue;
                }

                LineSegment final1 = new LineSegment(receiver, facade.pointAlong(fraction1));
                LineSegment final2 = new LineSegment(receiver, facade.pointAlong(fraction2));

                LineSegment linkSegment = new LineSegment(final1.lineIntersection(mirrored), final2.lineIntersection(mirrored));
                reflections.add(new ReflectionTuple(facade, linkSegment));
            }
        }
        return reflections;
    }

    double getMultipleReflectionCorrection(LineSegment segment) {
        final Coordinate coordinate = segment.midPoint();

        Coordinate candidateRight = getReflectionSegment(coordinate, segment, 400);
        Coordinate candidateLeft = null;
        if(candidateRight != null) {
            final double remainingD = 400 - candidateRight.distance(coordinate);
            if(remainingD > 0) {
                candidateLeft = getReflectionSegment(coordinate, segment, -remainingD);
            }
        } else {
            return 0;
        }
        if (candidateLeft != null){
            double w = candidateLeft.distance(candidateRight);
            return Math.min(2 * Math.min(candidateLeft.z, candidateRight.z) / w, 1.6);
        }

        return 0;
    }

    private Coordinate getReflectionSegment(Coordinate coordinate, LineSegment segment, double length) {

		if (segment.getLength() == 0) {
			// yyyy added this here to avoid crashing feb'2023 - cr
			logger.warn("Zero length line segment on {}", coordinate);
			return null;
		}

        final Coordinate right = segment.pointAlongOffset(0.5, length);
        LineSegment segmentRight = new LineSegment(coordinate, right);

        final LineString lineStringRight = segmentRight.toGeometry(geomFactory);
        Envelope envelope = lineStringRight.getEnvelopeInternal();
        final Collection<NoiseBarrier> query = barrierContext.query(envelope);

        LineSegment candidate = null;
        Coordinate intersection = null;
        for (NoiseBarrier noiseBarrier : query) {
            if (noiseBarrier.getGeometry().intersects(lineStringRight)) {
                final LineString ring = ((Polygon) noiseBarrier.getGeometry().getGeometry()).getExteriorRing();
                for (int j = 1; j < ring.getNumPoints(); ++j) {
                    Coordinate p1 = ring.getCoordinateN(j);
                    Coordinate p2 = ring.getCoordinateN(j - 1);
                    LineSegment edge = new LineSegment(p1, p2);
                    final Coordinate intersection1 = segmentRight.intersection(edge);
                    if (intersection1 != null) {
                        if (candidate != null) {
                            final int i2 = candidate.orientationIndex(coordinate);
                            final int i3 = candidate.orientationIndex(edge.p0);
                            final int i4 = candidate.orientationIndex(edge.p1);
                            if ((i2 + i3) * i4 > 0) {
                                candidate = edge;
                                intersection = intersection1;
                                intersection.z = noiseBarrier.getHeight();
                            }
                        } else {
                            candidate = edge;
                            intersection = intersection1;
                            intersection.z = noiseBarrier.getHeight();
                        }
                    }
                }
            }
        }

        if (intersection != null) {
            final double angle = Angle.toDegrees(Angle.angleBetween(segmentRight.p0, intersection, candidate.p1));
            if (angle < 95 && angle > 85) {
                return intersection;
            }
        }
        return null;
    }

    private boolean hit(LineSegment facade, LineSegment originalLink) {

        if (facade.distance(originalLink) > 100) {
            return false;
        }

        LineSegment line = new LineSegment(originalLink.midPoint(), facade.midPoint());
        final LineString lineString = line.toGeometry(geomFactory);
        Envelope envelope = new Envelope(lineString.getEnvelopeInternal());
        final Collection<NoiseBarrier> query = barrierContext.query(envelope);

        for (NoiseBarrier noiseBarrier : query) {
            if (noiseBarrier.getGeometry().intersects(lineString)) {
                final LineString ring = ((Polygon) noiseBarrier.getGeometry().getGeometry()).getExteriorRing();
                for (int i = 1; i < ring.getNumPoints(); ++i) {
                    Coordinate p1 = ring.getCoordinateN(i);
                    Coordinate p2 = ring.getCoordinateN(i - 1);
                    LineSegment segment = new LineSegment(p1, p2);
                    if (intersects(line, segment)) {
                        if (!facade.equalsTopo(segment)) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    private static boolean intersects(LineSegment segment1, LineSegment segment2) {

        double dx0 = segment1.p1.x - segment1.p0.x;
        double dx1 = segment2.p1.x - segment2.p0.x;

        double dy0 = segment1.p1.y - segment1.p0.y;
        double dy1 = segment2.p1.y - segment2.p0.y;

        double p0 = dy1 * (segment2.p1.x - segment1.p0.x) - dx1 * (segment2.p1.y - segment1.p0.y);
        double p1 = dy1 * (segment2.p1.x - segment1.p1.x) - dx1 * (segment2.p1.y - segment1.p1.y);
        double p2 = dy0 * (segment1.p1.x - segment2.p0.x) - dx0 * (segment1.p1.y - segment2.p0.y);
        double p3 = dy0 * (segment1.p1.x - segment2.p1.x) - dx0 * (segment1.p1.y - segment2.p1.y);
        return (p0 * p1 <= 0) && (p2 * p3 <= 0);
    }
}
