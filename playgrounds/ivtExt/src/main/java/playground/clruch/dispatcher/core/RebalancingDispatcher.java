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
        setRoboTaxiDiversion(roboTaxi, destination, AVStatus.REBALANCEDRIVE);
        eventsManager.processEvent(RebalanceVehicleEvent.create(getTimeNow(), roboTaxi, destination));
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
