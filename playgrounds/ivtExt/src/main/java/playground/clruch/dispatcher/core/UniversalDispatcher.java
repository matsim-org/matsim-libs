// code by jph
package playground.clruch.dispatcher.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.schedule.DriveTask;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Schedules;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.dvrp.util.LinkTimePair;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.router.util.TravelTime;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import playground.clruch.ScenarioServer;
import playground.clruch.net.SimulationDistribution;
import playground.clruch.net.SimulationObject;
import playground.clruch.net.SimulationObjectCompiler;
import playground.clruch.net.SimulationObjects;
import playground.clruch.router.FuturePathContainer;
import playground.clruch.router.FuturePathFactory;
import playground.clruch.utils.AVLocation;
import playground.clruch.utils.AVTaskAdapter;
import playground.clruch.utils.GlobalAssert;
import playground.clruch.utils.SafeConfig;
import playground.sebhoerl.avtaxi.config.AVDispatcherConfig;
import playground.sebhoerl.avtaxi.data.AVVehicle;
import playground.sebhoerl.avtaxi.dispatcher.AVDispatcher;
import playground.sebhoerl.avtaxi.dispatcher.AbstractDispatcher;
import playground.sebhoerl.avtaxi.passenger.AVRequest;
import playground.sebhoerl.avtaxi.schedule.AVDriveTask;
import playground.sebhoerl.avtaxi.schedule.AVStayTask;
import playground.sebhoerl.plcpc.ParallelLeastCostPathCalculator;

/** purpose of {@link UniversalDispatcher} is to collect and manage {@link AVRequest}s alternative
 * implementation of {@link AVDispatcher}; supersedes
 * {@link AbstractDispatcher}. */
public abstract class UniversalDispatcher extends VehicleMaintainer {
    private final FuturePathFactory futurePathFactory;

    protected final Set<AVRequest> pendingRequests = new LinkedHashSet<>(); // access via
                                                                            // getAVRequests()
    private final Set<AVRequest> publishPeriodMatchedRequests = new HashSet<>(); // requests which
                                                                                 // are matched
                                                                                 // within a publish
                                                                                 // period.
    // private final Map<AVVehicle, Link> vehiclesWithCustomer = new HashMap<>();
    // TODO visibility of map to private
    protected final BiMap<AVRequest, RoboTaxi> pickupRegister = HashBiMap.create();

    /** map stores most recently known location of vehicles. map is used in case obtaining the
     * vehicle location fails */
    // private final Map<AVVehicle, Link> vehicleLocations = new HashMap<>();

    private final double pickupDurationPerStop;
    private final double dropoffDurationPerStop;
    protected int publishPeriod; // not final, so that dispatchers can disable, or manipulate

    private int total_matchedRequests = 0;
    protected Integer AVVEHILCECOUNT = null;

    protected UniversalDispatcher( //
            AVDispatcherConfig avDispatcherConfig, //
            TravelTime travelTime, //
            ParallelLeastCostPathCalculator parallelLeastCostPathCalculator, //
            EventsManager eventsManager //
    ) {
        super(eventsManager);
        futurePathFactory = new FuturePathFactory(parallelLeastCostPathCalculator, travelTime);

        pickupDurationPerStop = avDispatcherConfig.getParent().getTimingParameters().getPickupDurationPerStop();
        dropoffDurationPerStop = avDispatcherConfig.getParent().getTimingParameters().getDropoffDurationPerStop();

        SafeConfig safeConfig = SafeConfig.wrap(avDispatcherConfig);
        setInfoLinePeriod(safeConfig.getInteger("infoLinePeriod", 10));
        publishPeriod = safeConfig.getInteger("publishPeriod", 10);
    }

    @Override
    void executePickups() {
        // complete all matchings if vehicle has arrived on link
        Set<AVRequest> reqToRemove = new HashSet<>();
        for (AVRequest avRequest : pickupRegister.keySet()) {
            RoboTaxi pickupVehicle = pickupRegister.get(avRequest);
            Link pickupVehicleLink = pickupVehicle.getDivertableLocation();
            if (avRequest.getFromLink().equals(pickupVehicleLink) && pendingRequests.contains(avRequest) && pickupVehicle.isInStayTask()) {
                setAcceptRequest(pickupVehicle, avRequest);
                boolean status = reqToRemove.add(avRequest);
                GlobalAssert.that(status);
            }
        }
        reqToRemove.stream().forEach(v -> pickupRegister.remove(v));
    }

