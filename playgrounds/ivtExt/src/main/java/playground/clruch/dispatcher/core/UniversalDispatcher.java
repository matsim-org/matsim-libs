package playground.clruch.dispatcher.core;

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

import playground.clruch.export.AVStatus;
import playground.clruch.gfx.MatsimStaticDatabase;
import playground.clruch.net.RequestContainer;
import playground.clruch.net.SimulationObject;
import playground.clruch.net.SimulationServer;
import playground.clruch.net.SimulationSubscriber;
import playground.clruch.net.SimulationSubscriberSet;
import playground.clruch.net.VehicleContainer;
import playground.clruch.router.FuturePathContainer;
import playground.clruch.router.FuturePathFactory;
import playground.clruch.utils.AVLocation;
import playground.clruch.utils.AVTaskAdapter;
import playground.clruch.utils.GlobalAssert;
import playground.clruch.utils.SafeConfig;
import playground.clruch.utils.ScheduleUtils;
import playground.sebhoerl.avtaxi.config.AVDispatcherConfig;
import playground.sebhoerl.avtaxi.data.AVVehicle;
import playground.sebhoerl.avtaxi.dispatcher.AVDispatcher;
import playground.sebhoerl.avtaxi.dispatcher.AbstractDispatcher;
import playground.sebhoerl.avtaxi.passenger.AVRequest;
import playground.sebhoerl.avtaxi.schedule.AVDriveTask;
import playground.sebhoerl.avtaxi.schedule.AVStayTask;
import playground.sebhoerl.plcpc.ParallelLeastCostPathCalculator;

/**
 * purpose of {@link UniversalDispatcher} is to collect and manage {@link AVRequest}s
 * alternative implementation of {@link AVDispatcher}; supersedes {@link AbstractDispatcher}.
 */
public abstract class UniversalDispatcher extends VehicleMaintainer {
    private final FuturePathFactory futurePathFactory;

    private final Set<AVRequest> pendingRequests = new LinkedHashSet<>(); // access via getAVRequests()
    private final Set<AVRequest> matchedRequests = new HashSet<>(); // for data integrity, private!
    private final Map<AVVehicle, Link> vehiclesWithCustomer = new HashMap<>();

    private final double pickupDurationPerStop;
    private final double dropoffDurationPerStop;
    private final int publishPeriod;

    private int total_matchedRequests = 0;

    protected UniversalDispatcher( //
            AVDispatcherConfig avDispatcherConfig, //
            TravelTime travelTime, //
            ParallelLeastCostPathCalculator parallelLeastCostPathCalculator, //
            EventsManager eventsManager //
    ) {
        super(eventsManager);
        this.futurePathFactory = new FuturePathFactory(parallelLeastCostPathCalculator, travelTime);

        pickupDurationPerStop = avDispatcherConfig.getParent().getTimingParameters().getPickupDurationPerStop();
        dropoffDurationPerStop = avDispatcherConfig.getParent().getTimingParameters().getDropoffDurationPerStop();

        SafeConfig safeConfig = SafeConfig.wrap(avDispatcherConfig);
        setInfoLinePeriod(safeConfig.getInteger("infoLinePeriod", 10));
        publishPeriod = safeConfig.getInteger("publishPeriod", 10);
    }

    @Override
    void updateDatastructures(Collection<AVVehicle> stayVehicles) {
        stayVehicles.forEach(vehiclesWithCustomer::remove);
    }

    /**
     * function call leaves the state of the {@link UniversalDispatcher} unchanged.
     * successive calls to the function return the identical collection.
     * 
     * @return collection of all requests that have not been matched
     */
    protected synchronized final Collection<AVRequest> getAVRequests() {
        pendingRequests.removeAll(matchedRequests);
        matchedRequests.clear();
        return Collections.unmodifiableCollection(pendingRequests);
    }

