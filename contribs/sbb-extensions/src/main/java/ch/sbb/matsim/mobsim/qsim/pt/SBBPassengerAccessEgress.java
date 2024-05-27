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
package ch.sbb.matsim.mobsim.qsim.pt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.BoardingDeniedEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.framework.PassengerAgent;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.pt.PTPassengerAgent;
import org.matsim.core.mobsim.qsim.pt.PassengerAccessEgress;
import org.matsim.core.mobsim.qsim.pt.TransitStopAgentTracker;
import org.matsim.core.mobsim.qsim.pt.TransitStopHandler;
import org.matsim.core.mobsim.qsim.pt.TransitVehicle;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;

/**
 * This class contains a lot of code from org.matsim.core.mobsim.qsim.pt.PassengerAccessEgressImpl, some methods with small adaptations, other directly copied. The aforementioned class is
 * package-protected and thus not visible from this package, making it impossible to inherit from it and re-use parts of the code.
 *
 * @author mrieser / SBB
 */
public class SBBPassengerAccessEgress implements PassengerAccessEgress {

    private final InternalInterface internalInterface;
    private final TransitStopAgentTracker agentTracker;
    private final EventsManager eventsManager;
    private final boolean isGeneratingDeniedBoardingEvents;

    SBBPassengerAccessEgress(InternalInterface internalInterface, TransitStopAgentTracker agentTracker, Scenario scenario, EventsManager eventsManager) {
        this.internalInterface = internalInterface;
        this.agentTracker = agentTracker;
        this.eventsManager = eventsManager;
        this.isGeneratingDeniedBoardingEvents = scenario.getConfig().vspExperimental().isGeneratingBoardingDeniedEvents();
    }

    /**
     * Allows passengers to leave and/or board a vehicle according to the vehicle's accessTime, egressTime and doorOperation mode.
     * <p>
     * The code is to a large part a copy of PassengerAccessEgressImpl.calculateStopTimeAndTriggerBoarding. It could not be directly used as the class itself is package-protected and not visible in
     * our package.
     *
     * @return 0.0 (no more agents currently to board or leave), or 1.0 (there were passenger actions this time step, need to recheck next time step again)
     */
    double handlePassengersWithPhysicalLimits(TransitStopFacility stop, TransitVehicle vehicle, TransitLine line, TransitRoute route, List<TransitRouteStop> upcomingStops, double now) {
        ArrayList<PTPassengerAgent> passengersLeaving = findPassengersLeaving(vehicle, stop);
        int freeCapacity = vehicle.getPassengerCapacity() - vehicle.getPassengers().size() + passengersLeaving.size();
        List<PTPassengerAgent> passengersEntering = findPassengersEntering(route, line, vehicle, stop, upcomingStops, freeCapacity, now);

        TransitStopHandler stopHandler = vehicle.getStopHandler();
        double stopTime = stopHandler.handleTransitStop(stop, now, passengersLeaving, passengersEntering, this, vehicle);
        if (stopTime == 0.0) { // (de-)boarding is complete when the additional stopTime is 0.0
            if (this.isGeneratingDeniedBoardingEvents) {
                List<PTPassengerAgent> stillWaiting = findAllPassengersWaiting(route, line, vehicle, stop, upcomingStops, now);
                this.fireBoardingDeniedEvents(vehicle, now, stillWaiting);
            }
        }
        return stopTime;
    }

    /**
     * Allows all passengers wanting to leave the vehicle to do so immediately in the current time step, not taking constraints like number of doors and their passenger capacity into account. Allows
     * all passengers wanting to board the vehicle to do so immediately, given there is still some free capacity left in the vehicle.
     */
    double handleAllPassengersImmediately(TransitStopFacility stop, TransitVehicle vehicle, TransitLine line, TransitRoute route, List<TransitRouteStop> upcomingStops, double now) {
        List<PTPassengerAgent> leavingPassengers = findPassengersLeaving(vehicle, stop);
        for (PTPassengerAgent passenger : leavingPassengers) {
            handlePassengerLeaving(passenger, vehicle, passenger.getDestinationLinkId(), now);
        }

        int freeCapacity = vehicle.getPassengerCapacity() - vehicle.getPassengers().size();

        List<PTPassengerAgent> boardingPassengers = findPassengersEntering(route, line, vehicle, stop, upcomingStops, freeCapacity, now);
        for (PTPassengerAgent passenger : boardingPassengers) {
            handlePassengerEntering(passenger, vehicle, passenger.getDesiredAccessStopId(), now);
        }
        if (this.isGeneratingDeniedBoardingEvents) {
            List<PTPassengerAgent> stillWaiting = findAllPassengersWaiting(route, line, vehicle, stop, upcomingStops, now);
            this.fireBoardingDeniedEvents(vehicle, now, stillWaiting);
        }
        return 0.0;
    }

