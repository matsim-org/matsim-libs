/* *********************************************************************** *
 * project: org.matsim.* 												   *
 *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2023 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package ch.sbb.matsim.analysis.skims;

import ch.sbb.matsim.analysis.skims.RooftopUtils.ODConnection;
import ch.sbb.matsim.routing.pt.raptor.RaptorParameters;
import ch.sbb.matsim.routing.pt.raptor.RaptorRoute;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptor;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorCore.TravelInfo;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorData;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

/**
 * Calculates zone-to-zone matrices containing a number of performance indicators related to public transport.
 * <p>
 * Amongst the performance indicators are: - travel time (measured from the first departure to the last arrival, without access/egress time) - travel distance (measured from first departure to last
 * arrival, without access/egress time) - access time - egress time - perceived frequency - share of travel time within trains - share of travel distance within trains
 * <p>
 * The main idea to calculate the frequencies follows the rooftop-algorithm from Niek Guis (ca. 2015).
 * <p>
 * Idea of the algorithm for a single origin-destination (OD) pair: - Given a time window (e.g. 07:00 - 08:00), - find all possible (useful) connections between O and D - for each minute in the time
 * window, calculate the required adaption time to catch the next best connection (can be x minutes earlier or later) - average the minutely adaption times over the time window - based on the average
 * adaption time, the service frequency can be calculated
 * <p>
 * Idea of the algorithm for a full zone-to-zone matrix: - given n points per zone - for each point, find the possible stops to be used as departure or arrival stops. - for each zone-to-zone
 * combination, calculate the average adaption time to travel from each point to each other point in the destination zone. - this results in n x n average adaption travel times per zone-to-zone
 * combination. - average the n x n adaption times and store this value as the zone-to-zone adaption time.
 * <p>
 * A basic implementation for calculating the travel times between m zones would result in m^2 * n^2 pt route calculations, which could take a very long time. The actual algorithm makes use of
 * LeastCostPathTrees, reducing the computational effort down to the calculation of m*n LeastCostPathTrees. In addition, it supports running the calculation in parallel to reduce the time required to
 * compute one matrix.
 * <p>
 * If no connection can be found between two zones (can happen when there is no transit stop in a zone), the corresponding matrix cells contain the value "0" for the perceived frequency, and
 * "Infinity" for all other skim matrices.
 *
 * @author mrieser / SBB
 */
public class PTSkimMatrices {

    private PTSkimMatrices() {
    }

