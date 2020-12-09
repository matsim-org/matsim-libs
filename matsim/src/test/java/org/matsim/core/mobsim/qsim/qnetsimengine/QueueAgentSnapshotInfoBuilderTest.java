package org.matsim.core.mobsim.qsim.qnetsimengine;

import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.OptionalTime;
import org.matsim.facilities.Facility;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vis.snapshotwriters.AgentSnapshotInfo;
import org.matsim.vis.snapshotwriters.SnapshotLinkWidthCalculator;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class QueueAgentSnapshotInfoBuilderTest {

    @Test
    public void positionVehiclesAlongLine_singleVehicleFreeFlow() {

        var setUp = new SimpleTestSetUp();
        List<AgentSnapshotInfo> outCollection = new ArrayList<>();
        var now = 1.0;
        var vehicles = createVehicles(setUp.link, 1, setUp.linkEnterTime + setUp.linkLength / setUp.freespeed);
        var builder = new QueueAgentSnapshotInfoBuilder(setUp.scenario, new SnapshotLinkWidthCalculator());

        // act
        builder.positionVehiclesAlongLine(
                outCollection,
                now,
                vehicles,
                setUp.linkLength,
                setUp.linkCapacity,
                setUp.fromCoord,
                setUp.toCoord,
                1 / setUp.linkCapacity, // this would mean the flow capacity is 100
                setUp.freespeed,
                1,
                new LinkedList<>()
        );

        // assert
        assertEquals(1, outCollection.size());
        AgentSnapshotInfo firstEntry = outCollection.iterator().next();

        assertEquals(setUp.linkLength / setUp.freespeed * now, firstEntry.getEasting(), 0.00001);
        assertEquals(-18.75, firstEntry.getNorthing(), 0.00001); // the calculator assumes an offset to the right of the driving direction ...

        var vehicle = vehicles.iterator().next();
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

        var setUp = new SimpleTestSetUp();
        List<AgentSnapshotInfo> outCollection = new ArrayList<>();
        var vehicles = createVehicles(setUp.link, 20, 0);
        var now = 100.0;
        var builder = new QueueAgentSnapshotInfoBuilder(setUp.scenario, new SnapshotLinkWidthCalculator());

        // act
        builder.positionVehiclesAlongLine(
                outCollection,
                now,
                vehicles,
                setUp.linkLength,
                setUp.linkCapacity,
                setUp.fromCoord,
                setUp.toCoord,
                1 / setUp.linkCapacity, // this would mean the flow capacity is 100
                setUp.freespeed,
                1,
                new LinkedList<>()
        );

        // assert
        assertEquals(vehicles.size(), outCollection.size());

        // we have put more cars onto the link than it can actually fit. The calculator should squeeze all vehicles onto the link now
        //outCollection.sort(Comparator.comparingDouble(AgentSnapshotInfo::getEasting));
        var expectedEasting = setUp.linkLength;
        var offsetBetweenVehicles = setUp.linkLength / outCollection.size();

        for (AgentSnapshotInfo info : outCollection) {
            assertEquals(expectedEasting, info.getEasting(), 0.0001);
            expectedEasting -= offsetBetweenVehicles;
        }
    }

    @Test
    public void positionVehiclesAlongLine_queueAtEnd() {

        var setUp = new SimpleTestSetUp();
        // use other vehicle list than in simple set up
        var vehicles = createVehicles(setUp.link, 5, 0);
        List<AgentSnapshotInfo> outCollection = new ArrayList<>();
        var builder = new QueueAgentSnapshotInfoBuilder(setUp.scenario, new SnapshotLinkWidthCalculator());

        // act
        builder.positionVehiclesAlongLine(
                outCollection,
                setUp.now,
                vehicles,
                setUp.linkLength,
                setUp.linkCapacity,
                setUp.fromCoord,
                setUp.toCoord,
                1 / setUp.linkCapacity,
                setUp.freespeed,
                1,
                new LinkedList<>()
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

        var setUp = new SimpleTestSetUp();
        List<AgentSnapshotInfo> outCollection = new ArrayList<>();
        var builder = new QueueAgentSnapshotInfoBuilder(setUp.scenario, new SnapshotLinkWidthCalculator());

        // act
        var newCount = builder.positionVehiclesFromWaitingList(outCollection, setUp.link, setUp.counter, setUp.waitingList);

        // assert
        assertEquals(setUp.waitingList.size(), outCollection.size());

        // the positions should be at (0.9 * linkLength, -18.75 - 3.75 * waitingListIndex)
        // all should drive a car
        // agentId, vehicleId, linkId should all be 1
        var expectedNorthing = -18.75;
        for (AgentSnapshotInfo info : outCollection) {

            assertEquals(setUp.linkLength * 0.9, info.getEasting(), 0.000001);
            assertEquals(expectedNorthing, info.getNorthing(), 0.00001);
            assertEquals(AgentSnapshotInfo.AgentState.PERSON_DRIVING_CAR, info.getAgentState());
            assertEquals(Id.createPersonId(1), info.getId());
            assertEquals(Id.createLinkId(1), info.getLinkId());
            assertEquals(Id.createVehicleId(1), info.getVehicleId());

            expectedNorthing -= 3.75;
        }
    }

    private static class SimpleTestSetUp {

        final Config config = ConfigUtils.createConfig();
        final Scenario scenario = ScenarioUtils.createScenario(config);
        final double freespeed = 10.0;
        final double linkLength = 100.0;
        final double linkCapacity = 10;
        final double linkEnterTime = 0.0;
        final double now = 10.0;
        final int counter = 0;
        final Coord fromCoord = new Coord(0, 0);
        final Coord toCoord = new Coord(linkLength, 0);
        Link link = NetworkUtils.createLink(
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

        SimpleTestSetUp() {
            config.qsim().setSnapshotStyle(QSimConfigGroup.SnapshotStyle.queue);
        }
    }

    /**
     * Mock up for MobsimDriverAgent
     */
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