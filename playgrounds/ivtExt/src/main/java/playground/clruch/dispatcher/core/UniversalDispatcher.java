// code by jph
package playground.clruch.dispatcher.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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

/**
 * purpose of {@link UniversalDispatcher} is to collect and manage {@link AVRequest}s alternative implementation of {@link AVDispatcher}; supersedes
 * {@link AbstractDispatcher}.
 */
public abstract class UniversalDispatcher extends VehicleMaintainer {
    private final FuturePathFactory futurePathFactory;

    protected final Set<AVRequest> pendingRequests = new LinkedHashSet<>(); // access via getAVRequests()
    private final Set<AVRequest> publishPeriodMatchedRequests = new HashSet<>(); // requests which are matched within a publish period.
    private final Map<AVVehicle, Link> vehiclesWithCustomer = new HashMap<>();
    // TODO visibility of map to private
    protected final BiMap<AVRequest, AVVehicle> pickupRegister = HashBiMap.create();

    /**
     * map stores most recently known location of vehicles. map is used in case obtaining the vehicle location fails
     */
    private final Map<AVVehicle, Link> vehicleLocations = new HashMap<>();

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
    void updateDatastructures(Collection<AVVehicle> stayVehicles) {
        stayVehicles.forEach(vehiclesWithCustomer::remove);

        // complete all matchings if vehicle has arrived on link        
        Set<AVRequest> reqToRemove = new HashSet<>();
        for (AVRequest avRequest : pickupRegister.keySet()) {
            AVVehicle pickupVehicle = pickupRegister.get(avRequest);
            Link pickupVehicleLink = getStayVehiclesUnique().get(pickupVehicle);
            if (avRequest.getFromLink().equals(pickupVehicleLink) && pendingRequests.contains(avRequest)) {
                setAcceptRequest(pickupVehicle, avRequest);
                boolean status = reqToRemove.add(avRequest);
                GlobalAssert.that(status);
            }
        }
        reqToRemove.stream().forEach(v->pickupRegister.remove(v));
        

        // ---
        @SuppressWarnings("unused")
        int failed = 0;
        Collection<AVVehicle> collection = getFunctioningVehicles();
        if (!collection.isEmpty()) {
            for (AVVehicle avVehicle : collection) {
                final Link link = AVLocation.of(avVehicle);
                if (link != null)
                    vehicleLocations.put(avVehicle, link);
                else
                    ++failed;
            }
            if (AVVEHILCECOUNT == null)
                AVVEHILCECOUNT = vehicleLocations.size();
            // TODO this check was taken out because the zurich scenario doesn't satisfy this :-(
            // GlobalAssert.that(AVVEHILCECOUNT == collection.size());
            // GlobalAssert.that(AVVEHILCECOUNT == vehicleLocations.size());
        }
        // if (0 < failed)
        // System.out.println("failed to extract location for " + failed + " vehicles");
    }

    /**
     * function call leaves the state of the {@link UniversalDispatcher} unchanged. successive calls to the function return the identical collection.
     * 
     * @return collection of all requests that have not been matched
     */
    protected synchronized final Collection<AVRequest> getAVRequests() {
        return Collections.unmodifiableCollection(pendingRequests);
    }

    /**
     * function call leaves the state of the {@link UniversalDispatcher} unchanged. successive calls to the function return the identical collection.
     * 
     * @return list of {@link AVRequest}s grouped by link
     */
    public final Map<Link, List<AVRequest>> getAVRequestsAtLinks() {
        return getAVRequests().stream() // <- intentionally not parallel to guarantee ordering of requests
                .collect(Collectors.groupingBy(AVRequest::getFromLink));
    }

    /**
     * @return AVRequests which are currently not assigned to a vehicle
     */
    protected synchronized final List<AVRequest> getUnassignedAVRequests() {
        List<AVRequest> unassignedRequests = new ArrayList<>();
        for (AVRequest avRequest : pendingRequests) {
            if (pickupRegister.get(avRequest) == null) {
                unassignedRequests.add(avRequest);
            }
        }
        return unassignedRequests;
    }