    public static <T> PTSkimMatrices.PtIndicators<T> calculateSkimMatrices(SwissRailRaptorData raptorData, Map<T, Coord[]> coordsPerZone, double minDepartureTime, double maxDepartureTime,
            double stepSize_seconds, RaptorParameters parameters, int numberOfThreads, BiPredicate<TransitLine, TransitRoute> trainDetector, CoordAggregator coordAggregator) {
        // prepare calculation
        Set<T> zoneIds = coordsPerZone.keySet();
        PtIndicators<T> pti = new PtIndicators<>(zoneIds);
        Config config = ConfigUtils.createConfig();

        // do calculation
        ConcurrentLinkedQueue<T> originZones = new ConcurrentLinkedQueue<>(zoneIds);

        Counter counter = new Counter("PT-FrequencyMatrix-" + Time.writeTime(minDepartureTime) + "-" + Time.writeTime(maxDepartureTime) + " zone ", " / " + coordsPerZone.size());
        Thread[] threads = new Thread[numberOfThreads];
        for (int i = 0; i < numberOfThreads; i++) {
            SwissRailRaptor raptor = new SwissRailRaptor.Builder(raptorData, config).build();
            RowWorker<T> worker = new RowWorker<>(originZones, zoneIds, coordsPerZone, pti, raptor, parameters, minDepartureTime, maxDepartureTime, stepSize_seconds, counter, trainDetector, coordAggregator);
            threads[i] = new Thread(worker, "PT-FrequencyMatrix-" + Time.writeTime(minDepartureTime) + "-" + Time.writeTime(maxDepartureTime) + "-" + i);
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

        for (T fromZoneId : zoneIds) {
            for (T toZoneId : zoneIds) {
                float count = pti.dataCountMatrix.get(fromZoneId, toZoneId);
                if (count == 0) {
                    pti.adaptionTimeMatrix.set(fromZoneId, toZoneId, Float.POSITIVE_INFINITY);
                    pti.frequencyMatrix.set(fromZoneId, toZoneId, 0);
                    pti.distanceMatrix.set(fromZoneId, toZoneId, Float.POSITIVE_INFINITY);
                    pti.travelTimeMatrix.set(fromZoneId, toZoneId, Float.POSITIVE_INFINITY);
                    pti.accessTimeMatrix.set(fromZoneId, toZoneId, Float.POSITIVE_INFINITY);
                    pti.egressTimeMatrix.set(fromZoneId, toZoneId, Float.POSITIVE_INFINITY);
                    pti.transferCountMatrix.set(fromZoneId, toZoneId, Float.POSITIVE_INFINITY);
                    pti.trainDistanceShareMatrix.set(fromZoneId, toZoneId, Float.POSITIVE_INFINITY);
                    pti.trainTravelTimeShareMatrix.set(fromZoneId, toZoneId, Float.POSITIVE_INFINITY);
                } else {
                    float avgFactor = 1.0f / count;
                    float adaptionTime = pti.adaptionTimeMatrix.multiply(fromZoneId, toZoneId, avgFactor);
                    pti.distanceMatrix.multiply(fromZoneId, toZoneId, avgFactor);
                    pti.travelTimeMatrix.multiply(fromZoneId, toZoneId, avgFactor);
                    pti.accessTimeMatrix.multiply(fromZoneId, toZoneId, avgFactor);
                    pti.egressTimeMatrix.multiply(fromZoneId, toZoneId, avgFactor);
                    pti.trainDistanceShareMatrix.multiply(fromZoneId, toZoneId, avgFactor);
                    pti.trainTravelTimeShareMatrix.multiply(fromZoneId, toZoneId, avgFactor);
                    pti.transferCountMatrix.multiply(fromZoneId, toZoneId, avgFactor);
                    float frequency = (float) ((maxDepartureTime - minDepartureTime) / adaptionTime / 4.0);
                    pti.frequencyMatrix.set(fromZoneId, toZoneId, frequency);
                }
            }
        }

        return pti;
    }

    static class RowWorker<T> implements Runnable {

        private final ConcurrentLinkedQueue<T> originZones;
        private final Set<T> destinationZones;
        private final Map<T, Coord[]> coordsPerZone;
        private final PtIndicators<T> pti;
        private final SwissRailRaptor raptor;
        private final RaptorParameters parameters;
        private final double minDepartureTime;
        private final double maxDepartureTime;
        private final double stepSize;
        private final Counter counter;
        private final BiPredicate<TransitLine, TransitRoute> trainDetector;
		private final CoordAggregator coordAggregator;

		RowWorker(ConcurrentLinkedQueue<T> originZones, Set<T> destinationZones, Map<T, Coord[]> coordsPerZone, PtIndicators<T> pti, SwissRailRaptor raptor, RaptorParameters parameters,
				  double minDepartureTime, double maxDepartureTime, double stepSize, Counter counter, BiPredicate<TransitLine, TransitRoute> trainDetector, CoordAggregator coordAggregator) {
            this.originZones = originZones;
            this.destinationZones = destinationZones;
            this.coordsPerZone = coordsPerZone;
            this.pti = pti;
            this.raptor = raptor;
            this.parameters = parameters;
            this.minDepartureTime = minDepartureTime;
            this.maxDepartureTime = maxDepartureTime;
            this.stepSize = stepSize;
            this.counter = counter;
            this.trainDetector = trainDetector;
			this.coordAggregator = coordAggregator;
        }

        private static Collection<TransitStopFacility> findStopCandidates(Coord coord, SwissRailRaptor raptor, RaptorParameters parameters) {
            Collection<TransitStopFacility> stops = raptor.getUnderlyingData().findNearbyStops(coord.getX(), coord.getY(), parameters.getSearchRadius());
            if (stops.isEmpty()) {
                TransitStopFacility nearest = raptor.getUnderlyingData().findNearestStop(coord.getX(), coord.getY());
                double nearestStopDistance = CoordUtils.calcEuclideanDistance(coord, nearest.getCoord());
                stops = raptor.getUnderlyingData().findNearbyStops(coord.getX(), coord.getY(), nearestStopDistance + parameters.getExtensionRadius());
            }
            return stops;
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
				var weightedRelevantFromCoords = coordAggregator.aggregateCoords(fromCoords);
					for (var fromCoord : weightedRelevantFromCoords) {
                        calcForRow(fromZoneId, fromCoord.coord(),fromCoord.weight());
                    }
                }
            }
        }

