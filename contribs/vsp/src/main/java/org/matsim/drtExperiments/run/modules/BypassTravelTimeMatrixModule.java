package org.matsim.drtExperiments.run.modules;

import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;

public class BypassTravelTimeMatrixModule extends AbstractDvrpModeQSimModule {
    private final DrtConfigGroup drtConfigGroup;

    public BypassTravelTimeMatrixModule(DrtConfigGroup drtConfigGroup) {
        super( drtConfigGroup.getMode() );
        this.drtConfigGroup = drtConfigGroup;
    }

    @Override
    protected void configureQSim() {
//        bindModal(OnlineSolver.class).toProvider(modalProvider(
//                getter -> new OnlineSolverBasicInsertionStrategy(getter.getModal(Network.class), drtConfigGroup,
//                        new DummyTravelTimeMatrix(), getter.getModal(TravelTime.class),
//                        getter.getModal(TravelDisutilityFactory.class).createTravelDisutility(getter.getModal(TravelTime.class)))));

    }
}
