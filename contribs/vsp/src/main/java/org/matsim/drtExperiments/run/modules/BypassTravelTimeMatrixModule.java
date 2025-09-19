//package org.matsim.drtExperiments.run.modules;
//
//import org.matsim.api.core.v01.network.Network;
//import org.matsim.contrib.drt.run.DrtConfigGroup;
//import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;
//import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
//import org.matsim.core.router.util.TravelTime;
//import org.matsim.drtExperiments.onlineStrategy.DummyTravelTimeMatrix;
//import org.matsim.drtExperiments.onlineStrategy.OnlineSolver;
//import org.matsim.drtExperiments.onlineStrategy.OnlineSolverBasicInsertionStrategy;
//
//public class BypassTravelTimeMatrixModule extends AbstractDvrpModeQSimModule {
//    private final DrtConfigGroup drtConfigGroup;
//
//    public BypassTravelTimeMatrixModule(DrtConfigGroup drtConfigGroup) {
//        super(drtConfigGroup.mode);
//        this.drtConfigGroup = drtConfigGroup;
//    }
//
//    @Override
//    protected void configureQSim() {
//        bindModal(OnlineSolver.class).toProvider(modalProvider(
//                getter -> new OnlineSolverBasicInsertionStrategy(getter.getModal(Network.class), drtConfigGroup,
//                        new DummyTravelTimeMatrix(), getter.getModal(TravelTime.class),
//                        getter.getModal(TravelDisutilityFactory.class).createTravelDisutility(getter.getModal(TravelTime.class)))));
//
//    }
//}