        private void calcForRow(T fromZoneId, Coord fromCoord, double fromCoordWeight) {
            double walkSpeed = this.parameters.getBeelineWalkSpeed();

            Collection<TransitStopFacility> fromStops = findStopCandidates(fromCoord, this.raptor, this.parameters);
            Map<Id<TransitStopFacility>, Double> accessTimes = new HashMap<>();
            for (TransitStopFacility stop : fromStops) {
                double distance = CoordUtils.calcEuclideanDistance(fromCoord, stop.getCoord());
                double accessTime = distance / walkSpeed;
                accessTimes.put(stop.getId(), accessTime);
            }

            List<Map<Id<TransitStopFacility>, TravelInfo>> trees = new ArrayList<>();

            double timeWindow = this.maxDepartureTime - this.minDepartureTime;
            double endTime = this.maxDepartureTime + timeWindow;
            for (double time = this.minDepartureTime - timeWindow; time < endTime; time += this.stepSize) {
                Map<Id<TransitStopFacility>, TravelInfo> tree = this.raptor.calcTree(fromStops, time, this.parameters, null);
                trees.add(tree);
            }

            for (T toZoneId : this.destinationZones) {
                Coord[] toCoords = this.coordsPerZone.get(toZoneId);
                if (toCoords != null) {
                    for (Coord toCoord : toCoords) {
                        calcForOD(fromZoneId, toZoneId, toCoord, accessTimes, trees, (float) fromCoordWeight);
                    }
                }
            }
        }

        private void calcForOD(T fromZoneId, T toZoneId, Coord toCoord, Map<Id<TransitStopFacility>, Double> accessTimes, List<Map<Id<TransitStopFacility>, TravelInfo>> trees, float fromCoordWeight) {
            double walkSpeed = this.parameters.getBeelineWalkSpeed();

            Collection<TransitStopFacility> toStops = findStopCandidates(toCoord, this.raptor, this.parameters);
            Map<Id<TransitStopFacility>, Double> egressTimes = new HashMap<>();
            for (TransitStopFacility stop : toStops) {
                double distance = CoordUtils.calcEuclideanDistance(stop.getCoord(), toCoord);
                double egressTime = distance / walkSpeed;
                egressTimes.put(stop.getId(), egressTime);
            }

            List<ODConnection> connections = buildODConnections(trees, accessTimes, egressTimes);
            if (connections.isEmpty()) {
                return;
            }

            connections = RooftopUtils.sortAndFilterConnections(connections, maxDepartureTime);

            double avgAdaptionTime = RooftopUtils.calcAverageAdaptionTime(connections, minDepartureTime, maxDepartureTime);

            this.pti.adaptionTimeMatrix.add(fromZoneId, toZoneId, (float) avgAdaptionTime);

            Map<ODConnection, Double> connectionShares = RooftopUtils.calcConnectionShares(connections, minDepartureTime, maxDepartureTime);

            float accessTime = 0;
            float egressTime = 0;
            float transferCount = 0;
            float travelTime = 0;

            double totalDistance = 0;
            double trainDistance = 0;
            double totalInVehTime = 0;
            double trainInVehTime = 0;

            for (Map.Entry<ODConnection, Double> e : connectionShares.entrySet()) {
                ODConnection connection = e.getKey();
                double share = e.getValue();

                accessTime += share * accessTimes.get(connection.travelInfo.departureStop).floatValue();
                egressTime += share * (float) connection.egressTime;
                transferCount += share * (float) connection.transferCount;
                travelTime += share * (float) connection.totalTravelTime();

                double connTotalDistance = 0;
                double connTrainDistance = 0;
                double connTotalInVehTime = 0;
                double connTrainInVehTime = 0;

                RaptorRoute route = connection.travelInfo.getRaptorRoute();
                for (RaptorRoute.RoutePart part : route.getParts()) {
                    if (part.line != null) {
                        // it's a non-transfer part, an actual pt stage

                        boolean isTrain = this.trainDetector.test(part.line, part.route);
                        double inVehicleTime = part.arrivalTime - part.boardingTime;

                        connTotalDistance += part.distance;
                        connTotalInVehTime += inVehicleTime;

                        if (isTrain) {
                            connTrainDistance += part.distance;
                            connTrainInVehTime += inVehicleTime;
                        }
                    }
                }

                totalDistance += share * connTotalDistance;
                trainDistance += share * connTrainDistance;
                totalInVehTime += share * connTotalInVehTime;
                trainInVehTime += share * connTrainInVehTime;
            }

            float trainShareByTravelTime = (float) (trainInVehTime / totalInVehTime);
            float trainShareByDistance = (float) (trainDistance / totalDistance);

            this.pti.accessTimeMatrix.add(fromZoneId, toZoneId, accessTime*fromCoordWeight);
            this.pti.egressTimeMatrix.add(fromZoneId, toZoneId, egressTime*fromCoordWeight);
            this.pti.transferCountMatrix.add(fromZoneId, toZoneId, transferCount*fromCoordWeight);
            this.pti.travelTimeMatrix.add(fromZoneId, toZoneId, travelTime*fromCoordWeight);
            this.pti.distanceMatrix.add(fromZoneId, toZoneId, (float) totalDistance*fromCoordWeight);
            this.pti.trainDistanceShareMatrix.add(fromZoneId, toZoneId, trainShareByDistance*fromCoordWeight);
            this.pti.trainTravelTimeShareMatrix.add(fromZoneId, toZoneId, trainShareByTravelTime*fromCoordWeight);

            this.pti.dataCountMatrix.add(fromZoneId, toZoneId, fromCoordWeight);
        }

