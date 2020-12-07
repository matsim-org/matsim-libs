package org.matsim.core.mobsim.qsim.qnetsimengine;

import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.mobsim.framework.DriverAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.qsim.agents.PlanBasedDriverAgentImpl;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.misc.OptionalTime;
import org.matsim.facilities.Facility;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vis.snapshotwriters.AgentSnapshotInfo;
import org.matsim.vis.snapshotwriters.SnapshotLinkWidthCalculator;

import java.util.*;

import static org.junit.Assert.*;

public class QueueAgentSnapshotInfoBuilderTest {

    @Test
    public void positionVehiclesAlongLine_singleVehicleFreeFlow() {

        final var config = ConfigUtils.createConfig();
        config.qsim().setSnapshotStyle(QSimConfigGroup.SnapshotStyle.queue);
        final var scenario = ScenarioUtils.createScenario(config);
        final var freespeed = 10.0;
        final var linkLength = 100.0;
        final var linkCapacity = 10;
        final var linkEnterTime = 0.0;
        final var now = 1.0;
        final var fromCoord = new Coord(0,0);
        final var toCoord = new Coord(linkLength, 0);

        // the test object
        var builder = new QueueAgentSnapshotInfoBuilder(scenario, new SnapshotLinkWidthCalculator());

        // the list to be filled
        Collection<AgentSnapshotInfo> outCollection = new ArrayList<>();

        // create a vehicles on the link
        VehicleType type = VehicleUtils.createVehicleType(Id.create(TransportMode.car, VehicleType.class ) );
        type.setMaximumVelocity(freespeed + 10); // faster than link's freespeed
        var vehicle = new QVehicleImpl(VehicleUtils.createVehicle(Id.createVehicleId(1), type));
        vehicle.setEarliestLinkExitTime(linkEnterTime + linkLength / freespeed);
        vehicle.setDriver(new TestDriverAgent(Id.createPersonId(1)));
        vehicle.setCurrentLink(NetworkUtils.createLink(
                Id.createLinkId(1),
                NetworkUtils.createNode(Id.createNodeId(1), fromCoord),
                NetworkUtils.createNode(Id.createNodeId(2), toCoord),
                NetworkUtils.createNetwork(),
                linkLength,
                freespeed,
                linkCapacity,
                1
        ));
        Collection<MobsimVehicle> vehicles = List.of(vehicle);

        // holes which we are not interested in at the moment
        Queue<QueueWithBuffer.Hole> holes = new LinkedList<>();

        // act
        builder.positionVehiclesAlongLine(
                outCollection,
                now,
                vehicles,
                linkLength,
                linkCapacity,
                fromCoord,
                toCoord,
                1/100.0, // this would mean the flow capacity is 100
                freespeed,
                1,
                holes
                );

        // assert
        assertEquals(1, outCollection.size());
        AgentSnapshotInfo firstEntry = outCollection.iterator().next();

        assertEquals(linkLength / freespeed * now, firstEntry.getEasting(), 0.00001);
        assertEquals(-18.75, firstEntry.getNorthing(), 0.00001); // the calculator assumes an offset to the right of the driving direction ...
        assertEquals(vehicle.getDriver().getId(), firstEntry.getId());
        assertEquals(vehicle.getCurrentLink().getId(), firstEntry.getLinkId());
        assertEquals(vehicle.getId(), firstEntry.getVehicleId());
        assertEquals(1.0, firstEntry.getColorValueBetweenZeroAndOne(), 0.00001);
        assertEquals(AgentSnapshotInfo.AgentState.PERSON_DRIVING_CAR, firstEntry.getAgentState());
    }

    private static Collection<QVehicle> createVehicles(Link link, int size, double exitTimeOfFirstVehicle) {

        VehicleType type = VehicleUtils.createVehicleType(Id.create(TransportMode.car, VehicleType.class ) );
        type.setMaximumVelocity(link.getFreespeed() + 10); // faster than link's freespeed
        var exitTime = exitTimeOfFirstVehicle;

        List<QVehicle> result = new ArrayList<>();

        for (int i = 0; i < size; i++) {

            var vehicle = new QVehicleImpl(VehicleUtils.createVehicle(Id.createVehicleId(1), type));
            vehicle.setEarliestLinkExitTime(exitTime);
            vehicle.setDriver(new TestDriverAgent(Id.createPersonId(1)));
            vehicle.setCurrentLink(link);

            result.add(vehicle);
            exitTime += 5;
        }
        return result;
    }

