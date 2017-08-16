// TODO update documentation

// code by jph
// refactoring, large changes by clruch
package playground.clruch.dispatcher.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Schedules;
import org.matsim.contrib.dvrp.schedule.Task;
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
    private final Set<AVRequest> pendingRequests = new LinkedHashSet<>();
    private final Set<AVRequest> publishPeriodMatchedRequests = new HashSet<>();
    private final BiMap<AVRequest, RoboTaxi> pickupRegister = HashBiMap.create();
    private final double pickupDurationPerStop;
    private final double dropoffDurationPerStop;
    protected int publishPeriod; // not final, so that dispatchers can disable, or manipulate
    private int total_matchedRequests = 0;
    private Integer AVVEHILCECOUNT = null;

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


    
 // ===================================================================================
    // GET AVREQUEST functions
    

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

    
    // ===================================================================================
    // GET ROBOTAXI functions
    
    /** Example call
     * getRoboTaxiSubset(AVStatus.STAY, AVStatus.DRIVEWITHCUSTOMER)
     * 
     * @param status AVStatus of desired robotaxis, e.g., STAY,DRIVETOCUSTOMER,...
     * @return list of robotaxis which are in AVStatus status */
    public final List<RoboTaxi> getRoboTaxiSubset(AVStatus... status) {
        Set<AVStatus> enumSet = EnumSet.noneOf(AVStatus.class);
        for (AVStatus s : status)
            enumSet.add(s);
        return getRoboTaxiSubset(enumSet);
    }

    private final List<RoboTaxi> getRoboTaxiSubset(Set<AVStatus> status) {
        return getRoboTaxis().stream().filter(status::contains).collect(Collectors.toList());
    }

    /** @return divertable robotaxis which currently not on a pickup drive */
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
    
    
    // ===================================================================================
    // SET ROBOTAXI functions

    
    protected void setRoboTaxiPickup(RoboTaxi roboTaxi, AVRequest avRequest) {
        GlobalAssert.that(roboTaxi.isWithoutCustomer());
        roboTaxi.setAVStatus(AVStatus.DRIVETOCUSTMER);

        // 1) enter information into pickup table
        RoboTaxi beforeTaxi = pickupRegister.forcePut(avRequest, roboTaxi);

        // 2) set vehicle diversion
        setRoboTaxiDiversion(roboTaxi, avRequest.getFromLink(), AVStatus.DRIVETOCUSTMER);
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
    protected final void setRoboTaxiDiversion(RoboTaxi robotaxi, Link destination, AVStatus avstatus) {
        GlobalAssert.that(robotaxi.isWithoutCustomer());

        robotaxi.setAVStatus(avstatus);

        // in case vehicle was previously picking up, remove from pickup register
        if (!avstatus.equals(AVStatus.DRIVETOCUSTMER)) {
            if (pickupRegister.containsValue(robotaxi)) {
                pickupRegister.remove(pickupRegister.inverse().get(robotaxi), robotaxi);
            }
        }

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
    
    
    // ===================================================================================
    // ROBOTAXI HANDLING functions

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

        ++total_matchedRequests;
    }
    
    protected final AVRequest getRoboTaxiPickupRequest(RoboTaxi robotaxi) {
        return pickupRegister.inverse().get(robotaxi);
    }

    
    // ===================================================================================
    // OTHER get functions
    
    protected int getDispatchPeriod(SafeConfig safeConfig, int alt) {
        int redispatchPeriod = safeConfig.getInteger("dispatchPeriod", alt);
        ScenarioServer.scenarioParameters.redispatchPeriod = redispatchPeriod;
        return redispatchPeriod;
    }

    protected int getRebalancingPeriod(SafeConfig safeConfig, int alt) {
        int rebalancingPeriod = safeConfig.getInteger("rebalancingPeriod", alt);
        ScenarioServer.scenarioParameters.rebalancingPeriod = rebalancingPeriod;
        return rebalancingPeriod;
    }
    
    
    // ===================================================================================
    // ITERATION STEP RELATED methods

    
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
    // TODO remove unused argument
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

    }
    

    /** called when a new request enters the system */
    @Override
    public final void onRequestSubmitted(AVRequest request) {
        boolean status = pendingRequests.add(request); // <- store request
        GlobalAssert.that(status);
    }

    @Override
    protected final void stopUnusedVehicles() {
        // stop all vehicles which are not on a pickup or rebalancing mission.
        for (RoboTaxi roboTaxi : getRoboTaxis()) {
            boolean isOnPickup = pickupRegister.containsValue(roboTaxi); // pickupRegister.values().contains(roboTaxi.getAVVehicle());
            boolean isOnExtra = extraCheck(roboTaxi);
            boolean isStaying = roboTaxi.isVehicleInStayTask();
            boolean isWithoutCustomer = roboTaxi.isWithoutCustomer();
            if (!isOnPickup && !isOnExtra && roboTaxi.getDirective() == null && !isStaying && isWithoutCustomer) {
                setRoboTaxiDiversion(roboTaxi, roboTaxi.getDivertableLocation(), AVStatus.REBALANCEDRIVE);
            }
        }
        GlobalAssert.that(pickupRegister.size() <= pendingRequests.size());
    }

    /* package */ boolean extraCheck(RoboTaxi vehicleLinkPair) {
        return false;
    }

    @Override
    protected final void consistencySubCheck() {
        // there cannot be more pickup vehicles than open reqests
        GlobalAssert.that(pickupRegister.size() <= pendingRequests.size());
    }

    @Override
    protected final void notifySimulationSubscribers(long round_now) {
        if (publishPeriod > 0 && round_now % publishPeriod == 0) {
            SimulationObjectCompiler simulationObjectCompiler = SimulationObjectCompiler.create( //
                    round_now, getInfoLine(), total_matchedRequests);

            // adding requests submitted and matched within current publish period
            simulationObjectCompiler.insertRequests(publishPeriodMatchedRequests);
            // adding requests open in >=1 publish periods
            simulationObjectCompiler.insertRequests(getAVRequests());

            simulationObjectCompiler.insertVehicles(super.getRoboTaxis());
            SimulationObject simulationObject = simulationObjectCompiler.compile();

            // in the first pass, the vehicles is typically empty
            // in that case, the simObj will not be stored or communicated
            if (SimulationObjects.hasVehicles(simulationObject)) {
                // store simObj and distribute to clients
                SimulationDistribution.of(simulationObject);
            }
            publishPeriodMatchedRequests.clear();
        }
    }

    @Override
    protected String getInfoLine() {
        return String.format("%s R=(%5d) MR=%6d", //
                super.getInfoLine(), //
                getAVRequests().size(), //
                total_matchedRequests);
    }

}