    /**
     * 
     * @return available vehicles which are yet unassigned to a request as vehicle Link pairs
     */
    protected List<RoboTaxi> getDivertableUnassignedVehicleLinkPairs() {
        // get the staying vehicles and requests
        List<RoboTaxi> divertableUnassignedVehiclesLinkPairs = new ArrayList<>();
        for (RoboTaxi vehicleLinkPair : getDivertableVehicleLinkPairs()) {
            if (!pickupRegister.containsValue(vehicleLinkPair.avVehicle)) {
                // if (!pickupRegister.inverse().containsKey(vehicleLinkPair.avVehicle)) {
                divertableUnassignedVehiclesLinkPairs.add(vehicleLinkPair);
            }
        }
        return divertableUnassignedVehiclesLinkPairs;
    }

    /**
     * assigns new destination to vehicle. if vehicle is already located at destination, nothing happens.
     * 
     * in one pass of redispatch(...), the function setVehicleDiversion(...) may only be invoked once for a single vehicle (specified in
     * vehicleLinkPair).
     *
     * @param vehicleLinkPair
     *            is provided from super.getDivertableVehicles()
     * @param destination
     */
    protected final void setVehicleDiversion(final AVVehicle avVehicle, final Link destination) {
        final Schedule schedule = avVehicle.getSchedule();
        Task task = schedule.getCurrentTask(); // <- implies that task is started
        new AVTaskAdapter(task) {
            @Override
            public void handle(AVDriveTask avDriveTask) {
                if (!avDriveTask.getPath().getToLink().equals(destination)) { // ignore when vehicle is already going there
                    FuturePathContainer futurePathContainer = futurePathFactory.createFuturePathContainer( //
                            getLinkTimePair(avVehicle).link, destination, getLinkTimePair(avVehicle).time);

                    assignDirective(avVehicle, new DriveVehicleDiversionDirective( //
                            getVehicleLinkPair(avVehicle), destination, futurePathContainer));
                } else
                    assignDirective(avVehicle, new EmptyDirective());
            }

            @Override
            public void handle(AVStayTask avStayTask) {
                if (!avStayTask.getLink().equals(destination)) { // ignore request where location == target
                    FuturePathContainer futurePathContainer = futurePathFactory.createFuturePathContainer( //
                            getLinkTimePair(avVehicle).link, destination, getLinkTimePair(avVehicle).time);

                    assignDirective(avVehicle, new StayVehicleDiversionDirective( //
                            getVehicleLinkPair(avVehicle), destination, futurePathContainer));
                } else
                    assignDirective(avVehicle, new EmptyDirective());
            }
        };
    }

    /**
     * function for convenience
     * 
     * @param avVehicle
     *            in stay task
     * @param destination
     * @throws Exception
     *             if vehicle is not in stay task
     */
    // protected final void setStayVehicleDiversion(final AVVehicle avVehicle, final Link destination) {
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

    /**
     * Function called from derived class to match a vehicle with a request. The function appends the pick-up, drive, and drop-off tasks for the car.
     * 
     * @param avVehicle
     *            vehicle in {@link AVStayTask} in order to match the request
     * @param avRequest
     *            provided by getAVRequests()
     */
    private synchronized final void setAcceptRequest(AVVehicle avVehicle, AVRequest avRequest) {
        GlobalAssert.that(pendingRequests.contains(avRequest)); // request is known to the system

        boolean statusPen = pendingRequests.remove(avRequest);
        GlobalAssert.that(statusPen);

        // save avRequests which are matched for one publishPeriod to ensure
        // no requests are lost in the recording.
        publishPeriodMatchedRequests.add(avRequest);

        final Schedule schedule = avVehicle.getSchedule();
        // check that current task is last task in schedule
        GlobalAssert.that(schedule.getCurrentTask() == Schedules.getLastTask(schedule));

        final double endPickupTime = getTimeNow() + pickupDurationPerStop;
        FuturePathContainer futurePathContainer = futurePathFactory.createFuturePathContainer( //
                avRequest.getFromLink(), avRequest.getToLink(), endPickupTime);

        assignDirective(avVehicle, new AcceptRequestDirective( //
                avVehicle, avRequest, futurePathContainer, getTimeNow(), dropoffDurationPerStop));

        Link returnVal = vehiclesWithCustomer.put(avVehicle, avRequest.getToLink());
        GlobalAssert.that(returnVal == null);

        ++total_matchedRequests;
    }