    @Test
    public void positionVehiclesAlongLine_congestedAboveCapacityLimit() {

        final var config = ConfigUtils.createConfig();
        config.qsim().setSnapshotStyle(QSimConfigGroup.SnapshotStyle.queue);
        final var scenario = ScenarioUtils.createScenario(config);
        final var freespeed = 10.0;
        final var linkLength = 100.0;
        final var linkCapacity = 10;
        final var now = 100;
        final var fromCoord = new Coord(0,0);
        final var toCoord = new Coord(linkLength, 0);

        // the test object
        var builder = new QueueAgentSnapshotInfoBuilder(scenario, new SnapshotLinkWidthCalculator());

        // the list to be filled
        List<AgentSnapshotInfo> outCollection = new ArrayList<>();

        var link = NetworkUtils.createLink(
                Id.createLinkId(1),
                NetworkUtils.createNode(Id.createNodeId(1), fromCoord),
                NetworkUtils.createNode(Id.createNodeId(2), toCoord),
                NetworkUtils.createNetwork(),
                linkLength,
                freespeed,
                linkCapacity,
                1
        );

        var vehicles = createVehicles(link, 20, 0);

        // holes which we are not interested in at the moment
        Queue<QueueWithBuffer.Hole> holes = new LinkedList<>();

        // act
        builder.positionVehiclesAlongLine(
                outCollection,
                now,
                vehicles,
                linkLength,
                linkCapacity,
                fromCoord,
                toCoord,
                1/100.0, // this would mean the flow capacity is 100
                freespeed,
                1,
                holes
        );

        // assert
        assertEquals(20, outCollection.size());

        // we have put more cars onto the link than it can actually fit. The calculator should squeeze all vehicles onto the link now
        //outCollection.sort(Comparator.comparingDouble(AgentSnapshotInfo::getEasting));
        var expectedEasting = linkLength;
        var offsetBetweenVehicles = linkLength / outCollection.size();

        for (AgentSnapshotInfo info : outCollection) {
            assertEquals(expectedEasting, info.getEasting(), 0.0001);
            expectedEasting -= offsetBetweenVehicles;
        }
    }

    @Test
    public void positionVehiclesAlongLine_queueAtEnd() {

        final var config = ConfigUtils.createConfig();
        config.qsim().setSnapshotStyle(QSimConfigGroup.SnapshotStyle.queue);
        final var scenario = ScenarioUtils.createScenario(config);
        final var freespeed = 10.0;
        final var linkLength = 100.0;
        final var linkCapacity = 10;
        final var linkEnterTime = 0.0;
        final var now = 10.0;
        final var fromCoord = new Coord(0,0);
        final var toCoord = new Coord(linkLength, 0);

        // the test object
        var builder = new QueueAgentSnapshotInfoBuilder(scenario, new SnapshotLinkWidthCalculator());

        // the list to be filled
        List<AgentSnapshotInfo> outCollection = new ArrayList<>();

        var link = NetworkUtils.createLink(
                Id.createLinkId(1),
                NetworkUtils.createNode(Id.createNodeId(1), fromCoord),
                NetworkUtils.createNode(Id.createNodeId(2), toCoord),
                NetworkUtils.createNetwork(),
                linkLength,
                freespeed,
                linkCapacity,
                1
        );

        var vehicles = createVehicles(link, 5, 0);

        // holes which we are not interested in at the moment
        Queue<QueueWithBuffer.Hole> holes = new LinkedList<>();

        // act
        builder.positionVehiclesAlongLine(
                outCollection,
                now,
                vehicles,
                linkLength,
                linkCapacity,
                fromCoord,
                toCoord,
                1/100.0, // this would mean the flow capacity is 100
                freespeed,
                1,
                holes
        );

        // assert
        assertEquals(5, outCollection.size());

        // we expect 2 cars to be congested at the end of the link with an offset of 10 and a speed of 0
        var vehicle1 = outCollection.get(0);
        assertEquals(100, vehicle1.getEasting(), 0.0001);
        assertEquals(0.0, vehicle1.getColorValueBetweenZeroAndOne(), 0.00001);
        var vehicle2 = outCollection.get(1);
        assertEquals(90, vehicle2.getEasting(), 0.00001);
        assertEquals(0.0, vehicle2.getColorValueBetweenZeroAndOne(), 0.00001);

        // we expect 1 car to be at the beginning of the queue (2 vehicles * 10 + 10 [its own offset]) with a speed of 1.0
        var vehicle3 = outCollection.get(2);
        assertEquals(80, vehicle3.getEasting(), 0.0001);
        assertEquals(1.0, vehicle3.getColorValueBetweenZeroAndOne(), 0.0001);

        // we expect 2 cars to be at freeflow somewhere on the link
        var vehicle4 = outCollection.get(3);
        assertEquals(50, vehicle4.getEasting(), 0.0001);
        assertEquals(1.0, vehicle4.getColorValueBetweenZeroAndOne(), 0.0001);
        var vehicle5 = outCollection.get(4);
        assertEquals(0, vehicle5.getEasting(), 0.0001);
        assertEquals(1.0, vehicle5.getColorValueBetweenZeroAndOne(), 0.0001);
    }

