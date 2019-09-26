/*
 * Copyright (C) Schweizerische Bundesbahnen SBB, 2017.
 */

package ch.sbb.matsim.routing.pt.raptor;

import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.facilities.Facility;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author mrieser / SBB
 */
public class RaptorRoute {

    final Facility fromFacility;
    final Facility toFacility;
    private final double totalCosts;
    private double departureTime = Double.NaN;
    private double travelTime =  0;
    private int ptLegCount = 0;
    private List<RoutePart> editableParts = new ArrayList<>();
    final List<RoutePart> parts = Collections.unmodifiableList(this.editableParts);

    RaptorRoute(Facility fromFacility, Facility toFacility, double totalCosts) {
        this.fromFacility = fromFacility;
        this.toFacility = toFacility;
        this.totalCosts = totalCosts;
    }

    void addNonPt(TransitStopFacility fromStop, TransitStopFacility toStop, double depTime, double travelTime, double distance, String mode) {
        this.editableParts.add(new RoutePart(fromStop, toStop, mode, depTime, Double.NaN, depTime + travelTime, distance, null, null, null));
        if (Double.isNaN(this.departureTime)) {
            this.departureTime = depTime;
        }
        this.travelTime += travelTime;
    }

    void addPlanElements(double depTime, double travelTime, List<? extends PlanElement> planElements) {
        this.editableParts.add(new RoutePart(null, null, null, depTime, Double.NaN, depTime + travelTime, Double.NaN, null, null, planElements));
        if (Double.isNaN(this.departureTime)) {
            this.departureTime = depTime;
        }
        this.travelTime += travelTime;
    }

    void addPt(TransitStopFacility fromStop, TransitStopFacility toStop, TransitLine line, TransitRoute route, String mode, double depTime, double boardingTime, double arrivalTime, double distance) {
        this.editableParts.add(new RoutePart(fromStop, toStop, mode, depTime, boardingTime, arrivalTime, distance, line, route, null));
        if (Double.isNaN(this.departureTime)) {
            this.departureTime = depTime;
        }
        this.travelTime += (arrivalTime - depTime);
        this.ptLegCount++;
    }

    public double getTotalCosts() {
        return this.totalCosts;
    }

    public double getDepartureTime() {
        return this.departureTime;
    }

    public double getTravelTime() {
        return this.travelTime;
    }

    public int getNumberOfTransfers() {
        if (this.ptLegCount > 0) {
            return this.ptLegCount - 1;
        }
        return 0;
    }

    public Iterable<RoutePart> getParts() {
        return this.parts;
    }

    public static final class RoutePart {
        public final TransitStopFacility fromStop;
        public final TransitStopFacility toStop;
        public final String mode;
        public final double depTime;
        public final double boardingTime;
        public final double arrivalTime;
        public final double distance;
        public final TransitLine line;
        public final TransitRoute route;
        final List<? extends PlanElement> planElements;

        RoutePart(TransitStopFacility fromStop, TransitStopFacility toStop, String mode, double depTime, double boardingTime, double arrivalTime, double distance, TransitLine line, TransitRoute route, List<? extends PlanElement> planElements) {
            this.fromStop = fromStop;
            this.toStop = toStop;
            this.mode = mode;
            this.depTime = depTime;
            this.boardingTime = boardingTime;
            this.arrivalTime = arrivalTime;
            this.distance = distance;
            this.line = line;
            this.route = route;
            this.planElements = planElements;
        }
    }
}
