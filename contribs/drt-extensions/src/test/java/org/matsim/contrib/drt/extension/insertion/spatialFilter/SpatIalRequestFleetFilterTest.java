package org.matsim.contrib.drt.extension.insertion.spatialFilter;

import com.google.common.collect.ImmutableList;
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
import org.matsim.contrib.drt.schedule.DefaultDrtStopTask;
import org.matsim.contrib.dvrp.fleet.*;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.network.NetworkUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.testcases.fakes.FakeLink;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author nkuehnel / MOIA
 */
public class SpatIalRequestFleetFilterTest {

    public static final Id<DvrpVehicle> V_1_ID = Id.create("v1", DvrpVehicle.class);

    private static final Id<Link> linkId = Id.createLinkId(1);


    @RegisterExtension
    public MatsimTestUtils utils = new MatsimTestUtils();

    @Test
    void test() {

        Link link = prepareNetworkAndLink();
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
        MobsimTimer timer = new MobsimTimer(1);
        timer.setTime(0);

        // vehicle entry
        Waypoint.Start start = start(null, 0, link, 1);//not a STOP -> pickup cannot be appended
        DvrpVehicle vehicle = fleet.getVehicles().get(V_1_ID);
        var vehicleEntry = entry(vehicle, start);

        DrtRequest dummyRequest = request("r1", link, link, 0., 0, 0, 0);

        SpatialRequestFleetFilter spatialRequestFleetFilter = new SpatialRequestFleetFilter(fleet, timer, new SpatialFilterInsertionSearchQSimModule.SpatialInsertionFilterSettings(2, 0, 0, true, 0, 100));
        Collection<VehicleEntry> filtered = spatialRequestFleetFilter.filter(dummyRequest, Map.of(V_1_ID, vehicleEntry), 0);
        Assertions.assertThat(filtered).isEmpty();
    }

    @Test
    void testReturnAllIfEmpty() {

        Link link = prepareNetworkAndLink();
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
        MobsimTimer timer = new MobsimTimer(1);
        timer.setTime(0);

        // vehicle entry
        Waypoint.Start start = start(null, 0, link, 1);//not a STOP -> pickup cannot be appended
        DvrpVehicle vehicle = fleet.getVehicles().get(V_1_ID);
        var vehicleEntry = entry(vehicle, start);

        DrtRequest dummyRequest = request("r1", link, link, 0., 0, 0, 0);

        SpatialRequestFleetFilter spatialRequestFleetFilter = new SpatialRequestFleetFilter(fleet, timer,
                new SpatialFilterInsertionSearchQSimModule.SpatialInsertionFilterSettings(2,
                        1, 0, true, 1, 100));
        Collection<VehicleEntry> filtered = spatialRequestFleetFilter.filter(dummyRequest, Map.of(V_1_ID, vehicleEntry), 0);
        Assertions.assertThat(filtered).isNotEmpty();
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
        return new Waypoint.Start(task, link, time, occupancy);
    }

    private Waypoint.Stop stop(DefaultDrtStopTask stopTask, int outgoingOccupancy) {
        return new Waypoint.Stop(stopTask, outgoingOccupancy);
    }

}