    @Override
    void updateDatastructures(Collection<AVVehicle> stayVehicles) {

        @SuppressWarnings("unused")
        int failed = 0;
        Collection<RoboTaxi> robotaxis = getRoboTaxis();
        if (!robotaxis.isEmpty()) {
            for (RoboTaxi robotaxi : robotaxis) {
                final Link link = AVLocation.of(robotaxi.getAVVehicle());
                if (link != null) {
                    robotaxi.setCurrentLocation(link);
                } else {
                    ++failed;
                }

            }
            if (AVVEHILCECOUNT == null)
                AVVEHILCECOUNT = getRoboTaxis().size();
        }

        // stayVehicles.forEach(vehiclesWithCustomer::remove); // TODO still needed?

        // // ---
        // @SuppressWarnings("unused")
        // int failed = 0;
        // Collection<AVVehicle> collection = getFunctioningVehicles();
        // if (!collection.isEmpty()) {
        // for (AVVehicle avVehicle : collection) {
        // final Link link = AVLocation.of(avVehicle);
        // if (link != null)
        // vehicleLocations.put(avVehicle, link);
        // else
        // ++failed;
        // }
        // if (AVVEHILCECOUNT == null)
        // AVVEHILCECOUNT = vehicleLocations.size();
        // // TODO this check was taken out because the zurich scenario doesn't satisfy this :-(
        // // GlobalAssert.that(AVVEHILCECOUNT == collection.size());
        // // GlobalAssert.that(AVVEHILCECOUNT == vehicleLocations.size());
        // }
        // if (0 < failed)
        // System.out.println("failed to extract location for " + failed + " vehicles");
    }

    /** function call leaves the state of the {@link UniversalDispatcher} unchanged. successive
     * calls to the function return the identical collection.
     * 
     * @return collection of all requests that have not been matched */
    protected synchronized final Collection<AVRequest> getAVRequests() {
        return Collections.unmodifiableCollection(pendingRequests);
    }

    /** function call leaves the state of the {@link UniversalDispatcher} unchanged. successive
     * calls to the function return the identical collection.
     * 
     * @return list of {@link AVRequest}s grouped by link */
    public final Map<Link, List<AVRequest>> getAVRequestsAtLinks() {
        return getAVRequests().stream() // <- intentionally not parallel to guarantee ordering of
                                        // requests
                .collect(Collectors.groupingBy(AVRequest::getFromLink));
    }

    /** @return AVRequests which are currently not assigned to a vehicle */
    protected synchronized final List<AVRequest> getUnassignedAVRequests() {
        List<AVRequest> unassignedRequests = new ArrayList<>();
        for (AVRequest avRequest : pendingRequests) {
            if (pickupRegister.get(avRequest) == null) {
                unassignedRequests.add(avRequest);
            }
        }
        return unassignedRequests;
    }

    protected final Collection<RoboTaxi> getDivertableUnassignedRoboTaxis() {
        Collection<RoboTaxi> divertableUnassignedRoboTaxis = new ArrayList<>();
        for (RoboTaxi roboTaxi : getDivertableRoboTaxis()) {
            if (!pickupRegister.containsValue(roboTaxi)) {
                divertableUnassignedRoboTaxis.add(roboTaxi);
            }
        }

        GlobalAssert.that(!divertableUnassignedRoboTaxis.stream().anyMatch(pickupRegister::containsValue));
        GlobalAssert.that(divertableUnassignedRoboTaxis.stream().allMatch(RoboTaxi::isWithoutCustomer));

        return divertableUnassignedRoboTaxis;
    }

    protected final void setRoboTaxiDiversion(RoboTaxi robotaxi, Link destination) {
        GlobalAssert.that(robotaxi.isWithoutCustomer());

        final Schedule schedule = robotaxi.getAVVehicle().getSchedule();
        Task task = schedule.getCurrentTask(); // <- implies that task is started
        new AVTaskAdapter(task) {
            @Override
            public void handle(AVDriveTask avDriveTask) {
                if (!avDriveTask.getPath().getToLink().equals(destination)) { // ignore when vehicle
                                                                              // is already going
                                                                              // there
                    FuturePathContainer futurePathContainer = futurePathFactory.createFuturePathContainer( //
                            robotaxi.getDivertableLocation(), destination, robotaxi.getDivertableTime());

                    assignDirective(robotaxi, new DriveVehicleDiversionDirective( //
                            robotaxi, destination, futurePathContainer));
                } else

                    assignDirective(robotaxi, new EmptyDirective());
            }

            @Override
            public void handle(AVStayTask avStayTask) {
                if (!avStayTask.getLink().equals(destination)) { // ignore request where location ==
                                                                 // target
                    FuturePathContainer futurePathContainer = futurePathFactory.createFuturePathContainer( //
                            robotaxi.getDivertableLocation(), destination, robotaxi.getDivertableTime());

                    assignDirective(robotaxi, new StayVehicleDiversionDirective( //
                            robotaxi, destination, futurePathContainer));
                } else
                    assignDirective(robotaxi, new EmptyDirective());
            }
        };
    }