    /**
     * function call leaves the state of the {@link UniversalDispatcher} unchanged.
     * successive calls to the function return the identical collection.
     * 
     * @return list of {@link AVRequest}s grouped by link
     */
    public final Map<Link, List<AVRequest>> getAVRequestsAtLinks() {
        return getAVRequests().stream() // <- intentionally not parallel to guarantee ordering of requests
                .collect(Collectors.groupingBy(AVRequest::getFromLink));
    }

    /**
     * Function called from derived class to match a vehicle with a request.
     * The function appends the pick-up, drive, and drop-off tasks for the car.
     * 
     * @param avVehicle
     *            vehicle in {@link AVStayTask} in order to match the request
     * @param avRequest
     *            provided by getAVRequests()
     */
    protected synchronized final void setAcceptRequest(AVVehicle avVehicle, AVRequest avRequest) {
        GlobalAssert.that(pendingRequests.contains(avRequest)); // request is known to the system

        boolean status = matchedRequests.add(avRequest);
        GlobalAssert.that(status); // matchedRequests did not already contain avRequest

        final Schedule schedule = avVehicle.getSchedule();
        GlobalAssert.that(schedule.getCurrentTask() == Schedules.getLastTask(schedule)); // check that current task is last task in schedule

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
     * assigns new destination to vehicle.
     * if vehicle is already located at destination, nothing happens.
     * 
     * in one pass of redispatch(...), the function setVehicleDiversion(...)
     * may only be invoked once for a single vehicle (specified in vehicleLinkPair).
     *
     * @param vehicleLinkPair
     *            is provided from super.getDivertableVehicles()
     * @param destination
     */
    protected final void setVehicleDiversion(final VehicleLinkPair vehicleLinkPair, final Link destination) {
        final Schedule schedule = vehicleLinkPair.avVehicle.getSchedule();
        Task task = schedule.getCurrentTask(); // <- implies that task is started
        new AVTaskAdapter(task) {
            @Override
            public void handle(AVDriveTask avDriveTask) {
                if (!avDriveTask.getPath().getToLink().equals(destination)) { // ignore when vehicle is already going there
                    FuturePathContainer futurePathContainer = futurePathFactory.createFuturePathContainer( //
                            vehicleLinkPair.linkTimePair.link, destination, vehicleLinkPair.linkTimePair.time);

                    assignDirective(vehicleLinkPair.avVehicle, new DriveVehicleDiversionDirective( //
                            vehicleLinkPair, destination, futurePathContainer));
                } else
                    assignDirective(vehicleLinkPair.avVehicle, new EmptyDirective());
            }

            @Override
            public void handle(AVStayTask avStayTask) {
                if (!avStayTask.getLink().equals(destination)) { // ignore request where location == target
                    FuturePathContainer futurePathContainer = futurePathFactory.createFuturePathContainer( //
                            vehicleLinkPair.linkTimePair.link, destination, vehicleLinkPair.linkTimePair.time);

                    assignDirective(vehicleLinkPair.avVehicle, new StayVehicleDiversionDirective( //
                            vehicleLinkPair, destination, futurePathContainer));
                } else
                    assignDirective(vehicleLinkPair.avVehicle, new EmptyDirective());
            }
        };
    }

    public final void setVehicleDiversion(final Entry<VehicleLinkPair, Link> entry) {
        setVehicleDiversion(entry.getKey(), entry.getValue());
    }

    @Override
    public void onNextLinkEntered(AVVehicle avVehicle, DriveTask driveTask, LinkTimePair linkTimePair) {
        // default implementation: for now, do nothing
    }

    @Override
    public final void onRequestSubmitted(AVRequest request) {
        pendingRequests.add(request); // <- store request
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

    @Override
    protected final void notifySimulationSubscribers(long round_now) {
        if (SimulationServer.INSTANCE.isRunning() && //
                round_now % publishPeriod == 0 && 0 < getAVRequests().size()) { // TODO not final criteria
            if (SimulationSubscriberSet.INSTANCE.isEmpty())
                System.out.println("waiting for connections...");
            // block for connections
            while (SimulationSubscriberSet.INSTANCE.isEmpty())
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            final MatsimStaticDatabase db = MatsimStaticDatabase.INSTANCE;

            final SimulationObject simulationObject = new SimulationObject();
            simulationObject.infoLine = getInfoLine();
            simulationObject.now = round_now;
            simulationObject.total_matchedRequests = total_matchedRequests;
            {
                // REQUESTS
                for (AVRequest avRequest : getAVRequests()) {
                    RequestContainer requestContainer = new RequestContainer();
                    requestContainer.requestId = avRequest.getId().toString();
                    requestContainer.fromLinkId = db.getLinkIndex(avRequest.getFromLink());
                    requestContainer.submissionTime = avRequest.getSubmissionTime();
                    requestContainer.toLinkId = db.getLinkIndex(avRequest.getToLink());
                    simulationObject.requests.add(requestContainer);
                }
            }
            {
                // VEHICLES
                final Map<String, VehicleContainer> vehicleMap = new HashMap<>();

                for (Entry<AVVehicle, Link> entry : getVehiclesWithCustomer().entrySet()) {
                    VehicleContainer vehicleContainer = new VehicleContainer();
                    AVVehicle avVehicle = entry.getKey();
                    final String key = avVehicle.getId().toString();
                    final Link fromLink = AVLocation.of(avVehicle);
                    if (fromLink != null) {
                        vehicleContainer.linkId = db.getLinkIndex(fromLink);
                        vehicleContainer.avStatus = AVStatus.DRIVEWITHCUSTOMER;
                        vehicleContainer.destinationLinkId = db.getLinkIndex(entry.getValue());
                        vehicleMap.put(key, vehicleContainer);
                    } else {
                        System.out.println("location extraction fail (1):");
                        System.out.println(ScheduleUtils.toString(avVehicle.getSchedule()));
                    }
                }

                for (Entry<AVVehicle, Link> entry : getRebalancingVehicles().entrySet()) {
                    VehicleContainer vehicleContainer = new VehicleContainer();
                    AVVehicle avVehicle = entry.getKey();
                    final String key = avVehicle.getId().toString();
                    final Link fromLink = AVLocation.of(avVehicle);
                    if (fromLink != null) {
                        vehicleContainer.linkId = db.getLinkIndex(fromLink);
                        vehicleContainer.avStatus = AVStatus.REBALANCEDRIVE;
                        vehicleContainer.destinationLinkId = db.getLinkIndex(entry.getValue());
                        vehicleMap.put(key, vehicleContainer);
                    } else {
                        System.out.println("location extraction fail (2):");
                        System.out.println(ScheduleUtils.toString(avVehicle.getSchedule()));
                    }
                }

                // divertible vehicles are either stay, pickup, or rebalancing...
                for (VehicleLinkPair vlp : getDivertableVehicles()) {
                    final String key = vlp.avVehicle.getId().toString();
                    if (!vehicleMap.containsKey(key)) {
                        VehicleContainer vehicleContainer = new VehicleContainer();
                        vehicleContainer.linkId = db.getLinkIndex(vlp.linkTimePair.link);
                        if (vlp.isVehicleInStayTask()) {
                            vehicleContainer.avStatus = AVStatus.STAY;
                        } else {
                            vehicleContainer.avStatus = AVStatus.DRIVETOCUSTMER;
                            vehicleContainer.destinationLinkId = db.getLinkIndex(vlp.getCurrentDriveDestination());
                        }
                        vehicleMap.put(key, vehicleContainer);
                    }
                }

                simulationObject.vehicles = vehicleMap.values().stream().collect(Collectors.toList());
            }

            for (SimulationSubscriber simulationSubscriber : SimulationSubscriberSet.INSTANCE)
                simulationSubscriber.handle(simulationObject);
        }
    }

    @Override
    public String getInfoLine() {
        return String.format("%s R=(%5d) MR=%6d", //
                super.getInfoLine(), //
                getAVRequests().size(), //
                total_matchedRequests);
    }

}