    @Test
    public void positionAgentsInActivities() {

        fail("Not yet implemented");
    }

    @Test
    public void positionVehiclesFromWaitingList() {

        final var config = ConfigUtils.createConfig();
        config.qsim().setSnapshotStyle(QSimConfigGroup.SnapshotStyle.queue);
        final var scenario = ScenarioUtils.createScenario(config);
        final var freespeed = 10.0;
        final var linkLength = 100.0;
        final var linkCapacity = 10;
        final var linkEnterTime = 0.0;
        final var now = 10.0;
        final var counter = 0;
        final var fromCoord = new Coord(0,0);
        final var toCoord = new Coord(linkLength, 0);

        // the test object
        var builder = new QueueAgentSnapshotInfoBuilder(scenario, new SnapshotLinkWidthCalculator());

        List<AgentSnapshotInfo> outCollection = new ArrayList<>();
        var link = NetworkUtils.createLink(
                Id.createLinkId(1),
                NetworkUtils.createNode(Id.createNodeId(1), fromCoord),
                NetworkUtils.createNode(Id.createNodeId(2), toCoord),
                NetworkUtils.createNetwork(),
                linkLength,
                freespeed,
                linkCapacity,
                1
        );
        Queue<QVehicle> waitingList = new LinkedList<>(createVehicles(link, 10, 10));

        // act
        var newCount = builder.positionVehiclesFromWaitingList(outCollection, link, counter, waitingList);

        // assert
        assertEquals(10, outCollection.size());

        // the positions should be at (0.9 * linkLength, -18.75 - 3.75 * waitingListIndex)
        // all should drive a car
        // agentId, vehicleId, linkId should all be 1
        var expectedNorthing = -18.75;
        for (AgentSnapshotInfo info : outCollection) {

            assertEquals(linkLength * 0.9, info.getEasting(), 0.000001);
            assertEquals(expectedNorthing, info.getNorthing(), 0.00001);
            assertEquals(AgentSnapshotInfo.AgentState.PERSON_DRIVING_CAR, info.getAgentState());
            assertEquals(Id.createPersonId(1), info.getId());
            assertEquals(Id.createLinkId(1), info.getLinkId());
            assertEquals(Id.createVehicleId(1), info.getVehicleId());

            expectedNorthing -= 3.75;
        }
    }

    private static class TestDriverAgent implements MobsimDriverAgent {

        private final Id<Person> personId;

        private TestDriverAgent(Id<Person> personId) {
            this.personId = personId;
        }


        @Override
        public Id<Link> chooseNextLinkId() {
            return null;
        }

        @Override
        public void notifyMoveOverNode(Id<Link> newLinkId) {

        }

        @Override
        public boolean isWantingToArriveOnCurrentLink() {
            return false;
        }

        @Override
        public Id<Person> getId() {
            return personId;
        }

        @Override
        public Id<Link> getCurrentLinkId() {
            return null;
        }

        @Override
        public Id<Link> getDestinationLinkId() {
            return null;
        }

        @Override
        public String getMode() {
            return TransportMode.car;
        }

        @Override
        public void setVehicle(MobsimVehicle veh) {

        }

        @Override
        public MobsimVehicle getVehicle() {
            return null;
        }

        @Override
        public Id<Vehicle> getPlannedVehicleId() {
            return null;
        }

        @Override
        public State getState() {
            return null;
        }

        @Override
        public double getActivityEndTime() {
            return 0;
        }

        @Override
        public void endActivityAndComputeNextState(double now) {

        }

        @Override
        public void endLegAndComputeNextState(double now) {

        }

        @Override
        public void setStateToAbort(double now) {

        }

        @Override
        public OptionalTime getExpectedTravelTime() {
            return null;
        }

        @Override
        public Double getExpectedTravelDistance() {
            return null;
        }

        @Override
        public void notifyArrivalOnLinkByNonNetworkMode(Id<Link> linkId) {

        }

        @Override
        public Facility getCurrentFacility() {
            return null;
        }

        @Override
        public Facility getDestinationFacility() {
            return null;
        }
    }

}