    /** function for convenience
     * 
     * @param avVehicle
     *            in stay task
     * @param destination
     * @throws Exception
     *             if vehicle is not in stay task */
    // protected final void setStayVehicleDiversion(final AVVehicle avVehicle, final Link
    // destination) {
    // setVehicleDiversion(VehicleLinkPairs.ofStayVehicle(avVehicle, getTimeNow()), destination);
    //
    // }

    public final void setVehicleDiversion(final Entry<AVVehicle, Link> entry) {
        setVehicleDiversion(entry.getKey(), entry.getValue());
    }

    @Override
    public void onNextLinkEntered(AVVehicle avVehicle, DriveTask driveTask, LinkTimePair linkTimePair) {
        // default implementation: for now, do nothing
    }

    /** Function called from derived class to match a vehicle with a request. The function appends
     * the pick-up, drive, and drop-off tasks for the car.
     * 
     * @param avVehicle
     *            vehicle in {@link AVStayTask} in order to match the request
     * @param avRequest
     *            provided by getAVRequests() */
    private synchronized final void setAcceptRequest(RoboTaxi roboTaxi, AVRequest avRequest) {
        GlobalAssert.that(pendingRequests.contains(avRequest)); // request is known to the system

        boolean statusPen = pendingRequests.remove(avRequest);
        GlobalAssert.that(statusPen);

        roboTaxi.setAVStatus(AVStatus.DRIVEWITHCUSTOMER);

        // save avRequests which are matched for one publishPeriod to ensure
        // no requests are lost in the recording.
        publishPeriodMatchedRequests.add(avRequest);

        final Schedule schedule = roboTaxi.getAVVehicle().getSchedule();
        // check that current task is last task in schedule
        GlobalAssert.that(schedule.getCurrentTask() == Schedules.getLastTask(schedule));

        final double endPickupTime = getTimeNow() + pickupDurationPerStop;
        FuturePathContainer futurePathContainer = futurePathFactory.createFuturePathContainer( //
                avRequest.getFromLink(), avRequest.getToLink(), endPickupTime);

        assignDirective(roboTaxi, new AcceptRequestDirective( //
                roboTaxi, avRequest, futurePathContainer, getTimeNow(), dropoffDurationPerStop));

        // assignDirective(avVehicle, new AcceptRequestDirective( //
        // avVehicle, avRequest, futurePathContainer, getTimeNow(), dropoffDurationPerStop));

        // Link returnVal = vehiclesWithCustomer.put(roboTaxi.getAVVehicle(),
        // avRequest.getToLink());
        // GlobalAssert.that(returnVal == null);

        ++total_matchedRequests;
    }

    // /** @param entry
    // * <VehicleLinkPair,AVRequest> sets AVVehicle to pickup AVRequest */
    // // TODO find a way to make this protected again.
    // protected void setVehiclePickup(AVVehicle avVehicle, AVRequest avRequest) {
    // // 1) enter information into pickup table
    // pickupRegister.forcePut(avRequest, avVehicle);
    //
    // // 2) set vehicle diversion of AVVehicle
    // setVehicleDiversion(avVehicle, avRequest.getFromLink());
    // }

    protected void setRoboTaxiPickup(RoboTaxi roboTaxi, AVRequest avRequest) {
        GlobalAssert.that(roboTaxi.isWithoutCustomer());
        roboTaxi.setAVStatus(AVStatus.DRIVETOCUSTMER);

        // 1) enter information into pickup table
        RoboTaxi beforeTaxi = pickupRegister.forcePut(avRequest, roboTaxi);

        // 2) set vehicle diversion
        setRoboTaxiDiversion(roboTaxi, avRequest.getFromLink());
    }

    /** called when a new request enters the system */
    @Override
    public final void onRequestSubmitted(AVRequest request) {
        boolean status = pendingRequests.add(request); // <- store request
        GlobalAssert.that(status);
        protected_onRequestSubmitted_postProcessing(request, status);
    }

    protected void protected_onRequestSubmitted_postProcessing(AVRequest avRequest, boolean status) {
    }

    /** @return map of vehicles that carry a customer and their destination links */
    protected final List<RoboTaxi> getRoboTaxisWithCustomer() {
        return getRoboTaxis().stream().filter(v -> v.getAVStatus().equals(AVStatus.DRIVEWITHCUSTOMER)).collect(Collectors.toList());
    }