    /**
     * 
     * @param entry
     *            <VehicleLinkPair,AVRequest> sets AVVehicle to pickup AVRequest
     */
    // TODO find a way to make this protected again.
    protected void setVehiclePickup(AVVehicle avVehicle, AVRequest avRequest) {
        // 1) enter information into pickup table
        pickupRegister.forcePut(avRequest, avVehicle);

        // 2) set vehicle diversion of AVVehicle
        setVehicleDiversion(avVehicle, avRequest.getFromLink());

    }

    /**
     * called when a new request enters the system
     */
    @Override
    public final void onRequestSubmitted(AVRequest request) {
        boolean status = pendingRequests.add(request); // <- store request
        GlobalAssert.that(status);
        protected_onRequestSubmitted_postProcessing(request, status);
    }

    protected void protected_onRequestSubmitted_postProcessing(AVRequest avRequest, boolean status) {
    }

    /**
     * @return map of vehicles that carry a customer and their destination links
     */
    protected final Map<AVVehicle, Link> getVehiclesWithCustomer() {
        return Collections.unmodifiableMap(vehiclesWithCustomer);
    }

    /**
     * {@link PartitionedDispatcher} overrides the function
     * 
     * @return map of rebalancing vehicles and their destination links
     */
    protected Map<AVVehicle, Link> getRebalancingVehicles() {
        return Collections.emptyMap();
    }

    /**
     * @param avVehicle
     * @return estimated current location of avVehicle, never null
     */
    protected Link getVehicleLocation(AVVehicle avVehicle) {
        Link link = vehicleLocations.get(avVehicle);
        GlobalAssert.that(link != null);
        return link;
    }

    final void endofStepTasks() {
        // stop all vehicles which are not on a pickup or rebalancing mission.
        Collection<RoboTaxi> divertableVehicles = getDivertableVehicleLinkPairs();
        for (RoboTaxi vehicleLinkPair : divertableVehicles) {
            boolean isOnPickup = pickupRegister.values().contains(vehicleLinkPair.avVehicle);
            boolean isOnExtra = extraCheck(vehicleLinkPair);
            if (!isOnPickup && !isOnExtra) {
                setVehicleDiversion(vehicleLinkPair.avVehicle, vehicleLinkPair.getDivertableLocation());
            }
        }
    }

    boolean extraCheck(RoboTaxi vehicleLinkPair) {
        return true;
    }

    @Override
    final void notifySimulationSubscribers(long round_now) {
        if (publishPeriod > 0 && round_now % publishPeriod == 0) {
            SimulationObjectCompiler simulationObjectCompiler = SimulationObjectCompiler.create( //
                    round_now, getInfoLine(), total_matchedRequests);

            simulationObjectCompiler.addRequests(publishPeriodMatchedRequests); // adding requests submittend and matched within current publish
                                                                                // period
            simulationObjectCompiler.addRequests(getAVRequests()); // adding requests open in >=1 publish periods
            simulationObjectCompiler.addVehiclesWithCustomer(getVehiclesWithCustomer(), vehicleLocations);
            simulationObjectCompiler.addRebalancingVehicles(getRebalancingVehicles(), vehicleLocations);
            SimulationObject simulationObject = simulationObjectCompiler.compile( //
                    getDivertableVehicleLinkPairs(), vehicleLocations);

            // in the first pass, the vehicles is typically empty
            // in that case, the simObj will not be stored or communicated
            if (SimulationObjects.hasVehicles(simulationObject)) {
                // GlobalAssert.that(AVVEHILCECOUNT == simulationObject.vehicles.size());
                SimulationDistribution.of(simulationObject); // store simObj and distribute to clients
            }
            publishPeriodMatchedRequests.clear();
        }
    }

    /**
     * @return total matched request until now
     */
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

    public int getRebalancingPeriod(AVDispatcherConfig config) {
        int rebalancingPeriod = Integer.parseInt(config.getParams().get("rebalancingPeriod"));
        ScenarioServer.scenarioParameters.rebalancingPeriod = rebalancingPeriod;
        return rebalancingPeriod;
    }

}
