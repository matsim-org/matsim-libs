/*
 * Copyright (C) Schweizerische Bundesbahnen SBB, 2018.
 */

package ch.sbb.matsim.analysis.skims;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.misc.Counter;

/**
 * Calculates a zone-to-zone beeline distance matrix.
 * <p>
 * Idea of the algorithm: - select n random points per zone - for each zone-to-zone combination, calculate the beeline distance from each point to each other point in the destination zone. - this
 * results in n x n distances per zone-to-zone combination. - average the n x n distances and store this value as the zone-to-zone distance.
 *
 * @author mrieser / SBB
 */
public final class BeelineDistanceMatrix {

    private BeelineDistanceMatrix() {
    }

    public static <T> FloatMatrix<T> calculateBeelineDistanceMatrix(Set<T> zoneIds, Map<T, Coord[]> coordsPerZone, int numberOfThreads) {
        // prepare calculation
        FloatMatrix<T> matrix = new FloatMatrix<>(zoneIds, 0.0f);

        int numberOfPointsPerZone = coordsPerZone.values().iterator().next().length;

        // do calculation
        ConcurrentLinkedQueue<T> originZones = new ConcurrentLinkedQueue<>(zoneIds);

        Counter counter = new Counter("BeelineDistanceMatrix zone ", " / " + zoneIds.size());
        Thread[] threads = new Thread[numberOfThreads];
        for (int i = 0; i < numberOfThreads; i++) {
            BeelineDistanceMatrix.RowWorker<T> worker = new BeelineDistanceMatrix.RowWorker<>(originZones, zoneIds, coordsPerZone, matrix, counter);
            threads[i] = new Thread(worker, "BeelineDistanceMatrix-" + i);
            threads[i].start();
        }

        // wait until all threads have finished
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        matrix.multiply((float) (1.0 / numberOfPointsPerZone / numberOfPointsPerZone));
        return matrix;
    }

    private static class RowWorker<T> implements Runnable {

        private final ConcurrentLinkedQueue<T> originZones;
        private final Set<T> destinationZones;
        private final Map<T, Coord[]> coordsPerZone;
        private final FloatMatrix<T> matrix;
        private final Counter counter;

        RowWorker(ConcurrentLinkedQueue<T> originZones, Set<T> destinationZones, Map<T, Coord[]> coordsPerZone, FloatMatrix<T> matrix, Counter counter) {
            this.originZones = originZones;
            this.destinationZones = destinationZones;
            this.coordsPerZone = coordsPerZone;
            this.matrix = matrix;
            this.counter = counter;
        }

        @Override
        public void run() {
            while (true) {
                T fromZoneId = this.originZones.poll();
                if (fromZoneId == null) {
                    return;
                }

                this.counter.incCounter();
                Coord[] fromCoords = this.coordsPerZone.get(fromZoneId);
                if (fromCoords != null) {
                    for (Coord fromCoord : fromCoords) {

                        for (T toZoneId : this.destinationZones) {
                            Coord[] toCoords = this.coordsPerZone.get(toZoneId);
                            if (toCoords != null) {
                                for (Coord toCoord : toCoords) {
                                    double dist = CoordUtils.calcEuclideanDistance(fromCoord, toCoord);
                                    this.matrix.add(fromZoneId, toZoneId, (float) dist);
                                }
                            } else {
                                // this might happen if a zone has no geometry, for whatever reason...
                                this.matrix.set(fromZoneId, toZoneId, Float.POSITIVE_INFINITY);
                            }
                        }
                    }
                } else {
                    // this might happen if a zone has no geometry, for whatever reason...
                    for (T toZoneId : this.destinationZones) {
                        this.matrix.set(fromZoneId, toZoneId, Float.POSITIVE_INFINITY);
                    }
                }
            }
        }
    }
}