    /** {@link PartitionedDispatcher} overrides the function
     * 
     * @return map of rebalancing vehicles and their destination links */
    protected List<RoboTaxi> getRebalancingRoboTaxis() {
        return Collections.emptyList();
    }

    @Override
    final void stopUnusedVehicles() {
        // stop all vehicles which are not on a pickup or rebalancing mission.
        for (RoboTaxi roboTaxi : getRoboTaxis()) {
            boolean isOnPickup = pickupRegister.containsValue(roboTaxi); // pickupRegister.values().contains(roboTaxi.getAVVehicle());
            boolean isOnExtra = extraCheck(roboTaxi);
            boolean isStaying = roboTaxi.isVehicleInStayTask();
            boolean isWithoutCustomer = roboTaxi.isWithoutCustomer();
            if (!isOnPickup && !isOnExtra && roboTaxi.getDirective() == null && !isStaying && isWithoutCustomer) {
                setRoboTaxiDiversion(roboTaxi, roboTaxi.getDivertableLocation());
            }
        }
        GlobalAssert.that(pickupRegister.size() <= pendingRequests.size());
    }

    boolean extraCheck(RoboTaxi vehicleLinkPair) {
        return false;
    }

    @Override
    final void consistencySubCheck() {
        // there cannot be more pickup vehicles than open reqests
        GlobalAssert.that(pickupRegister.size() <= pendingRequests.size());
    }

    @Override
    final void notifySimulationSubscribers(long round_now) {
        if (publishPeriod > 0 && round_now % publishPeriod == 0) {
            SimulationObjectCompiler simulationObjectCompiler = SimulationObjectCompiler.create( //
                    round_now, getInfoLine(), total_matchedRequests);

            // adding requests submitted and matched within current publish period
            simulationObjectCompiler.addRequests(publishPeriodMatchedRequests);
            // adding requests open in >=1 publish periods
            simulationObjectCompiler.addRequests(getAVRequests());
            
            simulationObjectCompiler.addVehicles(super.getRoboTaxis());
            SimulationObject simulationObject = simulationObjectCompiler.compile();
            
            // in the first pass, the vehicles is typically empty
            // in that case, the simObj will not be stored or communicated
            if (SimulationObjects.hasVehicles(simulationObject)) {
                // GlobalAssert.that(AVVEHILCECOUNT == simulationObject.vehicles.size());
                // store simObj and distribute to clients
                SimulationDistribution.of(simulationObject);
            }
            publishPeriodMatchedRequests.clear();
        }
    }

    /** @return total matched request until now */
    public int getTotalMatchedRequests() {
        return total_matchedRequests;
    }

    @Override
    public String getInfoLine() {
        return String.format("%s R=(%5d) MR=%6d", //
                super.getInfoLine(), //
                getAVRequests().size(), //
                total_matchedRequests);
    }

    public int getDispatchPeriod(SafeConfig safeConfig, int alt) {
        int redispatchPeriod = safeConfig.getInteger("dispatchPeriod", alt);
        ScenarioServer.scenarioParameters.redispatchPeriod = redispatchPeriod;
        return redispatchPeriod;
    }

    public int getRebalancingPeriod(SafeConfig safeConfig, int alt) {
        int rebalancingPeriod = safeConfig.getInteger("rebalancingPeriod", alt);
        ScenarioServer.scenarioParameters.rebalancingPeriod = rebalancingPeriod;
        return rebalancingPeriod;
    }

    // public int getRebalancingPeriod(AVDispatcherConfig config) {
    // int rebalancingPeriod = Integer.parseInt(config.getParams().get("rebalancingPeriod"));
    // ScenarioServer.scenarioParameters.rebalancingPeriod = rebalancingPeriod;
    // return rebalancingPeriod;
    // }

    public void printPickupRegister() {
        if (pickupRegister.size() > 0) {
            System.out.println("=============================");
            System.out.println("printing pickup register:");
            for (AVRequest avR : pickupRegister.keySet()) {
                System.out.println("RoboTaxi " + pickupRegister.get(avR).getId() + " and AVRequest " + avR.getId());
            }
            System.out.println("=============================");
        }

    }

    // ===========================================================================================================
    // ===========================================================================================================
    // ===========================================================================================================
    // OLD FUNCTIONS TO DELETE
    // ===========================================================================================================
    // ===========================================================================================================
    // ===========================================================================================================

