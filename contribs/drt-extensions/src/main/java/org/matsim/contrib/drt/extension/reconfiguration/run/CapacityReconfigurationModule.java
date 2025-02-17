package org.matsim.contrib.drt.extension.reconfiguration.run;

import org.matsim.contrib.drt.extension.reconfiguration.logic.CapacityReconfigurationLogic;
import org.matsim.contrib.drt.extension.reconfiguration.logic.NoopReconfigurationLogic;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;

public class CapacityReconfigurationModule extends AbstractDvrpModeModule {
    private final double reconfigurationTaskDuration;

    public CapacityReconfigurationModule(String mode, double reconfigurationTaskDuration) {
        super(mode);
        this.reconfigurationTaskDuration = reconfigurationTaskDuration;
    }

    @Override
    public void install() {
        installQSimModule(new CapacityReconfigurationQSimModule(getMode(), reconfigurationTaskDuration));
        bindModal(CapacityReconfigurationLogic.class).toInstance(new NoopReconfigurationLogic());
    }
}
