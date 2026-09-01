package org.matsim.contrib.pseudosimulation.distributed.listeners.events.transit;

import org.matsim.api.core.v01.Id;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class TransitPerformance implements Serializable {
    private Map<String, DwellEventsForLine> linesToStopDwellEvents = new HashMap<>();

    public BoardingModel getBoardingModel() {
        return boardingModel;
    }

    private Random rand = MatsimRandom.getLocalInstance();

    private BoardingModel boardingModel;

    public TransitPerformance(BoardingModel boardingModel) {
        this.boardingModel = boardingModel;
    }

    TransitPerformance(BoardingModel boardingModel, Random random) {
        this.boardingModel = boardingModel;
        this.rand = random;
    }

    public TransitPerformance() {
        this.boardingModel = new BoardingModelStochasticLinear();
    }

    public void setBoardingModel(BoardingModel boardingModel) {
        this.boardingModel = boardingModel;
    }

    public void addVehicleDwellEventAtStop(Id<TransitLine> line, Id<TransitRoute> route, Id<TransitStopFacility> stopId, DwellEvent dwellEvent) {
        DwellEventsForLine stopDwellEventsForLine = linesToStopDwellEvents.get(line.toString());
        if (stopDwellEventsForLine == null) {
            stopDwellEventsForLine = new DwellEventsForLine();
            linesToStopDwellEvents.put(line.toString(),stopDwellEventsForLine);
        }
        stopDwellEventsForLine.addVehicleDwellEventAtStop(route, stopId, dwellEvent);
    }


    public Tuple<Double, Double> getRouteTravelTime(Id<TransitLine> line, Id<TransitRoute> route, Id<TransitStopFacility> originStop, Id<TransitStopFacility> destinationStop, double time) {
        try {
            return linesToStopDwellEvents.get(line.toString()).getRouteTravelTime(route, originStop, destinationStop, time);
        }catch(NullPointerException ne){
            return new Tuple<>(Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY);
        }
    }


    private class DwellEventsAtStop implements Serializable {
        private static final int SAMPLE_SIZE = 6;
        private List<DwellEvent> dwellEvents;
        private boolean chronologicallyOrdered = true;

        public DwellEventsAtStop() {
            this.dwellEvents = new ArrayList<>();

        }

        public void addVehicleDwellEventAtStop(DwellEvent dwellEvent) {
            if (!dwellEvents.isEmpty()
                    && dwellEvent.getArrivalTime() < dwellEvents.get(dwellEvents.size() - 1).getArrivalTime()) {
                chronologicallyOrdered = false;
            }
            this.dwellEvents.add(dwellEvent);
        }

        public Tuple<Double, Double> getTravelTime(Id<TransitStopFacility> destinationStop, double time) {
            int firstCandidate = chronologicallyOrdered ? firstArrivalAtOrAfter(time) : 0;
            int start = Math.max(0, firstCandidate - (SAMPLE_SIZE - 1));
            double[] recentInVehicleTimes = new double[SAMPLE_SIZE];
            int recentCount = 0;
            int nextSlot = 0;
            for (int index = start; index < dwellEvents.size(); index++) {
                DwellEvent dwellEvent = dwellEvents.get(index);
                recentInVehicleTimes[nextSlot] = dwellEvent.getVehicle()
                        .getInVehicleTime(dwellEvent, destinationStop);
                nextSlot = (nextSlot + 1) % SAMPLE_SIZE;
                recentCount = Math.min(recentCount + 1, SAMPLE_SIZE);
                if (dwellEvent.getArrivalTime() >= time) {
                    if (!boardingModel.canBoard(dwellEvent.getOccupancyAtDeparture())) {
                        continue;
                    }
                    int oldestSlot = recentCount == SAMPLE_SIZE ? nextSlot : 0;
                    int sampledSlot = (oldestSlot + rand.nextInt(recentCount)) % SAMPLE_SIZE;
                    double inVehicleTime = recentInVehicleTimes[sampledSlot];
                    if (!Double.isInfinite(inVehicleTime)) {
                        return new Tuple<>(dwellEvent.getArrivalTime() - time, inVehicleTime);
                    }
                }
            }
            return new Tuple<>(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
        }

        private int firstArrivalAtOrAfter(double time) {
            int low = 0;
            int high = dwellEvents.size();
            while (low < high) {
                int middle = (low + high) >>> 1;
                if (dwellEvents.get(middle).getArrivalTime() < time) {
                    low = middle + 1;
                } else {
                    high = middle;
                }
            }
            return low;
        }
    }

    private class DwellEventsForRoute implements Serializable {

        Map<String, DwellEventsAtStop> dwellEventsAtStops = new HashMap<>();

        private void addVehicleDwellEventAtStop(Id<TransitStopFacility> stopId, DwellEvent dwellEvent) {
            DwellEventsAtStop dwellEvents = dwellEventsAtStops.get(stopId.toString());
            if (dwellEvents == null) {
                dwellEvents = new DwellEventsAtStop();
                dwellEventsAtStops.put(stopId.toString(), dwellEvents);
            }
            dwellEvents.addVehicleDwellEventAtStop(dwellEvent);
        }

        public Tuple<Double, Double> getTravelTime(Id<TransitStopFacility> originStop, Id<TransitStopFacility> destinationStop, double time) {
            return dwellEventsAtStops.get(originStop.toString()).getTravelTime(destinationStop, time);
        }

    }

    private class DwellEventsForLine implements Serializable{

        private Map<String, DwellEventsForRoute> routesToDwellEvents = new HashMap<>();

        private void addVehicleDwellEventAtStop(Id<TransitRoute> route, Id<TransitStopFacility> stopId, DwellEvent dwellEvent) {
            DwellEventsForRoute stopDwellEventsForRoute = routesToDwellEvents.get(route.toString());
            if (stopDwellEventsForRoute == null) {
                stopDwellEventsForRoute = new DwellEventsForRoute();
                routesToDwellEvents.put(route.toString(),stopDwellEventsForRoute);
            }
            stopDwellEventsForRoute.addVehicleDwellEventAtStop(stopId, dwellEvent);
        }


        public Tuple<Double, Double> getRouteTravelTime(Id<TransitRoute> route, Id<TransitStopFacility> originStop, Id<TransitStopFacility> destinationStop, double time) {
            return routesToDwellEvents.get(route.toString()).getTravelTime(originStop, destinationStop, time);
        }
    }

}
