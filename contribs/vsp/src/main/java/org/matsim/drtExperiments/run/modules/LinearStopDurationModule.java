//package org.matsim.drtExperiments.run.modules;
//
//import org.matsim.contrib.drt.optimizer.insertion.IncrementalStopDurationEstimator;
//import org.matsim.contrib.drt.passenger.DrtRequest;
//import org.matsim.contrib.drt.run.DrtConfigGroup;
//import org.matsim.contrib.drt.schedule.DrtStopTask;
//import org.matsim.contrib.drt.schedule.StopDurationEstimator;
//import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
//import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
//
//public class LinearStopDurationModule extends AbstractDvrpModeModule {
//    private final DrtConfigGroup drtConfigGroup;
//
//    public LinearStopDurationModule(DrtConfigGroup drtConfigGroup) {
//        super(drtConfigGroup.getMode());
//        this.drtConfigGroup = drtConfigGroup;
//    }
//
//    @Override
//    public void install() {
//        bindModal(StopDurationEstimator.class).toInstance((vehicle, dropoffRequests, pickupRequests) -> drtConfigGroup.stopDuration * (dropoffRequests.size() + pickupRequests.size()));
//        bindModal(IncrementalStopDurationEstimator.class).toInstance(new LinearDrtStopDurationEstimator(drtConfigGroup.stopDuration));
//    }
//
//    public static record LinearDrtStopDurationEstimator(
//            double fixedStopDuration) implements IncrementalStopDurationEstimator {
//        @Override
//        public double calcForPickup(DvrpVehicle vehicle, DrtStopTask stopTask, DrtRequest pickupRequest) {
//            return fixedStopDuration;
//        }
//
//        @Override
//        public double calcForDropoff(DvrpVehicle vehicle, DrtStopTask stopTask, DrtRequest dropoffRequest) {
//            return fixedStopDuration;
//        }
//    }
//}
