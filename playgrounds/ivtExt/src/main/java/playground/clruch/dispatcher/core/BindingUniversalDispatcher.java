package playground.clruch.dispatcher.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.router.util.TravelTime;

import playground.clruch.utils.GlobalAssert;
import playground.sebhoerl.avtaxi.config.AVDispatcherConfig;
import playground.sebhoerl.avtaxi.data.AVVehicle;
import playground.sebhoerl.avtaxi.passenger.AVRequest;
import playground.sebhoerl.avtaxi.schedule.AVStayTask;
import playground.sebhoerl.plcpc.ParallelLeastCostPathCalculator;

public abstract class BindingUniversalDispatcher extends UniversalDispatcher {

    private final Set<AVRequest> assignedRequests = new HashSet<>(); // pending requests which are assigned to an AV
    private final Set<AVRequest> unassignedRequests = new HashSet<>(); // pending requests which are still unassigned
    private final Set<AVVehicle> assignedVehicles = new HashSet<>(); // vehicles which are currently assigned to a request
    
    private final Map<AVRequest, AVVehicle> matchings = new HashMap<>(); // history of customer/AV matchings

    protected BindingUniversalDispatcher( //
            AVDispatcherConfig avDispatcherConfig, //
            TravelTime travelTime, //
            ParallelLeastCostPathCalculator parallelLeastCostPathCalculator, //
            EventsManager eventsManager //
    ) {
        super(avDispatcherConfig, travelTime, parallelLeastCostPathCalculator, eventsManager);
    }

    /**
     * @return AVRequests which are currently not assigned to a vehicle
     */
    protected synchronized final Collection<AVRequest> getUnassignedAVRequests() {
        GlobalAssert.that(pendingRequests.size() == assignedRequests.size() + unassignedRequests.size());
        return Collections.unmodifiableCollection(unassignedRequests);
    }

    /**
     * Function called from derived class to match a vehicle with a request. The function appends the pick-up, drive, and drop-off tasks for the car.
     * 
     * @param avVehicle
     *            vehicle in {@link AVStayTask} in order to match the request
     * @param avRequest
     *            provided by getAVRequests()
     */
    @Override
    protected void protected_setAcceptRequest_postProcessing(AVVehicle avVehicle, AVRequest avRequest) {
        // this is only functional if the Set "assignedVehicles" was used (e.g. by
        // SingleHeuristicDispatcher)
        boolean succPR = pendingRequests.remove(avRequest);
        boolean succAR = assignedRequests.remove(avRequest);
        boolean succAV = assignedVehicles.remove(avVehicle);
        GlobalAssert.that(succAV == succAR && succAR == succPR);
        GlobalAssert.that(succPR);
        GlobalAssert.that(matchings.remove(avRequest) != null);
        GlobalAssert.that(pendingRequests.size() == unassignedRequests.size() + assignedRequests.size());
    }

    /**
     * Sends a stay vehicle to a customer in a way such that it can be made unavailalbe for future dispatcher calls
     * 
     * @param avVehicle
     * @param avRequest
     */
    protected final void sendStayVehicleCustomer(AVVehicle avVehicle, AVRequest avRequest) {
        setStayVehicleDiversion(avVehicle, avRequest.getFromLink());

        boolean succReqU = unassignedRequests.remove(avRequest);
        boolean succReqA = assignedRequests.add(avRequest);
        GlobalAssert.that(succReqU == succReqA);
        GlobalAssert.that(succReqA);

        // declare vehicle as assigned
        boolean succVeh = assignedVehicles.add(avVehicle);
        GlobalAssert.that(succVeh);

        // add pair to matchings
        AVVehicle former = matchings.put(avRequest, avVehicle);
        GlobalAssert.that(former == null);

        GlobalAssert.that(pendingRequests.size() == unassignedRequests.size() + assignedRequests.size());

        // ++total_matchedRequests; // jan made this private and removed effect of line
    }

    /**
     * 
     * @return available vehicles which are yet unassigned to a request as vehicle Link pairs
     */
    protected List<VehicleLinkPair> getavailableUnassignedVehicleLinkPairs() {
        // get the staying vehicles and requests
        List<AVVehicle> availableVehicles = getStayVehicles().values().stream().flatMap(Queue::stream).collect(Collectors.toList());
        List<AVVehicle> availableUnassignedVehicles = availableVehicles.stream() //
                .filter(v -> !assignedVehicles.contains(v)) //
                .collect(Collectors.toList());
        List<VehicleLinkPair> returnList = new ArrayList<>();
        for (AVVehicle avVehicle : availableUnassignedVehicles)
            returnList.add(VehicleLinkPairs.ofStayVehicle(avVehicle, getTimeNow()));
        return returnList;
    }

    @Override
    protected void protected_onRequestSubmitted_postProcessing(AVRequest request, boolean status) {
        boolean succAddU = unassignedRequests.add(request);
        GlobalAssert.that(status == succAddU);
        GlobalAssert.that(pendingRequests.size() == unassignedRequests.size() + assignedRequests.size());
    }

    /**
     * 
     * @return matchings between AVRequests and AVVehicles
     */
    protected Map<AVRequest, AVVehicle> getMatchings() {
        return matchings;
    }

}