        private List<ODConnection> buildODConnections(List<Map<Id<TransitStopFacility>, TravelInfo>> trees, Map<Id<TransitStopFacility>, Double> accessTimes,
                Map<Id<TransitStopFacility>, Double> egressTimes) {
            List<ODConnection> connections = new ArrayList<>();

            for (Map<Id<TransitStopFacility>, TravelInfo> tree : trees) {
                for (Map.Entry<Id<TransitStopFacility>, Double> egressEntry : egressTimes.entrySet()) {
                    Id<TransitStopFacility> egressStopId = egressEntry.getKey();
                    Double egressTime = egressEntry.getValue();
                    TravelInfo info = tree.get(egressStopId);
                    if (info != null && !info.isWalkOnly()) {
                        Double accessTime = accessTimes.get(info.departureStop);
                        ODConnection connection = new ODConnection(info.ptDepartureTime, info.ptTravelTime, accessTime, egressTime, info.transferCount, info);
                        connections.add(connection);
                    }
                }
            }

            return connections;
        }
    }

    public static class PtIndicators<T> {

        public final FloatMatrix<T> adaptionTimeMatrix;
        public final FloatMatrix<T> frequencyMatrix;

        public final FloatMatrix<T> distanceMatrix;
        public final FloatMatrix<T> travelTimeMatrix;
        public final FloatMatrix<T> accessTimeMatrix;
        public final FloatMatrix<T> egressTimeMatrix;
        public final FloatMatrix<T> transferCountMatrix;
        public final FloatMatrix<T> trainTravelTimeShareMatrix;
        public final FloatMatrix<T> trainDistanceShareMatrix;

        public final FloatMatrix<T> dataCountMatrix; // how many values/routes were taken into account to calculate the averages

        PtIndicators(Set<T> zones) {
            this.adaptionTimeMatrix = new FloatMatrix<>(zones, 0);
            this.frequencyMatrix = new FloatMatrix<>(zones, 0);

            this.distanceMatrix = new FloatMatrix<>(zones, 0);
            this.travelTimeMatrix = new FloatMatrix<>(zones, 0);
            this.accessTimeMatrix = new FloatMatrix<>(zones, 0);
            this.egressTimeMatrix = new FloatMatrix<>(zones, 0);
            this.transferCountMatrix = new FloatMatrix<>(zones, 0);
            this.dataCountMatrix = new FloatMatrix<>(zones, 0);
            this.trainTravelTimeShareMatrix = new FloatMatrix<>(zones, 0);
            this.trainDistanceShareMatrix = new FloatMatrix<>(zones, 0);
        }
    }

	public interface CoordAggregator{
		default List<CalculateSkimMatrices.WeightedCoord> aggregateCoords(Coord[] coords){
			return Arrays.stream(coords).map(coord -> new CalculateSkimMatrices.WeightedCoord(coord,1.0)).collect(Collectors.toList());
		}
	}

}
