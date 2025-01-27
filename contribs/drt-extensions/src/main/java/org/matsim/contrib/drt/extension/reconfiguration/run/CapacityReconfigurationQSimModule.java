package org.matsim.contrib.drt.extension.reconfiguration.run;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.extension.reconfiguration.CapacityReconfigurationEngine;
import org.matsim.contrib.drt.extension.reconfiguration.logic.CapacityReconfigurationLogic;
import org.matsim.contrib.drt.extension.reconfiguration.logic.NoopReconfigurationLogic;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelTime;

public class CapacityReconfigurationQSimModule extends AbstractDvrpModeQSimModule {
    private final double changeTaskDuration;

    public CapacityReconfigurationQSimModule(String mode, double changeTaskDuration) {
        super(mode);
        this.changeTaskDuration = changeTaskDuration;
    }

    @Override
    protected void configureQSim() {
        addModalComponent(CapacityReconfigurationEngine.class, modalProvider(getter -> {
            TravelTime travelTime = getter.getModal(TravelTime.class);

            return new CapacityReconfigurationEngine(
                    getter.getModal(Fleet.class),
                    getter.getModal(Network.class),
                    getter.getModal(TravelDisutilityFactory.class)
                            .createTravelDisutility(travelTime),
                    travelTime,
                    getter
                            .getModal(CapacityReconfigurationLogic.class),
                    changeTaskDuration);
        }));

        bind(CapacityReconfigurationLogic.class).toInstance(new NoopReconfigurationLogic());
    }
}