    @Override
    public boolean handlePassengerLeaving(PTPassengerAgent passenger, MobsimVehicle vehicle, Id<Link> toLinkId, double time) {
        boolean removed = vehicle.removePassenger(passenger);
        if (removed) {
            this.eventsManager.processEvent(new PersonLeavesVehicleEvent(time, passenger.getId(), vehicle.getVehicle().getId()));
            MobsimAgent agent = (MobsimAgent) passenger;
            agent.notifyArrivalOnLinkByNonNetworkMode(toLinkId);
            agent.endLegAndComputeNextState(time);
            this.internalInterface.arrangeNextAgentState(agent);
        }
        return removed;
    }

    @Override
    public boolean handlePassengerEntering(PTPassengerAgent passenger, MobsimVehicle vehicle, Id<TransitStopFacility> fromStopFacilityId, double time) {
        boolean entered = vehicle.addPassenger(passenger);
        if (entered) {
            this.agentTracker.removeAgentFromStop(passenger, fromStopFacilityId);
            Id<Person> agentId = passenger.getId();
            Id<Link> linkId = passenger.getCurrentLinkId();
            this.internalInterface.unregisterAdditionalAgentOnLink(agentId, linkId);
            MobsimDriverAgent agent = (MobsimDriverAgent) passenger;
            this.eventsManager.processEvent(new PersonEntersVehicleEvent(time, agent.getId(), vehicle.getVehicle().getId()));
        }
        return entered;
    }

    private ArrayList<PTPassengerAgent> findPassengersLeaving(TransitVehicle vehicle,
            final TransitStopFacility stop) {
        ArrayList<PTPassengerAgent> passengersLeaving = new ArrayList<>();
        for (PassengerAgent passenger : vehicle.getPassengers()) {
            if (((PTPassengerAgent) passenger).getExitAtStop(stop)) {
                passengersLeaving.add((PTPassengerAgent) passenger);
            }
        }
        return passengersLeaving;
    }

    /**
     * Finds all agents that want to enter the specified line.
     */
    private List<PTPassengerAgent> findPassengersEntering(TransitRoute transitRoute, TransitLine transitLine, TransitVehicle vehicle,
            final TransitStopFacility stop, List<TransitRouteStop> stopsToCome, int freeCapacity, double now) {
        List<PTPassengerAgent> passengers = this.agentTracker.getAgentsAtStop().get(stop.getId());
        if (passengers != null) {
            ArrayList<PTPassengerAgent> passengersEntering = new ArrayList<>();
            for (PTPassengerAgent agent : passengers) {
                if (freeCapacity == 0) {
                    break;
                }
                if (agent.getEnterTransitRoute(transitLine, transitRoute, stopsToCome, vehicle)) {
                    passengersEntering.add(agent);
                    freeCapacity--;
                }
            }
            return passengersEntering;
        }
        return Collections.emptyList();
    }

    private List<PTPassengerAgent> findAllPassengersWaiting(TransitRoute transitRoute, TransitLine transitLine, TransitVehicle vehicle,
            final TransitStopFacility stop, List<TransitRouteStop> stopsToCome, double now) {
        List<PTPassengerAgent> passengers = this.agentTracker.getAgentsAtStop().get(stop.getId());
        if (passengers != null) {
            ArrayList<PTPassengerAgent> passengersEntering = new ArrayList<>();
            for (PTPassengerAgent agent : passengers) {
                if (agent.getEnterTransitRoute(transitLine, transitRoute, stopsToCome, vehicle)) {
                    passengersEntering.add(agent);
                }
            }
            return passengersEntering;
        }
        return Collections.emptyList();
    }

    private void fireBoardingDeniedEvents(TransitVehicle vehicle, double now, List<PTPassengerAgent> agents) {
        Id<Vehicle> vehicleId = vehicle.getId();
        for (PTPassengerAgent agent : agents) {
            this.eventsManager.processEvent(new BoardingDeniedEvent(now, agent.getId(), vehicleId));
        }
    }

}
