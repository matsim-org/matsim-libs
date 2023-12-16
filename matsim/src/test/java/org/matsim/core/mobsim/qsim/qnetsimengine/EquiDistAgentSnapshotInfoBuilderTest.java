package org.matsim.core.mobsim.qsim.qnetsimengine;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.framework.PassengerAgent;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.pt.TransitDriverAgent;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.OptionalTime;
import org.matsim.facilities.Facility;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vis.snapshotwriters.AgentSnapshotInfo;
import org.matsim.vis.snapshotwriters.SnapshotLinkWidthCalculator;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class EquiDistAgentSnapshotInfoBuilderTest {

	@Test
	void positionVehiclesAlongLine_singleVehicleFreeFlow(){

        var setUp = new SimpleTestSetUp();
        List<AgentSnapshotInfo> outCollection = new ArrayList<>();
        var now = 1.0;
        var vehicles = createVehicles(setUp.link, 1, setUp.linkEnterTime + setUp.linkLength / setUp.freespeed);
        var builder = new EquiDistAgentSnapshotInfoBuilder(setUp.scenario, new SnapshotLinkWidthCalculator());

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

        // i guess this belong into the center of the link
        assertEquals(setUp.linkLength / 2, firstEntry.getEasting(), 0.00001);
    }

	@Test
	void positionVehiclesAlongLine_congestedAboveCapacityLimit() {

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
    private static class TestDriverAgent implements MobsimDriverAgent, MobsimAgent {

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

    private static class TestTransitDriverAgent extends TestDriverAgent implements TransitDriverAgent {

        private TestTransitDriverAgent(Id<Person> personId) {
            super(personId);
        }

        @Override
        public TransitStopFacility getNextTransitStop() {
            return null;
        }

        @Override
        public double handleTransitStop(TransitStopFacility stop, double now) {
            return 0;
        }

        @Override
        public String getMode() {
            return TransportMode.pt;
        }
    }

    private static class TestPassengerAgent extends TestDriverAgent implements PassengerAgent {

        private TestPassengerAgent(Id<Person> personId) {
            super(personId);
        }

        @Override
        public String getMode() {
            return TransportMode.pt;
        }
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

    private static Queue<QVehicle> createTransitVehicles(Link link, int size, double exitTimeOfFirstVehicle) {

        VehicleType type = VehicleUtils.createVehicleType(Id.create(TransportMode.pt, VehicleType.class));
        type.setMaximumVelocity(link.getFreespeed() + 10); // faster than link's freespeed
        type.getCapacity().setSeats(10);
        var exitTime = exitTimeOfFirstVehicle;

        Queue<QVehicle> result = new LinkedList<>();

        for (int i = 0; i < size; i++) {

            var vehicle = new QVehicleImpl(VehicleUtils.createVehicle(Id.createVehicleId(1), type));
            vehicle.setEarliestLinkExitTime(exitTime);
            vehicle.setDriver(new TestTransitDriverAgent(Id.createPersonId(1)));
            vehicle.setCurrentLink(link);

            for (int b = 0; b < type.getCapacity().getSeats(); b++) {
                vehicle.addPassenger(new TestPassengerAgent(Id.createPersonId(1)));
            }

            result.add(vehicle);
            exitTime += 5;
        }
        return result;
    }

    private static Collection<TestDriverAgent> createAgents(int size) {

        List<TestDriverAgent> result = new ArrayList<>();
        for (int i = 0; i < size; i++) {

            var driver = new TestDriverAgent(Id.createPersonId(1));
            result.add(driver);
        }
        return result;
    }

}