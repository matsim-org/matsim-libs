package org.matsim.contrib.drt.extension.insertion.spatialFilter;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.drt.optimizer.VehicleEntry;
import org.matsim.contrib.drt.optimizer.Waypoint;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.drt.schedule.DrtTaskFactoryImpl;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.fleet.FleetSpecificationImpl;
import org.matsim.contrib.dvrp.fleet.Fleets;
import org.matsim.contrib.dvrp.fleet.ImmutableDvrpVehicleSpecification;
import org.matsim.contrib.dvrp.load.IntegersLoadType;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.network.NetworkUtils;
import org.matsim.testcases.MatsimTestUtils;

import com.google.common.collect.ImmutableList;

/**
 * @author nkuehnel / MOIA
 */
public class SpatIalRequestFleetFilterTest {

    public static final Id<DvrpVehicle> V_1_ID = Id.create("v1", DvrpVehicle.class);

    private static final Id<Link> linkId = Id.createLinkId(1);


    @RegisterExtension
    public MatsimTestUtils utils = new MatsimTestUtils();

    private static final IntegersLoadType loadType = new IntegersLoadType("passengers");

    @Test
    void test() {

        Link link = prepareNetworkAndLink();

        MobsimTimer timer = new MobsimTimer(1);
        timer.setTime(10);

        Fleet fleet = getFleet(link);
        var vehicleEntry = getVehicleEntry(link, fleet);

        DrtRequest dummyRequest = request("r1", link, link, 0., 0, 0, 0);

        DrtSpatialRequestFleetFilterParams params = new DrtSpatialRequestFleetFilterParams();
        params.updateInterval = 1;
        params.expansionFactor = 2;

        // mimic no finding of candidates by setting min higher by max (prevented by config consistency check in regular setup)
        params.minExpansion = 1;
        params.maxExpansion = 0;
        params.returnAllIfEmpty = false;
        SpatialRequestFleetFilter spatialRequestFleetFilter = new SpatialRequestFleetFilter(fleet, timer, params);
        Collection<VehicleEntry> filtered = spatialRequestFleetFilter.filter(dummyRequest, Map.of(V_1_ID, vehicleEntry), 0);
        Assertions.assertThat(filtered).isEmpty();
    }

    @Test
    void testReturnAllIfEmpty() {

        Link link = prepareNetworkAndLink();

        MobsimTimer timer = new MobsimTimer(1);
        timer.setTime(10);

        // vehicle entry
        Fleet fleet = getFleet(link);
        var vehicleEntry = getVehicleEntry(link, fleet);

        DrtRequest dummyRequest = request("r1", link, link, 0., 0, 0, 0);

        DrtSpatialRequestFleetFilterParams params = new DrtSpatialRequestFleetFilterParams();
        params.updateInterval = 1;
        params.expansionFactor = 2;
        params.minExpansion = 1;
        params.maxExpansion = 0;
        params.returnAllIfEmpty = true;
        SpatialRequestFleetFilter spatialRequestFleetFilter = new SpatialRequestFleetFilter(fleet, timer, params);
        Collection<VehicleEntry> filtered = spatialRequestFleetFilter.filter(dummyRequest, Map.of(V_1_ID, vehicleEntry), 0);
        Assertions.assertThat(filtered).isNotEmpty();
    }

    @NotNull
    private VehicleEntry getVehicleEntry(Link link, Fleet fleet) {
        Waypoint.Start start = start(null, 0, link, 1);//not a STOP -> pickup cannot be appended
        DvrpVehicle vehicle = fleet.getVehicles().get(V_1_ID);

        vehicle.getSchedule()
                .addTask(new DrtTaskFactoryImpl().createInitialTask(vehicle, vehicle.getServiceBeginTime(), vehicle.getServiceEndTime(),
                        vehicle.getStartLink()));

        Task task = vehicle.getSchedule().nextTask();

        vehicle.getSchedule().getCurrentTask().setEndTime(0);
        var vehicleEntry = entry(vehicle, start);
        return vehicleEntry;
    }

    @NotNull
    private static Fleet getFleet(Link link) {
        FleetSpecificationImpl fleetSpecification = new FleetSpecificationImpl();
        fleetSpecification.addVehicleSpecification(ImmutableDvrpVehicleSpecification.newBuilder()
                .id(V_1_ID)
                .startLinkId(link.getId())
                .capacity(6)
                .serviceBeginTime(0)
                .serviceEndTime(1000)
                .build()
        );

        Fleet fleet = Fleets.createDefaultFleet(fleetSpecification, dvrpVehicleSpecification -> link);
        return fleet;
    }

    @NotNull
    private static Link prepareNetworkAndLink() {
        Network network = NetworkUtils.createNetwork();

        Node fromNode = NetworkUtils.createAndAddNode(network, Id.createNodeId(1), new Coord(0, 0));
        Node toNode = NetworkUtils.createAndAddNode(network, Id.createNodeId(2), new Coord(0, 0));
        Link link = NetworkUtils.createAndAddLink(network, linkId, fromNode, toNode, 0, 0, 0, 0);
        return link;
    }

    private DrtRequest request(String id, Link fromLink, Link toLink, double submissionTime,
                               double latestArrivalTime, double earliestStartTime, double latestStartTime) {
        return DrtRequest.newBuilder()
                .id(Id.create(id, Request.class))
                .passengerIds(List.of(Id.createPersonId(id)))
                .submissionTime(submissionTime)
                .latestArrivalTime(latestArrivalTime)
                .latestStartTime(latestStartTime)
                .earliestStartTime(earliestStartTime)
                .fromLink(fromLink)
                .toLink(toLink)
                .mode("drt")
                .build();
    }

    private VehicleEntry entry(DvrpVehicle vehicle, Waypoint.Start start, Waypoint.Stop... stops) {
        List<Double> precedingStayTimes = Collections.nCopies(stops.length, 0.0);
        return new VehicleEntry(vehicle, start, ImmutableList.copyOf(stops), null, precedingStayTimes, 0);
    }

    private Waypoint.Start start(Task task, double time, Link link, int occupancy) {
        return new Waypoint.Start(task, link, time, loadType.fromArray(occupancy));
    }

}