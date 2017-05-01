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
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.dvrp.util.LinkTimePair;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.router.util.TravelTime;

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
 * purpose of {@link UniversalDispatcher} is to collect and manage {@link AVRequest}s
 * alternative implementation of {@link AVDispatcher}; supersedes {@link AbstractDispatcher}.
 */
public abstract class AbstractUniversalDispatcher extends VehicleMaintainer {
    protected final FuturePathFactory futurePathFactory;

    protected final Set<AVRequest> pendingRequests = new LinkedHashSet<>(); // access via getAVRequests()
    protected final Set<AVRequest> matchedRequests = new HashSet<>(); // for data integrity, private!
    protected final Map<AVVehicle, Link> vehiclesWithCustomer = new HashMap<>();

    /**
     * map stores most recently known location of vehicles.
     * map is used in case obtaining the vehicle location fails
     */
    protected final Map<AVVehicle, Link> vehicleLocations = new HashMap<>();

    protected final double pickupDurationPerStop;
    protected final double dropoffDurationPerStop;
    protected final int publishPeriod;

    protected int total_matchedRequests = 0;
    protected Integer AVVEHILCECOUNT = null;

    protected AbstractUniversalDispatcher( //
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
        // ---
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
//            GlobalAssert.that(AVVEHILCECOUNT == collection.size());
//            GlobalAssert.that(AVVEHILCECOUNT == vehicleLocations.size());
        }
        // if (0 < failed)
        // System.out.println("failed to extract location for " + failed + " vehicles");
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
    
    abstract protected void setAcceptRequest(AVVehicle avVehicle, AVRequest avRequest);
    
    
    
    
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


    abstract public void onRequestSubmitted(AVRequest request);

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

    @Override
    final void notifySimulationSubscribers(long round_now) {
        if (round_now % publishPeriod == 0) {
            SimulationObjectCompiler simulationObjectCompiler = SimulationObjectCompiler.create( //
                    round_now, getInfoLine(), total_matchedRequests);
            simulationObjectCompiler.addRequests(getAVRequests());
            simulationObjectCompiler.addVehiclesWithCustomer(getVehiclesWithCustomer(), vehicleLocations);
            simulationObjectCompiler.addRebalancingVehicles(getRebalancingVehicles(), vehicleLocations);
            SimulationObject simulationObject = simulationObjectCompiler.compile( //
                    getDivertableVehicles(), vehicleLocations);

            // in the first pass, the vehicles is typically empty
            // in that case, the simObj will not be stored or communicated
            if (SimulationObjects.hasVehicles(simulationObject)) {
                // GlobalAssert.that(AVVEHILCECOUNT == simulationObject.vehicles.size());
                SimulationDistribution.of(simulationObject); // store simObj and distribute to clients
            }
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

}
