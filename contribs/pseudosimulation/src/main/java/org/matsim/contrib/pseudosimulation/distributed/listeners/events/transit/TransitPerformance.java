package org.matsim.contrib.pseudosimulation.distributed.listeners.events.transit;

import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TransitPerformance implements Serializable {
    private Map<String, DwellEventsForLine> linesToStopDwellEvents = new HashMap<>();

    public BoardingModel getBoardingModel() {
        return boardingModel;
    }

    private BoardingModel boardingModel;

    public TransitPerformance(BoardingModel boardingModel) {
        this.boardingModel = boardingModel;
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
            return new Tuple(Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY);
        }
    }


    private class DwellEventsAtStop implements Serializable {
        private List<DwellEvent> dwellEvents;

        public DwellEventsAtStop() {
            this.dwellEvents = new ArrayList<>();

        }

        public void addVehicleDwellEventAtStop(DwellEvent dwellEvent) {
            this.dwellEvents.add(dwellEvent);
        }

        public Tuple<Double, Double> getTravelTime(Id<TransitStopFacility> destinationStop, double time) {
            double inVehicleTime = Double.POSITIVE_INFINITY;
            for (DwellEvent dwellEvent : dwellEvents) {
                if (dwellEvent.getArrivalTime() >= time) {
                    //check if it's possible to board
                    if (!boardingModel.canBoard(dwellEvent.getOccupancyAtDeparture())) continue;
                    inVehicleTime = dwellEvent.getVehicle().getInVehicleTime(dwellEvent, destinationStop);
                }
                if (!Double.isInfinite(inVehicleTime)) {
                    return new Tuple<>(dwellEvent.getArrivalTime() - time, inVehicleTime);
                }
            }
            return new Tuple<>(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
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
