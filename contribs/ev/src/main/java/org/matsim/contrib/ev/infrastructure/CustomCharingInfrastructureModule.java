package org.matsim.contrib.ev.infrastructure;

import org.matsim.core.controler.AbstractModule;

public class CustomCharingInfrastructureModule extends AbstractModule {
    private final ChargingInfrastructureSpecification infrastructureSpecification;

    public CustomCharingInfrastructureModule(ChargingInfrastructureSpecification infrastructureSpecification) {
        this.infrastructureSpecification = infrastructureSpecification;
    }

    @Override
    public void install() {
        bind(ChargingInfrastructureSpecification.class).toInstance(infrastructureSpecification);
    }
}
