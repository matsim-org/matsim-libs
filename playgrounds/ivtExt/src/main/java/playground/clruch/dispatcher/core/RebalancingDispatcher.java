// code by clruch and jph
package playground.clruch.dispatcher.core;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.router.util.TravelTime;

import playground.clruch.utils.GlobalAssert;
import playground.sebhoerl.avtaxi.config.AVDispatcherConfig;
import playground.sebhoerl.plcpc.ParallelLeastCostPathCalculator;

/** rebalancing capability (without virtual network) */
public abstract class RebalancingDispatcher extends UniversalDispatcher {

    // private final Map<RoboTaxi, Link> rebalancingVehicles = new HashMap<>();

    protected RebalancingDispatcher(AVDispatcherConfig avDispatcherConfig, TravelTime travelTime,
            ParallelLeastCostPathCalculator parallelLeastCostPathCalculator, EventsManager eventsManager) {
        super(avDispatcherConfig, travelTime, parallelLeastCostPathCalculator, eventsManager);
    }

    protected synchronized final void setRoboTaxiRebalance(final RoboTaxi roboTaxi, final Link destination) {
        GlobalAssert.that(roboTaxi.isWithoutCustomer());

        // redivert roboTaxi, generate rebalancing event
        setRoboTaxiDiversion(roboTaxi, destination,AVStatus.REBALANCEDRIVE);
        eventsManager.processEvent(RebalanceVehicleEvent.create(getTimeNow(), roboTaxi, destination));
    }

    @Override
    boolean extraCheck(RoboTaxi roboTaxi) {
        return roboTaxi.getAVStatus().equals(AVStatus.REBALANCEDRIVE);
    }

    protected List<RoboTaxi> getRebalancingRoboTaxis() {
        List<RoboTaxi> rebalancingRobotaxis = new ArrayList<>();
        for (RoboTaxi robotaxi : getRoboTaxis()) {
            if (robotaxi.getAVStatus().equals(AVStatus.REBALANCEDRIVE)) {
                rebalancingRobotaxis.add(robotaxi);
            }
        }
        return rebalancingRobotaxis;

    }

    protected List<RoboTaxi> getDivertableNotRebalancingRoboTaxis() {
        // return list of vehicles
        List<RoboTaxi> returnList = new ArrayList<>();
        for (RoboTaxi robotaxi : getDivertableRoboTaxis()) {
            if (!robotaxi.getAVStatus().equals(AVStatus.REBALANCEDRIVE)) {
                returnList.add(robotaxi);
            }
        }
        return returnList;
    }

}

// @Override
// public final void setVehiclePickup(AVVehicle avVehicle, AVRequest avRequest) {
// super.setVehiclePickup(avVehicle, avRequest);
// if (rebalancingVehicles.containsKey(avVehicle))
// rebalancingVehicles.remove(avVehicle);
// }

//// This function has to be called only after getVirtualNodeRebalancingVehicles
// protected synchronized final void setVehicleRebalance(final AVVehicle avVehicle, final Link
//// destination) {
// // in case vehicle is picking up, remove from pickup register
// if (pickupRegister.containsValue(avVehicle)) {
// AVRequest avRequest = pickupRegister.inverse().get(avVehicle);
// pickupRegister.forcePut(avRequest, null);
// // TODO do not use forcePut(avRequest,null) because it violates bijection.
// }
//
// // redivert the vehicle, then generate a rebalancing event and add to list of currently
// // rebalancing vehicles
// setVehicleDiversion(avVehicle, destination);
// eventsManager.processEvent(RebalanceVehicleEvent.create(getTimeNow(), avVehicle, destination));
// Link returnVal = rebalancingVehicles.put(RoboTaxi, destination);
// }

//// TODO get rid of the AVVehicle here
// @Override
// final void updateDatastructures(Collection<AVVehicle> stayVehicles) {
// // mandatory call
// super.updateDatastructures(stayVehicles);
//
// // remove rebalancing vehicles that have reached their destination
// for (AVVehicle avVehicle : stayVehicles) {
// Optional<RoboTaxi> optRob = getRoboTaxis().stream().filter(v ->
//// v.getAVVehicle().equals(avVehicle)).findAny();
// GlobalAssert.that(optRob.isPresent());
// rebalancingVehicles.remove(optRob.get());
// }
// }

// protected List<RoboTaxi> getDivertableUnassignedNotRebalancingVehicleLinkPairs() {
// return getDivertableUnassignedVehicleLinkPairs().stream() //
// .filter(v -> !rebalancingVehicles.containsKey(v.getAVVehicle())) //
// .collect(Collectors.toList());
// }
