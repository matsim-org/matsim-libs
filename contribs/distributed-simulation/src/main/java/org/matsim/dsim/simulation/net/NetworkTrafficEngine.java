package org.matsim.dsim.simulation.net;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.matsim.api.NextStateHandler;
import org.matsim.api.SimEngine;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.dsim.messages.CapacityUpdate;
import org.matsim.dsim.messages.SimStepMessage;
import org.matsim.dsim.messages.VehicleMsg;
import org.matsim.dsim.simulation.SimPerson;
import org.matsim.dsim.simulation.SimStepMessaging;

@Log4j2
public class NetworkTrafficEngine implements SimEngine {

    @Getter
    private final SimNetwork simNetwork;
    private final SimVehicleProvider simVehicleProvider;
    private final EventsManager em;

    private final ActiveNodes activeNodes;
    private final ActiveLinks activeLinks;
    private final Wait2Link wait2Link;
    private final boolean wait2LinkFirst;

    @Setter
    private NextStateHandler nextStateHandler;

    public NetworkTrafficEngine(Scenario scenario, SimStepMessaging simStepMessaging, EventsManager em, int part) {
        simNetwork = new SimNetwork(scenario.getNetwork(), scenario.getConfig(), this::handleVehicleIsFinished, part);
        simVehicleProvider = new SimVehicleProvider(scenario, em);
        this.em = em;
        activeNodes = new ActiveNodes(em);
        activeLinks = new ActiveLinks(simStepMessaging);
        activeLinks.setActivateNode(id -> this.activeNodes.activate(this.simNetwork.getNodes().get(id)));
        activeNodes.setActivateLink(activeLinks::activate);
        wait2Link = new Wait2Link(em, activeLinks);
        wait2LinkFirst = scenario.getConfig().qsim().isInsertingWaitingVehiclesBeforeDrivingVehicles();
    }

    @Override
    public void accept(SimPerson person, double now) {
        // this will be more complicated but do it the simple way first

        // place person into vehicle
        SimVehicle vehicle = simVehicleProvider.unparkVehicle(person, now);
        Id<Link> currentRouteElement = vehicle.getCurrentRouteElement();

        assert currentRouteElement != null : "Vehicle %s has no current route element".formatted(vehicle.getId());

        SimLink link = simNetwork.getLinks().get(currentRouteElement);

        assert link != null : STR."Link\{currentRouteElement} not found in partition on partition #\{simNetwork.getPart()}";

        wait2Link.accept(vehicle, link);
    }

    @Override
    public void process(SimStepMessage stepMessage, double now) {
        for (var vehicleMessage : stepMessage.getVehicleMsgs()) {
            processVehicleMessage(vehicleMessage, now);
        }
        for (var updateMessage : stepMessage.getCapacityUpdates()) {
            processUpdateMessage(updateMessage);
        }
    }

    private void processVehicleMessage(VehicleMsg vehicleMessage, double now) {
        SimVehicle vehicle = simVehicleProvider.vehicleFromMessage(vehicleMessage);
        Id<Link> linkId = vehicle.getCurrentRouteElement();
        SimLink link = simNetwork.getLinks().get(linkId);

        link.pushVehicle(vehicle, SimLink.LinkPosition.QStart, now);
        activeLinks.activate(link);
    }

    private void processUpdateMessage(CapacityUpdate updateMessage) {

        Id<Link> linkId = updateMessage.getLinkId();
        double released = updateMessage.getReleased();
        double consumed = updateMessage.getConsumed();
        SimLink link = simNetwork.getLinks().get(linkId);

        if (link instanceof SimLink.SplitOutLink so) {
            so.applyCapacityUpdate(released, consumed);
        } else {
            throw new RuntimeException("Only expecting capacity updates for SplitOutLinks");
        }
    }

    @Override
    public void doSimStep(double now) {
        // this inserts waiting vehicles, then moves vehicles over intersections, and then updates bookkeeping.
        // if the config flag is false, we move vehicles, insert waiting vehicles and then update bookkeeping.
        if (wait2LinkFirst) {
            wait2Link.doSimStep(now);
        }
        activeNodes.doSimStep(now);
        if (!wait2LinkFirst) {
            wait2Link.doSimStep(now);
        }
        activeLinks.doSimStep(now);
    }

    private SimLink.OnLeaveQueueInstruction handleVehicleIsFinished(SimVehicle vehicle, SimLink link, double now) {

        // the vehicle has more elements in the route. Keep going.
        if (vehicle.getNextRouteElement() != null)
            return SimLink.OnLeaveQueueInstruction.MoveToBuffer;

        // the vehicle has no more route elements. It should leave the network
        em.processEvent(new VehicleLeavesTrafficEvent(
                now, vehicle.getDriver().getId(), vehicle.getCurrentRouteElement(),
                vehicle.getId(), vehicle.getDriver().getCurrentLeg().getMode(),
                1.0
        ));

        SimPerson driver = simVehicleProvider.parkVehicle(vehicle, now);

        // TODO: assumes driver is person arriving
        // Assumes legMode=networkMode, which is not always the case

        em.processEvent(new PersonArrivalEvent(
                now, vehicle.getDriver().getId(), vehicle.getCurrentRouteElement(),
                vehicle.getDriver().getCurrentLeg().getMode()
        ));

        nextStateHandler.accept(driver, now);
        return SimLink.OnLeaveQueueInstruction.RemoveVehicle;
    }
}