    // /** Function called from derived class to match a vehicle with a request. The function
    // appends
    // * the pick-up, drive, and drop-off tasks for the car.
    // *
    // * @param avVehicle
    // * vehicle in {@link AVStayTask} in order to match the request
    // * @param avRequest
    // * provided by getAVRequests() */
    // @Deprecated
    // private synchronized final void setAcceptRequest(AVVehicle avVehicle, AVRequest avRequest) {
    // System.out.println("matched " + avVehicle.getId() + " and " + avRequest.getId());
    // GlobalAssert.that(pendingRequests.contains(avRequest)); // request is known to the system
    //
    // boolean statusPen = pendingRequests.remove(avRequest);
    // GlobalAssert.that(statusPen);
    //
    // // save avRequests which are matched for one publishPeriod to ensure
    // // no requests are lost in the recording.
    // publishPeriodMatchedRequests.add(avRequest);
    //
    // final Schedule schedule = avVehicle.getSchedule();
    // // check that current task is last task in schedule
    // GlobalAssert.that(schedule.getCurrentTask() == Schedules.getLastTask(schedule));
    //
    // final double endPickupTime = getTimeNow() + pickupDurationPerStop;
    // FuturePathContainer futurePathContainer = futurePathFactory.createFuturePathContainer( //
    // avRequest.getFromLink(), avRequest.getToLink(), endPickupTime);
    //
    // assignDirective(avVehicle, new AcceptRequestDirective( //
    // avVehicle, avRequest, futurePathContainer, getTimeNow(), dropoffDurationPerStop));
    //
    // // Link returnVal = vehiclesWithCustomer.put(avVehicle, avRequest.getToLink());
    // // GlobalAssert.that(returnVal == null);
    //
    // ++total_matchedRequests;
    // }

    @Deprecated
    /** @return available vehicles which are yet unassigned to a request as vehicle Link pairs */
    protected List<RoboTaxi> getDivertableUnassignedVehicleLinkPairs() {
        // get the staying vehicles and requests
        List<RoboTaxi> divertableUnassignedVehiclesLinkPairs = new ArrayList<>();
        for (RoboTaxi vehicleLinkPair : getDivertableVehicleLinkPairs()) {
            if (!pickupRegister.containsValue(vehicleLinkPair.getAVVehicle())) {
                // if (!pickupRegister.inverse().containsKey(vehicleLinkPair.avVehicle)) {
                divertableUnassignedVehiclesLinkPairs.add(vehicleLinkPair);
            }
        }
        return divertableUnassignedVehiclesLinkPairs;
    }

    /** assigns new destination to vehicle. if vehicle is already located at destination, nothing
     * happens.
     * 
     * in one pass of redispatch(...), the function setVehicleDiversion(...) may only be invoked
     * once for a single vehicle (specified in
     * vehicleLinkPair).
     *
     * @param vehicleLinkPair
     *            is provided from super.getDivertableVehicles()
     * @param destination */
    @Deprecated
    protected final void setVehicleDiversion(final AVVehicle avVehicle, final Link destination) {
        final Schedule schedule = avVehicle.getSchedule();
        Task task = schedule.getCurrentTask(); // <- implies that task is started
        new AVTaskAdapter(task) {
            @Override
            public void handle(AVDriveTask avDriveTask) {
                if (!avDriveTask.getPath().getToLink().equals(destination)) { // ignore when vehicle
                                                                              // is already going
                                                                              // there
                    FuturePathContainer futurePathContainer = futurePathFactory.createFuturePathContainer( //
                            getLinkTimePair(avVehicle).link, destination, getLinkTimePair(avVehicle).time);

                    assignDirective(avVehicle, new DriveVehicleDiversionDirective( //
                            getVehicleLinkPair(avVehicle), destination, futurePathContainer));
                } else
                    assignDirective(avVehicle, new EmptyDirective());
            }

            @Override
            public void handle(AVStayTask avStayTask) {
                if (!avStayTask.getLink().equals(destination)) { // ignore request where location ==
                                                                 // target
                    FuturePathContainer futurePathContainer = futurePathFactory.createFuturePathContainer( //
                            getLinkTimePair(avVehicle).link, destination, getLinkTimePair(avVehicle).time);

                    assignDirective(avVehicle, new StayVehicleDiversionDirective( //
                            getVehicleLinkPair(avVehicle), destination, futurePathContainer));
                } else
                    assignDirective(avVehicle, new EmptyDirective());
            }
        };
    }

}

/// ** @param avVehicle
// * @return estimated current location of avVehicle, never null */
// @Deprecated // use robotaxi.getDivertableLocation istead
// protected Link getVehicleLocation(AVVehicle avVehicle) {
// Link link = vehicleLocations.get(avVehicle);
// GlobalAssert.that(link != null);
// return link;
// }