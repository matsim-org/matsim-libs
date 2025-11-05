package org.matsim.contrib.ev;

import org.matsim.contrib.ev.infrastructure.ChargingInfrastructureSpecification;
import org.matsim.contrib.ev.infrastructure.CustomCharingInfrastructureModule;
import org.matsim.core.controler.Controler;

public class EvUtils {
    private EvUtils() {
    }

    static public void registerInfrastructure(Controler controller,
            ChargingInfrastructureSpecification infrastructureSpecification) {
        controller.addOverridingModule(new CustomCharingInfrastructureModule(infrastructureSpecification));
    }
}
